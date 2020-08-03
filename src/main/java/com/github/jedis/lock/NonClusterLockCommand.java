/*
 * Copyright 2019-2119 gao_xianglong@sina.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jedis.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Pool;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于non-cluster模式的相关lock命令,需要手动释放资源
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:05 下午
 */
public class NonClusterLockCommand implements LockCommand {
    private Pool pool;

    protected NonClusterLockCommand(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        Jedis jedis = null;
        try {
            jedis = (Jedis) pool.getResource();
            if (Objects.nonNull(jedis)) {
                return jedis.eval(script, keyCount, params);
            }
        } finally {
            if (Objects.nonNull(jedis)) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public String scriptLoad(String script) {
        Jedis jedis = null;
        try {
            jedis = (Jedis) pool.getResource();
            if (Objects.nonNull(jedis)) {
                return jedis.scriptLoad(script);
            }
        } finally {
            if (Objects.nonNull(jedis)) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public Object evalsha(String script, int keyCount, String... params) {
        Jedis jedis = null;
        try {
            jedis = (Jedis) pool.getResource();
            if (Objects.nonNull(jedis)) {
                return jedis.evalsha(script, keyCount, params);
            }
        } finally {
            if (Objects.nonNull(jedis)) {
                jedis.close();//手动释放资源
            }
        }
        return null;
    }

    @Override
    public void subscribe(Runnable callBack, JedisPubSub jedisPubSub, String... channels) {
        while (true) {
            try {
                Jedis jedis = null;
                try {
                    jedis = (Jedis) pool.getResource();
                    if (Objects.nonNull(jedis)) {
                        jedis.subscribe(jedisPubSub, channels);
                    }
                } finally {
                    if (Objects.nonNull(jedis)) {
                        jedis.close();
                    }
                }
            } catch (JedisConnectionException e) {
                callBack.run();
                try {
                    TimeUnit.SECONDS.sleep(1);//断线重连
                } catch (InterruptedException interruptedException) {
                    //...
                }
            }
        }
    }
}

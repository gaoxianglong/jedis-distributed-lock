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

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.TimeUnit;

/**
 * 基于cluster模式的相关lock命令
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:06 下午
 */
public class ClusterLockCommand implements LockCommand {
    private JedisCluster jedisCluster;

    protected ClusterLockCommand(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        return jedisCluster.eval(script, keyCount, params);
    }

    @Override
    public String scriptLoad(String script) {
        return jedisCluster.scriptLoad(script, script);
    }

    @Override
    public Object evalsha(String script, int keyCount, String... params) {
        return jedisCluster.evalsha(script, keyCount, params);
    }

    @Override
    public void subscribe(Runnable callBack, JedisPubSub jedisPubSub, String... channels) {
        while (true) {
            try {
                jedisCluster.subscribe(jedisPubSub, channels);
            } catch (Throwable e) {
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

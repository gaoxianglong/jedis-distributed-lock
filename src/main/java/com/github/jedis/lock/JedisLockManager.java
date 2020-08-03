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
import redis.clients.util.Pool;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式锁管理类
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:08 下午
 */
public class JedisLockManager {
    private Pool pool;
    private JedisCluster jedisCluster;
    private boolean isCluster;
    private Map<String, JedisLock> lockMap = new ConcurrentHashMap<>(32);

    public JedisLockManager(Pool pool) {
        this.pool = pool;
    }

    public JedisLockManager(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        isCluster = true;
    }

    /**
     * 获取分布式锁
     *
     * @param name
     * @return
     */
    public JedisLock getLock(String name) {
        Objects.requireNonNull(name);
        JedisLock lock = null;
        synchronized (this) {
            lock = lockMap.get(name);
            if (Objects.isNull(lock)) {
                LockCommand commands = null;
                if (isCluster) {
                    commands = new ClusterLockCommand(jedisCluster);
                } else {
                    commands = new NonClusterLockCommand(pool);
                }
                lock = new JedisReentrantLock(name, commands);
                lockMap.put(name, lock);
            }
        }
        return lock;
    }

    /**
     * 返回所有的分布式锁名称
     *
     * @return
     */
    public Set<String> getLocks() {
        return lockMap.keySet();
    }
}

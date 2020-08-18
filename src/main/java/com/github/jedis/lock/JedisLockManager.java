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

import java.util.*;
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
    private List<Pool> pools = null;
    private LockType lockType;
    private Map<String, JedisLock> lockMap = new ConcurrentHashMap<>(32);

    /**
     * 专用于红锁的构造函数
     *
     * @param pools
     */
    public JedisLockManager(List<Pool> pools) {
        this.pools = pools;
        lockType = LockType.RED;
    }

    /**
     * 专用于主备的构造函数
     *
     * @param pool
     */
    public JedisLockManager(Pool pool) {
        this.pool = pool;
        lockType = LockType.SINGLE;
    }

    /**
     * 专用于集群的构造函数
     *
     * @param jedisCluster
     */
    public JedisLockManager(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        lockType = LockType.CLUSTER;
    }

    /**
     * 锁类型
     */
    enum LockType {
        RED, CLUSTER, SINGLE;
    }

    /**
     * 获取分布式锁
     *
     * @param name
     * @return
     */
    public JedisLock getLock(String name) {
        Objects.requireNonNull(name);
        JedisLock result = null;
        synchronized (this) {
            result = lockMap.get(name);
            if (Objects.isNull(result)) {
                switch (lockType) {
                    case SINGLE:
                        result = new JedisReentrantLock(name, new NonClusterLockCommand(pool));
                        break;
                    case CLUSTER:
                        result = new JedisReentrantLock(name, new ClusterLockCommand(jedisCluster));
                        break;
                    case RED:
                        List<JedisLock> locks = new ArrayList<>();//每个redis节点对应一个重入锁
                        pools.forEach(pool -> locks.add(new JedisReentrantLock(name, new NonClusterLockCommand(pool))));
                        result = new JedisRedLock(locks);
                }
                lockMap.put(name, result);
            }
        }
        return result;
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

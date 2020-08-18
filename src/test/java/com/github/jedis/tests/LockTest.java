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
package com.github.jedis.tests;

import com.github.jedis.lock.JedisLock;
import com.github.jedis.lock.JedisLockManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.TimeUnit;

/**
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:18 下午
 */
public class LockTest {
    private volatile static JedisLock lock;

    @BeforeClass
    public static void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(10);
        config.setMaxIdle(50);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(1000);
        lock = new JedisLockManager(new JedisCluster(new HostAndPort("127.0.0.1", 6379),
                config)).getLock("mylock");
    }

    @Test
    public void lock() {
        try {
            //同步获取重入锁，当前线程如果获取锁资源失败则一直阻塞直至成功
            lock.lock();
            lock.lock();
            System.out.println("Get lock success...");
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void tryLock() {
        try {
            //同步获取重入锁，当前线程尝试获取锁资源，如果获取失败则快速失败
            Assert.assertTrue(lock.tryLock());
        } finally {
            lock.unlock();
        }
        try {
            //同步获取重入锁,当前线程尝试获取锁资源,如果在指定单位时间内都无法获取锁资源则返
            Assert.assertTrue(lock.tryLock(1, TimeUnit.SECONDS));
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void lockAsync() {
        try {
            //lock的异步方式
            lock.lockAsync().thenAccept(x -> System.out.println("Get lock success...")).get();
        } catch (Throwable e) {//...
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void trylockAsync() {
        try {
            //tryLock的异步方式
            lock.tryLockAsync().thenAccept(x -> System.out.println("Get lock success...")).get();
        } catch (Throwable e) {//...
        } finally {
            lock.unlock();
        }
        try {
            lock.tryLockAsync(1, TimeUnit.SECONDS).thenAccept(x -> System.out.println("Get lock success...")).get();
        } catch (Throwable e) {//...
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void forceUnlock() {
        try {
            lock.tryLock();
        } finally {
            lock.forceUnlock();//暴力释放锁
        }
    }
}

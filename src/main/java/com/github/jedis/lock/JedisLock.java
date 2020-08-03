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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 基于Jedis的分布式锁接口
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:04 下午
 */
public interface JedisLock {
    /**
     * 同步获取重入锁，当前线程如果获取锁资源失败则一直阻塞直至成功
     *
     * @return
     */
    void lock();

    /**
     * lock的异步方式
     *
     * @return
     */
    CompletableFuture<Void> lockAsync();

    /**
     * 同步获取重入锁，当前线程尝试获取锁资源，如果获取失败则快速失败
     *
     * @return
     */
    boolean tryLock();

    /**
     * tryLock的异步方式
     *
     * @return
     */
    CompletableFuture<Boolean> tryLockAsync();

    /**
     * 同步获取重入锁,当前线程尝试获取锁资源,如果在指定单位时间内都无法获取锁资源则返回
     *
     * @param time
     * @param unit
     * @return
     */
    boolean tryLock(long time, TimeUnit unit);

    /**
     * tryLock的异步方式
     *
     * @param time
     * @param unit
     * @return
     */
    CompletableFuture<Boolean> tryLockAsync(long time, TimeUnit unit);

    /**
     * 释放锁资源
     */
    void unlock();

    /**
     * unlock的异步方式
     */
    CompletableFuture<Void> unlockAsync();

    /**
     * 暴力释放锁资源
     */
    void forceUnlock();

    /**
     * forceUnlock的异步方式
     */
    CompletableFuture<Void> forceUnlockAsync();
}

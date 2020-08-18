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

import com.github.jedis.exceptions.JedisLockException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 合并锁,JedisRedLock超类
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/18 2:01 下午
 */
public abstract class JedisMultiLock implements JedisLock {
    protected List<JedisLock> locks;

    protected JedisMultiLock(List<JedisLock> locks) {
        this.locks = locks;
    }

    @Override
    public void lock() {
        Objects.requireNonNull(locks);
        long waitTime = locks.size() * 1500;//总最大等待时间
        while (true) {
            if (tryLock(waitTime, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
    }

    @Override
    public CompletableFuture<Void> lockAsync() {
        throw new JedisLockException("Features not supported");
    }

    @Override
    public boolean tryLock() {
        return tryLock(-1, TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<Boolean> tryLockAsync() {
        throw new JedisLockException("Features not supported");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        if (locks.size() < 3) {
            throw new JedisLockException("More than 3 redis nodes are required");
        }
        long beginTime = System.currentTimeMillis();//记录开始时间
        long remainTime = time != -1L ? unit.toMillis(time) : -1L;
        long lockTime = getLockWaitTime(remainTime);//获取每个子锁的超时时间
        AtomicInteger acquiredLocks = new AtomicInteger();//记录成功获取子锁的次数
        locks.stream().filter(lock -> Objects.nonNull(lock)).forEach(lock -> {
            boolean result;
            try {
                result = time == -1L ? lock.tryLock() : lock.tryLock(lockTime, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                result = false;
            }
            if (result) {
                acquiredLocks.incrementAndGet();
            }
        });
        //当出现子锁取锁失败时,N/2+1个节点成功则代表取锁成功
        if (acquiredLocks.get() >= (locks.size() - failedLocksLimit())) {
            //获取锁的使用时间
            long endTime = System.currentTimeMillis() - beginTime;
            if (remainTime != -1L) {
                if ((remainTime - endTime) <= 0L) {//锁使用时间<失效时间时，锁才算获取成功,排除tryLock
                    unlockInner(locks);//取锁超时后释放所有锁
                    return false;
                }
            }
            return true;
        } else {
            unlockInner(locks);//当失败次数达到阈值时，释放所有子锁
            return false;
        }
    }

    /**
     * 即使某些redis节点根本就没有加锁成功,
     * 但为了防止某些节点获取到锁但是客户端没有得到响应而导致接下来的一段时间不能被重新获取锁
     *
     * @param locks
     */
    protected abstract void unlockInner(List<JedisLock> locks);

    /**
     * 计算每个redis节点的拿锁时间
     *
     * @param remainTime
     * @return
     */
    protected abstract long getLockWaitTime(long remainTime);

    /**
     * 计算允许失败的次数
     *
     * @return
     */
    protected abstract int failedLocksLimit();

    @Override
    public CompletableFuture<Boolean> tryLockAsync(long time, TimeUnit unit) {
        throw new JedisLockException("Features not supported");
    }

    @Override
    public void unlock() {
        locks.forEach(lock -> {
            try {
                lock.unlock();
            } catch (JedisLockException e) {
                //...
            }
        });
    }

    @Override
    public CompletableFuture<Void> unlockAsync() {
        throw new JedisLockException("Features not supported");
    }

    @Override
    public void forceUnlock() {
        locks.forEach(lock -> {
            try {
                lock.forceUnlock();
            } catch (JedisLockException e) {
                //...
            }
        });
    }

    @Override
    public CompletableFuture<Void> forceUnlockAsync() {
        throw new JedisLockException("Features not supported");
    }
}

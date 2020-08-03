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
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * 分布式可重入锁实现
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:09 下午
 */
public class JedisReentrantLock implements JedisLock {
    private String name;
    private LockCommand client;
    private boolean isListener;
    /**
     * watchdog,定时更新当前锁的ttl
     */
    private Future<?> future;
    private String lockScript, unLockScript, forceUnLockScript, updateTTLScript;
    private ThreadLocal<String> threadLocal = new ThreadLocal<>();
    /**
     * 相关订阅者
     */
    private Set<Thread> subscribers = Collections.synchronizedSet(new HashSet<>());
    /**
     * 工作线程组，可回收缓存线程池，空闲线程允许进行回收
     */
    private Executor workerGroup = new ThreadPoolExecutor(10, 500, 2000, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadPoolExecutor.AbortPolicy());
    /**
     * watchdog监听线程组
     */
    private ScheduledExecutorService watchGroup = Executors.newScheduledThreadPool(10);

    protected JedisReentrantLock(String name, LockCommand client) {
        this.name = name;
        this.client = client;
    }

    @Override
    public void lock() {
        lock(acquireVisitorId());
    }

    private void lock(String visitorId) {
        Long ttl = acquireLock(visitorId);
        if (ttl == -1) {
            watchDog(visitorId); //添加watchdog
            return;
        }
        subscribe();
        try {
            while (true) {
                ttl = acquireLock(visitorId);
                if (ttl == -1) {
                    watchDog(visitorId);
                    break;
                }
                if (ttl >= 0) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(ttl));
                }
            }
        } finally {
            unsubscribe();
        }
    }

    @Override
    public CompletableFuture<Void> lockAsync() {
        String visitorId = acquireVisitorId();
        return CompletableFuture.runAsync(() -> lock(visitorId), workerGroup);
    }

    @Override
    public boolean tryLock() {
        return tryLock(acquireVisitorId());
    }

    private boolean tryLock(String visitorId) {
        boolean result = false;
        if (acquireLock(visitorId) == -1) {
            result = true;
        }
        return result;
    }

    @Override
    public CompletableFuture<Boolean> tryLockAsync() {
        String visitorId = acquireVisitorId();
        return CompletableFuture.supplyAsync(() -> tryLock(visitorId), workerGroup);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        return tryLock(acquireVisitorId(), time, unit);
    }

    private boolean tryLock(String visitorId, long time, TimeUnit unit) {
        Objects.requireNonNull(unit);
        if (time < 0) {
            throw new RuntimeException("Parameter time must be >= 0");
        }
        Long ttl = acquireLock(visitorId);
        if (ttl == -1) {
            watchDog(visitorId);
            return true;
        }
        subscribe();
        try {
            LockSupport.parkNanos(unit.toNanos(time));
            ttl = acquireLock(visitorId);
            if (ttl == -1) {
                watchDog(visitorId);
                return true;
            }
        } finally {
            unsubscribe();
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> tryLockAsync(long time, TimeUnit unit) {
        String visitorId = acquireVisitorId();
        return CompletableFuture.supplyAsync(() -> tryLock(visitorId, time, unit), workerGroup);
    }

    @Override
    public void unlock() {
        unlock(acquireVisitorId());
    }

    /**
     * 调用Lua脚本执行解锁原子操作
     *
     * @param visitorId
     */
    private void unlock(String visitorId) {
        synchronized (this) {
            if (Objects.isNull(unLockScript)) {
                //优先调用SCRIPT LOAD加载Lua脚本
                unLockScript = client.scriptLoad(Constants.ACQUIRE_UNLOCK_SCRIPT);
            }
        }
        Long result = null;
        try {
            result = (Long) client.evalsha(unLockScript, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL), visitorId);
        } catch (JedisNoScriptException e) {
            //当evalsha命令执行失败时,先执行eval缓存脚本
            result = (Long) client.eval(Constants.ACQUIRE_UNLOCK_SCRIPT, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL), visitorId);
        } finally {
            /**
             * 并发环境下,目标线程可能存在取锁成功但解锁失败的情况，为了避免目标线程多次取锁/解锁操作导致重入次数永远不为0,watchDog不退出导致其他线程取不到锁的情况,
             * 需要在解锁出现异常时重设当前线程的visitorId,自身和其他线程等待孤锁自然失效
             */
            if (Objects.isNull(result)) {
                threadLocal.remove();//reset visitorId
                if (Objects.nonNull(future)) {
                    future.cancel(true);//watchdog exit
                }
                return;
            }
            if (result == 1) {
                if (Objects.nonNull(future)) {
                    future.cancel(true);
                }
            } else if (result == 0) {
                throw new JedisLockException(String.format("attempt to unlock lock, not locked by " +
                        "current thread by visitor id: %s", acquireVisitorId()));
            }
        }
    }

    @Override
    public CompletableFuture<Void> unlockAsync() {
        String visitorId = acquireVisitorId();
        return CompletableFuture.runAsync(() -> unlock(visitorId), workerGroup);
    }

    @Override
    public void forceUnlock() {
        synchronized (this) {
            if (Objects.isNull(forceUnLockScript)) {
                //优先调用SCRIPT LOAD加载Lua脚本
                forceUnLockScript = client.scriptLoad(Constants.ACQUIRE_FORCE_UNLOCK_SCRIPT);
            }
        }
        Long result = null;
        try {
            result = (Long) client.evalsha(forceUnLockScript, 1, name);
        } catch (JedisNoScriptException e) {
            //当evalsha命令执行失败时,先执行eval缓存脚本
            result = (Long) client.eval(Constants.ACQUIRE_FORCE_UNLOCK_SCRIPT, 1, name);
        } catch (ClassCastException e) {
            //...
        } finally {
            if (result == 1) {
                if (Objects.nonNull(future)) {
                    future.cancel(true);//watchdog退出
                }
            }
        }
    }

    @Override
    public CompletableFuture<Void> forceUnlockAsync() {
        return CompletableFuture.runAsync(() -> forceUnlock(), workerGroup);
    }

    /**
     * 每隔10秒重设当前锁ttl
     *
     * @param visitorId
     */
    private void watchDog(String visitorId) {
        if (Objects.nonNull(future)) {
            future.cancel(true);//从队列中移除任务
        }
        future = watchGroup.scheduleAtFixedRate(() -> {
            if (Objects.isNull(updateTTLScript)) {
                updateTTLScript = client.scriptLoad(Constants.UPDATE_LOCK_TTL_SCRIPT);
            }
            try {
                client.evalsha(updateTTLScript, 1, name, visitorId,
                        String.valueOf(Constants.DEFAULT_KEY_TTL));
            } catch (JedisNoScriptException e) {
                //当evalsha命令执行失败时,先执行eval缓存脚本
                client.eval(Constants.UPDATE_LOCK_TTL_SCRIPT, 1, name, visitorId,
                        String.valueOf(Constants.DEFAULT_KEY_TTL));
            }
        }, Constants.DEFAULT_UPDATE_TIME, Constants.DEFAULT_UPDATE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 订阅目标通道,等待信号来临时唤醒当前线程继续拿锁
     */
    private void subscribe() {
        Thread thread = Thread.currentThread();
        synchronized (subscribers) {
            if (!isListener) {
                isListener = true;
                workerGroup.execute(() -> client.subscribe(() -> {
                    if (Objects.nonNull(future)) {
                        future.cancel(true);//断线重连时回调
                    }
                }, new SubscribeListener(subscribers), name));
            }
            subscribers.add(thread);
        }
    }

    /**
     * 取消订阅的目标线程
     */
    private void unsubscribe() {
        Thread thread = Thread.currentThread();
        synchronized (subscribers) {
            if (subscribers.contains(thread)) {
                subscribers.remove(thread);
            }
        }
    }

    /**
     * 获取访问者id
     *
     * @return
     */
    private String acquireVisitorId() {
        String visitorId = threadLocal.get();
        if (Objects.isNull(visitorId)) {
            //访问者标识采用uuid+threadId
            visitorId = String.format("%s:%s", UUID.randomUUID().toString(),
                    Thread.currentThread().getId());
            threadLocal.set(visitorId);
        }
        return visitorId;
    }

    /**
     * 调用Lua脚本获取分布式锁
     *
     * @param visitorId
     * @return
     */
    private Long acquireLock(String visitorId) {
        synchronized (this) {
            if (Objects.isNull(lockScript)) {
                //优先调用SCRIPT LOAD加载Lua脚本
                lockScript = client.scriptLoad(Constants.ACQUIRE_LOCK_SCRIPT);
            }
        }
        try {
            return (Long) client.evalsha(lockScript, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL));
        } catch (ClassCastException e) {
            return 100L;//jedis串消息,屏蔽此异常
        } catch (JedisNoScriptException e) {
            //当evalsha命令执行失败时,先执行eval缓存脚本
            return (Long) client.eval(Constants.ACQUIRE_LOCK_SCRIPT, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL));
        }
    }
}

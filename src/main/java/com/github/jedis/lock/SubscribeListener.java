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

import redis.clients.jedis.JedisPubSub;

import java.util.Set;
import java.util.concurrent.locks.LockSupport;

/**
 * 订阅者实现
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:07 下午
 */
public class SubscribeListener extends JedisPubSub {
    private Set<Thread> subscribers;

    protected SubscribeListener(Set<Thread> subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public void onMessage(String channel, String message) {
        synchronized (subscribers) {
            subscribers.parallelStream().forEach(x -> {
                LockSupport.unpark(x);//唤醒等待的业务线程
            });
        }
    }
}

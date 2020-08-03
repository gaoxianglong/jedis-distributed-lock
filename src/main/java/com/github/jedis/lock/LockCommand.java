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

/**
 * 封装实现分布式锁需要使用到的相关redis命令类
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:05 下午
 */
public interface LockCommand {
    Object eval(String script, int keyCount, String... params);

    String scriptLoad(String script);

    Object evalsha(String script, int keyCount, String... params);

    void subscribe(Runnable callBack, JedisPubSub jedisPubSub, String... channels);
}

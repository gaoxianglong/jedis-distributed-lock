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
package com.github.jedis.exceptions;

import redis.clients.jedis.exceptions.JedisException;

/**
 * 分布式锁异常基类
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:00 下午
 */
public class JedisLockException extends JedisException {
    private static final long serialVersionUID = -1249619682740569559L;

    public JedisLockException(String message) {
        super(message);
    }

    public JedisLockException(Throwable e) {
        super(e);
    }

    public JedisLockException(String message, Throwable cause) {
        super(message, cause);
    }
}

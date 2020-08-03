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

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于jedis分布式锁注解
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:08 下午
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /**
     * 分布式锁资源名称
     *
     * @return
     */
    String name();

    /**
     * 尝试获取锁资源的最大时间,time和unit参数仅针对trylock方式
     *
     * @return
     */
    long time() default 0L;

    /**
     * 时间单位
     *
     * @return
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * 锁类型,对应JedisLock
     *
     * @return
     */
    LockType type() default LockType.LOCK;

    /**
     * 锁类型,对应JedisLock
     */
    enum LockType {
        /**
         * 对应lock
         */
        LOCK,
        /**
         * 对应try_lock
         */
        TRY_LOCK,
    }
}

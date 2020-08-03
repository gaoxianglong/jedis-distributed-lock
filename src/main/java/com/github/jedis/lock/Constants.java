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

/**
 * 缺省静态常量相关类
 *
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:03 下午
 */
public class Constants {
    /**
     * 获取分布式锁脚本
     * 如果成功获取到锁资源返回-1,反之为pttl
     * KEY[1]:大Key,ARGV[1]:小Key,ARGV[2]:过期时间
     * <p>
     * Cluster模式下不支持多参数,否则会触发异常"CROSSSLOT Keys in request don't hash to the same slot"
     * <p>
     * CROSSSLOT Keys in request don't hash to the same slot
     */
    public static final String ACQUIRE_LOCK_SCRIPT = "if (redis.call('EXISTS', KEYS[1]) == 0) then " +
            "redis.call('HINCRBY', KEYS[1], ARGV[1], 1); " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "return -1; " +
            "end; " +
            "if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('HINCRBY', KEYS[1], ARGV[1], 1); " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "return -1; " +
            "end; " +
            "return redis.call('PTTL', KEYS[1]);";
    /**
     * 解锁脚本,完全解锁成功返回1,一次解锁成功返回2,解锁失败返回0
     */
    public static final String ACQUIRE_UNLOCK_SCRIPT = "if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('HINCRBY', KEYS[1], ARGV[1], -1); " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "if (tonumber(redis.call('HGET', KEYS[1], ARGV[1])) < 1) then " +
            "redis.call('DEL', KEYS[1]); " +
            "redis.call('PUBLISH', KEYS[1], 1); " +
            "return 1; " +
            "end; " +
            "return 2; " +
            "end; " +
            "return 0;";
    /**
     * 暴力解锁脚本,解锁成功返回1,反之返回0
     */
    public static final String ACQUIRE_FORCE_UNLOCK_SCRIPT = "if (redis.call('DEL', KEYS[1]) == 1) then " +
            "redis.call('PUBLISH', KEYS[1], 1); " +
            "return 1; " +
            "end; " +
            "return 0;";

    /**
     * 每隔10秒刷新当前锁ttl脚本
     */
    public static final String UPDATE_LOCK_TTL_SCRIPT = "if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "end;";
    /**
     * 缺省TTL为30000毫秒
     */
    public static final int DEFAULT_KEY_TTL = 0x7530;

    /**
     * watchdog的缺省更新时间为10,单位秒
     */
    public static int DEFAULT_UPDATE_TIME = 0xa;
}

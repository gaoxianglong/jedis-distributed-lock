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

import com.github.jedis.lock.DistributedLock;
import com.github.jedis.lock.JedisLockManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.TimeUnit;

/**
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/3 6:19 下午
 */
@SpringBootApplication
public class LockBySpring {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(LockBySpring.class, args);
        User user = (User) context.getBean("user");
        new Thread(() -> {
            user.testA();
        }).start();
        new Thread(() -> {
            user.testA();
        }).start();
    }

}

@Configuration
class SpringConfiguration {
    @Bean
    public JedisCluster jedisCluster() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(20);
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxWaitMillis(2000);
        return new JedisCluster(new HostAndPort("127.0.0.1", 6379), poolConfig);
    }

    @Bean
    public JedisLockManager jedisLockManager() {
        JedisLockManager jedisLockManager = new JedisLockManager(jedisCluster());
        return jedisLockManager;
    }
}

@Component()
class User {
    @DistributedLock(name = "mylock")
    public void testA() {
        testB();
    }

    @DistributedLock(name = "mylock")
    public void testB() {
        testC();
    }

    @DistributedLock(name = "mylock")
    public void testC() {
        try {
            TimeUnit.SECONDS.sleep(2);
            System.out.println(String.format("thread:%s get lock", Thread.currentThread().getName()));
        } catch (InterruptedException e) {
            //...
        }
    }
}

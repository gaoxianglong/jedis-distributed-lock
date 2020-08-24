# jedis-distributed-lock
架构设计文档：
[高容错性分布式锁架构设计方案 -01](https://xie.infoq.cn/article/4d571787a3280ef3094338f9b)
[高容错性分布式锁架构设计方案 -02](https://xie.infoq.cn/article/545a3accd173d6517ebd0ad59)

## single-lock
提供有JedisLockManager类，如果大家在程序中需要使用到lock，则可以使用如下方式创建JedisLock对象，如下所示：
```java
JedisLockManager manager = new JedisLockManager(cluster or poll);
JedisLock lock = manager.getLock("mylock");
```

JedisLockManager为重量级对象，全局仅需创建一次即可。当然，如果Redis的部署的是非集群环境（比如：Single、Master/Slave），JedisLockManager的入参还支持直接传递Pool。

假设成功获取到JedisLock对象后，分布式锁的相关操作API如下所示：
```Java
JedisLockManager manager = new JedisLockManager(cluster or poll);
//获取重入锁
JedisLock lock = manager.getLock("mylock");
lock.lock();//同步获取锁，拿不到锁则阻塞业务线程
lock.unlock();//同步释放锁

lock.tryLock();//尝试获取锁，拿不到锁则立即返回
lock.unlock();

lock.tryLock(2, TimeUnit.SECONDS);//尝试获取锁，带最长等待时间
lock.unlock();

lock.lockAsync().thenAccept(x -> System.out.println("get lock success")).get();//lock()的异步方式
lock.unlock();

lock.tryLockAsync().get();//tryLock()的异步方式
lock.unlock();

lock.tryLockAsync(2, TimeUnit.SECONDS).get();//tryLock()的异步方式
lock.unlock();

lock.forceUnlock();//暴力解锁，异步方式forceUnlockAsync()
```
### 基于springboot
API的整体使用非常简单，当然，如果你并不想直接使用API来使用分布式锁，而是希望基于springboot，那么还提供有@annotation的方式实现对lock的支持。
首先我们需要定义好config，如下所示：
```Java
@ComponentScan("com.github.jedis")
@Configuration
class SpringConfiguration {
    @Bean
    public JedisCluster jedisCluster() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        //TODO 省略数据源配置
        return new JedisCluster(new HostAndPort("127.0.0.1", 6379), poolConfig);
    }

    @Bean
    public JedisLockManager jedisLockManager() {
        JedisLockManager jedisLockManager = new JedisLockManager(jedisCluster());
        return jedisLockManager;
    }
}
```

基于注解的使用方式如下：
```Java
@DistributedLock(name = "mylock")
public void methodA() {}

@DistributedLock(name = "mylock", type = DistributedLock.LockType.TRY_LOCK)
public void methodB(String aa) {}

@DistributedLock(name = "mylock", type = DistributedLock.LockType.TRY_LOCK, time = 2, unit = TimeUnit.SECONDS)
public void methodC() {}
```

如果在程序中是通过使用注解的方式来使用分布式锁，则无需手动归还锁资源，目标方法执行结束后会自动释放。在此大家需要注意，当type为DistributedLock.LockType.TRY_LOCK时，如果当前线程没有获取到锁资源，则会抛出JedisLockException异常，业务上需要自行捕获并处理。

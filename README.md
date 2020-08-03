# jedis-distributed-lock
jedis分布式锁(可重入)

提供有JedisLockManager类，如果大家在程序中需要使用到分布式锁，那么则可以使用如下方式创建JedisLock对象，如下所示：
JedisLockManager manager = new JedisLockManager(cluster or poll);
JedisLock lock = manager.getLock("mylock");

JedisLockManager为重量级对象，全局仅需创建一次即可。当然，如果使用的是非集群环境（比如：单机、主备），JedisLockManager的入参还支持传递Pool。

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


API的整体使用非常简单，当然，如果你并不想直接使用JedisY API来使用分布式锁，而是基于springboot，jedisY还提供有注解的方式实现对分布式锁的支持。
首先我们需要定义好config，如下所示：
```Java
@Bean
public JedisLockManager jedisLockManager() {
    JedisLockManager jedisLockManager = new JedisLockManager(jedisCluster());
    return jedisLockManager;
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

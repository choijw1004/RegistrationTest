package com.course.registration.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisConnectivityTest {

    @Autowired
    private RedissonClient redisson;

    @Test
    void testRedisSetGet() {
        assertNotNull(redisson, "RedissonClient 빈이 주입되지 않았습니다");
        RBucket<String> bucket = redisson.getBucket("test:key");
        bucket.set("hello", 5, TimeUnit.SECONDS);
        assertEquals("hello", bucket.get(), "Redis에 set/get이 정상 동작하지 않습니다");
    }

    @Test
    void testRedisLock() throws InterruptedException {
        RLock lock = redisson.getLock("test:lock");
        // 최대 3초간 락 유지
        lock.lock(3, TimeUnit.SECONDS);
        try {
            assertTrue(lock.isHeldByCurrentThread(), "락을 획득하지 못했습니다");
        } finally {
            lock.unlock();
        }
    }
}

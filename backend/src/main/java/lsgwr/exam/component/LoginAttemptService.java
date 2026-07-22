/***********************************************************
 * @Description : A-10 修复：登录尝试限制器，防止暴力破解
 *               基于内存的滑动窗口计数器，不依赖 Redis
 ***********************************************************/
package lsgwr.exam.component;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginAttemptService {
    /** 最大尝试次数 */
    private static final int MAX_ATTEMPTS = 5;
    /** 锁定时长（毫秒），默认 15 分钟 */
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

    private static class AttemptRecord {
        AtomicInteger count = new AtomicInteger(0);
        volatile long lockUntil = 0;
    }

    private final Map<String, AttemptRecord> attemptMap = new ConcurrentHashMap<>();

    /**
     * 检查是否被锁定
     */
    public boolean isLocked(String key) {
        AttemptRecord record = attemptMap.get(key);
        if (record == null) {
            return false;
        }
        if (record.lockUntil > 0 && System.currentTimeMillis() < record.lockUntil) {
            return true;
        }
        // 锁定已过期，重置
        if (record.lockUntil > 0 && System.currentTimeMillis() >= record.lockUntil) {
            attemptMap.remove(key);
        }
        return false;
    }

    /**
     * 记录一次失败尝试
     */
    public void recordFailure(String key) {
        AttemptRecord record = attemptMap.computeIfAbsent(key, k -> new AttemptRecord());
        int count = record.count.incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            record.lockUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
        }
    }

    /**
     * 登录成功时重置计数
     */
    public void recordSuccess(String key) {
        attemptMap.remove(key);
    }

    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String key) {
        AttemptRecord record = attemptMap.get(key);
        if (record == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - record.count.get());
    }
}

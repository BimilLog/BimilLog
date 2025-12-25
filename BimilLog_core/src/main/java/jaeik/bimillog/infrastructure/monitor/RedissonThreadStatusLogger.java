package jaeik.bimillog.infrastructure.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.EnumMap;
import java.util.Map;

@Component
@Slf4j
public class RedissonThreadStatusLogger {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Scheduled(fixedDelay = 10000L)
    public void logRedissonThreadStatus() {
        Map<Thread.State, Integer> states = countThreadStatesByPrefix("redisson");
        int total = states.values().stream().mapToInt(Integer::intValue).sum();
        int runnable = states.getOrDefault(Thread.State.RUNNABLE, 0);
        int blocked = states.getOrDefault(Thread.State.BLOCKED, 0);
        int waiting = states.getOrDefault(Thread.State.WAITING, 0);
        int timedWaiting = states.getOrDefault(Thread.State.TIMED_WAITING, 0);
        log.info("Redisson 스레드 상태: total={}, runnable={}, blocked={}, waiting={}, timed_waiting={}", total, runnable, blocked, waiting, timedWaiting);
    }

    private Map<Thread.State, Integer> countThreadStatesByPrefix(String prefix) {
        long[] ids = threadMXBean.getAllThreadIds();
        ThreadInfo[] infos = threadMXBean.getThreadInfo(ids);
        Map<Thread.State, Integer> counts = new EnumMap<>(Thread.State.class);
        if (infos == null) {
            return counts;
        }
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            String name = info.getThreadName();
            if (name == null || !name.startsWith(prefix)) {
                continue;
            }
            Thread.State state = info.getThreadState();
            counts.merge(state, 1, Integer::sum);
        }
        return counts;
    }
}

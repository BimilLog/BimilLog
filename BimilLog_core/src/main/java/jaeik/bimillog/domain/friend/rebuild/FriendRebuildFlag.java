package jaeik.bimillog.domain.friend.rebuild;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>친구 Redis 재구축 플래그</h2>
 * <p>재구축 중에는 리스너의 Redis 쓰기를 DLQ로 우회시키고,
 * DLQ 스케줄러의 실행을 차단합니다.</p>
 * <p>AtomicInteger 카운터로 친구관계/상호작용 동시 재구축을 안전하게 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@Slf4j
public class FriendRebuildFlag {
    private final AtomicInteger rebuildCount = new AtomicInteger(0);

    public void startRebuilding() {
        int count = rebuildCount.incrementAndGet();
        log.info("[리빌드 플래그] 재구축 시작, 활성 재구축 수: {}", count);
    }

    public void stopRebuilding() {
        int count = rebuildCount.decrementAndGet();
        log.info("[리빌드 플래그] 재구축 종료, 활성 재구축 수: {}", count);
    }

    public boolean isRebuilding() {
        return rebuildCount.get() > 0;
    }
}

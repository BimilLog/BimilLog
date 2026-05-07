package jaeik.bimillog.domain.friend.rebuild;

import jaeik.bimillog.domain.friend.repository.FriendAdminQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendRestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>Redis 재구축 템플릿</h2>
 * <p>친구관계/상호작용 점수 재구축의 공통 흐름을 정의합니다.</p>
 * <ol>
 *   <li>재구축 플래그 ON</li>
 *   <li>도메인별 Redis 키 정리 ({@link #cleanupRedis()})</li>
 *   <li>전체 memberId 적재한 큐 생성</li>
 *   <li>도메인별 프로듀서/컨슈머 실행 ({@link #executeRebuild(LinkedBlockingQueue)})</li>
 * </ol>
 * <p>플래그 해제({@link #stopRebuilding()})는 비동기 컨슈머 완료 시 자식에서 호출합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
@RequiredArgsConstructor
public abstract class RebuildTemplate {
    private final RedisFriendRestore redisFriendRestore;
    private final FriendAdminQueryRepository friendAdminQueryRepository;
    private final AtomicInteger rebuildCount = new AtomicInteger(0);

    public final void rebuild() {
        startRebuilding();
        cleanupRedis(redisFriendRestore);

        List<Long> allIds = friendAdminQueryRepository.getMemberId();
        LinkedBlockingQueue<Long> memberQueue = new LinkedBlockingQueue<>(allIds);

        executeRebuild(memberQueue);
    }

    protected abstract void cleanupRedis(RedisFriendRestore redisFriendRestore);

    protected abstract void executeRebuild(LinkedBlockingQueue<Long> memberQueue);

    public boolean isRebuilding() {
        return rebuildCount.get() > 0;
    }

    private void startRebuilding() {
        int count = rebuildCount.incrementAndGet();
        log.info("[리빌드 플래그] 재구축 시작, 활성 재구축 수: {}", count);
    }

    protected void stopRebuilding() {
        int count = rebuildCount.decrementAndGet();
        log.info("[리빌드 플래그] 재구축 종료, 활성 재구축 수: {}", count);
    }
}

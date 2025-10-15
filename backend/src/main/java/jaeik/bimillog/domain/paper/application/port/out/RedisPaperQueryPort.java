package jaeik.bimillog.domain.paper.application.port.out;

import java.util.List;

/**
 * <h2>롤링페이퍼 캐시 조회 포트</h2>
 * <p>Paper 도메인의 Redis 캐시 데이터 조회 작업을 담당하는 포트입니다.</p>
 * <p>실시간 인기 롤링페이퍼 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPaperQueryPort {

    /**
     * <h3>실시간 인기 롤링페이퍼 memberId 목록 조회</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 10개의 회원 ID를 조회합니다.</p>
     * <p>PaperQueryService에서 실시간 인기 롤링페이퍼 목록 요청 시 호출됩니다.</p>
     * <p>롤링페이퍼는 회원당 1개만 존재하므로 memberId를 반환합니다.</p>
     *
     * @return List&lt;Long&gt; 상위 10개 회원 ID 목록 (점수 내림차순)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> getRealtimePopularMemberIds();

}

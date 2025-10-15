package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;

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
     * <h3>실시간 인기 롤링페이퍼 조회 (Rank, Score 포함)</h3>
     * <p>Redis Sorted Set에서 점수가 높은 상위 10개의 롤링페이퍼 정보를 조회합니다.</p>
     * <p>memberId, rank, popularityScore가 채워진 PopularPaperInfo 리스트를 반환합니다.</p>
     * <p>PaperCacheService에서 실시간 인기 롤링페이퍼 목록 조회 시 호출됩니다.</p>
     *
     * @return List&lt;PopularPaperInfo&gt; 상위 10개 롤링페이퍼 정보 (memberId, rank, popularityScore만 설정됨)
     * @author Jaeik
     * @since 2.0.0
     */
    List<PopularPaperInfo> getRealtimePopularPapersWithRankAndScore();

}

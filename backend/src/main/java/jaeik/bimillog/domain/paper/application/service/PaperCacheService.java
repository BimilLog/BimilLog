package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperCacheUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.RedisPaperQueryPort;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static jaeik.bimillog.infrastructure.adapter.out.redis.paper.RedisPaperKeys.REALTIME_PAPER_SCORE_KEY;

/**
 * <h2>롤링페이퍼 캐시 서비스</h2>
 * <p>롤링페이퍼 캐시 관리 관련 UseCase 인터페이스의 구현체로서 캐시 조회 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간 인기 롤링페이퍼 조회</p>
 * <p>Redis Sorted Set에서 점수 기반 순위 조회 후 DB 데이터를 결합하여 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperCacheService implements PaperCacheUseCase{

    private final RedisPaperQueryPort redisPaperQueryPort;
    private final PaperQueryPort paperQueryPort;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * <h3>실시간 인기 롤링페이퍼 조회</h3>
     * <p>Redis Sorted Set에서 점수 기반 순위를 조회하고 DB에서 추가 정보를 보강하여 반환합니다.</p>
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기)
     * @return Page<PopularPaperInfo> 실시간 인기 롤링페이퍼 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PopularPaperInfo> getRealtimePapers(Pageable pageable) {
        // 1. Pageable에서 페이징 정보 추출
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int start = page * size;
        int end = start + size - 1;

        // 2. Redis Sorted Set 전체 크기 조회 (total)
        Long total = redisTemplate.opsForZSet().zCard(REALTIME_PAPER_SCORE_KEY);
        if (total == null || total == 0) {
            return Page.empty(pageable);
        }

        // 3. Redis에서 지정된 범위로 조회 (memberId, rank, popularityScore)
        List<PopularPaperInfo> popularPapers = redisPaperQueryPort
                .getRealtimePopularPapersWithRankAndScore(start, end);

        if (popularPapers.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. DB에서 memberName과 24시간 이내 메시지 수 조회 후 채우기
        paperQueryPort.enrichPopularPaperInfos(popularPapers);

        // 5. 페이징 정보와 함께 Page로 변환하여 반환
        return new PageImpl<>(popularPapers, pageable, total);
    }
}

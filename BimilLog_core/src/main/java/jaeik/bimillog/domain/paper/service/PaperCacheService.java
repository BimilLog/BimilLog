package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.repository.PaperQueryRepository;
import jaeik.bimillog.domain.paper.adapter.PaperToMemberAdapter;
import jaeik.bimillog.infrastructure.log.CacheMetricsLogger;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperQueryAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jaeik.bimillog.infrastructure.redis.RedisKey.REALTIME_PAPER_SCORE_KEY;

/**
 * <h2>롤링페이퍼 캐시 서비스</h2>
 * <p>롤링페이퍼 캐시 관리 관련 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간 인기 롤링페이퍼 조회</p>
 * <p>Redis Sorted Set에서 점수 기반 순위 조회 후 DB 데이터를 결합하여 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperCacheService {
    private final RedisPaperQueryAdapter redisPaperQueryAdapter;
    private final PaperQueryRepository paperQueryRepository;
    private final PaperToMemberAdapter paperToMemberAdapter;
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
    public Page<PopularPaperInfo> getRealtimePapers(Pageable pageable) {
        // 1. Pageable에서 페이징 정보 추출
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int start = page * size;
        int end = start + size - 1;

        // 2. Redis Sorted Set 전체 크기 조회 (total)
        Long total = redisTemplate.opsForZSet().zCard(REALTIME_PAPER_SCORE_KEY);
        if (total == null || total == 0) {
            CacheMetricsLogger.miss(log, "paper:realtime:zcard", REALTIME_PAPER_SCORE_KEY, "zcard_empty");
            return Page.empty(pageable);
        }
        CacheMetricsLogger.hit(log, "paper:realtime:zcard", REALTIME_PAPER_SCORE_KEY, total);

        // 3. Redis에서 지정된 범위로 조회 (memberId, rank, popularityScore)
        List<PopularPaperInfo> popularPapers = redisPaperQueryAdapter
                .getRealtimePopularPapersWithRankAndScore(start, end);

        if (popularPapers.isEmpty()) {
            CacheMetricsLogger.miss(log, "paper:realtime:payload", REALTIME_PAPER_SCORE_KEY, "tuples_empty");
            return Page.empty(pageable);
        }
        CacheMetricsLogger.hit(log, "paper:realtime:payload", REALTIME_PAPER_SCORE_KEY, popularPapers.size());

        // 4. memberIds 추출 및 memberName 주입
        List<Long> memberIds = popularPapers.stream()
                .map(PopularPaperInfo::getMemberId)
                .collect(Collectors.toList());

        Map<Long, String> memberNameMap = paperToMemberAdapter.findMemberNamesByIds(memberIds);

        popularPapers.forEach(info ->
                info.setMemberName(memberNameMap.getOrDefault(info.getMemberId(), ""))
        );

        // 5. DB에서 24시간 이내 메시지 수 조회 후 채우기
        paperQueryRepository.enrichPopularPaperInfos(popularPapers);

        // 6. 페이징 정보와 함께 Page로 변환하여 반환
        return new PageImpl<>(popularPapers, pageable, total);
    }
}

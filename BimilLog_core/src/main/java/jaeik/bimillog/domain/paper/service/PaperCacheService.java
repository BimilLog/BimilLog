package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.repository.PaperQueryRepository;
import jaeik.bimillog.domain.paper.adapter.PaperToMemberAdapter;
import jaeik.bimillog.domain.global.dto.CursorPageResponse;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperQueryAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>롤링페이퍼 캐시 서비스</h2>
 * <p>롤링페이퍼 캐시 관리 관련 비즈니스 로직을 오케스트레이션합니다.</p>
 * <p>실시간 인기 롤링페이퍼 조회</p>
 * <p>Redis Sorted Set에서 점수 기반 순위 조회 후 DB 데이터를 결합하여 제공</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Log(logResult = false, logExecutionTime = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperCacheService {
    private final RedisPaperQueryAdapter redisPaperQueryAdapter;
    private final PaperQueryRepository paperQueryRepository;
    private final PaperToMemberAdapter paperToMemberAdapter;

    /**
     * <h3>실시간 인기 롤링페이퍼 조회 (커서 기반)</h3>
     * <p>Redis Sorted Set에서 점수 기반 순위를 조회하고 DB에서 추가 정보를 보강하여 반환합니다.</p>
     * <p>커서는 마지막으로 조회한 rank(순위)이며, null이면 처음부터 조회합니다.</p>
     *
     * @param cursor 마지막으로 조회한 순위 (null이면 처음부터)
     * @param size 조회할 개수
     * @return CursorPageResponse 커서 기반 페이지 응답
     */
    public CursorPageResponse<PopularPaperInfo> getRealtimePapers(Long cursor, int size) {
        // 1. 커서에서 조회 범위 계산
        int start = (cursor == null) ? 0 : cursor.intValue();
        int end = start + size; // size + 1개 조회하여 다음 페이지 존재 여부 판단

        // 2. Redis에서 지정된 범위로 조회 (memberId, rank, popularityScore)
        List<PopularPaperInfo> popularPapers = redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(start, end);

        if (popularPapers.isEmpty()) {
            return CursorPageResponse.of(Collections.emptyList(), null);
        }

        // 3. 다음 페이지 존재 여부 판단
        boolean hasNext = popularPapers.size() > size;
        if (hasNext) {
            popularPapers = popularPapers.subList(0, size);
        }

        // 4. memberIds 추출 및 memberName 주입
        List<Long> memberIds = popularPapers.stream()
                .map(PopularPaperInfo::getMemberId)
                .collect(Collectors.toList());

        Map<Long, String> memberNameMap = paperToMemberAdapter.findMemberNamesByIds(memberIds);

        popularPapers.forEach(info ->
                info.setMemberName(memberNameMap.getOrDefault(info.getMemberId(), ""))
        );

        // 5. DB에서 24시간 이내 메시지 수 조회 후 채우기
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        Map<Long, Integer> messageCountMap = paperQueryRepository.enrichPopularPaperInfos(memberIds, twentyFourHoursAgo);
        popularPapers.forEach(info ->
                info.setRecentMessageCount(messageCountMap.getOrDefault(info.getMemberId(), 0))
        );

        // 6. 다음 커서 계산 (마지막 항목의 rank)
        Long nextCursor = hasNext ? (long) popularPapers.getLast().getRank() : null;
        return CursorPageResponse.of(popularPapers, nextCursor);
    }
}

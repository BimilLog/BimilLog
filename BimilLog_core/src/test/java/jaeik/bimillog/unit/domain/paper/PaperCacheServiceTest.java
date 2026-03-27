package jaeik.bimillog.unit.domain.paper;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.repository.PaperQueryRepository;
import jaeik.bimillog.domain.paper.adapter.PaperToMemberAdapter;
import jaeik.bimillog.domain.paper.service.PaperCacheService;
import jaeik.bimillog.domain.global.dto.CursorPageResponse;
import jaeik.bimillog.infrastructure.redis.paper.RedisPaperQueryAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PaperCacheService 단위 테스트</h2>
 * <p>실시간 인기 롤링페이퍼 커서 기반 조회 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 */
@DisplayName("PaperCacheService 테스트")
@Tag("unit")
class PaperCacheServiceTest extends BaseUnitTest {

    @Mock
    private RedisPaperQueryAdapter redisPaperQueryAdapter;

    @Mock
    private PaperQueryRepository paperQueryRepository;

    @Mock
    private PaperToMemberAdapter paperToMemberAdapter;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private PaperCacheService paperCacheService;

    @Test
    @DisplayName("커서 null - Redis 조회 결과가 비어있으면 빈 응답 반환")
    void shouldReturnEmpty_WhenRedisResultEmpty() {
        // Given
        given(redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 10))
                .willReturn(Collections.emptyList());

        // When
        CursorPageResponse<PopularPaperInfo> result = paperCacheService.getRealtimePapers(null, 10);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        verify(paperToMemberAdapter, never()).findMemberNamesByIds(anyList());
    }

    @Test
    @DisplayName("정상 조회 - memberName과 recentMessageCount가 채워짐")
    void shouldEnrichPopularPapers_WhenDataExists() {
        // Given
        List<PopularPaperInfo> papers = new ArrayList<>();
        PopularPaperInfo info1 = createPaperInfo(1L, 1, 100.0);
        PopularPaperInfo info2 = createPaperInfo(2L, 2, 80.0);
        papers.add(info1);
        papers.add(info2);

        given(redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 10))
                .willReturn(papers);
        given(paperToMemberAdapter.findMemberNamesByIds(List.of(1L, 2L)))
                .willReturn(Map.of(1L, "user1", 2L, "user2"));
        given(paperQueryRepository.enrichPopularPaperInfos(eq(List.of(1L, 2L)), any(Instant.class)))
                .willReturn(Map.of(1L, 5, 2L, 3));

        // When
        CursorPageResponse<PopularPaperInfo> result = paperCacheService.getRealtimePapers(null, 10);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.nextCursor()).isNull(); // 2개 < 10이므로 다음 페이지 없음

        assertThat(result.content().get(0).getMemberName()).isEqualTo("user1");
        assertThat(result.content().get(0).getRecentMessageCount()).isEqualTo(5);

        assertThat(result.content().get(1).getMemberName()).isEqualTo("user2");
        assertThat(result.content().get(1).getRecentMessageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("다음 페이지 존재 - nextCursor가 마지막 항목의 rank")
    void shouldReturnNextCursor_WhenHasNextPage() {
        // Given: size=2인데 3개 반환 → 다음 페이지 있음
        List<PopularPaperInfo> papers = new ArrayList<>();
        papers.add(createPaperInfo(1L, 1, 100.0));
        papers.add(createPaperInfo(2L, 2, 80.0));
        papers.add(createPaperInfo(3L, 3, 60.0));

        given(redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 2))
                .willReturn(papers);
        given(paperToMemberAdapter.findMemberNamesByIds(List.of(1L, 2L)))
                .willReturn(Map.of(1L, "user1", 2L, "user2"));
        given(paperQueryRepository.enrichPopularPaperInfos(eq(List.of(1L, 2L)), any(Instant.class)))
                .willReturn(Collections.emptyMap());

        // When
        CursorPageResponse<PopularPaperInfo> result = paperCacheService.getRealtimePapers(null, 2);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(2L); // 마지막 항목 rank=2
    }

    @Test
    @DisplayName("커서 지정 - 해당 rank부터 조회")
    void shouldStartFromCursor_WhenCursorProvided() {
        // Given: cursor=5, size=3 → start=5, end=8
        given(redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(5, 8))
                .willReturn(Collections.emptyList());

        // When
        CursorPageResponse<PopularPaperInfo> result = paperCacheService.getRealtimePapers(5L, 3);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        verify(redisPaperQueryAdapter).getRealtimePopularPapersWithRankAndScore(5, 8);
    }

    @Test
    @DisplayName("메시지 수 없는 회원은 0으로 설정")
    void shouldSetZeroMessageCount_WhenNoRecentMessages() {
        // Given
        List<PopularPaperInfo> papers = new ArrayList<>();
        papers.add(createPaperInfo(1L, 1, 100.0));

        given(redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 10))
                .willReturn(papers);
        given(paperToMemberAdapter.findMemberNamesByIds(List.of(1L)))
                .willReturn(Map.of(1L, "user1"));
        given(paperQueryRepository.enrichPopularPaperInfos(eq(List.of(1L)), any(Instant.class)))
                .willReturn(Collections.emptyMap());

        // When
        CursorPageResponse<PopularPaperInfo> result = paperCacheService.getRealtimePapers(null, 10);

        // Then
        assertThat(result.content().get(0).getRecentMessageCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("memberName 없는 회원은 빈 문자열")
    void shouldSetEmptyMemberName_WhenMemberNotFound() {
        // Given
        List<PopularPaperInfo> papers = new ArrayList<>();
        papers.add(createPaperInfo(999L, 1, 50.0));

        given(redisPaperQueryAdapter.getRealtimePopularPapersWithRankAndScore(0, 10))
                .willReturn(papers);
        given(paperToMemberAdapter.findMemberNamesByIds(List.of(999L)))
                .willReturn(Collections.emptyMap());
        given(paperQueryRepository.enrichPopularPaperInfos(eq(List.of(999L)), any(Instant.class)))
                .willReturn(Collections.emptyMap());

        // When
        CursorPageResponse<PopularPaperInfo> result = paperCacheService.getRealtimePapers(null, 10);

        // Then
        assertThat(result.content().get(0).getMemberName()).isEmpty();
    }

    private PopularPaperInfo createPaperInfo(Long memberId, int rank, double score) {
        PopularPaperInfo info = new PopularPaperInfo();
        info.setMemberId(memberId);
        info.setRank(rank);
        info.setPopularityScore(score);
        return info;
    }
}

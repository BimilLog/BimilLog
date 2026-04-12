package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.RealtimeScoreFallbackStore;
import jaeik.bimillog.domain.post.service.RealtimePostCacheService;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.redis.post.RedisPostRealTimeAdapter;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>RealtimePostCacheService 테스트</h2>
 * <p>ZSet → DB PK 조회 기반 실시간 인기글 조회 로직을 검증합니다.</p>
 * <p>서킷브레이커 폴백은 @CircuitBreaker AOP로 동작하므로 단위 테스트에서는 정상 경로만 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealtimePostCacheService 테스트")
@Tag("unit")
class RealtimePostPopularServiceTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private RedisPostRealTimeAdapter redisPostRealTimeAdapter;

    @Mock
    private RealtimeScoreFallbackStore realtimeScoreFallbackStore;

    @Mock
    private PostUtil postUtil;

    @InjectMocks
    private RealtimePostCacheService realtimePostCacheService;

    @Test
    @DisplayName("ZSet 비어있음 → 빈 페이지 반환")
    void shouldReturnEmptyPage_WhenZSetIsEmpty() {
        // Given
        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(List.of());

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(postQueryRepository, never()).findByIdsFetchMember(any());
    }

    @Test
    @DisplayName("ZSet에 ID 있음 → DB PK 조회 후 ZSet 순서로 반환")
    void shouldReturnDbPosts_InZSetOrder() {
        // Given
        List<Long> zsetIds = List.of(2L, 1L);
        List<PostSimpleDetail> dbPosts = List.of(
                PostTestDataBuilder.createPostSearchResult(1L, "인기글1"),
                PostTestDataBuilder.createPostSearchResult(2L, "인기글2")
        );

        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(zsetIds);
        given(postQueryRepository.findByIdsFetchMember(zsetIds)).willReturn(dbPosts);
        given(postUtil.paginate(any(), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(inv.getArgument(0)));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).hasSize(2);
        // ZSet 순서(2L → 1L)로 정렬되어야 함
        assertThat(result.getContent().get(0).getId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("DB에서 일부 게시글 없음 → 존재하는 것만 반환")
    void shouldReturnOnlyExistingPosts_WhenSomeNotFoundInDb() {
        // Given
        List<Long> zsetIds = List.of(1L, 2L, 3L);
        List<PostSimpleDetail> dbPosts = List.of(
                PostTestDataBuilder.createPostSearchResult(1L, "글1"),
                PostTestDataBuilder.createPostSearchResult(3L, "글3")
                // 2L은 DB에 없음 (삭제된 글 등)
        );

        given(redisPostRealTimeAdapter.getRangePostId()).willReturn(zsetIds);
        given(postQueryRepository.findByIdsFetchMember(zsetIds)).willReturn(dbPosts);
        given(postUtil.paginate(any(), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(inv.getArgument(0)));

        // When
        Page<PostSimpleDetail> result = realtimePostCacheService.getRealtimePosts();

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(3L);
    }
}

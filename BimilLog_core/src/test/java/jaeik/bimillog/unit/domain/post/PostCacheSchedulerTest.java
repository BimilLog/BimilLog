package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.scheduler.FeaturedPostScheduler;
import jaeik.bimillog.domain.post.scheduler.PostCacheScheduler;
import jaeik.bimillog.infrastructure.redis.RedisKey;
import jaeik.bimillog.infrastructure.redis.post.RedisPostListUpdateAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCacheScheduler 테스트</h2>
 * <p>게시글 캐시 동기화 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지/첫 페이지 캐시 갱신 흐름을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCacheScheduler 테스트")
@Tag("unit")
class PostCacheSchedulerTest {

    @Mock
    private RedisPostListUpdateAdapter redisPostListUpdateAdapter;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private FeaturedPostScheduler featuredPostScheduler;

    private PostCacheScheduler postCacheScheduler;

    @BeforeEach
    void setUp() {
        postCacheScheduler = new PostCacheScheduler(
                redisPostListUpdateAdapter,
                postQueryRepository,
                postRepository,
                featuredPostScheduler
        );
    }

    // ==================== 공지사항 ====================

    @Test
    @DisplayName("공지사항 캐시 갱신 - 성공 (DB 조회 → JSON LIST 전체 교체 with TTL)")
    void shouldRefreshNoticePosts_WhenPostsExist() {
        // Given
        Post mockPost1 = mock(Post.class);
        given(mockPost1.getId()).willReturn(1L);
        given(mockPost1.getTitle()).willReturn("공지1");
        given(mockPost1.getViews()).willReturn(0);
        given(mockPost1.getLikeCount()).willReturn(0);
        given(mockPost1.getCreatedAt()).willReturn(Instant.now());
        given(mockPost1.getMember()).willReturn(null);
        given(mockPost1.getMemberName()).willReturn("관리자");
        given(mockPost1.getCommentCount()).willReturn(0);
        given(mockPost1.isWeekly()).willReturn(false);
        given(mockPost1.isLegend()).willReturn(false);
        given(mockPost1.isNotice()).willReturn(true);

        Post mockPost2 = mock(Post.class);
        given(mockPost2.getId()).willReturn(2L);
        given(mockPost2.getTitle()).willReturn("공지2");
        given(mockPost2.getViews()).willReturn(0);
        given(mockPost2.getLikeCount()).willReturn(0);
        given(mockPost2.getCreatedAt()).willReturn(Instant.now());
        given(mockPost2.getMember()).willReturn(null);
        given(mockPost2.getMemberName()).willReturn("관리자");
        given(mockPost2.getCommentCount()).willReturn(0);
        given(mockPost2.isWeekly()).willReturn(false);
        given(mockPost2.isLegend()).willReturn(false);
        given(mockPost2.isNotice()).willReturn(true);

        given(postRepository.findByIsNoticeTrueOrderByIdDesc()).willReturn(List.of(mockPost1, mockPost2));

        // When
        postCacheScheduler.refreshNoticePosts();

        // Then
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.POST_NOTICE_JSON_KEY), any(), eq(RedisKey.DEFAULT_CACHE_TTL));
    }

    @Test
    @DisplayName("공지사항 캐시 갱신 - 공지 없으면 스킵")
    void shouldSkipNoticeRefresh_WhenNoNoticePosts() {
        // Given
        given(postRepository.findByIsNoticeTrueOrderByIdDesc()).willReturn(Collections.emptyList());

        // When
        postCacheScheduler.refreshNoticePosts();

        // Then
        verify(redisPostListUpdateAdapter, never()).replaceList(any(), any(), any());
    }

    // ==================== 첫 페이지 ====================

    @Test
    @DisplayName("첫 페이지 캐시 갱신 - 성공 (DB 조회 → JSON LIST 전체 교체)")
    void shouldRefreshFirstPageCache_WhenPostsExist() {
        // Given
        PostSimpleDetail post1 = createPostSimpleDetail(1L, "첫페이지글1", 1L);
        PostSimpleDetail post2 = createPostSimpleDetail(2L, "첫페이지글2", 2L);
        List<PostSimpleDetail> posts = List.of(post1, post2);

        given(postQueryRepository.findBoardPostsByCursor(null, RedisKey.FIRST_PAGE_SIZE)).willReturn(posts);

        // When
        postCacheScheduler.refreshFirstPageCache();

        // Then
        verify(redisPostListUpdateAdapter).replaceList(eq(RedisKey.FIRST_PAGE_JSON_KEY), anyList(), eq(RedisKey.DEFAULT_CACHE_TTL));
    }

    // 테스트 유틸리티 메서드들
    private PostSimpleDetail createPostSimpleDetail(Long postId, String title, Long memberId) {
        return PostSimpleDetail.builder()
                .id(postId)
                .title(title)
                .viewCount(0)
                .likeCount(0)
                .createdAt(Instant.now())
                .memberId(memberId)
                .memberName(memberId != null ? "회원" + memberId : "비회원")
                .commentCount(0)
                .build();
    }
}

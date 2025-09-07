package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.application.port.out.PostCacheQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostCacheSyncService;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostQueryService 테스트</h2>
 * <p>게시글 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>게시판 조회, 인기글 조회, 검색, 캐시 처리 등의 복잡한 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostQueryService 테스트")
class PostQueryServiceTest {

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private PostLikeQueryPort postLikeQueryPort;


    @Mock
    private PostCacheSyncService postCacheSyncService;

    @Mock
    private PostCacheQueryPort postCacheQueryPort;




    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("게시판 조회 - 성공")
    void shouldGetBoard_Successfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchResult postResult = createPostSearchResult(1L, "제목1");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(postResult), pageable, 1);

        given(postQueryPort.findByPage(pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getBoard(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("제목1");
        
        verify(postQueryPort).findByPage(pageable);
    }

    @Test
    @DisplayName("게시판 조회 - 빈 결과")
    void shouldGetBoard_WhenEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(postQueryPort.findByPage(pageable)).willReturn(emptyPage);

        // When
        Page<PostSearchResult> result = postQueryService.getBoard(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        
        verify(postQueryPort).findByPage(pageable);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 일반 게시글 (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenNotPopularPost_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long userId = 2L;
        
        // 캐시에 없음 (인기글이 아님)
        given(postCacheQueryPort.getCachedPostIfExists(postId)).willReturn(null);
        
        // 최적화된 JOIN 쿼리 결과
        PostDetail mockPostDetail = createMockPostDetail(postId, userId);
        given(postQueryPort.findPostDetailWithCounts(postId, userId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(postCacheQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryPort).findPostDetailWithCounts(postId, userId); // 1회 DB 쿼리
        verify(postQueryPort, never()).findById(any()); // 기존 개별 쿼리 호출 안함
        verify(postLikeQueryPort, never()).countByPost(any());
        verify(postLikeQueryPort, never()).existsByPostIdAndUserId(any(), any());
//        verify(postCommentQueryPort, never()).countByPostId(any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 인기글인 경우 (캐시에서 조회, 최적화)")
    void shouldGetPost_WhenPopularPostFromCache_Optimized() {
        // Given
        Long postId = 1L;
        Long userId = 2L;
        
        PostDetail cachedFullPost = createPostDetail(postId, "캐시된 인기글", "캐시된 내용");
        
        // 최적화: 한번의 호출로 캐시 존재 여부와 데이터를 함께 확인
        given(postCacheQueryPort.getCachedPostIfExists(postId)).willReturn(cachedFullPost);
        
        // 좋아요 정보만 추가 확인 (Post 엔티티 로드 없이 ID로만 확인)
        given(postLikeQueryPort.existsByPostIdAndUserId(postId, userId)).willReturn(false);

        // When
        PostDetail result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse();
        
        verify(postCacheQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postLikeQueryPort).existsByPostIdAndUserId(postId, userId);
        verify(postQueryPort, never()).findById(any()); // 캐시 히트 시 DB 조회 안함
        verify(postQueryPort, never()).findPostDetailWithCounts(any(), any()); // JOIN 쿼리도 호출 안함
        verify(postLikeQueryPort, never()).countByPost(any()); // 캐시에서 가져올 때는 호출 안함
    }

    @Test
    @DisplayName("게시글 상세 조회 - 캐시 miss (최적화된 JOIN 쿼리 사용)")
    void shouldGetPost_WhenCacheMiss_WithOptimizedQuery() {
        // Given
        Long postId = 1L;
        Long userId = 2L;
        
        // 캐시에 상세 정보가 없음 (인기글이 아니거나 캐시 만료)
        given(postCacheQueryPort.getCachedPostIfExists(postId)).willReturn(null);
        
        // 최적화된 JOIN 쿼리로 DB에서 조회
        PostDetail mockPostDetail = createMockPostDetail(postId, userId);
        given(postQueryPort.findPostDetailWithCounts(postId, userId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(postCacheQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출 (최적화)
        verify(postQueryPort).findPostDetailWithCounts(postId, userId); // 1회 DB JOIN 쿼리 (최적화)
        
        // 기존 개별 쿼리들은 호출되지 않음을 검증
        verify(postQueryPort, never()).findById(any());
        verify(postLikeQueryPort, never()).countByPost(any());
        verify(postLikeQueryPort, never()).existsByPostIdAndUserId(any(), any());
//        verify(postCommentQueryPort, never()).countByPostId(any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 (userId null, 최적화)")
    void shouldGetPost_WhenAnonymousUser() {
        // Given
        Long postId = 1L;
        Long userId = null;
        
        // 캐시에 없음 (인기글이 아니거나 캐시 만료)
        given(postCacheQueryPort.getCachedPostIfExists(postId)).willReturn(null);
        
        // 최적화된 JOIN 쿼리로 DB에서 조회 (익명 사용자이므로 isLiked는 false)
        PostDetail mockPostDetail = createMockPostDetail(postId, userId);
        given(postQueryPort.findPostDetailWithCounts(postId, userId))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse(); // 익명 사용자는 항상 false
        
        verify(postCacheQueryPort).getCachedPostIfExists(postId); // 1회 Redis 호출
        verify(postQueryPort).findPostDetailWithCounts(postId, userId); // 1회 JOIN 쿼리
        
        // 기존 개별 쿼리들은 호출되지 않음
        verify(postQueryPort, never()).findById(any());
        verify(postLikeQueryPort, never()).countByPost(any());
        verify(postLikeQueryPort, never()).existsByPostIdAndUserId(any(), any());
//        verify(postCommentQueryPort, never()).countByPostId(any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글 (최적화)")
    void shouldThrowException_WhenPostNotFound_Optimized() {
        // Given
        Long postId = 999L;
        Long userId = 1L;
        
        // 캐시에도 없고 DB에도 없는 경우
        given(postCacheQueryPort.getCachedPostIfExists(postId)).willReturn(null);
        given(postQueryPort.findPostDetailWithCounts(postId, userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, userId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postCacheQueryPort).getCachedPostIfExists(postId);
        verify(postQueryPort).findPostDetailWithCounts(postId, userId);
        // 기존 개별 쿼리는 호출되지 않음
        verify(postQueryPort, never()).findById(any());
    }

    @Test
    @DisplayName("게시글 검색 - 성공")
    void shouldSearchPost_Successfully() {
        // Given
        String type = "title";
        String query = "검색어";
        Pageable pageable = PageRequest.of(0, 10);
        
        PostSearchResult searchResult = createPostSearchResult(1L, "검색 결과");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postQueryPort.findBySearch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        
        verify(postQueryPort).findBySearch(type, query, pageable);
    }

    @Test
    @DisplayName("게시글 검색 - 빈 결과")
    void shouldSearchPost_WhenNoResults() {
        // Given
        String type = "content";
        String query = "존재하지않는검색어";
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(postQueryPort.findBySearch(type, query, pageable)).willReturn(emptyPage);

        // When
        Page<PostSearchResult> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(postQueryPort).findBySearch(type, query, pageable);
    }




    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 있음")
    void shouldGetPopularPostLegend_WhenCacheExists() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 10);
        
        PostSearchResult legendPost1 = createPostSearchResult(1L, "레전드 게시글 1");
        PostSearchResult legendPost2 = createPostSearchResult(2L, "레전드 게시글 2");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(legendPost1, legendPost2), pageable, 2);

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(true);
        given(postCacheQueryPort.getCachedPostListPaged(pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("레전드 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("레전드 게시글 2");
        
        verify(postCacheQueryPort).hasPopularPostsCache(type);
        verify(postCacheQueryPort).getCachedPostListPaged(pageable);
        verify(postCacheSyncService, never()).updateLegendaryPosts();
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 캐시 없음, 업데이트 후 조회")
    void shouldGetPopularPostLegend_WhenNoCacheUpdateThenGet() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 5);
        
        PostSearchResult legendPost = createPostSearchResult(1L, "업데이트된 레전드 게시글");
        Page<PostSearchResult> updatedPage = new PageImpl<>(List.of(legendPost), pageable, 1);

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(false);
        given(postCacheQueryPort.getCachedPostListPaged(pageable)).willReturn(updatedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result).isEqualTo(updatedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("업데이트된 레전드 게시글");
        
        verify(postCacheQueryPort).hasPopularPostsCache(type);
        verify(postCacheSyncService).updateLegendaryPosts();
        verify(postCacheQueryPort).getCachedPostListPaged(pageable);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 빈 결과")
    void shouldGetPopularPostLegend_WhenEmptyResults() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(true);
        given(postCacheQueryPort.getCachedPostListPaged(pageable)).willReturn(emptyPage);

        // When
        Page<PostSearchResult> result = postQueryService.getPopularPostLegend(type, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        
        verify(postCacheQueryPort).hasPopularPostsCache(type);
        verify(postCacheQueryPort).getCachedPostListPaged(pageable);
        verify(postCacheSyncService, never()).updateLegendaryPosts();
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 다양한 페이지 크기 처리")
    void shouldGetPopularPostLegend_WithDifferentPageSizes() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        Pageable smallPage = PageRequest.of(1, 5);  // 두 번째 페이지, 5개씩
        
        List<PostSearchResult> legendPosts = List.of(
            createPostSearchResult(6L, "레전드 6"),
            createPostSearchResult(7L, "레전드 7"),
            createPostSearchResult(8L, "레전드 8")
        );
        Page<PostSearchResult> expectedPage = new PageImpl<>(legendPosts, smallPage, 15); // 전체 15개 중 6~8번

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(true);
        given(postCacheQueryPort.getCachedPostListPaged(smallPage)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getPopularPostLegend(type, smallPage);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getNumber()).isEqualTo(1); // 현재 페이지
        assertThat(result.getSize()).isEqualTo(5); // 페이지 크기
        assertThat(result.getTotalElements()).isEqualTo(15); // 전체 요소 수
        assertThat(result.getTotalPages()).isEqualTo(3); // 전체 페이지 수
        
        verify(postCacheQueryPort).getCachedPostListPaged(smallPage);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - 잘못된 타입으로 호출시 예외 발생")
    void shouldThrowException_WhenGetPopularPostLegendWithNonLegendType() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME; // LEGEND가 아닌 타입
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPopularPostLegend(type, pageable))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.INVALID_INPUT_VALUE);

        // 타입 검증에서 바로 예외가 발생하므로 다른 메서드들은 호출되지 않음
        verifyNoInteractions(postCacheQueryPort);
        verifyNoInteractions(postCacheSyncService);
    }

    @Test
    @DisplayName("레전드 인기 게시글 페이징 조회 - null 타입으로 호출시 예외 발생")
    void shouldThrowException_WhenGetPopularPostLegendWithNullType() {
        // Given
        PostCacheFlag type = null;
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPopularPostLegend(type, pageable))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.INVALID_INPUT_VALUE);

        verifyNoInteractions(postCacheQueryPort);
        verifyNoInteractions(postCacheSyncService);
    }


    @Test
    @DisplayName("공지사항 조회 - 성공")
    void shouldGetNoticePosts_Successfully() {
        // Given
        PostSearchResult noticePost = createPostSearchResult(1L, "공지사항");
        List<PostSearchResult> noticePosts = List.of(noticePost);

        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(noticePosts);

        // When
        List<PostSearchResult> result = postQueryService.getNoticePosts();

        // Then
        assertThat(result).isEqualTo(noticePosts);
        assertThat(result).hasSize(1);
        
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.NOTICE);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    void shouldFindById_WhenPostExists() {
        // Given
        Long postId = 1L;

        Post mockPost = Post.builder()
                .id(postId)
                .title("Test Post")
                .build();
        given(postQueryPort.findById(postId)).willReturn(mockPost);

        // When
        Post result = postQueryService.findById(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
        
        verify(postQueryPort).findById(postId);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 존재하지 않음")
    void shouldFindById_WhenPostNotExists() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postQueryService.findById(postId))
                .isInstanceOf(PostCustomException.class)
                .hasMessage(PostErrorCode.POST_NOT_FOUND.getMessage());
        
        verify(postQueryPort).findById(postId);
    }

    @Test
    @DisplayName("사용자 작성 게시글 조회 - 성공")
    void shouldGetUserPosts_Successfully() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchResult userPost = createPostSearchResult(1L, "사용자 게시글");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(userPost), pageable, 1);

        given(postQueryPort.findPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getUserPosts(userId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        
        verify(postQueryPort).findPostsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("사용자 추천한 게시글 조회 - 성공")
    void shouldGetUserLikedPosts_Successfully() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchResult likedPost = createPostSearchResult(1L, "추천한 게시글");
        Page<PostSearchResult> expectedPage = new PageImpl<>(List.of(likedPost), pageable, 1);

        given(postQueryPort.findLikedPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = postQueryService.getUserLikedPosts(userId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        
        verify(postQueryPort).findLikedPostsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 캐시 모두 있음")
    void shouldGetRealtimeAndWeeklyPosts_WhenBothCachesExist() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(
            createPostSearchResult(1L, "실시간 인기글 1"),
            createPostSearchResult(2L, "실시간 인기글 2")
        );
        List<PostSearchResult> weeklyPosts = List.of(
            createPostSearchResult(3L, "주간 인기글 1"),
            createPostSearchResult(4L, "주간 인기글 2")
        );

        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("realtime", "weekly");
        assertThat(result.get("realtime")).hasSize(2);
        assertThat(result.get("weekly")).hasSize(2);
        assertThat(result.get("realtime").get(0).getTitle()).isEqualTo("실시간 인기글 1");
        assertThat(result.get("weekly").get(0).getTitle()).isEqualTo("주간 인기글 1");
        
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService, never()).updateRealtimePopularPosts();
        verify(postCacheSyncService, never()).updateWeeklyPopularPosts();
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 실시간 캐시 없음")
    void shouldGetRealtimeAndWeeklyPosts_WhenRealtimeCacheMissing() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(createPostSearchResult(1L, "업데이트된 실시간"));
        List<PostSearchResult> weeklyPosts = List.of(createPostSearchResult(2L, "기존 주간"));

        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(false);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).hasSize(1);
        assertThat(result.get("weekly")).hasSize(1);
        
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService).updateRealtimePopularPosts();
        verify(postCacheSyncService, never()).updateWeeklyPopularPosts();
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 주간 캐시 없음")
    void shouldGetRealtimeAndWeeklyPosts_WhenWeeklyCacheMissing() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(createPostSearchResult(1L, "기존 실시간"));
        List<PostSearchResult> weeklyPosts = List.of(createPostSearchResult(2L, "업데이트된 주간"));

        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(false);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).hasSize(1);
        assertThat(result.get("weekly")).hasSize(1);
        
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService, never()).updateRealtimePopularPosts();
        verify(postCacheSyncService).updateWeeklyPopularPosts();
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 두 캐시 모두 없음")
    void shouldGetRealtimeAndWeeklyPosts_WhenBothCachesMissing() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(createPostSearchResult(1L, "새로운 실시간"));
        List<PostSearchResult> weeklyPosts = List.of(createPostSearchResult(2L, "새로운 주간"));

        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(false);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(false);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).hasSize(1);
        assertThat(result.get("weekly")).hasSize(1);
        
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheSyncService).updateRealtimePopularPosts();
        verify(postCacheSyncService).updateWeeklyPopularPosts();
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("실시간/주간 인기글 일괄 조회 - 빈 결과 처리")
    void shouldGetRealtimeAndWeeklyPosts_WhenEmptyResults() {
        // Given
        List<PostSearchResult> emptyRealtimePosts = Collections.emptyList();
        List<PostSearchResult> emptyWeeklyPosts = Collections.emptyList();

        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(emptyRealtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(emptyWeeklyPosts);

        // When
        Map<String, List<PostSearchResult>> result = postQueryService.getRealtimeAndWeeklyPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("realtime")).isEmpty();
        assertThat(result.get("weekly")).isEmpty();
        
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).hasPopularPostsCache(PostCacheFlag.WEEKLY);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.REALTIME);
        verify(postCacheQueryPort).getCachedPostList(PostCacheFlag.WEEKLY);
    }

    @Test
    @DisplayName("복합 인기글 조회 시나리오 - 여러 타입 동시 요청")
    void shouldHandleMultiplePopularPostRequests() {
        // Given
        List<PostSearchResult> realtimePosts = List.of(createPostSearchResult(1L, "실시간"));
        List<PostSearchResult> weeklyPosts = List.of(createPostSearchResult(2L, "주간"));
        List<PostSearchResult> legendPosts = List.of(createPostSearchResult(3L, "전설"));

        // 모든 캐시가 있다고 가정
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.LEGEND)).willReturn(true);
        
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);
        given(postCacheQueryPort.getCachedPostListPaged(PageRequest.of(0, 10))).willReturn(new PageImpl<>(legendPosts));

        // When
        Map<String, List<PostSearchResult>> realtimeAndWeekly = postQueryService.getRealtimeAndWeeklyPosts();
        Page<PostSearchResult> legendResult = postQueryService.getPopularPostLegend(PostCacheFlag.LEGEND, PageRequest.of(0, 10));

        // Then
        assertThat(realtimeAndWeekly).hasSize(2);
        assertThat(realtimeAndWeekly.get("realtime")).hasSize(1);
        assertThat(realtimeAndWeekly.get("weekly")).hasSize(1);
        assertThat(legendResult.getContent()).hasSize(1);
        
        verify(postCacheQueryPort, times(3)).hasPopularPostsCache(any());
        verify(postCacheQueryPort, times(2)).getCachedPostList(any());
        verify(postCacheQueryPort, times(1)).getCachedPostListPaged(any());
        verifyNoInteractions(postCacheSyncService);
    }

    // 테스트 유틸리티 메서드들
    private PostSearchResult createPostSearchResult(Long id, String title) {
        return PostSearchResult.builder()
                .id(id)
                .title(title)
                .build();
    }

    private PostDetail createPostDetail(Long id, String title, String content) {
        return createPostDetail(id, title, content, 1L, false);
    }
    
    private PostDetail createPostDetail(Long id, String title, String content, Long userId, boolean isLiked) {
        return PostDetail.builder()
                .id(id)
                .title(title)
                .content(content)
                .viewCount(10)
                .likeCount(5)
                .postCacheFlag(null)
                .createdAt(java.time.Instant.now())
                .userId(userId)
                .userName("testUser")
                .commentCount(3)
                .isNotice(false)
                .isLiked(isLiked)
                .build();
    }

    /**
     * <h3>PostDetail Mock 생성</h3>
     * <p>JOIN 쿼리 테스트용 Mock 객체를 생성합니다.</p>
     */
    private PostDetail createMockPostDetail(Long postId, Long userId) {
        return createPostDetail(postId, "Test Title", "Test Content", 1L, userId != null);
    }
}
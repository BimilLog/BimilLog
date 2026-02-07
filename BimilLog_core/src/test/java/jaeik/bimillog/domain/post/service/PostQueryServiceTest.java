package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.async.PostViewCountSync;
import jaeik.bimillog.domain.post.async.RealtimePostSync;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.repository.*;
import jaeik.bimillog.domain.post.util.PostUtil;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisFirstPagePostAdapter;

import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostQueryService 테스트</h2>
 * <p>게시글 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>게시판 조회, 상세 조회를 테스트합니다.</p>
 * <p>상세 캐시 제거됨 - 모든 상세 조회는 DB에서 직접 수행</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@DisplayName("PostQueryService 테스트")
@Tag("unit")
class PostQueryServiceTest extends BaseUnitTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostReadModelQueryRepository postReadModelQueryRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostUtil postUtil;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedisFirstPagePostAdapter redisFirstPagePostAdapter;

    @Mock
    private PostViewCountSync postViewCountSync;

    @Mock
    private RealtimePostSync realtimePostSync;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("게시판 조회 (커서 기반, 비회원) - 캐시 히트")
    void shouldGetBoardByCursor_ForGuest_CacheHit() {
        // Given
        Long cursor = null;
        int size = 10;
        PostSimpleDetail postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        List<PostSimpleDetail> posts = List.of(postResult);

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(posts);
        given(postUtil.removePostsWithBlacklist(null, posts)).willReturn(posts);

        // When
        var result = postQueryService.getBoardByCursor(cursor, size, null);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().getTitle()).isEqualTo("제목1");
        assertThat(result.hasNext()).isFalse();

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postReadModelQueryRepository, never()).findBoardPostsByCursor(any(), anyInt());
    }

    @Test
    @DisplayName("게시판 조회 (커서 기반, 비회원) - 캐시 히트 + size 제한")
    void shouldGetBoardByCursor_ForGuest_CacheHit_WithSizeLimit() {
        // Given
        Long cursor = null;
        int size = 2;
        List<PostSimpleDetail> cachedPosts = List.of(
                PostTestDataBuilder.createPostSearchResult(5L, "제목5"),
                PostTestDataBuilder.createPostSearchResult(4L, "제목4"),
                PostTestDataBuilder.createPostSearchResult(3L, "제목3"),
                PostTestDataBuilder.createPostSearchResult(2L, "제목2"),
                PostTestDataBuilder.createPostSearchResult(1L, "제목1")
        );

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(cachedPosts);
        given(postUtil.removePostsWithBlacklist(null, cachedPosts)).willReturn(cachedPosts);

        // When
        var result = postQueryService.getBoardByCursor(cursor, size, null);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).getTitle()).isEqualTo("제목5");
        assertThat(result.content().get(1).getTitle()).isEqualTo("제목4");
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(4L);

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postReadModelQueryRepository, never()).findBoardPostsByCursor(any(), anyInt());
    }

    @Test
    @DisplayName("게시판 조회 (커서 기반, 비회원) - 캐시 미스 시 DB 조회")
    void shouldGetBoardByCursor_ForGuest_CacheMiss() {
        // Given
        Long cursor = null;
        int size = 10;
        PostSimpleDetail postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        List<PostSimpleDetail> posts = List.of(postResult);

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(Collections.emptyList());
        given(postReadModelQueryRepository.findBoardPostsByCursor(null, 20)).willReturn(posts);
        given(postUtil.removePostsWithBlacklist(null, posts)).willReturn(posts);

        // When
        var result = postQueryService.getBoardByCursor(cursor, size, null);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().getTitle()).isEqualTo("제목1");
        assertThat(result.hasNext()).isFalse();

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postReadModelQueryRepository).findBoardPostsByCursor(null, 20);
    }

    @Test
    @DisplayName("게시판 조회 (두 번째 페이지) - DB 직접 조회")
    void shouldGetBoardByCursor_SecondPage_DirectDB() {
        // Given
        Long cursor = 100L;
        int size = 10;
        PostSimpleDetail postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        List<PostSimpleDetail> posts = List.of(postResult);

        given(postReadModelQueryRepository.findBoardPostsByCursor(cursor, size)).willReturn(posts);
        given(postUtil.removePostsWithBlacklist(null, posts)).willReturn(posts);

        // When
        var result = postQueryService.getBoardByCursor(cursor, size, null);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().getTitle()).isEqualTo("제목1");

        verify(redisFirstPagePostAdapter, never()).getFirstPage();
        verify(postReadModelQueryRepository).findBoardPostsByCursor(cursor, size);
    }

    @Test
    @DisplayName("게시판 조회 (첫 페이지, 회원) - 캐시 히트 + 블랙리스트 필터링")
    void shouldGetBoardByCursor_ForMember_CacheHit_WithBlacklistFiltering() {
        // Given
        Long cursor = null;
        int size = 20;
        Long memberId = 100L;

        List<PostSimpleDetail> cachedPosts = List.of(
                PostTestDataBuilder.createPostSearchResultWithMemberId(1L, "게시글1", 1L),
                PostTestDataBuilder.createPostSearchResultWithMemberId(2L, "블랙게시글", 2L),
                PostTestDataBuilder.createPostSearchResultWithMemberId(3L, "게시글3", 3L)
        );
        List<PostSimpleDetail> filteredPosts = List.of(
                PostTestDataBuilder.createPostSearchResultWithMemberId(1L, "게시글1", 1L),
                PostTestDataBuilder.createPostSearchResultWithMemberId(3L, "게시글3", 3L)
        );

        given(redisFirstPagePostAdapter.getFirstPage()).willReturn(cachedPosts);
        given(postUtil.removePostsWithBlacklist(memberId, cachedPosts)).willReturn(filteredPosts);

        // When
        var result = postQueryService.getBoardByCursor(cursor, size, memberId);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content()).extracting(PostSimpleDetail::getId).containsExactly(1L, 3L);
        assertThat(result.hasNext()).isFalse();

        verify(redisFirstPagePostAdapter).getFirstPage();
        verify(postUtil).removePostsWithBlacklist(memberId, cachedPosts);
    }

    @Test
    @DisplayName("게시글 상세 조회 - DB 직접 조회 (회원)")
    void shouldGetPostDetail_WhenMember() {
        // Given
        Long postId = 1L;
        Long memberId = 2L;
        Long postAuthorId = 1L;

        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("게시글 제목")
                .content("게시글 내용")
                .memberId(postAuthorId)
                .isLiked(false)
                .viewCount(100)
                .likeCount(50)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(10)
                .build();
        given(postQueryRepository.findPostDetail(postId, null))
                .willReturn(Optional.of(mockPostDetail));

        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(true);

        // When
        PostDetail result = postQueryService.getPost(postId, memberId, "test-viewer");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isTrue();

        verify(postQueryRepository).findPostDetail(postId, null);
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
    }

    @Test
    @DisplayName("게시글 상세 조회 - DB 직접 조회 (비회원)")
    void shouldGetPostDetail_WhenAnonymous() {
        // Given
        Long postId = 1L;
        Long memberId = null;
        Long postAuthorId = 1L;

        PostDetail mockPostDetail = PostDetail.builder()
                .id(postId)
                .title("게시글 제목")
                .content("게시글 내용")
                .memberId(postAuthorId)
                .isLiked(false)
                .viewCount(10)
                .likeCount(5)
                .createdAt(Instant.now())
                .memberName("testMember")
                .commentCount(3)
                .build();
        given(postQueryRepository.findPostDetail(postId, null))
                .willReturn(Optional.of(mockPostDetail));

        // When
        PostDetail result = postQueryService.getPost(postId, memberId, "test-viewer");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse();

        verify(postQueryRepository).findPostDetail(postId, null);
        verify(postLikeRepository, never()).existsByPostIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        Long memberId = 1L;

        given(postQueryRepository.findPostDetail(postId, null)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, memberId, "test-viewer"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryRepository).findPostDetail(postId, null);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    void shouldFindById_WhenPostExists() {
        // Given
        Long postId = 1L;

        Post mockPost = PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "Test Post", "Content"));
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

        // When
        Optional<Post> result = postQueryService.findById(postId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(postId);

        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 존재하지 않음")
    void shouldFindById_WhenPostNotExists() {
        // Given
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When
        Optional<Post> result = postQueryService.findById(postId);

        // Then
        assertThat(result).isEmpty();
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("사용자 작성 게시글 조회 - 성공")
    void shouldGetUserPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail userPost = PostTestDataBuilder.createPostSearchResult(1L, "사용자 게시글");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(userPost), pageable, 1);

        Page<PostSimpleDetail> emptyLikedPage = Page.empty();
        given(postQueryRepository.findPostsByMemberId(memberId, pageable, memberId)).willReturn(expectedPage);
        given(postQueryRepository.findLikedPostsByMemberId(memberId, pageable)).willReturn(emptyLikedPage);

        // When
        MemberActivityPost result = postQueryService.getMemberActivityPosts(memberId, pageable);

        // Then
        assertThat(result.getWritePosts()).isEqualTo(expectedPage);
        assertThat(result.getWritePosts().getContent()).hasSize(1);

        verify(postQueryRepository).findPostsByMemberId(memberId, pageable, memberId);
    }

    @Test
    @DisplayName("사용자 추천한 게시글 조회 - 성공")
    void shouldGetUserLikedPosts_Successfully() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail likedPost = PostTestDataBuilder.createPostSearchResult(1L, "추천한 게시글");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(likedPost), pageable, 1);

        Page<PostSimpleDetail> emptyWritePage = Page.empty();
        given(postQueryRepository.findPostsByMemberId(memberId, pageable, memberId)).willReturn(emptyWritePage);
        given(postQueryRepository.findLikedPostsByMemberId(memberId, pageable)).willReturn(expectedPage);

        // When
        MemberActivityPost result = postQueryService.getMemberActivityPosts(memberId, pageable);

        // Then
        assertThat(result.getLikedPosts()).isEqualTo(expectedPage);
        assertThat(result.getLikedPosts().getContent()).hasSize(1);

        verify(postQueryRepository).findLikedPostsByMemberId(memberId, pageable);
    }

}

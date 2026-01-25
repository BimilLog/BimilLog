package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.adapter.PostToCommentAdapter;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.repository.*;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.resilience.DbFallbackGateway;
import jaeik.bimillog.infrastructure.resilience.FallbackType;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostQueryService 테스트</h2>
 * <p>게시글 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>게시판 조회, 상세 조회, 검색 기능을 테스트합니다.</p>
 * <p>상세 캐시 제거됨 - 모든 상세 조회는 DB에서 직접 수행</p>
 *
 * @author Jaeik
 * @version 2.7.0
 */
@DisplayName("PostQueryService 테스트")
@Tag("unit")
class PostQueryServiceTest extends BaseUnitTest {

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostSearchRepository postSearchRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostToCommentAdapter postToCommentAdapter;

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DbFallbackGateway dbFallbackGateway;

    @InjectMocks
    private PostQueryService postQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(postToCommentAdapter.findCommentCountsByPostIds(anyList()))
                .thenReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("게시판 조회 - 성공")
    void shouldGetBoard_Successfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PostSimpleDetail postResult = PostTestDataBuilder.createPostSearchResult(1L, "제목1");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(postResult), pageable, 1);

        given(postQueryRepository.findByPage(pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.getBoard(pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("제목1");

        verify(postQueryRepository).findByPage(pageable, null);
    }

    @Test
    @DisplayName("게시글 상세 조회 - DB 직접 조회 (회원)")
    @SuppressWarnings("unchecked")
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
                .isNotice(false)
                .build();
        given(postQueryRepository.findPostDetail(postId, null))
                .willReturn(Optional.of(mockPostDetail));

        // DbFallbackGateway가 Supplier를 실행하도록 Mock 설정
        given(dbFallbackGateway.executeDetail(eq(FallbackType.NORMAL_DETAIL), eq(postId), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Optional<PostDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // 회원이므로 isLiked 조회
        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(true);

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isTrue();

        verify(dbFallbackGateway).executeDetail(eq(FallbackType.NORMAL_DETAIL), eq(postId), any(Supplier.class));
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
    }

    @Test
    @DisplayName("게시글 상세 조회 - DB 직접 조회 (비회원)")
    @SuppressWarnings("unchecked")
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
                .isNotice(false)
                .build();
        given(postQueryRepository.findPostDetail(postId, null))
                .willReturn(Optional.of(mockPostDetail));

        // DbFallbackGateway가 Supplier를 실행하도록 Mock 설정
        given(dbFallbackGateway.executeDetail(eq(FallbackType.NORMAL_DETAIL), eq(postId), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Optional<PostDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // When
        PostDetail result = postQueryService.getPost(postId, memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isFalse();

        verify(dbFallbackGateway).executeDetail(eq(FallbackType.NORMAL_DETAIL), eq(postId), any(Supplier.class));
        verify(postLikeRepository, never()).existsByPostIdAndMemberId(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글")
    @SuppressWarnings("unchecked")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        Long memberId = 1L;

        // DB에도 없음
        given(postQueryRepository.findPostDetail(postId, null)).willReturn(Optional.empty());

        // DbFallbackGateway가 Supplier를 실행하도록 Mock 설정
        given(dbFallbackGateway.executeDetail(eq(FallbackType.NORMAL_DETAIL), eq(postId), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<Optional<PostDetail>> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(dbFallbackGateway).executeDetail(eq(FallbackType.NORMAL_DETAIL), eq(postId), any(Supplier.class));
    }

    @Test
    @DisplayName("게시글 검색 - 전문검색 전략 (3글자 이상 + TITLE)")
    void shouldSearchPost_UsingFullTextSearch_When3CharsOrMore() {
        // Given
        PostSearchType type = PostSearchType.TITLE;
        String query = "검색어"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByFullTextSearch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByFullTextSearch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 전문검색 조건 충족 시 결과 그대로 반환")
    void shouldNotFallback_WhenFullTextSearchSelected() {
        // Given
        PostSearchType type = PostSearchType.TITLE_CONTENT;
        String query = "검색어"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSimpleDetail> emptyPage = Page.empty(pageable);

        given(postSearchRepository.findByFullTextSearch(type, query, pageable, null)).willReturn(emptyPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(emptyPage);

        verify(postSearchRepository).findByFullTextSearch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByPartialMatch(type, query, pageable, null);
    }

    @Test
    @DisplayName("게시글 검색 - 접두사 검색 전략 (WRITER + 4글자 이상)")
    void shouldSearchPost_UsingPrefixMatch_WhenWriter4CharsOrMore() {
        // Given
        PostSearchType type = PostSearchType.WRITER;
        String query = "작성자이름"; // 5글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "작성자 검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByPrefixMatch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByPrefixMatch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 부분검색 전략 (WRITER + 4글자 미만)")
    void shouldSearchPost_UsingPartialMatch_WhenWriterLessThan4Chars() {
        // Given
        PostSearchType type = PostSearchType.WRITER;
        String query = "작성자"; // 3글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "부분 검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByPartialMatch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByPartialMatch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게시글 검색 - 부분검색 전략 (3글자 미만)")
    void shouldSearchPost_UsingPartialMatch_WhenLessThan3Chars() {
        // Given
        PostSearchType type = PostSearchType.TITLE;
        String query = "검색"; // 2글자
        Pageable pageable = PageRequest.of(0, 10);

        PostSimpleDetail searchResult = PostTestDataBuilder.createPostSearchResult(1L, "부분 검색 결과");
        Page<PostSimpleDetail> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postSearchRepository.findByPartialMatch(type, query, pageable, null)).willReturn(expectedPage);

        // When
        Page<PostSimpleDetail> result = postQueryService.searchPost(type, query, pageable, null);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);

        verify(postSearchRepository).findByPartialMatch(type, query, pageable, null);
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
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
        given(postToCommentAdapter.findCommentCountsByPostIds(List.of(1L))).willReturn(Map.of(1L, 0));

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
        given(postToCommentAdapter.findCommentCountsByPostIds(List.of(1L))).willReturn(Map.of(1L, 0));

        // When
        MemberActivityPost result = postQueryService.getMemberActivityPosts(memberId, pageable);

        // Then
        assertThat(result.getLikedPosts()).isEqualTo(expectedPage);
        assertThat(result.getLikedPosts().getContent()).hasSize(1);

        verify(postQueryRepository).findLikedPostsByMemberId(memberId, pageable);
    }

}

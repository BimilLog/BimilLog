package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
    private UserLoadPort userLoadPort;

    @Mock
    private PostCacheSyncService postCacheSyncService;

    @Mock
    private PostCacheQueryPort postCacheQueryPort;

    @Mock
    private User user;

    @Mock
    private Post post;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("게시판 조회 - 성공")
    void shouldGetBoard_Successfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        SimplePostResDTO postDTO = createSimplePostResDTO(1L, "제목1");
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(List.of(postDTO), pageable, 1);

        given(postQueryPort.findByPage(pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.getBoard(pageable);

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
        Page<SimplePostResDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(postQueryPort.findByPage(pageable)).willReturn(emptyPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.getBoard(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        
        verify(postQueryPort).findByPage(pageable);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 인기글이 아닌 경우 (DB에서 조회)")
    void shouldGetPost_WhenNotPopularPost() {
        // Given
        Long postId = 1L;
        Long userId = 2L;
        
        // 모든 캐시에 해당 게시글이 없음
        given(postCacheQueryPort.hasPopularPostsCache(any())).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(any())).willReturn(Collections.emptyList());
        
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        given(postLikeQueryPort.countByPost(post)).willReturn(5L);
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(true);

        // When
        FullPostResDTO result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(postQueryPort).findById(postId);
        verify(userLoadPort).getReferenceById(userId);
        verify(postLikeQueryPort).countByPost(post);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 인기글인 경우 (캐시에서 조회)")
    void shouldGetPost_WhenPopularPostFromCache() {
        // Given
        Long postId = 1L;
        Long userId = 2L;
        
        SimplePostResDTO cachedSimplePost = createSimplePostResDTO(postId, "캐시된 인기글");
        FullPostResDTO cachedFullPost = createFullPostResDTO(postId, "캐시된 인기글", "캐시된 내용");
        
        // 실시간 인기글 캐시에 존재
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(List.of(cachedSimplePost));
        given(postCacheQueryPort.getCachedPost(postId)).willReturn(cachedFullPost);
        
        // 좋아요 정보만 추가 확인
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(false);

        // When
        FullPostResDTO result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isEqualTo(cachedFullPost);
        assertThat(result.isLiked()).isFalse();
        
        verify(postCacheQueryPort).getCachedPost(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        verify(postLikeQueryPort, never()).countByPost(any()); // 캐시에서 가져올 때는 호출 안함
    }

    @Test
    @DisplayName("게시글 상세 조회 - 인기글이지만 캐시 miss")
    void shouldGetPost_WhenPopularPostButCacheMiss() {
        // Given
        Long postId = 1L;
        Long userId = 2L;
        
        SimplePostResDTO cachedSimplePost = createSimplePostResDTO(postId, "인기글");
        
        // 캐시에는 있지만 상세 정보가 없음
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(List.of(cachedSimplePost));
        given(postCacheQueryPort.getCachedPost(postId)).willReturn(null);
        
        // DB에서 조회
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(userLoadPort.getReferenceById(userId)).willReturn(user);
        given(postLikeQueryPort.countByPost(post)).willReturn(3L);
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(true);

        // When
        FullPostResDTO result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(postCacheQueryPort).getCachedPost(postId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).countByPost(post);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 (userId null)")
    void shouldGetPost_WhenAnonymousUser() {
        // Given
        Long postId = 1L;
        Long userId = null;
        
        // 인기글이 아님
        given(postCacheQueryPort.hasPopularPostsCache(any())).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(any())).willReturn(Collections.emptyList());
        
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.countByPost(post)).willReturn(10L);

        // When
        FullPostResDTO result = postQueryService.getPost(postId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).countByPost(post);
        verify(userLoadPort, never()).getReferenceById(any());
        verify(postLikeQueryPort, never()).existsByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글")
    void shouldThrowException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        Long userId = 1L;
        
        // 인기글이 아님
        given(postCacheQueryPort.hasPopularPostsCache(any())).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(any())).willReturn(Collections.emptyList());
        
        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPost(postId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postQueryPort).findById(postId);
    }

    @Test
    @DisplayName("게시글 검색 - 성공")
    void shouldSearchPost_Successfully() {
        // Given
        String type = "title";
        String query = "검색어";
        Pageable pageable = PageRequest.of(0, 10);
        
        SimplePostResDTO searchResult = createSimplePostResDTO(1L, "검색 결과");
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(List.of(searchResult), pageable, 1);

        given(postQueryPort.findBySearch(type, query, pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.searchPost(type, query, pageable);

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
        
        Page<SimplePostResDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        given(postQueryPort.findBySearch(type, query, pageable)).willReturn(emptyPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.searchPost(type, query, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(postQueryPort).findBySearch(type, query, pageable);
    }

    @Test
    @DisplayName("인기 게시글 조회 - 캐시 있음 (실시간)")
    void shouldGetPopularPosts_WhenCacheExists() {
        // Given
        PostCacheFlag type = PostCacheFlag.REALTIME;
        SimplePostResDTO popularPost = createSimplePostResDTO(1L, "실시간 인기글");
        List<SimplePostResDTO> cachedPosts = List.of(popularPost);

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(true);
        given(postCacheQueryPort.getCachedPostList(type)).willReturn(cachedPosts);

        // When
        List<SimplePostResDTO> result = postQueryService.getPopularPosts(type);

        // Then
        assertThat(result).isEqualTo(cachedPosts);
        assertThat(result).hasSize(1);
        
        verify(postCacheQueryPort).hasPopularPostsCache(type);
        verify(postCacheQueryPort).getCachedPostList(type);
        verify(postCacheSyncService, never()).updateRealtimePopularPosts();
    }

    @Test
    @DisplayName("인기 게시글 조회 - 캐시 없음, 업데이트 후 조회 (주간)")
    void shouldGetPopularPosts_WhenNoCacheUpdateThenGet() {
        // Given
        PostCacheFlag type = PostCacheFlag.WEEKLY;
        SimplePostResDTO popularPost = createSimplePostResDTO(1L, "주간 인기글");
        List<SimplePostResDTO> updatedPosts = List.of(popularPost);

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(false);
        given(postCacheQueryPort.getCachedPostList(type)).willReturn(updatedPosts);

        // When
        List<SimplePostResDTO> result = postQueryService.getPopularPosts(type);

        // Then
        assertThat(result).isEqualTo(updatedPosts);
        
        verify(postCacheQueryPort).hasPopularPostsCache(type);
        verify(postCacheSyncService).updateWeeklyPopularPosts();
        verify(postCacheQueryPort).getCachedPostList(type);
    }

    @Test
    @DisplayName("인기 게시글 조회 - 전설의 게시글")
    void shouldGetPopularPosts_LegendaryPosts() {
        // Given
        PostCacheFlag type = PostCacheFlag.LEGEND;
        
        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(false);
        given(postCacheQueryPort.getCachedPostList(type)).willReturn(Collections.emptyList());

        // When
        List<SimplePostResDTO> result = postQueryService.getPopularPosts(type);

        // Then
        assertThat(result).isEmpty();
        
        verify(postCacheSyncService).updateLegendaryPosts();
        verify(postCacheQueryPort).getCachedPostList(type);
    }

    @Test
    @DisplayName("인기 게시글 조회 - 잘못된 캐시 타입")
    void shouldThrowException_WhenInvalidCacheType() {
        // Given
        PostCacheFlag type = PostCacheFlag.NOTICE; // getPopularPosts에서 지원하지 않는 타입

        given(postCacheQueryPort.hasPopularPostsCache(type)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postQueryService.getPopularPosts(type))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(postCacheQueryPort).hasPopularPostsCache(type);
        verifyNoInteractions(postCacheSyncService);
    }

    @Test
    @DisplayName("공지사항 조회 - 성공")
    void shouldGetNoticePosts_Successfully() {
        // Given
        SimplePostResDTO noticePost = createSimplePostResDTO(1L, "공지사항");
        List<SimplePostResDTO> noticePosts = List.of(noticePost);

        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.NOTICE)).willReturn(noticePosts);

        // When
        List<SimplePostResDTO> result = postQueryService.getNoticePosts();

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

        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        // When
        Optional<Post> result = postQueryService.findById(postId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(post);
        
        verify(postQueryPort).findById(postId);
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 존재하지 않음")
    void shouldFindById_WhenPostNotExists() {
        // Given
        Long postId = 999L;

        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When
        Optional<Post> result = postQueryService.findById(postId);

        // Then
        assertThat(result).isEmpty();
        
        verify(postQueryPort).findById(postId);
    }

    @Test
    @DisplayName("사용자 작성 게시글 조회 - 성공")
    void shouldGetUserPosts_Successfully() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        SimplePostResDTO userPost = createSimplePostResDTO(1L, "사용자 게시글");
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(List.of(userPost), pageable, 1);

        given(postQueryPort.findPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.getUserPosts(userId, pageable);

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
        SimplePostResDTO likedPost = createSimplePostResDTO(1L, "추천한 게시글");
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(List.of(likedPost), pageable, 1);

        given(postQueryPort.findLikedPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = postQueryService.getUserLikedPosts(userId, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        
        verify(postQueryPort).findLikedPostsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("복합 인기글 조회 시나리오 - 여러 타입 동시 요청")
    void shouldHandleMultiplePopularPostRequests() {
        // Given
        List<SimplePostResDTO> realtimePosts = List.of(createSimplePostResDTO(1L, "실시간"));
        List<SimplePostResDTO> weeklyPosts = List.of(createSimplePostResDTO(2L, "주간"));
        List<SimplePostResDTO> legendPosts = List.of(createSimplePostResDTO(3L, "전설"));

        // 모든 캐시가 있다고 가정
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.REALTIME)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.WEEKLY)).willReturn(true);
        given(postCacheQueryPort.hasPopularPostsCache(PostCacheFlag.LEGEND)).willReturn(true);
        
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.REALTIME)).willReturn(realtimePosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.WEEKLY)).willReturn(weeklyPosts);
        given(postCacheQueryPort.getCachedPostList(PostCacheFlag.LEGEND)).willReturn(legendPosts);

        // When
        List<SimplePostResDTO> realtimeResult = postQueryService.getPopularPosts(PostCacheFlag.REALTIME);
        List<SimplePostResDTO> weeklyResult = postQueryService.getPopularPosts(PostCacheFlag.WEEKLY);
        List<SimplePostResDTO> legendResult = postQueryService.getPopularPosts(PostCacheFlag.LEGEND);

        // Then
        assertThat(realtimeResult).hasSize(1);
        assertThat(weeklyResult).hasSize(1);
        assertThat(legendResult).hasSize(1);
        
        verify(postCacheQueryPort, times(3)).hasPopularPostsCache(any());
        verify(postCacheQueryPort, times(3)).getCachedPostList(any());
        verifyNoInteractions(postCacheSyncService);
    }

    // 테스트 유틸리티 메서드들
    private SimplePostResDTO createSimplePostResDTO(Long id, String title) {
        return SimplePostResDTO.builder()
                .id(id)
                .title(title)
                .build();
    }

    private FullPostResDTO createFullPostResDTO(Long id, String title, String content) {
        return FullPostResDTO.builder()
                .id(id)
                .title(title)
                .content(content)
                .build();
    }
}
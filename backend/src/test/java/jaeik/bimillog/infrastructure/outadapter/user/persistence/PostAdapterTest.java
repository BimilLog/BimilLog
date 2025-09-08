package jaeik.bimillog.infrastructure.outadapter.user.persistence;

import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.post.UserToPostAdapter;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostAdapter 테스트</h2>
 * <p>사용자 도메인에서 게시글 도메인으로의 게시글 조회 어댑터 테스트</p>
 * <p>게시글 작성 목록 및 추천 게시글 목록 조회 기능 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PostAdapterTest {

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @InjectMocks
    private UserToPostAdapter userToPostAdapter;

    @Test
    @DisplayName("정상 케이스 - 사용자 작성 게시글 목록 조회")
    void shouldFindPostsByUserId_WhenValidUserIdProvided() {
        // Given: 사용자 ID와 페이지 정보, 예상 게시글 목록
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<PostSearchResult> posts = Arrays.asList(
            PostSearchResult.builder()
                .id(1L)
                .title("첫 번째 게시글")
                .content("첫 번째 게시글 내용")
                .userName("작성자1")
                .userId(userId)
                .createdAt(Instant.now().minusSeconds(86400))
                .likeCount(5)
                .commentCount(3)
                .viewCount(10)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .isNotice(false)
                .build(),
            PostSearchResult.builder()
                .id(2L)
                .title("두 번째 게시글")
                .content("두 번째 게시글 내용")
                .userName("작성자1")
                .userId(userId)
                .createdAt(Instant.now().minusSeconds(172800))
                .likeCount(10)
                .commentCount(7)
                .viewCount(20)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .isNotice(false)
                .build()
        );
        
        Page<PostSearchResult> expectedPage = new PageImpl<>(posts, pageable, posts.size());
        given(postQueryUseCase.getUserPosts(eq(userId), any(Pageable.class))).willReturn(expectedPage);

        // When: 사용자 작성 게시글 목록 조회 실행
        Page<PostSearchResult> result = userToPostAdapter.findPostsByUserId(userId, pageable);

        // Then: 올바른 게시글 목록이 반환되고 UseCase가 호출되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("두 번째 게시글");
        assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(5);
        assertThat(result.getContent().get(1).getCommentCount()).isEqualTo(7);
        verify(postQueryUseCase).getUserPosts(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 추천 게시글 목록 조회")
    void shouldFindLikedPostsByUserId_WhenValidUserIdProvided() {
        // Given: 사용자 ID와 페이지 정보, 예상 추천 게시글 목록
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 5);
        
        List<PostSearchResult> likedPosts = Arrays.asList(
            PostSearchResult.builder()
                .id(3L)
                .title("추천한 게시글 1")
                .content("추천한 게시글 1 내용")
                .userName("다른작성자1")
                .userId(101L)
                .createdAt(Instant.now().minusSeconds(259200))
                .likeCount(15)
                .commentCount(8)
                .viewCount(50)
                .postCacheFlag(PostCacheFlag.LEGEND)
                .isNotice(false)
                .build(),
            PostSearchResult.builder()
                .id(4L)
                .title("추천한 게시글 2")
                .content("추천한 게시글 2 내용")
                .userName("다른작성자2")
                .userId(102L)
                .createdAt(Instant.now().minusSeconds(345600))
                .likeCount(20)
                .commentCount(12)
                .viewCount(80)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .isNotice(false)
                .build()
        );
        
        Page<PostSearchResult> expectedPage = new PageImpl<>(likedPosts, pageable, likedPosts.size());
        given(postQueryUseCase.getUserLikedPosts(eq(userId), any(Pageable.class))).willReturn(expectedPage);

        // When: 사용자 추천 게시글 목록 조회 실행
        Page<PostSearchResult> result = userToPostAdapter.findLikedPostsByUserId(userId, pageable);

        // Then: 올바른 추천 게시글 목록이 반환되고 UseCase가 호출되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("추천한 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("추천한 게시글 2");
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("다른작성자1");
        assertThat(result.getContent().get(1).getLikeCount()).isEqualTo(20);
        verify(postQueryUseCase).getUserLikedPosts(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("경계값 - 빈 게시글 목록 조회")
    void shouldHandleEmptyPosts_WhenNoPostsFound() {
        // Given: 게시글이 없는 사용자 ID와 빈 페이지 결과
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        given(postQueryUseCase.getUserPosts(eq(userId), any(Pageable.class))).willReturn(emptyPage);

        // When: 게시글이 없는 사용자의 게시글 목록 조회
        Page<PostSearchResult> result = userToPostAdapter.findPostsByUserId(userId, pageable);

        // Then: 빈 페이지가 올바르게 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(postQueryUseCase).getUserPosts(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("경계값 - 빈 추천 게시글 목록 조회")
    void shouldHandleEmptyLikedPosts_WhenNoLikedPostsFound() {
        // Given: 추천 게시글이 없는 사용자 ID와 빈 페이지 결과
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        given(postQueryUseCase.getUserLikedPosts(eq(userId), any(Pageable.class))).willReturn(emptyPage);

        // When: 추천 게시글이 없는 사용자의 추천 게시글 목록 조회
        Page<PostSearchResult> result = userToPostAdapter.findLikedPostsByUserId(userId, pageable);

        // Then: 빈 페이지가 올바르게 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(postQueryUseCase).getUserLikedPosts(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("예외 케이스 - null 사용자 ID로 게시글 조회")
    void shouldHandleNullUserId_WhenNullUserIdProvided() {
        // Given: null 사용자 ID와 빈 페이지 반환 설정
        Long nullUserId = null;
        Pageable pageable = PageRequest.of(0, 10);
        
        // 비즈니스 로직: null userId도 처리 가능하다면 빈 페이지 반환
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        given(postQueryUseCase.getUserPosts(eq(nullUserId), eq(pageable))).willReturn(emptyPage);

        // When: null 사용자 ID로 게시글 조회 실행
        Page<PostSearchResult> result = userToPostAdapter.findPostsByUserId(nullUserId, pageable);

        // Then: 빈 결과 반환 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(postQueryUseCase).getUserPosts(eq(nullUserId), eq(pageable));
    }

    @Test
    @DisplayName("예외 케이스 - null 페이지로 게시글 조회")
    void shouldHandleNullPageable_WhenNullPageableProvided() {
        // Given: 정상 사용자 ID와 null 페이지 정보, 빈 페이지 반환 설정
        Long userId = 1L;
        Pageable nullPageable = null;
        
        // 비즈니스 로직: null pageable도 처리 가능하다면 빈 페이지 반환
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        given(postQueryUseCase.getUserPosts(eq(userId), eq(nullPageable))).willReturn(emptyPage);

        // When: null 페이지 정보로 게시글 조회 실행
        Page<PostSearchResult> result = userToPostAdapter.findPostsByUserId(userId, nullPageable);

        // Then: 빈 결과 반환 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(postQueryUseCase).getUserPosts(eq(userId), eq(nullPageable));
    }

    @Test
    @DisplayName("예외 케이스 - null 사용자 ID로 추천 게시글 조회")
    void shouldHandleNullUserIdForLikedPosts_WhenNullUserIdProvided() {
        // Given: null 사용자 ID와 빈 페이지 반환 설정
        Long nullUserId = null;
        Pageable pageable = PageRequest.of(0, 10);
        
        // 비즈니스 로직: null userId도 처리 가능하다면 빈 페이지 반환
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        given(postQueryUseCase.getUserLikedPosts(eq(nullUserId), eq(pageable))).willReturn(emptyPage);

        // When: null 사용자 ID로 추천 게시글 조회 실행
        Page<PostSearchResult> result = userToPostAdapter.findLikedPostsByUserId(nullUserId, pageable);

        // Then: 빈 결과 반환 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(postQueryUseCase).getUserLikedPosts(eq(nullUserId), eq(pageable));
    }

    @Test
    @DisplayName("성능 - 대용량 게시글 목록 조회")
    void shouldHandleLargePostList_WhenLargePageRequested() {
        // Given: 대용량 페이지 요청
        Long userId = 1L;
        Pageable largePage = PageRequest.of(0, 1000);
        
        List<PostSearchResult> largePostList = Collections.nCopies(1000, 
            PostSearchResult.builder()
                .id(1L)
                .title("대용량 테스트 게시글")
                .content("대용량 테스트 내용")
                .userName("테스트작성자")
                .userId(userId)
                .createdAt(Instant.now())
                .likeCount(1)
                .commentCount(0)
                .viewCount(1)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .isNotice(false)
                .build());
        
        Page<PostSearchResult> largePage_ = new PageImpl<>(largePostList, largePage, 1000);
        given(postQueryUseCase.getUserPosts(eq(userId), any(Pageable.class))).willReturn(largePage_);

        // When: 대용량 게시글 목록 조회
        Page<PostSearchResult> result = userToPostAdapter.findPostsByUserId(userId, largePage);

        // Then: 대용량 데이터도 올바르게 처리되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1000);
        assertThat(result.getTotalElements()).isEqualTo(1000);
        verify(postQueryUseCase).getUserPosts(eq(userId), eq(largePage));
    }

    @Test
    @DisplayName("통합 - 게시글과 추천 게시글 모두 조회")
    void shouldHandleBothPostTypes_WhenBothMethodsCalled() {
        // Given: 동일한 사용자에 대한 게시글과 추천 게시글 설정
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        // 작성 게시글
        Page<PostSearchResult> userPosts = new PageImpl<>(Collections.singletonList(
                PostSearchResult.builder()
                    .id(1L)
                    .title("작성 게시글")
                    .content("작성한 게시글 내용")
                    .userName("testUser")
                    .userId(userId)
                    .createdAt(Instant.now())
                    .likeCount(1)
                    .commentCount(0)
                    .viewCount(5)
                    .postCacheFlag(PostCacheFlag.REALTIME)
                    .isNotice(false)
                    .build()
        ), pageable, 1);
        
        // 추천 게시글
        Page<PostSearchResult> likedPosts = new PageImpl<>(Collections.singletonList(
                PostSearchResult.builder()
                    .id(2L)
                    .title("추천 게시글")
                    .content("추천한 게시글 내용")
                    .userName("otherUser")
                    .userId(2L)
                    .createdAt(Instant.now())
                    .likeCount(10)
                    .commentCount(3)
                    .viewCount(50)
                    .postCacheFlag(PostCacheFlag.WEEKLY)
                    .isNotice(false)
                    .build()
        ), pageable, 1);
        
        given(postQueryUseCase.getUserPosts(eq(userId), any(Pageable.class))).willReturn(userPosts);
        given(postQueryUseCase.getUserLikedPosts(eq(userId), any(Pageable.class))).willReturn(likedPosts);

        // When: 두 메서드 모두 호출
        Page<PostSearchResult> userPostsResult = userToPostAdapter.findPostsByUserId(userId, pageable);
        Page<PostSearchResult> likedPostsResult = userToPostAdapter.findLikedPostsByUserId(userId, pageable);

        // Then: 각각 올바른 결과가 반환되는지 검증
        assertThat(userPostsResult.getContent().get(0).getTitle()).isEqualTo("작성 게시글");
        assertThat(likedPostsResult.getContent().get(0).getTitle()).isEqualTo("추천 게시글");
        
        verify(postQueryUseCase).getUserPosts(eq(userId), eq(pageable));
        verify(postQueryUseCase).getUserLikedPosts(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("데이터 매핑 - PostSearchResult에서 PostSearchResult로 변환 검증")
    void shouldCorrectlyMapData_WhenConvertingFromPostSearchResultToPostSearchResult() {
        // Given: 상세한 PostSearchResult 데이터
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 1);
        
        PostSearchResult postSearchResult = PostSearchResult.builder()
                .id(100L)
                .title("매핑 테스트 게시글")
                .content("매핑 테스트 내용")
                .userName("매핑테스트유저")
                .userId(userId)
                .createdAt(Instant.parse("2023-12-25T10:30:00Z"))
                .likeCount(25)
                .commentCount(15)
                .viewCount(100)
                .postCacheFlag(PostCacheFlag.LEGEND)
                .isNotice(true)
                .build();
        
        Page<PostSearchResult> postPage = new PageImpl<>(Collections.singletonList(postSearchResult), pageable, 1);
        given(postQueryUseCase.getUserPosts(eq(userId), any(Pageable.class))).willReturn(postPage);

        // When: 어댑터를 통해 변환 실행
        Page<PostSearchResult> result = userToPostAdapter.findPostsByUserId(userId, pageable);

        // Then: 모든 필드가 올바르게 매핑되었는지 검증
        assertThat(result.getContent()).hasSize(1);
        PostSearchResult dto = result.getContent().get(0);
        
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("매핑 테스트 게시글");
        assertThat(dto.getContent()).isEqualTo("매핑 테스트 내용");
        assertThat(dto.getUserName()).isEqualTo("매핑테스트유저");
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getCreatedAt()).isEqualTo(Instant.parse("2023-12-25T10:30:00Z"));
        assertThat(dto.getLikeCount()).isEqualTo(25);
        assertThat(dto.getCommentCount()).isEqualTo(15);
        assertThat(dto.getViewCount()).isEqualTo(100);
        assertThat(dto.getPostCacheFlag()).isEqualTo(PostCacheFlag.LEGEND);
        assertThat(dto.isNotice()).isTrue();
    }
}
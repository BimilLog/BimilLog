package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.out.LoadCommentPort;
import jaeik.growfarm.domain.user.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.comment.entity.SimpleCommentInfo;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserActivityService 테스트</h2>
 * <p>사용자 활동 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>순환참조 해결을 위해 UserQueryServiceTest에서 분리된 활동 관련 테스트</p>
 * <p>헥사고날 아키텍처 원칙에 따라 모든 외부 의존성을 Mock으로 격리하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityService 테스트")
class UserActivityServiceTest {

    @Mock
    private LoadPostPort loadPostPort;
    
    @Mock
    private LoadCommentPort loadCommentPort;

    @InjectMocks
    private UserActivityService userActivityService;

    @Test
    @DisplayName("사용자 작성 게시글 목록 조회 - 정상 케이스")
    void shouldGetUserPosts_WhenValidUserId() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<SimplePostResDTO> posts = Arrays.asList(
                SimplePostResDTO.builder()
                        .id(1L)
                        .title("게시글 1")
                        .content("내용 1")
                        .createdAt(Instant.now())
                        .build(),
                SimplePostResDTO.builder()
                        .id(2L)
                        .title("게시글 2")
                        .content("내용 2")
                        .createdAt(Instant.now())
                        .build()
        );
        
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(posts, pageable, posts.size());

        given(loadPostPort.findPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = userActivityService.getUserPosts(userId, pageable);

        // Then
        verify(loadPostPort).findPostsByUserId(userId, pageable);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("게시글 2");
    }

    @Test
    @DisplayName("사용자 추천한 게시글 목록 조회 - 정상 케이스")
    void shouldGetUserLikedPosts_WhenValidUserId() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<SimplePostResDTO> likedPosts = Arrays.asList(
                SimplePostResDTO.builder()
                        .id(3L)
                        .title("추천한 게시글 1")
                        .content("내용 3")
                        .createdAt(Instant.now())
                        .build()
        );
        
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(likedPosts, pageable, likedPosts.size());

        given(loadPostPort.findLikedPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = userActivityService.getUserLikedPosts(userId, pageable);

        // Then
        verify(loadPostPort).findLikedPostsByUserId(userId, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("추천한 게시글 1");
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회 - 정상 케이스")
    void shouldGetUserComments_WhenValidUserId() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<SimpleCommentInfo> comments = Arrays.asList(
                SimpleCommentInfo.builder()
                        .id(1L)
                        .postId(1L)
                        .userName("testUser")
                        .content("댓글 1")
                        .createdAt(Instant.now())
                        .likeCount(0)
                        .userLike(false)
                        .build(),
                SimpleCommentInfo.builder()
                        .id(2L)
                        .postId(1L)
                        .userName("testUser")
                        .content("댓글 2")
                        .createdAt(Instant.now())
                        .likeCount(0)
                        .userLike(false)
                        .build()
        );
        
        Page<SimpleCommentInfo> expectedPage = new PageImpl<>(comments, pageable, comments.size());

        given(loadCommentPort.findCommentsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimpleCommentInfo> result = userActivityService.getUserComments(userId, pageable);

        // Then
        verify(loadCommentPort).findCommentsByUserId(userId, pageable);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("댓글 1");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("댓글 2");
    }

    @Test
    @DisplayName("사용자 추천한 댓글 목록 조회 - 정상 케이스")
    void shouldGetUserLikedComments_WhenValidUserId() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<SimpleCommentInfo> likedComments = Arrays.asList(
                SimpleCommentInfo.builder()
                        .id(3L)
                        .postId(1L)
                        .userName("testUser")
                        .content("추천한 댓글 1")
                        .createdAt(Instant.now())
                        .likeCount(0)
                        .userLike(false)
                        .build()
        );
        
        Page<SimpleCommentInfo> expectedPage = new PageImpl<>(likedComments, pageable, likedComments.size());

        given(loadCommentPort.findLikedCommentsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimpleCommentInfo> result = userActivityService.getUserLikedComments(userId, pageable);

        // Then
        verify(loadCommentPort).findLikedCommentsByUserId(userId, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("추천한 댓글 1");
    }

    @Test
    @DisplayName("빈 게시글 목록 조회")
    void shouldReturnEmptyPage_WhenNoUserPosts() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimplePostResDTO> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(loadPostPort.findPostsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimplePostResDTO> result = userActivityService.getUserPosts(userId, pageable);

        // Then
        verify(loadPostPort).findPostsByUserId(userId, pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("빈 댓글 목록 조회")
    void shouldReturnEmptyPage_WhenNoUserComments() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimpleCommentInfo> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(loadCommentPort.findCommentsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimpleCommentInfo> result = userActivityService.getUserComments(userId, pageable);

        // Then
        verify(loadCommentPort).findCommentsByUserId(userId, pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("빈 추천 게시글 목록 조회")
    void shouldReturnEmptyPage_WhenNoUserLikedPosts() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimplePostResDTO> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(loadPostPort.findLikedPostsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimplePostResDTO> result = userActivityService.getUserLikedPosts(userId, pageable);

        // Then
        verify(loadPostPort).findLikedPostsByUserId(userId, pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("빈 추천 댓글 목록 조회")
    void shouldReturnEmptyPage_WhenNoUserLikedComments() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimpleCommentInfo> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(loadCommentPort.findLikedCommentsByUserId(userId, pageable)).willReturn(emptyPage);

        // When
        Page<SimpleCommentInfo> result = userActivityService.getUserLikedComments(userId, pageable);

        // Then
        verify(loadCommentPort).findLikedCommentsByUserId(userId, pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("대용량 게시글 목록 조회")
    void shouldHandleLargePostList_WhenManyUserPosts() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        
        // TODO: 테스트 실패 해결 - ArrayList 사용으로 mutable list 생성
        // 기존: Arrays.asList()는 immutable이므로 add() 호출 시 UnsupportedOperationException 발생
        // 수정: ArrayList 사용으로 동적 크기 변경 가능한 리스트 생성
        List<SimplePostResDTO> posts = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            posts.add(SimplePostResDTO.builder()
                    .id((long) i)
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .createdAt(Instant.now())
                    .build());
        }
        
        Page<SimplePostResDTO> expectedPage = new PageImpl<>(posts, pageable, posts.size());

        given(loadPostPort.findPostsByUserId(userId, pageable)).willReturn(expectedPage);

        // When
        Page<SimplePostResDTO> result = userActivityService.getUserPosts(userId, pageable);

        // Then
        verify(loadPostPort).findPostsByUserId(userId, pageable);
        assertThat(result.getContent()).hasSize(posts.size());
        assertThat(result.getTotalElements()).isEqualTo(posts.size());
    }

    @Test
    @DisplayName("페이지네이션 검증")
    void shouldHandlePagination_Correctly() {
        // Given
        Long userId = 1L;
        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);
        
        List<SimplePostResDTO> firstPagePosts = Arrays.asList(
                SimplePostResDTO.builder().id(1L).title("게시글 1").build(),
                SimplePostResDTO.builder().id(2L).title("게시글 2").build(),
                SimplePostResDTO.builder().id(3L).title("게시글 3").build(),
                SimplePostResDTO.builder().id(4L).title("게시글 4").build(),
                SimplePostResDTO.builder().id(5L).title("게시글 5").build()
        );
        
        List<SimplePostResDTO> secondPagePosts = Arrays.asList(
                SimplePostResDTO.builder().id(6L).title("게시글 6").build(),
                SimplePostResDTO.builder().id(7L).title("게시글 7").build()
        );
        
        Page<SimplePostResDTO> firstPageResult = new PageImpl<>(firstPagePosts, firstPage, 7);
        Page<SimplePostResDTO> secondPageResult = new PageImpl<>(secondPagePosts, secondPage, 7);

        given(loadPostPort.findPostsByUserId(userId, firstPage)).willReturn(firstPageResult);
        given(loadPostPort.findPostsByUserId(userId, secondPage)).willReturn(secondPageResult);

        // When
        Page<SimplePostResDTO> page1 = userActivityService.getUserPosts(userId, firstPage);
        Page<SimplePostResDTO> page2 = userActivityService.getUserPosts(userId, secondPage);

        // Then
        verify(loadPostPort).findPostsByUserId(userId, firstPage);
        verify(loadPostPort).findPostsByUserId(userId, secondPage);
        
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(7);
        assertThat(page2.getTotalElements()).isEqualTo(7);
    }
}
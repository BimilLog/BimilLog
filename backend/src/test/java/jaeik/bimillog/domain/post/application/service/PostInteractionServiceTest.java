package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.out.*;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>PostInteractionService 테스트</h2>
 * <p>게시글 상호작용 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>추천/추천취소, 조회수 증가 등의 다양한 시나리오를 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostInteractionService 테스트")
class PostInteractionServiceTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @Mock
    private PostLikeCommandPort postLikeCommandPort;

    @Mock
    private PostLikeQueryPort postLikeQueryPort;

    @Mock
    private LoadUserInfoPort loadUserInfoPort;

    @Mock
    private User user;

    @Mock
    private Post post;

    @InjectMocks
    private PostInteractionService postInteractionService;

    @Test
    @DisplayName("게시글 추천 - 처음 추천하는 경우")
    void shouldAddLike_WhenFirstTimeLiking() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(false);

        // When
        postInteractionService.likePost(userId, postId);

        // Then
        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        
        // ArgumentCaptor로 PostLike 객체 검증
        ArgumentCaptor<PostLike> postLikeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeCommandPort).save(postLikeCaptor.capture());
        PostLike savedPostLike = postLikeCaptor.getValue();
        assertThat(savedPostLike.getUser()).isEqualTo(user);
        assertThat(savedPostLike.getPost()).isEqualTo(post);
        
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("게시글 추천 - 이미 추천한 경우 (추천 취소)")
    void shouldRemoveLike_WhenAlreadyLiked() {
        // Given
        Long userId = 1L;
        Long postId = 123L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));
        given(postLikeQueryPort.existsByUserAndPost(user, post)).willReturn(true);

        // When
        postInteractionService.likePost(userId, postId);

        // Then
        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort).existsByUserAndPost(user, post);
        verify(postLikeCommandPort).deleteByUserAndPost(user, post);
        verify(postLikeCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("게시글 추천 - 존재하지 않는 게시글인 경우")
    void shouldThrowException_WhenLikingNonExistentPost() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);
        given(postQueryPort.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postInteractionService.likePost(userId, postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(loadUserInfoPort).getReferenceById(userId);
        verify(postQueryPort).findById(postId);
        verify(postLikeQueryPort, never()).existsByUserAndPost(any(), any());
        verify(postLikeCommandPort, never()).save(any());
        verify(postLikeCommandPort, never()).deleteByUserAndPost(any(), any());
    }

    @Test
    @DisplayName("조회수 증가 - 정상적인 게시글")
    void shouldIncrementViewCount_WhenValidPost() {
        // Given
        Long postId = 123L;

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postCommandPort).incrementViewByPostId(postId);
    }

    @Test
    @DisplayName("조회수 증가 - 존재하지 않는 게시글이어도 DB에서 처리됨")
    void shouldIncrementViewCount_EvenForNonExistentPost() {
        // Given
        Long postId = 999L;

        // When
        postInteractionService.incrementViewCount(postId);

        // Then
        verify(postCommandPort).incrementViewByPostId(postId);
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 사용자가 동시에 같은 게시글 추천")
    void shouldHandleConcurrentLikeOperations_OnSamePost() throws InterruptedException {
        // Given
        Long postId = 123L;
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 각 스레드마다 다른 사용자 ID
        given(postQueryPort.findById(postId)).willReturn(Optional.of(post));

        try {
            // When - 여러 스레드가 동시에 추천 작업 수행
            for (int i = 0; i < threadCount; i++) {
                final Long userId = (long) (i + 1);
                final User threadUser = mock(User.class);
                
                given(loadUserInfoPort.getReferenceById(userId)).willReturn(threadUser);
                given(postLikeQueryPort.existsByUserAndPost(threadUser, post)).willReturn(false);
                
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                        postInteractionService.likePost(userId, postId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 발생해도 계속 진행
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 모든 스레드 시작
            endLatch.await(); // 모든 스레드 완료 대기

            // Then - 모든 작업이 성공적으로 완료되어야 함
            assertThat(successCount.get()).isEqualTo(threadCount);
            verify(postQueryPort, times(threadCount)).findById(postId);
            verify(postLikeCommandPort, times(threadCount)).save(any(PostLike.class));
            
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("동시성 테스트 - 같은 사용자가 동시에 여러 게시글에 추천")
    void shouldHandleConcurrentLikeOperations_OnDifferentPosts() throws InterruptedException {
        // Given
        Long userId = 1L;
        int postCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(postCount);
        ExecutorService executor = Executors.newFixedThreadPool(postCount);
        AtomicInteger successCount = new AtomicInteger(0);

        given(loadUserInfoPort.getReferenceById(userId)).willReturn(user);

        try {
            // When - 여러 스레드가 동시에 다른 게시글에 추천 작업 수행
            for (int i = 0; i < postCount; i++) {
                final Long postId = (long) (i + 100);
                final Post threadPost = mock(Post.class);
                
                given(postQueryPort.findById(postId)).willReturn(Optional.of(threadPost));
                given(postLikeQueryPort.existsByUserAndPost(user, threadPost)).willReturn(false);
                
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                        postInteractionService.likePost(userId, postId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 발생해도 계속 진행
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 모든 스레드 시작
            endLatch.await(); // 모든 스레드 완료 대기

            // Then - 모든 작업이 성공적으로 완료되어야 함
            assertThat(successCount.get()).isEqualTo(postCount);
            verify(loadUserInfoPort, times(postCount)).getReferenceById(userId);
            verify(postLikeCommandPort, times(postCount)).save(any(PostLike.class));
            
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("동시성 테스트 - 조회수 증가 동시 처리")
    void shouldHandleConcurrentViewCountIncrements() throws InterruptedException {
        // Given
        Long postId = 456L;
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        try {
            // When - 여러 스레드가 동시에 조회수 증가 작업 수행
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                        postInteractionService.incrementViewCount(postId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 예외 발생해도 계속 진행
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 모든 스레드 시작
            endLatch.await(); // 모든 스레드 완료 대기

            // Then - 모든 조회수 증가 작업이 성공적으로 완료되어야 함
            assertThat(successCount.get()).isEqualTo(threadCount);
            verify(postCommandPort, times(threadCount)).incrementViewByPostId(postId);
            
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("비동기 작업 테스트 - CompletableFuture를 이용한 병렬 처리")
    void shouldHandleAsynchronousOperations() {
        // Given
        Long postId1 = 100L;
        Long postId2 = 200L;
        Long postId3 = 300L;
        
        // When - CompletableFuture로 비동기 조회수 증가
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> 
            postInteractionService.incrementViewCount(postId1));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> 
            postInteractionService.incrementViewCount(postId2));
        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> 
            postInteractionService.incrementViewCount(postId3));
        
        // 모든 비동기 작업 완료 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        allFutures.join(); // 모든 작업 완료까지 대기
        
        // Then - 모든 조회수 증가가 실행되어야 함
        verify(postCommandPort).incrementViewByPostId(postId1);
        verify(postCommandPort).incrementViewByPostId(postId2);
        verify(postCommandPort).incrementViewByPostId(postId3);
    }
}
package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

/**
 * <h2>PostNoticeListener 테스트</h2>
 * <p>게시글 공지 설정/해제 이벤트 리스너의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>이벤트 처리, 캐시 무효화, 비동기 동작, 로그 출력을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostNoticeListener 테스트")
class PostNoticeListenerTest {

    @Mock
    private PostCacheUseCase postCacheUseCase;

    @InjectMocks
    private PostNoticeListener postNoticeListener;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PostNoticeListener.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("게시글 공지 설정 이벤트 처리 - 성공")
    void shouldHandlePostSetAsNoticeEvent_Successfully() {
        // Given
        Long postId = 123L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);

        // When
        assertDoesNotThrow(() -> postNoticeListener.handlePostSetAsNotice(event));

        // Then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).addNoticeToCache(postId);
        });
        
        // 로그 검증
        assertThat(listAppender.list)
                .isNotEmpty()
                .anySatisfy(logEvent -> {
                    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
                    assertThat(logEvent.getFormattedMessage())
                            .contains("게시글 공지사항 설정 이벤트 수신: postId=" + postId)
                            .contains("캐시에 공지사항 추가 중");
                });
    }

    @Test
    @DisplayName("게시글 공지 해제 이벤트 처리 - 성공")
    void shouldHandlePostUnsetAsNoticeEvent_Successfully() {
        // Given
        Long postId = 456L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);

        // When
        assertDoesNotThrow(() -> postNoticeListener.handlePostUnsetAsNotice(event));

        // Then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).removeNoticeFromCache(postId);
        });
        
        // 로그 검증
        assertThat(listAppender.list)
                .isNotEmpty()
                .anySatisfy(logEvent -> {
                    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
                    assertThat(logEvent.getFormattedMessage())
                            .contains("게시글 공지사항 해제 이벤트 수신: postId=" + postId)
                            .contains("캐시에서 공지사항 제거 중");
                });
    }

    @Test
    @DisplayName("게시글 공지 설정 이벤트 처리 - 캐시 추가 실패 시 예외 전파하지 않음")
    void shouldNotPropagateException_WhenCacheAddFails() {
        // Given
        Long postId = 789L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);
        
        doThrow(new RuntimeException("Cache add failed")).when(postCacheUseCase).addNoticeToCache(postId);

        // When & Then
        assertDoesNotThrow(() -> postNoticeListener.handlePostSetAsNotice(event));
        
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).addNoticeToCache(postId);
        });
    }

    @Test
    @DisplayName("게시글 공지 해제 이벤트 처리 - 캐시 제거 실패 시 예외 전파하지 않음")
    void shouldNotPropagateException_WhenCacheRemoveFailsOnUnset() {
        // Given
        Long postId = 999L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);
        doThrow(new RuntimeException("Cache remove failed")).when(postCacheUseCase).removeNoticeFromCache(postId);

        // When & Then
        assertDoesNotThrow(() -> postNoticeListener.handlePostUnsetAsNotice(event));
        
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).removeNoticeFromCache(postId);
        });
    }

    @Test
    @DisplayName("이벤트 비동기 처리 검증")
    void shouldHandleEventsAsynchronously() {
        // Given
        PostSetAsNoticeEvent event1 = new PostSetAsNoticeEvent(111L);
        PostUnsetAsNoticeEvent event2 = new PostUnsetAsNoticeEvent(222L);

        // When
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> 
            postNoticeListener.handlePostSetAsNotice(event1));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> 
            postNoticeListener.handlePostUnsetAsNotice(event2));

        // Then
        assertDoesNotThrow(() -> {
            CompletableFuture.allOf(future1, future2).get(2, TimeUnit.SECONDS);
        });

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).addNoticeToCache(111L);
            verify(postCacheUseCase).removeNoticeFromCache(222L);
        });
    }
}
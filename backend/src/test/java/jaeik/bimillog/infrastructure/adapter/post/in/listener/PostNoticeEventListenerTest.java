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
 * <h2>PostNoticeEventListener 테스트</h2>
 * <p>게시글 공지 설정/해제 이벤트 리스너의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>이벤트 처리, 캐시 무효화, 비동기 동작, 로그 출력을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostNoticeEventListener 테스트")
class PostNoticeEventListenerTest {

    @Mock
    private PostCacheUseCase postCacheUseCase;

    @InjectMocks
    private PostNoticeEventListener postNoticeEventListener;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(PostNoticeEventListener.class);
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
        assertDoesNotThrow(() -> postNoticeEventListener.handlePostSetAsNotice(event));

        // Then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).deleteNoticeCache();
        });
        
        // 로그 검증
        assertThat(listAppender.list)
                .isNotEmpty()
                .anySatisfy(logEvent -> {
                    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
                    assertThat(logEvent.getFormattedMessage())
                            .contains("Post (ID: " + postId + ") set as notice event received")
                            .contains("Deleting notice cache");
                });
    }

    @Test
    @DisplayName("게시글 공지 해제 이벤트 처리 - 성공")
    void shouldHandlePostUnsetAsNoticeEvent_Successfully() {
        // Given
        Long postId = 456L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);

        // When
        assertDoesNotThrow(() -> postNoticeEventListener.handlePostUnsetAsNotice(event));

        // Then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).deleteNoticeCache();
        });
        
        // 로그 검증
        assertThat(listAppender.list)
                .isNotEmpty()
                .anySatisfy(logEvent -> {
                    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
                    assertThat(logEvent.getFormattedMessage())
                            .contains("Post (ID: " + postId + ") unset as notice event received")
                            .contains("Deleting notice cache");
                });
    }

    @Test
    @DisplayName("게시글 공지 설정 이벤트 처리 - 캐시 삭제 실패 시 예외 전파하지 않음")
    void shouldNotPropagateException_WhenCacheDeleteFails() {
        // Given
        Long postId = 789L;
        PostSetAsNoticeEvent event = new PostSetAsNoticeEvent(postId);
        doThrow(new RuntimeException("Cache delete failed")).when(postCacheUseCase).deleteNoticeCache();

        // When & Then
        assertDoesNotThrow(() -> postNoticeEventListener.handlePostSetAsNotice(event));
        
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).deleteNoticeCache();
        });
    }

    @Test
    @DisplayName("게시글 공지 해제 이벤트 처리 - 캐시 삭제 실패 시 예외 전파하지 않음")
    void shouldNotPropagateException_WhenCacheDeleteFailsOnUnset() {
        // Given
        Long postId = 999L;
        PostUnsetAsNoticeEvent event = new PostUnsetAsNoticeEvent(postId);
        doThrow(new RuntimeException("Cache delete failed")).when(postCacheUseCase).deleteNoticeCache();

        // When & Then
        assertDoesNotThrow(() -> postNoticeEventListener.handlePostUnsetAsNotice(event));
        
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase).deleteNoticeCache();
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
            postNoticeEventListener.handlePostSetAsNotice(event1));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> 
            postNoticeEventListener.handlePostUnsetAsNotice(event2));

        // Then
        assertDoesNotThrow(() -> {
            CompletableFuture.allOf(future1, future2).get(2, TimeUnit.SECONDS);
        });

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(postCacheUseCase, times(2)).deleteNoticeCache();
        });
    }
}
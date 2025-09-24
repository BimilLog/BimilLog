package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.infrastructure.adapter.out.sse.EmitterRepository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * <h2>SSE 테스트 헬퍼</h2>
 * <p>SSE 관련 테스트에서 반복되는 보일러플레이트 코드를 제거하기 위한 유틸리티</p>
 * <p>Emitter Mock 설정, EmitterId 생성, SSE 메시지 생성 등의 공통 기능 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class SseTestHelper {

    /**
     * EmitterId 생성 헬퍼
     * 형식: userId_tokenId_timestamp
     * 
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @param timestamp 타임스탬프
     * @return 생성된 EmitterId
     */
    public static String createEmitterId(Long userId, Long tokenId, long timestamp) {
        return String.format("%d_%d_%d", userId, tokenId, timestamp);
    }

    /**
     * 기본 EmitterId 생성 (현재 시간 사용)
     * 
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @return 생성된 EmitterId
     */
    public static String createEmitterId(Long userId, Long tokenId) {
        return createEmitterId(userId, tokenId, System.currentTimeMillis());
    }

    /**
     * 테스트용 기본 EmitterId 생성
     * 
     * @param userId 사용자 ID
     * @return 생성된 EmitterId (tokenId=100, timestamp=1234567890)
     */
    public static String defaultEmitterId(Long userId) {
        return createEmitterId(userId, 100L, 1234567890L);
    }

    /**
     * SseEmitter Mock 생성
     * 
     * @return Mock SseEmitter
     */
    public static SseEmitter createMockEmitter() {
        return mock(SseEmitter.class);
    }


    /**
     * EmitterRepository Mock 설정 헬퍼
     * 
     * @param repository Mock EmitterRepository
     * @param userId 사용자 ID
     * @param emitters 반환할 Emitter Map
     */
    public static void setupEmitterRepository(EmitterRepository repository,
                                             Long userId,
                                             Map<String, SseEmitter> emitters) {
        given(repository.findAllEmitterByUserId(userId)).willReturn(emitters);
    }

    /**
     * 단일 Emitter를 가진 EmitterRepository Mock 설정
     * 
     * @param repository Mock EmitterRepository
     * @param userId 사용자 ID
     * @param emitterId Emitter ID
     * @param emitter SseEmitter
     */
    public static void setupSingleEmitter(EmitterRepository repository,
                                         Long userId,
                                         String emitterId,
                                         SseEmitter emitter) {
        Map<String, SseEmitter> emitters = new HashMap<>();
        emitters.put(emitterId, emitter);
        setupEmitterRepository(repository, userId, emitters);
    }

    /**
     * 빈 EmitterRepository Mock 설정 (Emitter가 없는 경우)
     * 
     * @param repository Mock EmitterRepository
     * @param userId 사용자 ID
     */
    public static void setupEmptyRepository(EmitterRepository repository, Long userId) {
        given(repository.findAllEmitterByUserId(userId)).willReturn(new HashMap<>());
    }

    /**
     * EmitterRepository save 동작 설정
     * 
     * @param repository Mock EmitterRepository
     */
    public static void setupEmitterSave(EmitterRepository repository) {
        given(repository.save(anyString(), any(SseEmitter.class)))
            .willAnswer(invocation -> invocation.getArgument(1));
    }

    /**
     * SSE 메시지 생성 빌더
     */
    public static class SseMessageBuilder {
        private Long userId = 1L;
        private NotificationType type = NotificationType.COMMENT;
        private String message = "테스트 메시지";
        private String url = "/test/url";

        public SseMessageBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public SseMessageBuilder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public SseMessageBuilder message(String message) {
            this.message = message;
            return this;
        }

        public SseMessageBuilder url(String url) {
            this.url = url;
            return this;
        }

        public SseMessage build() {
            return SseMessage.of(userId, type, message, url);
        }
    }

    /**
     * SseMessage 빌더 시작
     * @return SseMessageBuilder
     */
    public static SseMessageBuilder sseMessage() {
        return new SseMessageBuilder();
    }

    /**
     * 기본 SSE 메시지 생성
     * @return 기본값이 설정된 SseMessage
     */
    public static SseMessage defaultSseMessage() {
        return sseMessage().build();
    }

    /**
     * 댓글 알림 SSE 메시지 생성
     * 
     * @param userId 사용자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId 게시글 ID
     * @return SseMessage
     */
    public static SseMessage commentMessage(Long userId, String commenterName, Long postId) {
        return sseMessage()
            .userId(userId)
            .type(NotificationType.COMMENT)
            .message(commenterName + "님이 댓글을 남겼습니다!")
            .url("/board/post/" + postId)
            .build();
    }

    /**
     * 롤링페이퍼 알림 SSE 메시지 생성
     * 
     * @param userId 사용자 ID
     * @param userName 사용자명
     * @return SseMessage
     */
    public static SseMessage paperMessage(Long userId, String userName) {
        return sseMessage()
            .userId(userId)
            .type(NotificationType.PAPER)
            .message("롤링페이퍼에 메시지가 작성되었어요!")
            .url("/rolling-paper/" + userName)
            .build();
    }

    /**
     * 다중 기기 시나리오를 위한 Emitter Map 생성
     * 
     * @param userId 사용자 ID
     * @param deviceCount 기기 수
     * @return 기기별 Emitter Map
     */
    public static Map<String, SseEmitter> createMultiDeviceEmitters(Long userId, int deviceCount) {
        Map<String, SseEmitter> emitters = new HashMap<>();
        for (int i = 0; i < deviceCount; i++) {
            String emitterId = createEmitterId(userId, 100L + i, 1234567890L + i);
            emitters.put(emitterId, createMockEmitter());
        }
        return emitters;
    }

    /**
     * EmitterRepository에서 특정 사용자의 모든 Emitter 삭제 검증
     * 
     * @param repository Mock EmitterRepository
     * @param userId 사용자 ID
     */
    public static void verifyDeleteAllEmitters(EmitterRepository repository, Long userId) {
        verify(repository).deleteAllEmitterByUserId(userId);
    }
}
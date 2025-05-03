package jaeik.growfarm.repository.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/*
 * SseEmitter Repository
 * SseEmitter 관련 메모리 작업을 정의하는 인터페이스
 * 수정일 : 2025-05-03
 */
public interface EmitterRepository {

    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    Map<String, SseEmitter> findAllEmitterByUserId(Long userId);

    void deleteById(String emitterId);

    void deleteAllEmitterByUserId(Long userId);

}

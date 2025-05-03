package jaeik.growfarm.repository.notification;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/*
 * SseEmitter Repository 구현체
 * SseEmitter 관련 메모리 작업을 구현하는 클래스
 * 수정일 : 2025-05-03
 */
@Repository
public class EmitterRepositoryImpl implements EmitterRepository {
    // 사용자 별 emitterId와 sseEmitter 객체를 저장하는 map
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 사용자 별 임의 emitterId 와 sseEmitter 객체를 emitter에 저장
    // emitterId는 userId와 현재시간을 조합하여 생성
    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    // 사용자 ID로 시작하는 데이터를 해당 map에서 찾아 반환
    @Override
    public Map<String, SseEmitter> findAllEmitterByUserId(Long userId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId.toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 특정 emitterId에 해당하는 데이터를 삭제
    @Override
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    // 사용자 ID로 시작하는 emitterId를 찾아 삭제
    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        List<String> emitterIds = emitters.keySet().stream()
                .filter(key -> key.startsWith(userId.toString()))
                .toList();

        emitterIds.forEach(emitters::remove);
    }

}

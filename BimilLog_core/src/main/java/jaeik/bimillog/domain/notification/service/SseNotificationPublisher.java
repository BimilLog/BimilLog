package jaeik.bimillog.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * <h2>SSE 알림 Redis Pub/Sub 발행기</h2>
 * <p>스케일아웃 환경에서 SSE 알림을 모든 인스턴스로 팬아웃하기 위한 발행기</p>
 * <p>각 인스턴스의 구독자가 수신 후, 해당 사용자의 이미터를 보유한 인스턴스만 실제 전송합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseNotificationPublisher {

    public static final String CHANNEL = "sse:notification";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(SseMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CHANNEL, payload);
        } catch (JsonProcessingException e) {
            log.error("SSE Pub/Sub 직렬화 실패: memberId={}, type={}", message.memberId(), message.type(), e);
            throw new IllegalStateException("SSE Pub/Sub 직렬화 실패", e);
        }
    }
}

package jaeik.bimillog.domain.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.repository.SseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * <h2>SSE 알림 Redis Pub/Sub 구독자</h2>
 * <p>모든 인스턴스가 구독하며, 수신 시 로컬 이미터에 해당 사용자가 있을 때만 실제 전송합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseNotificationSubscriber implements MessageListener {

    private final SseRepository sseRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            SseMessage sseMessage = objectMapper.readValue(message.getBody(), SseMessage.class);
            if (!sseRepository.hasEmitter(sseMessage.memberId())) {
                return;
            }
            sseRepository.send(sseMessage);
        } catch (Exception e) {
            log.error("SSE Pub/Sub 수신 처리 실패", e);
        }
    }
}

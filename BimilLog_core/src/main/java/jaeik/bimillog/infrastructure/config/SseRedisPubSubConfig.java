package jaeik.bimillog.infrastructure.config;

import jaeik.bimillog.domain.notification.listener.SseNotificationSubscriber;
import jaeik.bimillog.domain.notification.service.SseNotificationPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * <h2>SSE 알림 Redis Pub/Sub 설정</h2>
 * <p>스케일아웃 환경에서 SSE 알림 팬아웃을 위한 리스너 컨테이너 구성</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Configuration
public class SseRedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer sseNotificationListenerContainer(
            RedisConnectionFactory connectionFactory,
            SseNotificationSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(SseNotificationPublisher.CHANNEL));
        return container;
    }
}

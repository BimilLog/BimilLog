package jaeik.bimillog.springboot.mysql.redis;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import jaeik.bimillog.domain.notification.repository.SseRepository;
import jaeik.bimillog.domain.notification.service.SseNotificationPublisher;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * <h2>SSE 알림 Redis Pub/Sub 로컬 통합 테스트</h2>
 * <p>Publisher → Redis PUBLISH → 동일 JVM의 Subscriber 수신 → hasEmitter 체크 → 로컬 이미터 전달 경로 검증</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@DisplayName("SSE Pub/Sub 로컬 통합 테스트")
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.scheduling.enabled=false"
})
@Tag("local-integration")
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SseNotificationPubSubLocalIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationPubSubLocalIntegrationTest.class);

    @Autowired
    private SseService sseService;

    @Autowired
    private SseNotificationPublisher publisher;

    @MockitoSpyBean
    private SseRepository sseRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisMessageListenerContainer listenerContainer;

    private final MessageListener payloadLogger = (message, pattern) ->
            log.info("[TEST 수신 로그] channel={}, payload={}",
                    new String(message.getChannel()),
                    new String(message.getBody()));

    @BeforeAll
    void registerTestLogger() {
        listenerContainer.addMessageListener(payloadLogger, new ChannelTopic(SseNotificationPublisher.CHANNEL));
    }

    @AfterAll
    void removeTestLogger() {
        listenerContainer.removeMessageListener(payloadLogger);
    }

    private static final Long OWNER_MEMBER_ID = 9001L;
    private static final Long OTHER_MEMBER_ID = 9002L;
    private static final Long TOKEN_ID = 7001L;

    @AfterEach
    void tearDown() {
        clearEmitters();
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @BeforeEach
    void setUp() {
        clearEmitters();
    }

    @Test
    @DisplayName("publish → 해당 userId의 로컬 이미터 보유 시 send 호출")
    void shouldDeliverWhenLocalEmitterExists() {
        // Given: 로컬 이미터 강제 등록 (subscribe()의 초기 send 호출을 우회)
        RecordingEmitter emitter = registerLocalEmitter(OWNER_MEMBER_ID, TOKEN_ID);

        // When: 서비스 호출 → publisher → Redis PUBLISH → subscriber 수신
        sseService.sendNotification(OWNER_MEMBER_ID, NotificationType.COMMENT, "댓글 알림", "/board/post/1");

        // Then: 구독자가 hasEmitter=true 확인 후 sseRepository.send 를 호출해야 함
        verify(sseRepository, timeout(3000).atLeastOnce()).send(
                org.mockito.ArgumentMatchers.argThat(msg ->
                        msg.memberId().equals(OWNER_MEMBER_ID)
                        && msg.type() == NotificationType.COMMENT
                        && "댓글 알림".equals(msg.message()))
        );

        // 실제 이미터까지 이벤트 전달됐는지
        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(emitter.getSentEvents()).isNotEmpty());
    }

    @Test
    @DisplayName("publish → 해당 userId의 로컬 이미터 미보유 시 send 호출되지 않음")
    void shouldSkipWhenNoLocalEmitter() {
        // Given: OTHER 유저 이미터만 존재
        registerLocalEmitter(OTHER_MEMBER_ID, TOKEN_ID);

        // When: OWNER 대상 publish
        sseService.sendNotification(OWNER_MEMBER_ID, NotificationType.MESSAGE, "메시지 알림", "/rolling-paper/a");

        // Then: 일정 시간 내 send 가 호출되지 않아야 함
        // 구독자가 비동기로 수신하므로 대기 후 never 검증
        try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        verify(sseRepository, never()).send(
                org.mockito.ArgumentMatchers.argThat(msg -> msg.memberId().equals(OWNER_MEMBER_ID))
        );
    }

    @Test
    @DisplayName("Publisher 직접 호출 시에도 동일 채널 구독자가 수신")
    void shouldReceiveWhenPublisherCalledDirectly() {
        RecordingEmitter emitter = registerLocalEmitter(OWNER_MEMBER_ID, TOKEN_ID);

        SseMessage sseMessage = SseMessage.of(OWNER_MEMBER_ID, NotificationType.FRIEND, "친구 요청", "/friends?tab=received");
        publisher.publish(sseMessage);

        verify(sseRepository, timeout(3000).atLeastOnce()).send(
                org.mockito.ArgumentMatchers.argThat(msg ->
                        msg.memberId().equals(OWNER_MEMBER_ID)
                        && msg.type() == NotificationType.FRIEND)
        );

        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(emitter.getSentEvents()).isNotEmpty());
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private void clearEmitters() {
        Map<String, SseEmitter> emitters = (Map<String, SseEmitter>) ReflectionTestUtils.getField(sseRepository, "emitters");
        if (emitters != null) {
            emitters.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private RecordingEmitter registerLocalEmitter(Long memberId, Long tokenId) {
        Map<String, SseEmitter> emitters = (Map<String, SseEmitter>) ReflectionTestUtils.getField(sseRepository, "emitters");
        assertThat(emitters).isNotNull();
        if (!(emitters instanceof ConcurrentHashMap)) {
            emitters = new ConcurrentHashMap<>(emitters);
            ReflectionTestUtils.setField(sseRepository, "emitters", emitters);
        }
        String emitterId = memberId + "_" + tokenId + "_" + System.currentTimeMillis();
        RecordingEmitter emitter = new RecordingEmitter();
        emitters.put(emitterId, emitter);
        return emitter;
    }

    /**
     * 테스트용 SseEmitter — send 호출을 캡처하고 실제 서블릿 전송은 하지 않음.
     */
    static class RecordingEmitter extends SseEmitter {
        private final List<Object> sentEvents = new CopyOnWriteArrayList<>();

        RecordingEmitter() {
            super(0L);
        }

        @Override
        public void send(SseEventBuilder builder) {
            sentEvents.add(builder);
        }

        @Override
        public void send(Object object) {
            sentEvents.add(object);
        }

        public List<Object> getSentEvents() {
            return sentEvents;
        }
    }
}

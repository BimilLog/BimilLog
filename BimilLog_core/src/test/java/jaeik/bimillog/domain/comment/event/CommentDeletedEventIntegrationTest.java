package jaeik.bimillog.domain.comment.event;

import jaeik.bimillog.domain.paper.service.PaperScheduledService;
import jaeik.bimillog.domain.post.service.PostScheduledService;
import jaeik.bimillog.infrastructure.redis.post.RedisPostKeys;
import jaeik.bimillog.testutil.RedisTestHelper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>댓글 삭제 이벤트 워크플로우 로컬 통합 테스트</h2>
 * <p>댓글 삭제 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>로컬 MySQL + Redis 환경에서 실제 이벤트 처리 및 Redis 점수 업데이트를 검증합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
@DisplayName("댓글 삭제 이벤트 워크플로우 로컬 통합 테스트")
public class CommentDeletedEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final double COMMENT_DELETE_SCORE = -3.0;
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(5);
    private static final String SCORE_KEY = RedisPostKeys.REALTIME_POST_SCORE_KEY;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("댓글 삭제 이벤트 워크플로우 - 실시간 인기글 점수 감소")
    void commentDeletedEventWorkflow_ShouldCompleteScoreDecrement() {
        // Given: 게시글에 초기 점수 설정 (댓글 작성으로 인한 +3점)
        Long postId = 100L;
        redisTemplate.opsForZSet().add(SCORE_KEY, postId.toString(), 3.0);

        // When: 댓글 삭제 이벤트 발행
        CommentDeletedEvent event = new CommentDeletedEvent(postId);
        eventPublisher.publishEvent(event);

        // Then: 비동기 처리 완료 대기 후 점수 감소 확인 (3.0 - 3.0 = 0.0)
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, postId.toString());
                    assertThat(currentScore).isEqualTo(0.0);
                });
    }

    @Test
    @DisplayName("여러 다른 게시글의 댓글 삭제 이벤트 동시 처리")
    void multipleDifferentCommentDeletedEvents_ShouldProcessIndependently() {
        // Given: 3개의 게시글에 초기 점수 설정
        redisTemplate.opsForZSet().add(SCORE_KEY, "100", 10.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "101", 8.0);
        redisTemplate.opsForZSet().add(SCORE_KEY, "102", 6.0);

        // When: 3개의 댓글 삭제 이벤트 발행
        eventPublisher.publishEvent(new CommentDeletedEvent(100L));
        eventPublisher.publishEvent(new CommentDeletedEvent(101L));
        eventPublisher.publishEvent(new CommentDeletedEvent(102L));

        // Then: 모든 게시글의 점수가 -3점씩 감소
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    Double score100 = redisTemplate.opsForZSet().score(SCORE_KEY, "100");
                    Double score101 = redisTemplate.opsForZSet().score(SCORE_KEY, "101");
                    Double score102 = redisTemplate.opsForZSet().score(SCORE_KEY, "102");

                    assertThat(score100).isEqualTo(7.0);  // 10 - 3
                    assertThat(score101).isEqualTo(5.0);  // 8 - 3
                    assertThat(score102).isEqualTo(3.0);  // 6 - 3
                });
    }

    @Test
    @DisplayName("동일 게시글의 여러 댓글 삭제 이벤트 처리")
    void multipleDeleteEventsForSamePost_ShouldProcessAll() {
        // Given: 게시글에 초기 점수 설정 (댓글 3개 작성으로 인한 +9점)
        Long postId = 100L;
        redisTemplate.opsForZSet().add(SCORE_KEY, postId.toString(), 9.0);

        // When: 동일 게시글의 댓글 3개 삭제 이벤트 발행
        for (int i = 0; i < 3; i++) {
            eventPublisher.publishEvent(new CommentDeletedEvent(postId));
        }

        // Then: 점수가 -9점 감소 (3개 × -3점)
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, postId.toString());
                    assertThat(currentScore).isEqualTo(0.0);  // 9 - 9
                });
    }

    @Test
    @DisplayName("초기 점수가 없는 게시글의 댓글 삭제 시 음수 점수 적용")
    void commentDeletedForPostWithoutScore_ShouldApplyNegativeScore() {
        // Given: Redis에 초기 점수가 없는 게시글
        Long postId = 100L;

        // When: 댓글 삭제 이벤트 발행
        CommentDeletedEvent event = new CommentDeletedEvent(postId);
        eventPublisher.publishEvent(event);

        // Then: 음수 점수가 적용됨 (0 - 3 = -3)
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    Double currentScore = redisTemplate.opsForZSet().score(SCORE_KEY, postId.toString());
                    assertThat(currentScore).isEqualTo(-3.0);
                });
    }
}

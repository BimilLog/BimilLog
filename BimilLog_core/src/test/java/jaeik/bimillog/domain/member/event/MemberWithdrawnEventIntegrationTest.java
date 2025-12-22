package jaeik.bimillog.domain.member.event;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.SocialStrategyAdapter;
import jaeik.bimillog.infrastructure.api.social.SocialStrategy;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.testutil.RedisTestHelper;
import jaeik.bimillog.testutil.TestMembers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * <h2>사용자 탈퇴 이벤트 워크플로우 로컬 통합 테스트</h2>
 * <p>사용자 탈퇴 시 발생하는 모든 후속 처리를 검증하는 통합 테스트</p>
 * <p>로컬 MySQL + Redis 환경에서 실제 DB 동작을 검증합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Tag("local-integration")
@DisplayName("사용자 탈퇴 이벤트 워크플로우 로컬 통합 테스트")
class MemberWithdrawnEventIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    // 외부 API만 Mock
    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private SocialStrategyAdapter socialStrategyAdapter;

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(5);

    private Member testMember;

    private static final SocialStrategy NOOP_STRATEGY = new SocialStrategy() {
        @Override
        public SocialProvider getProvider() {
            return SocialProvider.KAKAO;
        }

        @Override
        public SocialMemberProfile getSocialToken(String code, String state) {
            throw new UnsupportedOperationException("테스트 전략에서는 소셜 토큰 발급을 지원하지 않습니다.");
        }

        @Override
        public void unlink(String socialId, String accessToken) {
            // no-op - 외부 API 호출 방지
        }

        @Override
        public void logout(String accessToken) {
            // no-op
        }

        @Override
        public void forceLogout(String socialId) {
            // no-op
        }

        @Override
        public String refreshAccessToken(String refreshToken) throws Exception {
            return "test-refreshed-token";
        }
    };

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);

        // 외부 API Mock 설정
        doReturn(NOOP_STRATEGY).when(socialStrategyAdapter).getStrategy(any());
        doNothing().when(sseService).deleteEmitters(any(), any());

        // 테스트 회원 생성
        testMember = TestMembers.createUniqueWithPrefix("withdrawTest");
        persistAndFlush(testMember);
    }

    private void persistAndFlush(Member member) {
        if (member.getSocialToken() != null) {
            entityManager.persist(member.getSocialToken());
        }
        entityManager.persist(member);
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화
    }

    @Test
    @DisplayName("사용자 탈퇴 이벤트 워크플로우 - 회원 삭제 완료")
    void userWithdrawnEventWorkflow_ShouldDeleteMember() {
        // Given
        Long memberId = testMember.getId();
        String socialId = testMember.getSocialId();
        SocialProvider provider = testMember.getProvider();

        // 회원이 존재하는지 확인
        assertThat(memberRepository.findById(memberId)).isPresent();

        // When: 회원 탈퇴 이벤트 발행
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(memberId, socialId, provider);
        eventPublisher.publishEvent(event);

        // Then: 비동기 처리 완료 대기 후 회원 삭제 확인
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    entityManager.clear(); // 캐시 초기화
                    assertThat(memberRepository.findById(memberId)).isEmpty();
                });
    }

    @Test
    @DisplayName("여러 사용자 탈퇴 이벤트 동시 처리")
    void multipleUserWithdrawnEvents() {
        // Given: 3명의 회원 생성
        Member member1 = TestMembers.createUniqueWithPrefix("withdraw1");
        Member member2 = TestMembers.createUniqueWithPrefix("withdraw2");
        Member member3 = TestMembers.createUniqueWithPrefix("withdraw3");

        persistAndFlush(member1);
        persistAndFlush(member2);
        persistAndFlush(member3);

        Long memberId1 = member1.getId();
        Long memberId2 = member2.getId();
        Long memberId3 = member3.getId();

        // When: 3개의 탈퇴 이벤트 발행
        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId1, member1.getSocialId(), member1.getProvider()));
        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId2, member2.getSocialId(), member2.getProvider()));
        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId3, member3.getSocialId(), member3.getProvider()));

        // Then: 모든 회원이 삭제됨
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    entityManager.clear();
                    assertThat(memberRepository.findById(memberId1)).isEmpty();
                    assertThat(memberRepository.findById(memberId2)).isEmpty();
                    assertThat(memberRepository.findById(memberId3)).isEmpty();
                });
    }

    @Test
    @DisplayName("소셜 토큰이 있는 회원 탈퇴")
    void userWithSocialToken_ShouldBeDeleted() {
        // Given: 소셜 토큰이 있는 회원
        Member memberWithToken = TestMembers.createUniqueWithPrefix("tokenTest");

        // 소셜 토큰 생성
        SocialToken socialToken = SocialToken.createSocialToken(
                "test-access-token",
                "test-refresh-token"
        );

        // 소셜 토큰 저장 및 회원과 연결
        entityManager.persist(socialToken);
        memberWithToken.updateSocialToken(socialToken);

        persistAndFlush(memberWithToken);

        Long memberId = memberWithToken.getId();

        // When: 탈퇴 이벤트 발행
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(
                memberId,
                memberWithToken.getSocialId(),
                memberWithToken.getProvider()
        );
        eventPublisher.publishEvent(event);

        // Then: 회원과 소셜 토큰 모두 삭제
        Awaitility.await()
                .atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> {
                    entityManager.clear();
                    assertThat(memberRepository.findById(memberId)).isEmpty();
                });
    }
}

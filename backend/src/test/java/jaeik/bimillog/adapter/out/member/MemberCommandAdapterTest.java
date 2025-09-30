package jaeik.bimillog.adapter.out.member;

import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.out.member.UserCommandAdapter;
import jaeik.bimillog.testutil.H2TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * <h2>UserCommandAdapter 통합 테스트</h2>
 * <p>사용자 명령 어댑터의 핵심 비즈니스 로직을 테스트합니다.</p>
 * <p>Native Query를 사용한 사용자와 설정 동시 삭제 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = jaeik.bimillog.BimilLogApplication.class
        )
)
@ActiveProfiles("h2test")
@Import({UserCommandAdapter.class, H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class MemberCommandAdapterTest {

    @Autowired
    private UserCommandAdapter userCommandAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    private Member testMember;
    private Setting testSetting;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testSetting = Setting.createSetting();
        testEntityManager.persist(testSetting);

        testMember = Member.createUser(
                "12345678",
                SocialProvider.KAKAO,
                "TestNickname",
                "https://test.com/profile.jpg",
                "testUser123",
                testSetting
        );
        testEntityManager.persist(testMember);
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("사용자와 설정이 동시에 삭제되어야 한다")
    void deleteUserAndSetting_ShouldDeleteBothUserAndSetting() {
        // Given
        Long userId = testMember.getId();
        Long settingId = testSetting.getId();

        // 삭제 전 존재 확인
        assertThat(testEntityManager.find(Member.class, userId)).isNotNull();
        assertThat(testEntityManager.find(Setting.class, settingId)).isNotNull();

        // When
        userCommandAdapter.deleteUserAndSetting(userId);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        assertThat(testEntityManager.find(Member.class, userId)).isNull();
        assertThat(testEntityManager.find(Setting.class, settingId)).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 삭제 시 예외가 발생하지 않아야 한다")
    void deleteUserAndSetting_WithNonExistentUser_ShouldNotThrowException() {
        // Given
        Long nonExistentUserId = 99999L;

        // When & Then
        assertThatCode(() -> userCommandAdapter.deleteUserAndSetting(nonExistentUserId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 삭제된 사용자를 재삭제해도 멱등성이 보장되어야 한다")
    void deleteUserAndSetting_Idempotency_ShouldBeGuaranteed() {
        // Given
        Long userId = testMember.getId();

        // 첫 번째 삭제
        userCommandAdapter.deleteUserAndSetting(userId);
        testEntityManager.flush();
        testEntityManager.clear();

        // When & Then - 두 번째 삭제
        assertThatCode(() -> {
            userCommandAdapter.deleteUserAndSetting(userId);
            testEntityManager.flush();
        }).doesNotThrowAnyException();

        // 여전히 삭제된 상태 확인
        assertThat(testEntityManager.find(Member.class, userId)).isNull();
    }

    @Test
    @DisplayName("여러 사용자가 있을 때 특정 사용자만 삭제되어야 한다")
    void deleteUserAndSetting_WithMultipleUsers_ShouldDeleteOnlyTargetUser() {
        // Given
        Setting otherSetting = Setting.createSetting();
        testEntityManager.persist(otherSetting);

        Member otherMember = Member.createUser(
                "87654321",
                SocialProvider.KAKAO,
                "OtherNickname",
                "https://test.com/other.jpg",
                "otherUser456",
                otherSetting
        );
        testEntityManager.persist(otherMember);
        testEntityManager.flush();
        testEntityManager.clear();

        Long targetUserId = testMember.getId();
        Long otherUserId = otherMember.getId();
        Long otherSettingId = otherSetting.getId();

        // When
        userCommandAdapter.deleteUserAndSetting(targetUserId);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        assertThat(testEntityManager.find(Member.class, targetUserId)).isNull();
        assertThat(testEntityManager.find(Member.class, otherUserId)).isNotNull();
        assertThat(testEntityManager.find(Setting.class, otherSettingId)).isNotNull();
    }
}
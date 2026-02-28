package jaeik.bimillog.datajpa.repository;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
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

/**
 * <h2>MemberRepository 통합 테스트</h2>
 * <p>사용자 Repository의 핵심 비즈니스 로직을 테스트합니다.</p>
 * <p>JPA Cascade를 사용한 사용자와 설정 동시 삭제 검증</p>
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
@Tag("datajpa-h2")
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberCommandRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Member testMember;
    private Setting testSetting;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비 (연관 엔티티 먼저 저장)
        testSetting = Setting.createSetting();
        testEntityManager.persist(testSetting);

        jaeik.bimillog.domain.auth.entity.SocialToken testSocialToken = jaeik.bimillog.domain.auth.entity.SocialToken.createSocialToken("test-access-token", "test-refresh-token");
        testEntityManager.persist(testSocialToken);

        testMember = Member.createMember(
                "12345678",
                SocialProvider.KAKAO,
                "TestNickname",
                "https://test.com/profile.jpg",
                "testUser123",
                testSetting,
                testSocialToken
        );
        testEntityManager.persist(testMember);
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    @DisplayName("사용자와 설정이 동시에 삭제되어야 한다")
    void deleteMemberAndSetting_ShouldDeleteBothMemberAndSetting() {
        // Given
        Long memberId = testMember.getId();
        Long settingId = testSetting.getId();

        // 삭제 전 존재 확인
        assertThat(testEntityManager.find(Member.class, memberId)).isNotNull();
        assertThat(testEntityManager.find(Setting.class, settingId)).isNotNull();

        // When
        memberRepository.deleteById(memberId);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        assertThat(testEntityManager.find(Member.class, memberId)).isNull();
        assertThat(testEntityManager.find(Setting.class, settingId)).isNull();
    }

    @Test
    @DisplayName("여러 사용자가 있을 때 특정 사용자만 삭제되어야 한다")
    void deleteMemberAndSetting_WithMultipleMembers_ShouldDeleteOnlyTargetMember() {
        // Given (연관 엔티티 먼저 저장)
        Setting otherSetting = Setting.createSetting();
        testEntityManager.persist(otherSetting);

        jaeik.bimillog.domain.auth.entity.SocialToken otherSocialToken = jaeik.bimillog.domain.auth.entity.SocialToken.createSocialToken("other-access", "other-refresh");
        testEntityManager.persist(otherSocialToken);

        Member otherMember = Member.createMember(
                "87654321",
                SocialProvider.KAKAO,
                "OtherNickname",
                "https://test.com/other.jpg",
                "otherUser456",
                otherSetting,
                otherSocialToken
        );
        testEntityManager.persist(otherMember);
        testEntityManager.flush();
        testEntityManager.clear();

        Long targetMemberId = testMember.getId();
        Long otherMemberId = otherMember.getId();
        Long otherSettingId = otherSetting.getId();

        // When
        memberRepository.deleteById(targetMemberId);
        testEntityManager.flush();
        testEntityManager.clear();

        // Then
        assertThat(testEntityManager.find(Member.class, targetMemberId)).isNull();
        assertThat(testEntityManager.find(Member.class, otherMemberId)).isNotNull();
        assertThat(testEntityManager.find(Setting.class, otherSettingId)).isNotNull();
    }
}
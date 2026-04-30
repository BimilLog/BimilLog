package jaeik.bimillog.datajpa.repository;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

        testMember = Member.createMember(
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

    /**
     * B-003 회귀: 닉네임 중간 부분 4글자 이상 검색이 결과를 반환해야 한다.
     * 이전: query.length() >= 4 일 때 StartingWith 만 사용 → 중간 부분 매칭 0건.
     * 현재: 항상 Containing 사용.
     */
    @Test
    @DisplayName("findByMemberNameContaining - 4글자 이상 중간 부분 검색도 결과를 반환한다 (B-003)")
    void findByMemberNameContaining_shouldMatchMiddleSubstring_overFourChars() {
        // Given: 'uiux-target-rich' 라는 닉네임을 가진 회원
        Setting middleSetting = Setting.createSetting();
        testEntityManager.persist(middleSetting);
        Member middleMember = Member.createMember(
                "social-uiux-rich",
                SocialProvider.KAKAO,
                "rich-social",
                "https://test.com/rich.jpg",
                "uiux-target-rich",
                middleSetting
        );
        testEntityManager.persist(middleMember);
        testEntityManager.flush();
        testEntityManager.clear();

        // When: 'target-r' (8글자) 같은 닉네임 중간 부분으로 검색
        Page<SimpleMemberDTO> result = memberRepository.findByMemberNameContaining(
                "target-r", PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "memberName")));

        // Then: B-003 회귀 — 부분 일치로 결과가 잡혀야 함
        assertThat(result.getContent())
                .extracting(SimpleMemberDTO::getMemberName)
                .contains("uiux-target-rich");
    }

    /**
     * B-003 회귀: 부분 일치 검색이 페이지네이션 정상 동작해야 한다.
     */
    @Test
    @DisplayName("findByMemberNameContaining - totalElements 가 정확히 계산된다 (페이지네이션)")
    void findByMemberNameContaining_shouldComputeTotalElements() {
        // Given: 동일 prefix(uiux-) 회원 3명 추가
        for (int i = 1; i <= 3; i++) {
            Setting s = Setting.createSetting();
            testEntityManager.persist(s);
            Member m = Member.createMember(
                    "social-uiux-target-" + i,
                    SocialProvider.KAKAO,
                    "nick-" + i,
                    "https://test.com/" + i + ".jpg",
                    "uiux-target-" + i,
                    s
            );
            testEntityManager.persist(m);
        }
        testEntityManager.flush();
        testEntityManager.clear();

        // When: size=2 로 1페이지 요청
        Page<SimpleMemberDTO> page = memberRepository.findByMemberNameContaining(
                "uiux-target", PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "memberName")));

        // Then: total = 3, totalPages = 2 (B-001 회귀 — totalElements 정확)
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("여러 사용자가 있을 때 특정 사용자만 삭제되어야 한다")
    void deleteMemberAndSetting_WithMultipleMembers_ShouldDeleteOnlyTargetMember() {
        // Given (연관 엔티티 먼저 저장)
        Setting otherSetting = Setting.createSetting();
        testEntityManager.persist(otherSetting);

        Member otherMember = Member.createMember(
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
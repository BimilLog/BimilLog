package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.MemberRole;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;

import java.util.function.Consumer;

/**
 * <h2>미리 정의된 테스트 회원 인스턴스</h2>
 * <p>테스트에서 바로 사용할 수 있는 사전 정의된 회원 객체들</p>
 * <p>성능 향상 및 코드 간소화를 위해 객체 재사용</p>
 * <p>모든 회원은 기본 Setting을 포함하여 생성됨</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestMembers {

    // 미리 정의된 회원 인스턴스들 (Setting 포함)
    public static final Member MEMBER_1;
    public static final Member MEMBER_2;
    public static final Member MEMBER_3;

    static {
        MEMBER_1 = Member.createMember(
                "kakao123456",
                SocialProvider.KAKAO,
                "테스트회원1",
                "http://example.com/profile1.jpg",
                "testUser1",
                createAllEnabledSetting(),
                createTestKakaoToken()
        );

        MEMBER_2 = Member.createMember(
                "kakao789012",
                SocialProvider.KAKAO,
                "테스트회원2",
                "http://example.com/profile2.jpg",
                "testUser2",
                createAllEnabledSetting(),
                createTestKakaoToken()
        );

        MEMBER_3 = Member.createMember(
                "kakao345678",
                SocialProvider.KAKAO,
                "테스트회원3",
                "http://example.com/profile3.jpg",
                "testUser3",
                createAllEnabledSetting(),
                createTestKakaoToken()
        );
    }

    /**
     * 기본 템플릿 회원 생성
     */
    public static Member createMember(String socialId, String memberName, String socialNickname) {
        return Member.createMember(
                socialId,
                SocialProvider.KAKAO,
                socialNickname,
                "http://example.com/profile.jpg",
                memberName,
                createAllEnabledSetting(),
                createTestKakaoToken()
        );
    }

    /**
     * 기존 회원을 복사하며 특정 ID 설정
     */
    public static Member copyWithId(Member member, Long id) {
        Member copied = Member.createMember(
                member.getSocialId(),
                member.getProvider(),
                member.getSocialNickname(),
                member.getThumbnailImage(),
                member.getMemberName(),
                cloneSetting(member.getSetting()),
                createTestKakaoToken()
        );
        TestFixtures.setFieldValue(copied, "id", id);
        return copied;
    }

    /**
     * 특정 소셜 ID를 가진 회원 생성
     */
    public static Member withSocialId(String socialId) {
        String generatedMemberName = MEMBER_1.getMemberName() + "_" + socialId;
        return createMember(
                socialId,
                generatedMemberName,
                MEMBER_1.getSocialNickname()
        );
    }

    /**
     * 특정 role을 가진 회원 생성 (ADMIN 생성 시 사용)
     */
    public static Member withRole(MemberRole role) {
        if (role == MemberRole.ADMIN) {
            Member admin = Member.createMember(
                    "kakao999999",
                    SocialProvider.KAKAO,
                    "관리자",
                    "http://example.com/admin.jpg",
                    "adminMember",
                    createAllDisabledSetting(),
                    createTestKakaoToken()
            );
            TestFixtures.setFieldValue(admin, "role", MemberRole.ADMIN);
            return admin;
        } else {
            return createMember("kakao-user", "testUser", "테스트회원");
        }
    }

    /**
     * 고유한 회원 생성 (타임스탬프 기반)
     * 통합 테스트에서 고유한 회원이 필요한 경우 사용
     */
    public static Member createUnique() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return createMember(
                "user_" + timestamp,
                "user_" + timestamp,
                "테스트회원_" + timestamp
        );
    }

    /**
     * 고유한 회원 생성 (접두사 지정)
     * @param prefix 회원 식별 접두사
     */
    public static Member createUniqueWithPrefix(String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return createMember(
                prefix + "_" + timestamp,
                prefix + "_" + timestamp,
                prefix + "_소셜닉네임"
        );
    }

    /**
     * 기본 설정 생성 (모든 알림 활성화)
     */
    public static Setting createAllEnabledSetting() {
        return createSetting(true, true, true);
    }

    /**
     * 커스텀 설정 생성
     */
    public static Setting createSetting(boolean messageNotification,
                                       boolean commentNotification,
                                       boolean postFeaturedNotification) {
        return Setting.builder()
                .messageNotification(messageNotification)
                .commentNotification(commentNotification)
                .postFeaturedNotification(postFeaturedNotification)
                .build();
    }

    /**
     * 모든 알림 비활성화된 설정
     */
    public static Setting createAllDisabledSetting() {
        return createSetting(false, false, false);
    }

    private static Setting cloneSetting(Setting original) {
        if (original == null) {
            return null;
        }
        return Setting.builder()
                .messageNotification(original.isMessageNotification())
                .commentNotification(original.isCommentNotification())
                .postFeaturedNotification(original.isPostFeaturedNotification())
                .build();
    }

    /**
     * 테스트용 KakaoToken 생성
     */
    private static KakaoToken createTestKakaoToken() {
        return KakaoToken.createKakaoToken(
                "test-kakao-access-token",
                "test-kakao-refresh-token"
        );
    }
}

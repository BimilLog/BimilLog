package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.infrastructure.redis.RedisMemberDataAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("회원 온보딩 서비스 로그인 헬퍼")
@Tag("unit")
class MemberOnboardingServiceLoginTest extends BaseUnitTest {

    @Mock private RedisMemberDataAdapter redisMemberDataAdapter;
    @Mock private GlobalCookieAdapter globalCookieAdapter;
    @Mock private GlobalJwtAdapter globalJwtAdapter;
    @Mock private GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;
    @Mock private GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    @InjectMocks private MemberOnboardingService onboardingService;

    @Test
    @DisplayName("기존 회원 동기화는 닉네임, 프로필 이미지, 토큰을 업데이트한다")
    void shouldUpdateExistingMember() {
        Member member = TestMembers.createMember("kakao-1", "tester", "oldNickname");
        TestFixtures.setFieldValue(member, "id", 1L);
        if (member.getSetting() != null) {
            TestFixtures.setFieldValue(member.getSetting(), "id", 10L);
        }

        SocialToken socialToken = SocialToken.createSocialToken("new-access", "new-refresh");

        Member updated = onboardingService.syncExistingMember(member, "newNickname", "http://image/new.jpg", socialToken);

        assertThat(updated).isSameAs(member);
        assertThat(member.getSocialNickname()).isEqualTo("newNickname");
        assertThat(member.getThumbnailImage()).isEqualTo("http://image/new.jpg");
        assertThat(member.getSocialToken()).isEqualTo(socialToken);

        verifyNoInteractions(redisMemberDataAdapter, globalCookieAdapter,
                globalJwtAdapter, globalAuthTokenSaveAdapter, globalSocialTokenCommandAdapter);
    }

    @Test
    @DisplayName("대기 중인 회원 저장은 임시 데이터를 레디스에 저장한다")
    void shouldStoreNewMemberInRedis() {
        SocialMemberProfile profile = new SocialMemberProfile(
                "kakao123",
                "test@example.com",
                SocialProvider.KAKAO,
                "testNickname",
                "profile.jpg",
                "access-token",
                "refresh-token"
        );

        onboardingService.storePendingMember(profile, "uuid-123");

        verify(redisMemberDataAdapter).saveTempData("uuid-123", profile);
    }
}
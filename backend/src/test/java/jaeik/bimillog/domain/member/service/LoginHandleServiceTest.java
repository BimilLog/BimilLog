package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.application.service.HandleMemberLoginService;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * <h2>HandleMemberLoginService 단위 테스트</h2>
 * <p>소셜 로그인 시 기존 회원 갱신과 신규 회원 임시 저장 로직을 검증합니다.</p>
 */
@DisplayName("HandleMemberLoginService 단위 테스트")
@Tag("unit")
class LoginHandleServiceTest extends BaseUnitTest {

    @Mock
    private RedisMemberDataPort redisMemberDataPort;

    @InjectMocks
    private HandleMemberLoginService loginHandleService;

    @Test
    @DisplayName("기존 회원 프로필과 카카오 토큰을 갱신한다")
    void shouldUpdateExistingMember() {
        Member member = TestMembers.createMember("kakao-1", "tester", "oldNickname");
        TestFixtures.setFieldValue(member, "id", 1L);
        if (member.getSetting() != null) {
            TestFixtures.setFieldValue(member.getSetting(), "id", 10L);
        }

        KakaoToken kakaoToken = KakaoToken.createKakaoToken("new-access", "new-refresh");

        Member updated = loginHandleService.handleExistingMember(member, "신규닉네임", "http://image/new.jpg", kakaoToken);

        assertThat(updated).isSameAs(member);
        assertThat(member.getSocialNickname()).isEqualTo("신규닉네임");
        assertThat(member.getThumbnailImage()).isEqualTo("http://image/new.jpg");
        assertThat(member.getKakaoToken()).isEqualTo(kakaoToken);

        verifyNoInteractions(redisMemberDataPort);
    }

    @Test
    @DisplayName("신규 회원 임시 데이터를 Redis에 저장한다")
    void shouldStoreNewMemberInRedis() {
        SocialMemberProfile profile = new SocialMemberProfile(
                "kakao123",
                "test@example.com",
                SocialProvider.KAKAO,
                "testNickname",
                "profile.jpg",
                "access-token",
                "refresh-token",
                "fcm-token"
        );
        String uuid = "uuid-123";

        loginHandleService.handleNewMember(profile, uuid);

        verify(redisMemberDataPort).saveTempData(uuid, profile);
    }
}

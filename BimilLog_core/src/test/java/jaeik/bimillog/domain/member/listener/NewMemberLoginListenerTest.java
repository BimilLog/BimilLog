package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.event.NewMemberLoginEvent;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.service.MemberOnboardingService;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * <h2>NewMemberLoginListener 단위 테스트</h2>
 * <p>신규 회원 로그인 이벤트 처리 검증</p>
 */
@DisplayName("NewMemberLoginListener 단위 테스트")
@Tag("unit")
class NewMemberLoginListenerTest extends BaseUnitTest {

    @Mock
    private MemberOnboardingService memberOnboardingService;

    @InjectMocks
    private NewMemberLoginListener newMemberLoginListener;

    @Test
    @DisplayName("신규 회원 로그인 이벤트를 받아 임시 정보를 저장한다")
    void shouldStorePendingMemberOnNewMemberLoginEvent() {
        // Given
        SocialMemberProfile memberProfile = SocialMemberProfile.of(
                "123456789",
                "test@example.com",
                SocialProvider.KAKAO,
                "테스트유저",
                "https://example.com/profile.jpg",
                "access-token",
                "refresh-token"
        );
        String uuid = "test-uuid-12345";
        NewMemberLoginEvent event = new NewMemberLoginEvent(memberProfile, uuid);

        // When
        newMemberLoginListener.SaveMemberInfoToSession(event);

        // Then
        verify(memberOnboardingService).storePendingMember(eq(memberProfile), eq(uuid));
    }
}

package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("회원 프로필 명령 서비스")
@Tag("unit")
class MemberProfileCommandServiceTest extends BaseUnitTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberProfileCommandService memberProfileCommandService;

    @Test
    @DisplayName("회원이 존재할 때 회원 설정을 업데이트한다")
    void shouldUpdateSettings() {
        Long memberId = 1L;
        Setting newSetting = Setting.createSetting();
        newSetting.updateSettings(false, false, true, false);

        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberProfileCommandService.updateMemberSettings(memberId, newSetting);

        verify(memberRepository).findById(memberId);
        assertThat(member.getSetting().isMessageNotification()).isFalse();
        assertThat(member.getSetting().isCommentNotification()).isFalse();
        assertThat(member.getSetting().isPostFeaturedNotification()).isTrue();
        assertThat(member.getSetting().isFriendSendNotification()).isFalse();
    }

    @Test
    @DisplayName("설정 업데이트 대상 회원을 찾을 수 없을 때 예외를 발생시킨다") // "throws when member not found for settings update"
    void shouldThrowWhenSettingsTargetMissing() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberProfileCommandService.updateMemberSettings(99L, Setting.createSetting()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("사용 가능할 때 회원 이름을 업데이트한다") // "updates member name when available"
    void shouldUpdateMemberName() {
        Long memberId = 1L;
        Member member = TestMembers.copyWithId(TestMembers.MEMBER_1, memberId);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        memberProfileCommandService.updateMemberName(memberId, "newNick");

        verify(memberRepository).findById(memberId);
        assertThat(member.getMemberName()).isEqualTo("newNick");
    }

    @Test
    @DisplayName("이름 변경 대상 회원을 찾을 수 없을 때 예외를 발생시킨다") // "throws when member not found for name change"
    void shouldThrowWhenMemberMissingOnNameChange() {
        given(memberRepository.findById(77L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberProfileCommandService.updateMemberName(77L, "nick"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_USER_NOT_FOUND.getMessage());
    }
}
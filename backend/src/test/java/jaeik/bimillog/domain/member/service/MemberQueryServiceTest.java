package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.service.MemberQueryService;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
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

/**
 * <h2>MemberQueryService 테스트</h2>
 * <p>사용자 조회 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>CLAUDE.md 테스트 철학에 따라 예외 처리 로직이 있는 findBySettingId만 테스트</p>
 * <p>단순 위임 메서드들은 테스트에서 제외 (테스트 불필요 카테고리)</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("MemberQueryService 테스트")
@Tag("unit")
class MemberQueryServiceTest extends BaseUnitTest {

    @Mock
    private MemberQueryPort memberQueryPort;

    @InjectMocks
    private MemberQueryService userQueryService;

    @Test
    @DisplayName("설정 ID로 설정 조회 - 정상 케이스")
    void shouldFindSetting_WhenValidSettingId() {
        // Given
        Long settingId = 1L;
        Setting expectedSetting = createSettingWithId(createCustomSetting(true, false, true), settingId);

        given(memberQueryPort.findSettingById(settingId)).willReturn(Optional.of(expectedSetting));

        // When
        Setting result = userQueryService.findBySettingId(settingId);

        // Then
        verify(memberQueryPort).findSettingById(settingId);
        assertThat(result).isEqualTo(expectedSetting);
        assertThat(result.getId()).isEqualTo(settingId);
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("설정 ID로 설정 조회 - 설정이 존재하지 않는 경우")
    void shouldThrowException_WhenSettingNotFound() {
        // Given
        Long settingId = 999L;

        given(memberQueryPort.findSettingById(settingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userQueryService.findBySettingId(settingId))
                .isInstanceOf(MemberCustomException.class)
                .hasMessage(MemberErrorCode.SETTINGS_NOT_FOUND.getMessage());

        verify(memberQueryPort).findSettingById(settingId);
    }

    /*
     * 단순 위임 메서드들은 CLAUDE.md 테스트 철학에 따라 제외:
     * - findByProviderAndSocialId(): 단순 포트 호출 후 반환
     * - findById(): 단순 포트 호출 후 반환
     * - existsByUserName(): 단순 포트 호출 후 반환
     * - findByUserName(): 단순 포트 호출 후 반환
     * - getReferenceById(): 단순 포트 호출 후 반환
     *
     * 이러한 메서드들은 프레임워크가 검증하는 기본 기능이며,
     * 오류 발생 시 메인 로직만으로 쉽게 해결 가능합니다.
     */
}
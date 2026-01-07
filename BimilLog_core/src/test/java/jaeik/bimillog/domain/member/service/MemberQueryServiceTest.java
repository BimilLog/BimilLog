package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.domain.member.repository.SettingRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
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
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SettingRepository settingRepository;

    @InjectMocks
    private MemberQueryService userQueryService;

    @Test
    @DisplayName("설정 ID로 설정 조회 - 정상 케이스")
    void shouldFindSetting_WhenValidSettingId() {
        // Given
        Long settingId = 1L;
        Setting expectedSetting = createSettingWithId(createCustomSetting(true, false, true), settingId);

        given(settingRepository.findById(settingId)).willReturn(Optional.of(expectedSetting));

        // When
        Setting result = userQueryService.findBySettingId(settingId);

        // Then
        verify(settingRepository).findById(settingId);
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

        given(settingRepository.findById(settingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userQueryService.findBySettingId(settingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_SETTINGS_NOT_FOUND.getMessage());

        verify(settingRepository).findById(settingId);
    }
}

package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.out.UserQueryPort;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.SettingDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>SettingQueryService 테스트</h2>
 * <p>설정 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>헥사고날 아키텍처 원칙에 따라 모든 외부 의존성을 Mock으로 격리하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettingQueryService 테스트")
class SettingQueryServiceTest {

    @Mock
    private UserQueryPort userQueryPort;

    @InjectMocks
    private SettingQueryService settingQueryService;

    @Test
    @DisplayName("설정 ID로 설정 조회 - 정상 케이스")
    void shouldFindSetting_WhenValidSettingId() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("설정 ID로 설정 조회 - 설정이 존재하지 않는 경우")
    void shouldThrowException_WhenSettingNotFound() {
        // Given
        Long nonexistentSettingId = 999L;

        given(userQueryPort.findSettingById(nonexistentSettingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingQueryService.findBySettingId(nonexistentSettingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SETTINGS_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findSettingById(nonexistentSettingId);
    }

    @Test
    @DisplayName("기본 설정값으로 생성된 설정 조회")
    void shouldFindSetting_WhenDefaultSettings() {
        // Given
        Long settingId = 1L;
        Setting defaultSetting = Setting.builder()
                .id(settingId)
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(defaultSetting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        // 기본값 확인 (Setting.createSetting()의 기본값에 따라)
        assertThat(result.isCommentNotification()).isTrue(); // 기본값
        assertThat(result.isPostFeaturedNotification()).isTrue(); // 기본값
        assertThat(result.isMessageNotification()).isTrue(); // 기본값
    }

    @Test
    @DisplayName("모든 알림이 비활성화된 설정 조회")
    void shouldFindSetting_WhenAllNotificationsDisabled() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isFalse();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isFalse();
    }

    @Test
    @DisplayName("모든 알림이 활성화된 설정 조회")
    void shouldFindSetting_WhenAllNotificationsEnabled() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isCommentNotification()).isTrue();
        assertThat(result.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("메시지 알림만 활성화된 설정 조회")
    void shouldFindSetting_WhenOnlyMessageNotificationEnabled() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isFalse();
    }

    @Test
    @DisplayName("댓글 알림만 활성화된 설정 조회")
    void shouldFindSetting_WhenOnlyCommentNotificationEnabled() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isFalse();
        assertThat(result.isCommentNotification()).isTrue();
        assertThat(result.isPostFeaturedNotification()).isFalse();
    }

    @Test
    @DisplayName("인기글 알림만 활성화된 설정 조회")
    void shouldFindSetting_WhenOnlyPostFeaturedNotificationEnabled() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isFalse();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("null 설정 ID로 조회 시도")
    void shouldThrowException_WhenSettingIdIsNull() {
        // Given
        Long nullSettingId = null;

        given(userQueryPort.findSettingById(nullSettingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingQueryService.findBySettingId(nullSettingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SETTINGS_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findSettingById(nullSettingId);
    }

    @Test
    @DisplayName("음수 설정 ID로 조회 시도")
    void shouldThrowException_WhenSettingIdIsNegative() {
        // Given
        Long negativeSettingId = -1L;

        given(userQueryPort.findSettingById(negativeSettingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingQueryService.findBySettingId(negativeSettingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SETTINGS_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findSettingById(negativeSettingId);
    }

    @Test
    @DisplayName("0 설정 ID로 조회 시도")
    void shouldThrowException_WhenSettingIdIsZero() {
        // Given
        Long zeroSettingId = 0L;

        given(userQueryPort.findSettingById(zeroSettingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingQueryService.findBySettingId(zeroSettingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SETTINGS_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findSettingById(zeroSettingId);
    }

    @Test
    @DisplayName("매우 큰 설정 ID로 조회 시도")
    void shouldThrowException_WhenSettingIdIsVeryLarge() {
        // Given
        Long largeSettingId = Long.MAX_VALUE;

        given(userQueryPort.findSettingById(largeSettingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingQueryService.findBySettingId(largeSettingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SETTINGS_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findSettingById(largeSettingId);
    }

    @Test
    @DisplayName("SettingDTO 변환 확인")
    void shouldConvertToDTO_Correctly() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        SettingDTO result = settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort).findSettingById(settingId);
        
        // SettingDTO 생성자가 올바르게 동작하는지 확인
        assertThat(result).isNotNull();
        assertThat(result.isMessageNotification()).isEqualTo(setting.isMessageNotification());
        assertThat(result.isCommentNotification()).isEqualTo(setting.isCommentNotification());
        assertThat(result.isPostFeaturedNotification()).isEqualTo(setting.isPostFeaturedNotification());
    }

    @Test
    @DisplayName("설정 조회 메서드 호출 횟수 확인")
    void shouldCallFindSettingByIdOnce() {
        // Given
        Long settingId = 1L;
        Setting setting = Setting.builder()
                .id(settingId)
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        given(userQueryPort.findSettingById(settingId)).willReturn(Optional.of(setting));

        // When
        settingQueryService.findBySettingId(settingId);

        // Then
        verify(userQueryPort, org.mockito.Mockito.times(1)).findSettingById(settingId);
    }

    @Test
    @DisplayName("여러 설정 ID로 연속 조회")
    void shouldHandleMultipleSequentialCalls() {
        // Given
        Long settingId1 = 1L;
        Long settingId2 = 2L;
        
        Setting setting1 = Setting.builder()
                .id(settingId1)
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        
        Setting setting2 = Setting.builder()
                .id(settingId2)
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();

        given(userQueryPort.findSettingById(settingId1)).willReturn(Optional.of(setting1));
        given(userQueryPort.findSettingById(settingId2)).willReturn(Optional.of(setting2));

        // When
        SettingDTO result1 = settingQueryService.findBySettingId(settingId1);
        SettingDTO result2 = settingQueryService.findBySettingId(settingId2);

        // Then
        verify(userQueryPort).findSettingById(settingId1);
        verify(userQueryPort).findSettingById(settingId2);
        
        assertThat(result1.isMessageNotification()).isTrue();
        assertThat(result1.isCommentNotification()).isTrue();
        assertThat(result1.isPostFeaturedNotification()).isTrue();
        
        assertThat(result2.isMessageNotification()).isFalse();
        assertThat(result2.isCommentNotification()).isFalse();
        assertThat(result2.isPostFeaturedNotification()).isFalse();
    }
}
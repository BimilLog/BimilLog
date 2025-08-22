package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.out.UserCommandPort;
import jaeik.growfarm.domain.user.application.port.out.UserQueryPort;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
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
import static org.mockito.Mockito.*;

/**
 * <h2>UserCommandService 테스트</h2>
 * <p>사용자 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>헥사고날 아키텍처 원칙에 따라 모든 외부 의존성을 Mock으로 격리하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandService 테스트")
class UserCommandServiceTest {

    @Mock
    private UserQueryPort userQueryPort;
    
    @Mock
    private UserCommandPort userCommandPort;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    @DisplayName("사용자 설정 수정 - 정상 케이스")
    void shouldUpdateUserSettings_WhenUserExists() {
        // Given
        Long userId = 1L;
        Setting existingSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();
        
        User user = User.builder()
                .id(userId)
                .userName("testUser")
                .setting(existingSetting)
                .build();
        
        SettingDTO settingDTO = SettingDTO.builder()
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserSettings(userId, settingDTO);

        // Then
        verify(userQueryPort).findById(userId);
        verify(userCommandPort).save(user);
        
        // Setting이 업데이트되었는지 확인
        assertThat(user.getSetting().isMessageNotification()).isFalse();
        assertThat(user.getSetting().isCommentNotification()).isFalse();
        assertThat(user.getSetting().isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("사용자 설정 수정 - 사용자가 존재하지 않는 경우")
    void shouldThrowException_WhenUserNotFoundForSettingUpdate() {
        // Given
        Long userId = 999L;
        SettingDTO settingDTO = SettingDTO.builder()
                .messageNotification(true)
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userCommandService.updateUserSettings(userId, settingDTO))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryPort).findById(userId);
        verify(userCommandPort, never()).save(any(User.class));
    }

    @Test
    @DisplayName("닉네임 변경 - 정상 케이스")
    void shouldUpdateUserName_WhenValidNewUserName() {
        // Given
        Long userId = 1L;
        String newUserName = "newUserName";
        
        User user = User.builder()
                .id(userId)
                .userName("oldUserName")
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
                .build();

        given(userQueryPort.existsByUserName(newUserName)).willReturn(false);
        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserName(userId, newUserName);

        // Then
        verify(userQueryPort).existsByUserName(newUserName);
        verify(userQueryPort).findById(userId);
        verify(userCommandPort).save(user);
        
        assertThat(user.getUserName()).isEqualTo(newUserName);
    }

    @Test
    @DisplayName("닉네임 변경 - 이미 존재하는 닉네임")
    void shouldThrowException_WhenUserNameAlreadyExists() {
        // Given
        Long userId = 1L;
        String existingUserName = "existingUser";

        given(userQueryPort.existsByUserName(existingUserName)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userCommandService.updateUserName(userId, existingUserName))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.EXISTED_NICKNAME.getMessage());
        
        verify(userQueryPort).existsByUserName(existingUserName);
        verify(userQueryPort, never()).findById(any());
        verify(userCommandPort, never()).save(any(User.class));
    }

    @Test
    @DisplayName("닉네임 변경 - 사용자가 존재하지 않는 경우")
    void shouldThrowException_WhenUserNotFoundForUserNameUpdate() {
        // Given
        Long userId = 999L;
        String newUserName = "newUserName";

        given(userQueryPort.existsByUserName(newUserName)).willReturn(false);
        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userCommandService.updateUserName(userId, newUserName))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        
        verify(userQueryPort).existsByUserName(newUserName);
        verify(userQueryPort).findById(userId);
        verify(userCommandPort, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 삭제 - 정상 케이스")
    void shouldDeleteUser_WhenValidUserId() {
        // Given
        Long userId = 1L;

        // When
        userCommandService.deleteById(userId);

        // Then
        verify(userCommandPort).deleteById(userId);
    }

    @Test
    @DisplayName("사용자 저장 - 정상 케이스")
    void shouldSaveUser_WhenValidUser() {
        // Given
        User user = User.builder()
                .userName("testUser")
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
                .build();
        
        User savedUser = User.builder()
                .id(1L)
                .userName("testUser")
                .provider(SocialProvider.KAKAO)
                .socialId("123456")
                .role(UserRole.USER)
                .build();

        given(userCommandPort.save(user)).willReturn(savedUser);

        // When
        User result = userCommandService.save(user);

        // Then
        verify(userCommandPort).save(user);
        assertThat(result).isEqualTo(savedUser);
        assertThat(result.getId()).isEqualTo(1L);
    }


    @Test
    @DisplayName("빈 문자열 닉네임 변경 시도")
    void shouldUpdateUserName_WhenEmptyUserName() {
        // Given
        Long userId = 1L;
        String emptyUserName = "";
        
        User user = User.builder()
                .id(userId)
                .userName("oldUserName")
                .build();

        given(userQueryPort.existsByUserName(emptyUserName)).willReturn(false);
        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserName(userId, emptyUserName);

        // Then
        verify(userCommandPort).save(user);
        assertThat(user.getUserName()).isEqualTo(emptyUserName);
    }

    @Test
    @DisplayName("null 닉네임 변경 시도")
    void shouldUpdateUserName_WhenNullUserName() {
        // Given
        Long userId = 1L;
        String nullUserName = null;
        
        User user = User.builder()
                .id(userId)
                .userName("oldUserName")
                .build();

        given(userQueryPort.existsByUserName(nullUserName)).willReturn(false);
        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserName(userId, nullUserName);

        // Then
        verify(userCommandPort).save(user);
        assertThat(user.getUserName()).isNull();
    }

    @Test
    @DisplayName("매우 긴 닉네임 변경")
    void shouldUpdateUserName_WhenVeryLongUserName() {
        // Given
        Long userId = 1L;
        String longUserName = "a".repeat(255); // 255자 길이
        
        User user = User.builder()
                .id(userId)
                .userName("oldUserName")
                .build();

        given(userQueryPort.existsByUserName(longUserName)).willReturn(false);
        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserName(userId, longUserName);

        // Then
        verify(userCommandPort).save(user);
        assertThat(user.getUserName()).isEqualTo(longUserName);
    }

    @Test
    @DisplayName("특수 문자 포함 닉네임 변경")
    void shouldUpdateUserName_WhenSpecialCharacterUserName() {
        // Given
        Long userId = 1L;
        String specialUserName = "user@#$%^&*()_+{}|:<>?[];',./";
        
        User user = User.builder()
                .id(userId)
                .userName("oldUserName")
                .build();

        given(userQueryPort.existsByUserName(specialUserName)).willReturn(false);
        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserName(userId, specialUserName);

        // Then
        verify(userCommandPort).save(user);
        assertThat(user.getUserName()).isEqualTo(specialUserName);
    }

    @Test
    @DisplayName("null 설정 DTO로 사용자 설정 수정")
    void shouldThrowException_WhenNullSettingDTO() {
        // Given
        Long userId = 1L;

        // When & Then
        assertThatThrownBy(() -> userCommandService.updateUserSettings(userId, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        
        // null 검증이 먼저 실행되므로 userQueryPort가 호출되지 않음
        verify(userQueryPort, never()).findById(any());
    }

    @Test
    @DisplayName("부분적 설정 업데이트")
    void shouldUpdateUserSettings_WhenPartialSettingDTO() {
        // Given
        Long userId = 1L;
        Setting existingSetting = Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();
        
        User user = User.builder()
                .id(userId)
                .setting(existingSetting)
                .build();
        
        // 부분적 설정만 포함된 DTO (darkModeEnabled만 설정)
        SettingDTO partialSettingDTO = SettingDTO.builder()
                .messageNotification(true)
                // commentNotification, likeNotification은 null
                .build();

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        userCommandService.updateUserSettings(userId, partialSettingDTO);

        // Then
        verify(userCommandPort).save(user);
        // 부분적 업데이트 동작은 Setting.updateSetting 메서드에 의존
    }
}
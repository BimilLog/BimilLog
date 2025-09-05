package jaeik.bimillog.infrastructure.outadapter.notification.persistence.notification;

import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.notification.out.persistence.notification.NotificationUtilAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>NotificationUtilAdapter 테스트</h2>
 * <p>알림 유틸리티 어댑터의 비즈니스 로직 검증</p>
 * <p>Emitter ID 생성 및 알림 수신 자격 확인 기능 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationUtilAdapter 테스트")
class NotificationUtilAdapterTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private NotificationUtilAdapter notificationUtilAdapter;

    private User testUser;
    private Setting enabledAllSetting;
    private Setting disabledAllSetting;
    private Setting partialEnabledSetting;

    @BeforeEach
    void setUp() {
        // Given: 모든 알림 활성화된 설정
        enabledAllSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        // Given: 모든 알림 비활성화된 설정
        disabledAllSetting = Setting.builder()
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();

        // Given: 일부 알림만 활성화된 설정
        partialEnabledSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        // Given: 테스트용 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(enabledAllSetting)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - Emitter ID 생성")
    void shouldMakeTimeIncludeId_WhenValidParametersProvided() {
        // Given: 사용자 ID와 토큰 ID
        Long userId = 1L;
        Long tokenId = 100L;

        // When: Emitter ID 생성
        String emitterId = notificationUtilAdapter.makeTimeIncludeId(userId, tokenId);

        // Then: 올바른 형식의 Emitter ID 생성 검증
        assertThat(emitterId).isNotNull();
        assertThat(emitterId).contains("1_100_");
        assertThat(emitterId).matches("1_100_\\d+"); // 정규식으로 형식 검증
        
        // 시간이 포함되어 있는지 확인 (밀리초는 현재 시간과 유사해야 함)
        String[] parts = emitterId.split("_");
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isEqualTo("1");
        assertThat(parts[1]).isEqualTo("100");
        assertThat(Long.parseLong(parts[2])).isGreaterThan(0L);
    }

    @Test
    @DisplayName("경계 케이스 - null 값으로 Emitter ID 생성")
    void shouldMakeTimeIncludeId_WhenNullParametersProvided() {
        // Given: null 값들
        Long userId = null;
        Long tokenId = null;

        // When: null 값으로 Emitter ID 생성
        String emitterId = notificationUtilAdapter.makeTimeIncludeId(userId, tokenId);

        // Then: null 값이 포함된 Emitter ID 생성 검증
        assertThat(emitterId).isNotNull();
        assertThat(emitterId).contains("null_null_");
        assertThat(emitterId).matches("null_null_\\d+");
    }

    @Test
    @DisplayName("경계 케이스 - 0 값으로 Emitter ID 생성")
    void shouldMakeTimeIncludeId_WhenZeroParametersProvided() {
        // Given: 0 값들
        Long userId = 0L;
        Long tokenId = 0L;

        // When: 0 값으로 Emitter ID 생성
        String emitterId = notificationUtilAdapter.makeTimeIncludeId(userId, tokenId);

        // Then: 0 값이 포함된 Emitter ID 생성 검증
        assertThat(emitterId).isNotNull();
        assertThat(emitterId).contains("0_0_");
        assertThat(emitterId).matches("0_0_\\d+");
    }

    @Test
    @DisplayName("정상 케이스 - ADMIN 알림 자격 확인 (항상 true)")
    void shouldReturnTrue_WhenNotificationTypeIsAdmin() {
        // Given: ADMIN 알림 타입
        Long userId = 1L;
        NotificationType type = NotificationType.ADMIN;

        // When: 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: 항상 true 반환 검증 (사용자 조회 없이)
        assertThat(result).isTrue();

        // Verify: 사용자 조회가 수행되지 않았는지 확인
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - INITIATE 알림 자격 확인 (항상 true)")
    void shouldReturnTrue_WhenNotificationTypeIsInitiate() {
        // Given: INITIATE 알림 타입
        Long userId = 1L;
        NotificationType type = NotificationType.INITIATE;

        // When: 알림 수신 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: 항상 true 반환 검증 (사용자 조회 없이)
        assertThat(result).isTrue();

        // Verify: 사용자 조회가 수행되지 않았는지 확인
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - PAPER 알림 자격 확인 (메시지 알림 활성화)")
    void shouldReturnTrue_WhenPaperNotificationEnabled() {
        // Given: 메시지 알림이 활성화된 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(enabledAllSetting)
                .build();

        Long userId = 1L;
        NotificationType type = NotificationType.PAPER;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: PAPER 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: true 반환 검증
        assertThat(result).isTrue();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - PAPER 알림 자격 확인 (메시지 알림 비활성화)")
    void shouldReturnFalse_WhenPaperNotificationDisabled() {
        // Given: 메시지 알림이 비활성화된 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(disabledAllSetting)
                .build();

        Long userId = 1L;
        NotificationType type = NotificationType.PAPER;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: PAPER 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: false 반환 검증
        assertThat(result).isFalse();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - COMMENT 알림 자격 확인 (댓글 알림 활성화)")
    void shouldReturnTrue_WhenCommentNotificationEnabled() {
        // Given: 댓글 알림이 활성화된 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(enabledAllSetting)
                .build();

        Long userId = 1L;
        NotificationType type = NotificationType.COMMENT;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: COMMENT 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: true 반환 검증
        assertThat(result).isTrue();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - COMMENT 알림 자격 확인 (댓글 알림 비활성화)")
    void shouldReturnFalse_WhenCommentNotificationDisabled() {
        // Given: 댓글 알림이 비활성화된 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(disabledAllSetting)
                .build();

        Long userId = 1L;
        NotificationType type = NotificationType.COMMENT;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: COMMENT 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: false 반환 검증
        assertThat(result).isFalse();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - POST_FEATURED 알림 자격 확인 (인기글 알림 활성화)")
    void shouldReturnTrue_WhenPostFeaturedNotificationEnabled() {
        // Given: 인기글 알림이 활성화된 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(enabledAllSetting)
                .build();

        Long userId = 1L;
        NotificationType type = NotificationType.POST_FEATURED;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: POST_FEATURED 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: true 반환 검증
        assertThat(result).isTrue();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - POST_FEATURED 알림 자격 확인 (인기글 알림 비활성화)")
    void shouldReturnFalse_WhenPostFeaturedNotificationDisabled() {
        // Given: 인기글 알림이 비활성화된 사용자
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(disabledAllSetting)
                .build();

        Long userId = 1L;
        NotificationType type = NotificationType.POST_FEATURED;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: POST_FEATURED 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: false 반환 검증
        assertThat(result).isFalse();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("경계 케이스 - 존재하지 않는 사용자 ID로 알림 자격 확인")
    void shouldReturnFalse_WhenUserNotFound() {
        // Given: 존재하지 않는 사용자 ID
        Long userId = 999L;
        NotificationType type = NotificationType.PAPER;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: false 반환 검증
        assertThat(result).isFalse();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - 일부 알림만 활성화된 사용자의 알림 자격 확인")
    void shouldReturnCorrectResult_WhenPartialNotificationEnabled() {
        // Given: 일부 알림만 활성화된 사용자 (메시지: O, 댓글: X, 인기글: O)
        testUser = User.builder()
                .id(1L)
                .socialId("12345")
                .provider(SocialProvider.KAKAO)
                .userName("테스트유저")
                .role(UserRole.USER)
                .setting(partialEnabledSetting)
                .build();

        Long userId = 1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When & Then: 메시지 알림 - 활성화됨
        boolean paperResult = notificationUtilAdapter.isEligibleForNotification(userId, NotificationType.PAPER);
        assertThat(paperResult).isTrue();

        // When & Then: 댓글 알림 - 비활성화됨
        boolean commentResult = notificationUtilAdapter.isEligibleForNotification(userId, NotificationType.COMMENT);
        assertThat(commentResult).isFalse();

        // When & Then: 인기글 알림 - 활성화됨
        boolean postFeaturedResult = notificationUtilAdapter.isEligibleForNotification(userId, NotificationType.POST_FEATURED);
        assertThat(postFeaturedResult).isTrue();

        // Verify: 사용자 조회가 3번 수행되었는지 확인
        verify(userQueryUseCase, times(3)).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("경계 케이스 - null 사용자 ID로 알림 자격 확인")
    void shouldReturnFalse_WhenUserIdIsNull() {
        // Given: null 사용자 ID
        Long userId = null;
        NotificationType type = NotificationType.PAPER;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.empty());

        // When: 알림 자격 확인
        boolean result = notificationUtilAdapter.isEligibleForNotification(userId, type);

        // Then: false 반환 검증
        assertThat(result).isFalse();

        // Verify: 사용자 조회 수행 확인
        verify(userQueryUseCase).findById(userId);
        verifyNoMoreInteractions(userQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - 동일한 매개변수로 여러 번 Emitter ID 생성 시 다른 값 생성")
    void shouldGenerateDifferentIds_WhenCalledMultipleTimes() throws InterruptedException {
        // Given: 동일한 사용자 ID와 토큰 ID
        Long userId = 1L;
        Long tokenId = 100L;

        // When: 첫 번째 Emitter ID 생성
        String firstId = notificationUtilAdapter.makeTimeIncludeId(userId, tokenId);
        
        // 시간 차이를 보장하기 위해 잠시 대기
        Thread.sleep(1);
        
        // When: 두 번째 Emitter ID 생성
        String secondId = notificationUtilAdapter.makeTimeIncludeId(userId, tokenId);

        // Then: 서로 다른 ID 생성 검증
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(firstId).matches("1_100_\\d+");
        assertThat(secondId).matches("1_100_\\d+");

        // 시간 부분만 다른지 확인
        String[] firstParts = firstId.split("_");
        String[] secondParts = secondId.split("_");
        assertThat(firstParts[0]).isEqualTo(secondParts[0]); // userId 동일
        assertThat(firstParts[1]).isEqualTo(secondParts[1]); // tokenId 동일
        assertThat(Long.parseLong(firstParts[2])).isLessThanOrEqualTo(Long.parseLong(secondParts[2])); // 시간은 증가
    }

    @Test
    @DisplayName("경계 케이스 - 매우 큰 ID 값으로 Emitter ID 생성")
    void shouldMakeTimeIncludeId_WhenVeryLargeIds() {
        // Given: 매우 큰 ID 값들
        Long userId = Long.MAX_VALUE;
        Long tokenId = Long.MAX_VALUE - 1;

        // When: 매우 큰 값으로 Emitter ID 생성
        String emitterId = notificationUtilAdapter.makeTimeIncludeId(userId, tokenId);

        // Then: 올바른 형식의 Emitter ID 생성 검증
        assertThat(emitterId).isNotNull();
        assertThat(emitterId).contains(String.valueOf(Long.MAX_VALUE));
        assertThat(emitterId).contains(String.valueOf(Long.MAX_VALUE - 1));
        assertThat(emitterId).matches(Long.MAX_VALUE + "_" + (Long.MAX_VALUE - 1) + "_\\d+");
    }
}
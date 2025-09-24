package jaeik.bimillog.domain.user.service;

import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.application.service.WithdrawService;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>WithdrawService 단위 테스트</h2>
 * <p>회원 탈퇴 서비스의 빔즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("WithdrawService 단위 테스트")
class WithdrawServiceTest extends BaseUnitTest {

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private DeleteUserPort deleteUserPort;

    @Mock
    private GlobalCookiePort globalCookiePort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private WithdrawService withdrawService;

    // 테스트용 사용자
    private User withdrawTestUser;
    private List<ResponseCookie> logoutCookies;

    @BeforeEach
    protected void setUpChild() {
        withdrawTestUser = createTestUserWithId(100L);

        logoutCookies = List.of(
                ResponseCookie.from("access_token", "").maxAge(0).build(),
                ResponseCookie.from("refresh_token", "").maxAge(0).build()
        );
    }

    @Test
    @DisplayName("정상적인 회원 탈퇴 처리")
    void shouldWithdraw_WhenValidUserDetails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userQueryPort.findById(100L)).willReturn(Optional.of(withdrawTestUser));
        given(globalCookiePort.getLogoutCookies()).willReturn(logoutCookies);

        // When
        List<ResponseCookie> result = withdrawService.withdraw(userDetails);

        // Then
        assertThat(result).isEqualTo(logoutCookies);

        // 사용자 조회 검증
        verify(userQueryPort).findById(100L);

        // 탈퇴 프로세스 수행 검증
        verify(deleteUserPort).performWithdrawProcess(100L);

        // 탈퇴 이벤트 발행 검증
        ArgumentCaptor<UserWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserWithdrawnEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(100L);
        assertThat(capturedEvent.socialId()).isEqualTo(withdrawTestUser.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(SocialProvider.KAKAO);

        // 로그아웃 쿠키 생성 검증
        verify(globalCookiePort).getLogoutCookies();
    }

    @Test
    @DisplayName("null 사용자 정보로 탈퇴 시 NULL_SECURITY_CONTEXT 예외 발생")
    void shouldThrowException_WhenUserDetailsIsNull() {
        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(null))
                .isInstanceOf(AuthCustomException.class)
                .hasFieldOrPropertyWithValue("authErrorCode", AuthErrorCode.NULL_SECURITY_CONTEXT);

        // null userDetails일 때는 어떤 메서드도 호출되지 않아야 함
    }

    @Test
    @DisplayName("존재하지 않는 사용자 탈퇴 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userQueryPort.findById(100L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", UserErrorCode.USER_NOT_FOUND);

        verify(userQueryPort).findById(100L);
        // 사용자가 없으므로 다른 작업들이 수행되지 않아야 함
    }

    @Test
    @DisplayName("탈퇴 프로세스 실패 시 예외 발생")
    void shouldThrowException_WhenWithdrawProcessFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userQueryPort.findById(100L)).willReturn(Optional.of(withdrawTestUser));
        doThrow(new RuntimeException("탈퇴 프로세스 실패"))
                .when(deleteUserPort).performWithdrawProcess(100L);

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("탈퇴 프로세스 실패");

        // 탈퇴 프로세스 실패로 인해 트랜잭션 롤백
        verify(userQueryPort).findById(100L);
        verify(deleteUserPort).performWithdrawProcess(100L);
        // 이벤트 발행은 롤백으로 인해 실행되지 않음
    }

    @Test
    @DisplayName("관리자 강제 탈퇴 처리 성공")
    void shouldForceWithdraw_WhenValidUserId() {
        // Given
        Long targetUserId = 200L;
        User targetUser = TestUsers.copyWithId(getOtherUser(), targetUserId);

        given(userQueryPort.findById(targetUserId)).willReturn(Optional.of(targetUser));

        // When
        withdrawService.forceWithdraw(targetUserId);

        // Then
        
        // 사용자 조회 검증
        verify(userQueryPort).findById(targetUserId);

        // 탈퇴 프로세스 수행 검증
        verify(deleteUserPort).performWithdrawProcess(targetUserId);

        // 탈퇴 이벤트 발행 검증
        ArgumentCaptor<UserWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserWithdrawnEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(targetUserId);
        assertThat(capturedEvent.socialId()).isEqualTo(targetUser.getSocialId());
        assertThat(capturedEvent.provider()).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 강제 탈퇴 시 USER_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenForceWithdrawUserNotFound() {
        // Given
        Long nonExistentUserId = 999L;
        given(userQueryPort.findById(nonExistentUserId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> withdrawService.forceWithdraw(nonExistentUserId))
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", UserErrorCode.USER_NOT_FOUND);

        verify(userQueryPort).findById(nonExistentUserId);
        // 사용자가 없으므로 다른 작업들이 수행되지 않아야 함
    }


    @Test
    @DisplayName("특정 토큰 정리 - 정상 케이스")
    void shouldCleanupSpecificToken_WhenValidParameters() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;

        // When
        withdrawService.cleanupSpecificToken(userId, tokenId);

        // Then
        verify(deleteUserPort).logoutUser(userId, tokenId);
    }

    @Test
    @DisplayName("특정 토큰 정리 - 다중 기기 로그아웃 시나리오")
    void shouldCleanupMultipleTokens_WhenMultipleDevices() {
        // Given - 동일 사용자의 여러 기기
        Long userId = 1L;
        Long tokenId1 = 100L;
        Long tokenId2 = 101L;
        Long tokenId3 = 102L;

        // When - 각 기기별로 개별 토큰 정리
        withdrawService.cleanupSpecificToken(userId, tokenId1);
        withdrawService.cleanupSpecificToken(userId, tokenId2);
        withdrawService.cleanupSpecificToken(userId, tokenId3);

        // Then - 각 토큰이 개별적으로 정리되어야 함
        verify(deleteUserPort).logoutUser(userId, tokenId1);
        verify(deleteUserPort).logoutUser(userId, tokenId2);
        verify(deleteUserPort).logoutUser(userId, tokenId3);
    }

    @Test
    @DisplayName("이벤트 발행 실패 시에도 탈퇴 프로세스 완료")
    void shouldCompleteWithdraw_WhenEventPublishingFails() {
        // Given
        given(userDetails.getUserId()).willReturn(100L);
        given(userQueryPort.findById(100L)).willReturn(Optional.of(withdrawTestUser));
        doThrow(new RuntimeException("이벤트 발행 실패"))
                .when(eventPublisher).publishEvent(any(UserWithdrawnEvent.class));

        // When & Then
        assertThatThrownBy(() -> withdrawService.withdraw(userDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이벤트 발행 실패");

        // 이벤트 발행 전까지의 모든 작업은 완료되어야 함
        verify(userQueryPort).findById(100L);
        verify(deleteUserPort).performWithdrawProcess(100L);
    }

    @Test
    @DisplayName("관리자 강제 탈퇴에서 탈퇴 데이터 처리 실패")
    void shouldHandleException_WhenForceWithdrawDataProcessFails() {
        // Given
        Long targetUserId = 200L;
        User targetUser = TestUsers.copyWithId(getOtherUser(), targetUserId);

        given(userQueryPort.findById(targetUserId)).willReturn(Optional.of(targetUser));
        doThrow(new RuntimeException("데이터 처리 실패"))
                .when(deleteUserPort).performWithdrawProcess(targetUserId);

        // When & Then
        assertThatThrownBy(() -> withdrawService.forceWithdraw(targetUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("데이터 처리 실패");

        verify(userQueryPort).findById(targetUserId);
        verify(deleteUserPort).performWithdrawProcess(targetUserId);
    }

    @Test
    @DisplayName("블랙리스트 추가 - 정상 케이스")
    void shouldAddToBlacklist_WhenUserExists() {
        // Given
        Long userId = 1L;
        User user = createTestUserWithId(userId);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));

        // When
        withdrawService.addToBlacklist(userId);

        // Then
        verify(userQueryPort).findById(userId);
        verify(deleteUserPort).saveBlackList(any(BlackList.class));
    }

    @Test
    @DisplayName("블랙리스트 추가 - 사용자가 존재하지 않는 경우")
    void shouldThrowException_WhenUserNotFoundForBlacklist() {
        // Given
        Long userId = 999L;
        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> withdrawService.addToBlacklist(userId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userQueryPort).findById(userId);
        verify(deleteUserPort, never()).saveBlackList(any(BlackList.class));
    }

    @Test
    @DisplayName("블랙리스트 추가 - null userId")
    void shouldThrowException_WhenNullUserIdForBlacklist() {
        // Given
        Long userId = null;

        // When & Then
        assertThatThrownBy(() -> withdrawService.addToBlacklist(userId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userQueryPort).findById(null);
        verify(deleteUserPort, never()).saveBlackList(any(BlackList.class));
    }

    @Test
    @DisplayName("블랙리스트 추가 - 중복 등록 시 예외 무시")
    void shouldIgnoreException_WhenDuplicateBlacklistEntry() {
        // Given
        Long userId = 1L;
        User user = TestUsers.copyWithId(TestUsers.USER1, userId);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        willThrow(new DataIntegrityViolationException("Duplicate entry")).given(deleteUserPort).saveBlackList(any(BlackList.class));

        // When (예외가 발생하지 않아야 함)
        withdrawService.addToBlacklist(userId);

        // Then
        verify(userQueryPort).findById(userId);
        verify(deleteUserPort).saveBlackList(any(BlackList.class));
    }

    @Test
    @DisplayName("사용자 제재 - 정상 케이스")
    void shouldBanUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        User user = TestUsers.copyWithId(TestUsers.USER1, userId);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        // JPA 변경 감지로 자동 저장되므로 userCommandPort 스터빙 불필요

        // When
        withdrawService.banUser(userId);

        // Then
        verify(userQueryPort).findById(userId);
        // JPA 변경 감지로 자동 저장되므로 명시적 save 호출 없음
        assertThat(user.getRole()).isEqualTo(UserRole.BAN);
    }

    @Test
    @DisplayName("사용자 제재 - 사용자가 존재하지 않는 경우")
    void shouldThrowException_WhenUserNotFoundForBan() {
        // Given
        Long userId = 999L;
        given(userQueryPort.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> withdrawService.banUser(userId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userQueryPort).findById(userId);
    }

    @Test
    @DisplayName("사용자 제재 - null userId")
    void shouldThrowException_WhenNullUserIdForBan() {
        // Given
        Long userId = null;

        // When & Then
        assertThatThrownBy(() -> withdrawService.banUser(userId))
                .isInstanceOf(UserCustomException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userQueryPort).findById(null);
    }

    @Test
    @DisplayName("사용자 제재 - 이미 BAN인 사용자")
    void shouldBanUser_WhenUserAlreadyBanned() {
        // Given
        Long userId = 1L;
        User user = TestUsers.copyWithId(TestUsers.USER1, userId);
        user.updateRole(UserRole.BAN);

        given(userQueryPort.findById(userId)).willReturn(Optional.of(user));
        // JPA 변경 감지로 자동 저장되므로 userCommandPort 스터빙 불필요

        // When
        withdrawService.banUser(userId);

        // Then
        verify(userQueryPort).findById(userId);
        // JPA 변경 감지로 자동 저장되므로 명시적 save 호출 없음
        assertThat(user.getRole()).isEqualTo(UserRole.BAN);
    }
}
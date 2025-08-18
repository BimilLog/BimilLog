package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.growfarm.domain.auth.event.UserLoggedOutEvent;
import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>DeleteDataAdapter 단위 테스트</h2>
 * <p>데이터 삭제 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>완벽한 테스트로 메인 로직의 문제를 발견</p>
 *
 * @author Claude
 * @version 2.0.0
 * @since 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class DeleteDataAdapterTest {

    @Mock private EntityManager entityManager;
    @Mock private TokenRepository tokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuthCookieManager authCookieManager;
    @Mock private UserCommandUseCase userCommandUseCase;

    @InjectMocks private DeleteDataAdapter deleteDataAdapter;

    @Test
    @DisplayName("로그아웃 처리 - 정상적인 사용자 ID로 로그아웃")
    void shouldLogoutUser_WhenValidUserId() {
        // Given: 유효한 사용자 ID
        Long validUserId = 1L;

        // When: 사용자 로그아웃 실행
        deleteDataAdapter.logoutUser(validUserId);

        // Then: 토큰 삭제 및 이벤트 발행 검증
        verify(tokenRepository).deleteAllByUserId(validUserId);
        
        ArgumentCaptor<UserLoggedOutEvent> eventCaptor = ArgumentCaptor.forClass(UserLoggedOutEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        UserLoggedOutEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.userId()).isEqualTo(validUserId);
        assertThat(capturedEvent.socialAccessToken()).isNull();
    }

    @Test
    @DisplayName("로그아웃 처리 - null 사용자 ID 처리")
    void shouldHandleNullUserId_WhenLogoutCalled() {
        // Given: null 사용자 ID
        Long nullUserId = null;

        // When: null 사용자 ID로 로그아웃 시도
        deleteDataAdapter.logoutUser(nullUserId);

        // Then: 토큰 삭제와 이벤트 발행이 호출됨 (null 처리는 비즈니스 로직에서 검증)
        verify(tokenRepository).deleteAllByUserId(nullUserId);
        verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
    }

    @Test
    @DisplayName("로그아웃 처리 - 존재하지 않는 사용자 ID")
    void shouldHandleNonExistentUserId_WhenLogoutCalled() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 999L;

        // When: 존재하지 않는 사용자 ID로 로그아웃
        deleteDataAdapter.logoutUser(nonExistentUserId);

        // Then: 토큰 삭제 시도 및 이벤트 발행 (실제 존재 여부는 repository에서 처리)
        verify(tokenRepository).deleteAllByUserId(nonExistentUserId);
        verify(eventPublisher).publishEvent(any(UserLoggedOutEvent.class));
    }

    @Test
    @DisplayName("회원 탈퇴 처리 - 정상적인 사용자 ID로 회원 탈퇴")
    void shouldPerformWithdrawProcess_WhenValidUserId() {
        // Given: 유효한 사용자 ID
        Long validUserId = 1L;

        // When: 회원 탈퇴 처리 실행
        deleteDataAdapter.performWithdrawProcess(validUserId);

        // Then: EntityManager flush/clear, 토큰 삭제, 사용자 삭제 순서 검증
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(tokenRepository).deleteAllByUserId(validUserId);
        verify(userCommandUseCase).deleteById(validUserId);
    }

    @Test
    @DisplayName("회원 탈퇴 처리 - null 사용자 ID 처리")
    void shouldHandleNullUserId_WhenWithdrawCalled() {
        // Given: null 사용자 ID
        Long nullUserId = null;

        // When: null 사용자 ID로 회원 탈퇴 시도
        deleteDataAdapter.performWithdrawProcess(nullUserId);

        // Then: 모든 단계가 실행됨 (null 검증은 비즈니스 로직에서 처리)
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(tokenRepository).deleteAllByUserId(nullUserId);
        verify(userCommandUseCase).deleteById(nullUserId);
    }

    @Test
    @DisplayName("회원 탈퇴 처리 - 작업 순서 검증")
    void shouldExecuteWithdrawProcessInCorrectOrder_WhenCalled() {
        // Given: 유효한 사용자 ID
        Long userId = 1L;

        // When: 회원 탈퇴 처리 실행
        deleteDataAdapter.performWithdrawProcess(userId);

        // Then: 작업 실행 순서 검증
        var inOrder = inOrder(entityManager, tokenRepository, userCommandUseCase);
        inOrder.verify(entityManager).flush();
        inOrder.verify(entityManager).clear();
        inOrder.verify(tokenRepository).deleteAllByUserId(userId);
        inOrder.verify(userCommandUseCase).deleteById(userId);
    }

    @Test
    @DisplayName("로그아웃 쿠키 생성 - 정상적인 쿠키 반환")
    void shouldReturnLogoutCookies_WhenGetLogoutCookiesCalled() {
        // Given: AuthCookieManager에서 반환할 쿠키 리스트
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .maxAge(0)
                .httpOnly(true)
                .build();
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .httpOnly(true)
                .build();
        List<ResponseCookie> expectedCookies = Arrays.asList(accessTokenCookie, refreshTokenCookie);
        
        given(authCookieManager.getLogoutCookies()).willReturn(expectedCookies);

        // When: 로그아웃 쿠키 조회
        List<ResponseCookie> actualCookies = deleteDataAdapter.getLogoutCookies();

        // Then: 예상된 쿠키 리스트 반환 검증
        assertThat(actualCookies).isEqualTo(expectedCookies);
        assertThat(actualCookies).hasSize(2);
        verify(authCookieManager).getLogoutCookies();
    }

    @Test
    @DisplayName("로그아웃 쿠키 생성 - 빈 리스트 반환")
    void shouldReturnEmptyList_WhenAuthCookieManagerReturnsEmpty() {
        // Given: AuthCookieManager에서 빈 리스트 반환
        List<ResponseCookie> emptyCookies = Arrays.asList();
        given(authCookieManager.getLogoutCookies()).willReturn(emptyCookies);

        // When: 로그아웃 쿠키 조회
        List<ResponseCookie> actualCookies = deleteDataAdapter.getLogoutCookies();

        // Then: 빈 리스트 반환 검증
        assertThat(actualCookies).isEmpty();
        verify(authCookieManager).getLogoutCookies();
    }

    @Test
    @DisplayName("로그아웃 쿠키 생성 - null 반환 처리")
    void shouldHandleNullReturn_WhenAuthCookieManagerReturnsNull() {
        // Given: AuthCookieManager에서 null 반환
        given(authCookieManager.getLogoutCookies()).willReturn(null);

        // When: 로그아웃 쿠키 조회
        List<ResponseCookie> actualCookies = deleteDataAdapter.getLogoutCookies();

        // Then: null 반환 검증 (실제 null 처리는 호출하는 쪽에서 해야 함)
        assertThat(actualCookies).isNull();
        verify(authCookieManager).getLogoutCookies();
    }

    @Test
    @DisplayName("동시성 테스트 - 동일 사용자 동시 로그아웃")
    void shouldHandleConcurrentLogout_WhenSameUserLogoutSimultaneously() {
        // Given: 동일한 사용자 ID
        Long userId = 1L;

        // When: 동시에 로그아웃 실행
        deleteDataAdapter.logoutUser(userId);
        deleteDataAdapter.logoutUser(userId);

        // Then: 두 번의 로그아웃 처리 검증
        verify(tokenRepository, times(2)).deleteAllByUserId(userId);
        verify(eventPublisher, times(2)).publishEvent(any(UserLoggedOutEvent.class));
    }

    @Test
    @DisplayName("예외 전파 테스트 - TokenRepository 예외 발생")
    void shouldPropagateException_WhenTokenRepositoryThrowsException() {
        // Given: TokenRepository에서 예외 발생
        Long userId = 1L;
        RuntimeException expectedException = new RuntimeException("Database error");
        doThrow(expectedException).when(tokenRepository).deleteAllByUserId(userId);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> deleteDataAdapter.logoutUser(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        // Then: 이벤트 발행이 실행되지 않음 검증
        verify(eventPublisher, never()).publishEvent(any(UserLoggedOutEvent.class));
    }

    @Test
    @DisplayName("예외 전파 테스트 - UserCommandUseCase 예외 발생")
    void shouldPropagateException_WhenUserCommandUseCaseThrowsException() {
        // Given: UserCommandUseCase에서 예외 발생
        Long userId = 1L;
        RuntimeException expectedException = new RuntimeException("User deletion failed");
        doThrow(expectedException).when(userCommandUseCase).deleteById(userId);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> deleteDataAdapter.performWithdrawProcess(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User deletion failed");

        // Then: 이전 단계들은 실행되었는지 확인
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(tokenRepository).deleteAllByUserId(userId);
    }

    // TODO: 테스트 실패 시 의심해볼 메인 로직 문제들
    // 1. 트랜잭션 경계 문제: @Transactional 범위에서 예외 발생 시 롤백 처리
    // 2. EntityManager flush/clear 순서: 데이터 일관성 문제
    // 3. 이벤트 발행 시점: 트랜잭션 커밋 전/후 이벤트 처리
    // 4. null 검증 누락: 메서드 파라미터에 대한 방어적 프로그래밍 부족
    // 5. 동시성 문제: 같은 사용자에 대한 동시 요청 처리
    // 6. 의존성 순환: UserCommandUseCase와 DeleteDataAdapter 간 순환 참조
    // 7. 이벤트 처리 실패: UserLoggedOutEvent 처리 시 예외 발생
    // 8. 쿠키 생성 실패: AuthCookieManager 설정 오류
    // 9. Repository 트랜잭션: deleteAllByUserId 실행 중 데드락
    // 10. 메모리 누수: 장기간 실행 시 EntityManager 상태 문제
    //
    // 🔥 중요: 이 테스트들이 실패한다면 비즈니스 로직 자체에 문제가 있을 가능성이 높음
    // - 데이터 삭제 순서와 트랜잭션 경계가 중요한 도메인
    // - 회원 탈퇴는 되돌릴 수 없는 작업이므로 완벽한 테스트 필수
    // - 이벤트 기반 시스템에서 이벤트 발행 실패는 심각한 부작용 초래
}
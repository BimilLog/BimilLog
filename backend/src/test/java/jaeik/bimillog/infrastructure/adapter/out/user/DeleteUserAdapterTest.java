package jaeik.bimillog.infrastructure.adapter.out.user;

import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.BlackListRepository;
import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.TokenRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jakarta.persistence.EntityManager;
import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>DeleteDataAdapter 단위 테스트</h2>
 * <p>데이터 삭제 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>완벽한 테스트로 메인 로직의 문제를 발견</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
class DeleteUserAdapterTest extends BaseUnitTest {

    @Mock private EntityManager entityManager;
    @Mock private TokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private BlackListRepository blackListRepository;

    @InjectMocks private DeleteUserAdapter deleteDataAdapter;

    @Test
    @DisplayName("로그아웃 처리 - 정상적인 사용자 ID로 로그아웃")
    void shouldLogoutUser_WhenValidUserId() {
        // Given: 유효한 사용자 ID
        Long validUserId = 1L;

        // When: 사용자 로그아웃 실행 (특정 토큰 삭제)
        Long tokenId = 123L;
        deleteDataAdapter.logoutUser(validUserId, tokenId);

        // Then: 특정 토큰 삭제 검증
        verify(tokenRepository).deleteById(tokenId);
    }


    @Test
    @DisplayName("로그아웃 처리 - 존재하지 않는 사용자 ID")
    void shouldHandleNonExistentUserId_WhenLogoutCalled() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 999L;

        // When: 존재하지 않는 사용자 ID로 모든 토큰 삭제 (tokenId = null)
        deleteDataAdapter.logoutUser(nonExistentUserId, null);

        // Then: 모든 토큰 삭제 시도 (실제 존재 여부는 repository에서 처리)
        verify(tokenRepository).deleteAllByUserId(nonExistentUserId);
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
        verify(userRepository).deleteById(validUserId);
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
        verify(userRepository).deleteById(nullUserId);
    }

    @Test
    @DisplayName("회원 탈퇴 처리 - 작업 순서 검증")
    void shouldExecuteWithdrawProcessInCorrectOrder_WhenCalled() {
        // Given: 유효한 사용자 ID
        Long userId = 1L;

        // When: 회원 탈퇴 처리 실행
        deleteDataAdapter.performWithdrawProcess(userId);

        // Then: 작업 실행 순서 검증
        var inOrder = inOrder(entityManager, tokenRepository, userRepository);
        inOrder.verify(entityManager).flush();
        inOrder.verify(entityManager).clear();
        inOrder.verify(tokenRepository).deleteAllByUserId(userId);
        inOrder.verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("블랙리스트 저장 - 정상 처리")
    void shouldSaveBlackList_WhenValidBlackList() {
        // Given: 유효한 블랙리스트 엔티티
        BlackList blackList = BlackList.createBlackList("kakao123", jaeik.bimillog.domain.user.entity.SocialProvider.KAKAO);

        // When: 블랙리스트 저장
        deleteDataAdapter.saveBlackList(blackList);

        // Then: 저장 검증
        verify(blackListRepository).save(blackList);
    }

    @Test
    @DisplayName("예외 전파 테스트 - TokenRepository 예외 발생")
    void shouldPropagateException_WhenTokenRepositoryThrowsException() {
        // Given: TokenRepository에서 예외 발생
        Long userId = 1L;
        RuntimeException expectedException = new RuntimeException("Database error");
        doThrow(expectedException).when(tokenRepository).deleteAllByUserId(userId);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> deleteDataAdapter.logoutUser(userId, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    @DisplayName("예외 전파 테스트 - UserRepository 예외 발생")
    void shouldPropagateException_WhenUserRepositoryThrowsException() {
        // Given: UserRepository에서 예외 발생
        Long userId = 1L;
        RuntimeException expectedException = new RuntimeException("User deletion failed");
        doThrow(expectedException).when(userRepository).deleteById(userId);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> deleteDataAdapter.performWithdrawProcess(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User deletion failed");

        // Then: 이전 단계들은 실행되었는지 확인
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(tokenRepository).deleteAllByUserId(userId);
    }
}
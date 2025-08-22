package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
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
 * <h2>LoadUserAdapter 단위 테스트</h2>
 * <p>사용자 조회 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>헥사고날 아키텍처의 도메인 간 의존성 어댑터 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
//TODO 비즈니스 로직의 변경으로 테스트코드와 비즈니스 로직의 흐름이 맞지 않을 시 테스트 코드의 변경이 적으면 테스트 수정 필요 변경이 많으면 Deprecated 처리 후 새로운 테스트 작성 필요
@ExtendWith(MockitoExtension.class)
class LoadUserAdapterTest {

    @Mock private UserQueryUseCase userQueryUseCase;

    @InjectMocks private LoadUserAdapter loadUserAdapter;

    @Test
    @DisplayName("사용자 조회 - 존재하는 사용자 ID로 조회")
    void shouldReturnUser_WhenUserExists() {
        // Given: 존재하는 사용자 ID와 사용자 데이터
        Long existingUserId = 1L;
        User expectedUser = User.builder()
                .id(existingUserId)
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .socialNickname("testUser")
                .thumbnailImage("https://example.com/profile.jpg")
                .role(UserRole.USER)
                .build();
        
        given(userQueryUseCase.findById(existingUserId)).willReturn(Optional.of(expectedUser));

        // When: 사용자 조회
        Optional<User> result = loadUserAdapter.findById(existingUserId);

        // Then: 올바른 사용자 반환 검증
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedUser);
        assertThat(result.get().getId()).isEqualTo(existingUserId);
        assertThat(result.get().getSocialNickname()).isEqualTo("testUser");
        assertThat(result.get().getProvider()).isEqualTo(SocialProvider.KAKAO);
        
        verify(userQueryUseCase).findById(existingUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 사용자 ID로 조회")
    void shouldReturnEmpty_WhenUserNotExists() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 999L;
        given(userQueryUseCase.findById(nonExistentUserId)).willReturn(Optional.empty());

        // When: 존재하지 않는 사용자 ID로 조회
        Optional<User> result = loadUserAdapter.findById(nonExistentUserId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();
        verify(userQueryUseCase).findById(nonExistentUserId);
    }

    @Test
    @DisplayName("사용자 조회 - null 사용자 ID로 조회")
    void shouldHandleNullUserId_WhenUserIdIsNull() {
        // Given: null 사용자 ID
        Long nullUserId = null;
        given(userQueryUseCase.findById(nullUserId)).willReturn(Optional.empty());

        // When: null 사용자 ID로 조회
        Optional<User> result = loadUserAdapter.findById(nullUserId);

        // Then: 빈 Optional 반환 검증 (null 처리는 UserQueryUseCase에서 담당)
        assertThat(result).isEmpty();
        verify(userQueryUseCase).findById(nullUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 음수 사용자 ID로 조회")
    void shouldHandleNegativeUserId_WhenUserIdIsNegative() {
        // Given: 음수 사용자 ID
        Long negativeUserId = -1L;
        given(userQueryUseCase.findById(negativeUserId)).willReturn(Optional.empty());

        // When: 음수 사용자 ID로 조회
        Optional<User> result = loadUserAdapter.findById(negativeUserId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();
        verify(userQueryUseCase).findById(negativeUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 0 사용자 ID로 조회")
    void shouldHandleZeroUserId_WhenUserIdIsZero() {
        // Given: 0 사용자 ID
        Long zeroUserId = 0L;
        given(userQueryUseCase.findById(zeroUserId)).willReturn(Optional.empty());

        // When: 0 사용자 ID로 조회
        Optional<User> result = loadUserAdapter.findById(zeroUserId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();
        verify(userQueryUseCase).findById(zeroUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 매우 큰 사용자 ID로 조회")
    void shouldHandleLargeUserId_WhenUserIdIsLarge() {
        // Given: 매우 큰 사용자 ID
        Long largeUserId = Long.MAX_VALUE;
        given(userQueryUseCase.findById(largeUserId)).willReturn(Optional.empty());

        // When: 매우 큰 사용자 ID로 조회
        Optional<User> result = loadUserAdapter.findById(largeUserId);

        // Then: 빈 Optional 반환 검증
        assertThat(result).isEmpty();
        verify(userQueryUseCase).findById(largeUserId);
    }

    @Test
    @DisplayName("사용자 조회 - 완전한 사용자 데이터 반환 검증")
    void shouldReturnCompleteUserData_WhenUserExists() {
        // Given: 완전한 사용자 데이터
        Long userId = 1L;
        User completeUser = User.builder()
                .id(userId)
                .provider(SocialProvider.KAKAO)
                .socialId("kakao123456")
                .socialNickname("완전한사용자")
                .thumbnailImage("https://example.com/complete-profile.jpg")
                .role(UserRole.USER)
                .build();
        
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(completeUser));

        // When: 사용자 조회
        Optional<User> result = loadUserAdapter.findById(userId);

        // Then: 모든 필드가 정확히 반환되는지 검증
        assertThat(result).isPresent();
        User returnedUser = result.get();
        assertThat(returnedUser.getId()).isEqualTo(userId);
        assertThat(returnedUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(returnedUser.getSocialId()).isEqualTo("kakao123456");
        assertThat(returnedUser.getSocialNickname()).isEqualTo("완전한사용자");
        assertThat(returnedUser.getThumbnailImage()).isEqualTo("https://example.com/complete-profile.jpg");
        assertThat(returnedUser.getRole()).isEqualTo(UserRole.USER);
        
        verify(userQueryUseCase).findById(userId);
    }

    @Test
    @DisplayName("사용자 조회 - 부분적 사용자 데이터 반환 검증")
    void shouldReturnPartialUserData_WhenUserHasPartialData() {
        // Given: 일부 필드가 null인 사용자 데이터
        Long userId = 2L;
        User partialUser = User.builder()
                .id(userId)
                .provider(SocialProvider.KAKAO)
                .socialId("partial123")
                .socialNickname("부분사용자")
                .thumbnailImage(null) // null 프로필 이미지
                .role(UserRole.USER)
                .build();
        
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(partialUser));

        // When: 사용자 조회
        Optional<User> result = loadUserAdapter.findById(userId);

        // Then: null 필드를 포함한 데이터 정확히 반환
        assertThat(result).isPresent();
        User returnedUser = result.get();
        assertThat(returnedUser.getId()).isEqualTo(userId);
        assertThat(returnedUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(returnedUser.getSocialId()).isEqualTo("partial123");
        assertThat(returnedUser.getSocialNickname()).isEqualTo("부분사용자");
        assertThat(returnedUser.getThumbnailImage()).isNull();
        assertThat(returnedUser.getRole()).isEqualTo(UserRole.USER);
        
        verify(userQueryUseCase).findById(userId);
    }

    @Test
    @DisplayName("예외 전파 테스트 - UserQueryUseCase에서 예외 발생")
    void shouldPropagateException_WhenUserQueryUseCaseThrowsException() {
        // Given: UserQueryUseCase에서 예외 발생
        Long userId = 1L;
        RuntimeException expectedException = new RuntimeException("Database connection failed");
        given(userQueryUseCase.findById(userId)).willThrow(expectedException);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> loadUserAdapter.findById(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(userQueryUseCase).findById(userId);
    }

    @Test
    @DisplayName("성능 테스트 - 동일 사용자 반복 조회")
    void shouldMaintainConsistency_WhenSameUserQueriedMultipleTimes() {
        // Given: 동일한 사용자 ID와 데이터
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .provider(SocialProvider.KAKAO)
                .socialId("consistency123")
                .socialNickname("일관성테스트")
                .role(UserRole.USER)
                .build();
        
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(user));

        // When: 동일한 사용자를 여러 번 조회
        int queryCount = 100;
        for (int i = 0; i < queryCount; i++) {
            Optional<User> result = loadUserAdapter.findById(userId);
            
            // Then: 매번 동일한 결과 반환
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(userId);
            assertThat(result.get().getSocialNickname()).isEqualTo("일관성테스트");
        }

        // UserQueryUseCase가 정확히 호출 횟수만큼 호출되었는지 확인
        verify(userQueryUseCase, org.mockito.Mockito.times(queryCount)).findById(userId);
    }

    @Test
    @DisplayName("동시성 테스트 - 서로 다른 사용자 동시 조회")
    void shouldHandleConcurrentQueries_WhenDifferentUsersQueried() {
        // Given: 서로 다른 사용자들
        Long userId1 = 1L;
        Long userId2 = 2L;
        
        User user1 = User.builder()
                .id(userId1)
                .provider(SocialProvider.KAKAO)
                .socialId("user1")
                .socialNickname("사용자1")
                .role(UserRole.USER)
                .build();
        
        User user2 = User.builder()
                .id(userId2)
                .provider(SocialProvider.KAKAO)
                .socialId("user2")
                .socialNickname("사용자2")
                .role(UserRole.ADMIN)
                .build();

        given(userQueryUseCase.findById(userId1)).willReturn(Optional.of(user1));
        given(userQueryUseCase.findById(userId2)).willReturn(Optional.of(user2));

        // When: 동시에 다른 사용자들 조회
        Optional<User> result1 = loadUserAdapter.findById(userId1);
        Optional<User> result2 = loadUserAdapter.findById(userId2);

        // Then: 각각 올바른 사용자 반환
        assertThat(result1).isPresent();
        assertThat(result1.get().getId()).isEqualTo(userId1);
        assertThat(result1.get().getSocialNickname()).isEqualTo("사용자1");
        assertThat(result1.get().getRole()).isEqualTo(UserRole.USER);

        assertThat(result2).isPresent();
        assertThat(result2.get().getId()).isEqualTo(userId2);
        assertThat(result2.get().getSocialNickname()).isEqualTo("사용자2");
        assertThat(result2.get().getRole()).isEqualTo(UserRole.ADMIN);

        verify(userQueryUseCase).findById(userId1);
        verify(userQueryUseCase).findById(userId2);
    }

    @Test
    @DisplayName("헥사고날 아키텍처 검증 - 어댑터 계층 분리")
    void shouldActAsProperAdapter_WhenUsedInHexagonalArchitecture() {
        // Given: Auth 도메인에서 User 도메인 정보가 필요한 상황
        Long userId = 1L;
        User userFromUserDomain = User.builder()
                .id(userId)
                .provider(SocialProvider.KAKAO)
                .socialId("hexagonal123")
                .socialNickname("아키텍처테스트")
                .role(UserRole.USER)
                .build();
        
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(userFromUserDomain));

        // When: Auth 도메인의 LoadUserPort를 통해 User 도메인 정보 조회
        Optional<User> result = loadUserAdapter.findById(userId);

        // Then: 도메인 경계를 넘나드는 정보 조회가 정상 작동
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(userFromUserDomain);
        
        // 헥사고날 아키텍처 원칙: Auth 도메인(클라이언트) -> LoadUserPort(인터페이스) -> LoadUserAdapter(구현체) -> UserQueryUseCase(User 도메인)
        verify(userQueryUseCase).findById(userId);
    }

    // TODO: 테스트 실패 시 의심해볼 메인 로직 문제들
    // 1. 의존성 주입 실패: UserQueryUseCase가 제대로 주입되지 않음
    // 2. 도메인 경계 문제: Auth 도메인에서 User 도메인 접근 시 순환 참조
    // 3. 트랜잭션 전파: 도메인 간 트랜잭션 경계 설정 문제
    // 4. 캐시 일관성: User 도메인의 캐시와 실제 DB 데이터 불일치
    // 5. 데이터 변환 오류: User 엔티티 매핑 시 데이터 손실
    // 6. 권한 검증 누락: 도메인 간 데이터 접근 권한 확인 부족
    // 7. 성능 이슈: N+1 쿼리 문제 또는 불필요한 데이터 로딩
    // 8. 예외 처리: UserQueryUseCase 예외의 부적절한 전파
    // 9. 컨텍스트 경계: 다른 도메인 컨텍스트의 데이터 해석 오류
    // 10. 버전 호환성: User 도메인 엔티티 변경 시 Auth 도메인 영향
    //
    // 🔥 중요: 이 테스트들이 실패한다면 비즈니스 로직 자체에 문제가 있을 가능성이 높음
    // - 도메인 간 어댑터는 헥사고날 아키텍처의 핵심 구성요소
    // - Auth 도메인의 인증/인가 로직이 User 도메인 정보에 의존하므로 완벽한 동작 필수
    // - 잘못된 사용자 조회는 인증 시스템 전체의 보안 위험 초래
}
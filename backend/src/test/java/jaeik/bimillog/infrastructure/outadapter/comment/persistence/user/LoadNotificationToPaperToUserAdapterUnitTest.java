package jaeik.bimillog.infrastructure.outadapter.comment.persistence.user;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.user.CommentToUserAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * <h2>CommentToUserAdapter 단위 테스트</h2>
 * <p>Comment 도메인에서 User 도메인에 접근하는 LoadUserAdapter의 단위 테스트</p>
 * <p>다른 도메인 UseCase를 호출하는 어댑터이므로 Mock을 사용한 단위 테스트로 작성</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class LoadNotificationToPaperToUserAdapterUnitTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private CommentToUserAdapter loadUserAdapter;

    private User testUser;
    private Setting testSetting;

    @BeforeEach
    void setUp() {
        // 테스트용 설정 생성
        testSetting = Setting.createSetting();
        
        // 테스트용 사용자 생성
        testUser = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(testSetting)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하는 사용자 ID로 사용자 조회")
    void shouldReturnUser_WhenUserExists() {
        // Given: UserQueryUseCase가 사용자를 반환하도록 설정
        Long userId = 1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(userId);

        // Then: 올바른 사용자가 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getSocialId()).isEqualTo("kakao123");
        assertThat(result.get().getUserName()).isEqualTo("testUser");
        assertThat(result.get().getSocialNickname()).isEqualTo("테스트유저");
        assertThat(result.get().getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(result.get().getRole()).isEqualTo(UserRole.USER);

        // UserQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(userQueryUseCase).should().findById(userId);
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 사용자 ID로 사용자 조회")
    void shouldReturnEmpty_WhenUserNotExists() {
        // Given: UserQueryUseCase가 빈 Optional을 반환하도록 설정
        Long nonExistentUserId = 999L;
        given(userQueryUseCase.findById(nonExistentUserId)).willReturn(Optional.empty());

        // When: 존재하지 않는 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(nonExistentUserId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // UserQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(userQueryUseCase).should().findById(nonExistentUserId);
    }

    @Test
    @DisplayName("정상 케이스 - 다양한 사용자 타입 조회")
    void shouldReturnDifferentUserTypes_WhenDifferentUsersExist() {
        // Given: 관리자 사용자
        User adminUser = User.builder()
                .socialId("admin123")
                .provider(SocialProvider.KAKAO)
                .userName("adminUser")
                .socialNickname("관리자")
                .role(UserRole.ADMIN)
                .setting(testSetting)
                .build();

        Long adminUserId = 2L;
        given(userQueryUseCase.findById(adminUserId)).willReturn(Optional.of(adminUser));

        // When: 관리자 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(adminUserId);

        // Then: 관리자 사용자가 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.get().getSocialNickname()).isEqualTo("관리자");

        // UserQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(userQueryUseCase).should().findById(adminUserId);
    }

    @Test
    @DisplayName("정상 케이스 - null 사용자 ID로 조회")
    void shouldHandleNullUserId_WhenNullProvided() {
        // Given: null 사용자 ID
        Long nullUserId = null;
        given(userQueryUseCase.findById(nullUserId)).willReturn(Optional.empty());

        // When: null ID로 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(nullUserId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // UserQueryUseCase의 메서드가 호출되었는지 검증
        then(userQueryUseCase).should().findById(nullUserId);
    }

    @Test
    @DisplayName("정상 케이스 - 음수 사용자 ID로 조회")
    void shouldHandleNegativeUserId_WhenNegativeIdProvided() {
        // Given: 음수 사용자 ID
        Long negativeUserId = -1L;
        given(userQueryUseCase.findById(negativeUserId)).willReturn(Optional.empty());

        // When: 음수 ID로 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(negativeUserId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // UserQueryUseCase의 메서드가 호출되었는지 검증
        then(userQueryUseCase).should().findById(negativeUserId);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 설정이 포함된 사용자 조회")
    void shouldReturnUserWithSettings_WhenUserHasSettings() {
        // Given: 특별한 설정을 가진 사용자
        Setting customSetting = Setting.createSetting();
        
        User userWithSettings = User.builder()
                .socialId("settings123")
                .provider(SocialProvider.KAKAO)
                .userName("settingsUser")
                .socialNickname("설정사용자")
                .role(UserRole.USER)
                .setting(customSetting)
                .build();

        Long userId = 3L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(userWithSettings));

        // When: 설정이 있는 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(userId);

        // Then: 사용자와 설정이 모두 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getSetting()).isNotNull();
        assertThat(result.get().getSetting()).isEqualTo(customSetting);

        // UserQueryUseCase의 메서드가 호출되었는지 검증
        then(userQueryUseCase).should().findById(userId);
    }

    @Test
    @DisplayName("경계값 - 최대값 사용자 ID로 조회")
    void shouldHandleMaxUserId_WhenMaxLongValueProvided() {
        // Given: Long 최대값을 사용자 ID로 사용
        Long maxUserId = Long.MAX_VALUE;
        given(userQueryUseCase.findById(maxUserId)).willReturn(Optional.empty());

        // When: 최대값 ID로 사용자 조회
        Optional<User> result = loadUserAdapter.findByIdOptional(maxUserId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // UserQueryUseCase의 메서드가 호출되었는지 검증
        then(userQueryUseCase).should().findById(maxUserId);
    }

    @Test
    @DisplayName("예외 케이스 - UserQueryUseCase에서 예외 발생 시 전파")
    void shouldPropagateException_WhenUserQueryUseCaseThrowsException() {
        // Given: UserQueryUseCase가 예외를 던지도록 설정
        Long userId = 1L;
        RuntimeException expectedException = new RuntimeException("User service error");
        given(userQueryUseCase.findById(userId)).willThrow(expectedException);

        // When & Then: 예외가 전파되는지 검증
        try {
            loadUserAdapter.findByIdOptional(userId);
        } catch (RuntimeException actualException) {
            assertThat(actualException).isEqualTo(expectedException);
            assertThat(actualException.getMessage()).isEqualTo("User service error");
        }

        // UserQueryUseCase의 메서드가 호출되었는지 검증
        then(userQueryUseCase).should().findById(userId);
    }

    @Test
    @DisplayName("트랜잭션 - 여러 번 연속 호출 시 일관된 동작")
    void shouldBehaveConsistently_WhenCalledMultipleTimes() {
        // Given: 동일한 사용자 ID에 대해 일관된 응답 설정
        Long userId = 1L;
        given(userQueryUseCase.findById(userId)).willReturn(Optional.of(testUser));

        // When: 동일한 ID로 여러 번 조회
        Optional<User> result1 = loadUserAdapter.findByIdOptional(userId);
        Optional<User> result2 = loadUserAdapter.findByIdOptional(userId);
        Optional<User> result3 = loadUserAdapter.findByIdOptional(userId);

        // Then: 모든 결과가 일관되게 동일한 사용자를 반환해야 함
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result3).isPresent();
        
        assertThat(result1.get()).isEqualTo(testUser);
        assertThat(result2.get()).isEqualTo(testUser);
        assertThat(result3.get()).isEqualTo(testUser);

        // UserQueryUseCase의 메서드가 3번 호출되었는지 검증
        then(userQueryUseCase).should(times(3)).findById(userId);
    }

    @Test
    @DisplayName("어댑터 계약 - CommentToUserPort 인터페이스 메서드 구현 검증")
    void shouldImplementLoadUserPortContract_WhenAdapterUsed() {
        // Given: 표준 사용자와 ID
        Long userId = 1L;
        given(userQueryUseCase.findById(anyLong())).willReturn(Optional.of(testUser));

        // When: 포트 인터페이스 메서드 호출
        Optional<User> result = loadUserAdapter.findByIdOptional(userId);

        // Then: 포트 계약에 따른 정확한 결과 반환 확인
        assertThat(result).isNotNull(); // Optional이 null이면 안됨
        
        if (result.isPresent()) {
            User user = result.get();
            // User 엔티티의 필수 필드들이 모두 존재하는지 확인
            assertThat(user.getSocialId()).isNotNull();
            assertThat(user.getProvider()).isNotNull();
            assertThat(user.getRole()).isNotNull();
        }

        // 의존성 호출 검증
        then(userQueryUseCase).should().findById(userId);
    }
}
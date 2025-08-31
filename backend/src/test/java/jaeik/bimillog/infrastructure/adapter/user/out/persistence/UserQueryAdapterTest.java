package jaeik.bimillog.infrastructure.adapter.user.out.persistence;

import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserQueryAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserQueryAdapter 테스트</h2>
 * <p>사용자 조회 어댑터의 persistence 동작 검증</p>
 * <p>User, Setting 엔티티의 다양한 조회 패턴 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserQueryAdapterTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SettingRepository settingRepository;

    @InjectMocks
    private UserQueryAdapter userQueryAdapter;

    @Test
    @DisplayName("정상 케이스 - ID로 사용자 조회")
    void shouldFindUser_WhenValidIdProvided() {
        // Given: 조회할 사용자 ID와 예상 결과
        Long userId = 1L;
        User expectedUser = User.builder()
                .id(userId)
                .userName("testUser")
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .build();
                
        given(userRepository.findById(userId)).willReturn(Optional.of(expectedUser));

        // When: 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findById(userId);

        // Then: 올바른 사용자가 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
        assertThat(result.get().getUserName()).isEqualTo("testUser");
        verify(userRepository).findById(eq(userId));
    }

    @Test
    @DisplayName("정상 케이스 - ID와 설정을 포함한 사용자 조회")
    void shouldFindUserWithSetting_WhenValidIdProvided() {
        // Given: 조회할 사용자 ID와 설정을 포함한 예상 결과
        Long userId = 1L;
        Setting setting = Setting.builder()
                .id(1L)
                .commentNotification(true)
                .messageNotification(false)
                .build();
                
        User expectedUser = User.builder()
                .id(userId)
                .userName("testUser")
                .setting(setting)
                .build();
                
        given(userRepository.findByIdWithSetting(userId)).willReturn(Optional.of(expectedUser));

        // When: 설정을 포함한 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findByIdWithSetting(userId);

        // Then: 사용자와 설정이 모두 포함되어 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
        assertThat(result.get().getSetting()).isNotNull();
        assertThat(result.get().getSetting().isCommentNotification()).isTrue();
        verify(userRepository).findByIdWithSetting(eq(userId));
    }

    @Test
    @DisplayName("정상 케이스 - 소셜 제공자와 소셜 ID로 사용자 조회")
    void shouldFindUserByProviderAndSocialId_WhenValidParametersProvided() {
        // Given: 소셜 제공자, 소셜 ID와 예상 결과
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "kakao123";
        User expectedUser = User.builder()
                .id(1L)
                .socialId(socialId)
                .provider(provider)
                .userName("kakaoUser")
                .build();
                
        given(userRepository.findByProviderAndSocialId(provider, socialId))
                .willReturn(Optional.of(expectedUser));

        // When: 소셜 정보로 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findByProviderAndSocialId(provider, socialId);

        // Then: 올바른 소셜 사용자가 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getSocialId()).isEqualTo(socialId);
        assertThat(result.get().getProvider()).isEqualTo(provider);
        verify(userRepository).findByProviderAndSocialId(eq(provider), eq(socialId));
    }

    @Test
    @DisplayName("정상 케이스 - 닉네임 존재 여부 확인")
    void shouldCheckUserNameExists_WhenUserNameProvided() {
        // Given: 확인할 닉네임
        String userName = "existingUser";
        given(userRepository.existsByUserName(userName)).willReturn(true);

        // When: 닉네임 존재 여부 확인 실행
        boolean result = userQueryAdapter.existsByUserName(userName);

        // Then: 존재 여부가 올바르게 반환되는지 검증
        assertThat(result).isTrue();
        verify(userRepository).existsByUserName(eq(userName));
    }

    @Test
    @DisplayName("정상 케이스 - 닉네임으로 사용자 조회")
    void shouldFindUserByUserName_WhenValidUserNameProvided() {
        // Given: 조회할 닉네임과 예상 결과
        String userName = "testUser";
        User expectedUser = User.builder()
                .id(1L)
                .userName(userName)
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .build();
                
        given(userRepository.findByUserName(userName)).willReturn(Optional.of(expectedUser));

        // When: 닉네임으로 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findByUserName(userName);

        // Then: 올바른 사용자가 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo(userName);
        verify(userRepository).findByUserName(eq(userName));
    }

    @Test
    @DisplayName("정상 케이스 - ID로 설정 조회")
    void shouldFindSetting_WhenValidSettingIdProvided() {
        // Given: 조회할 설정 ID와 예상 결과
        Long settingId = 1L;
        Setting expectedSetting = Setting.builder()
                .id(settingId)
                .commentNotification(true)
                .messageNotification(false)
                .build();
                
        given(settingRepository.findById(settingId)).willReturn(Optional.of(expectedSetting));

        // When: 설정 조회 실행
        Optional<Setting> result = userQueryAdapter.findSettingById(settingId);

        // Then: 올바른 설정이 조회되는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(settingId);
        assertThat(result.get().isCommentNotification()).isTrue();
        verify(settingRepository).findById(eq(settingId));
    }

    @Test
    @DisplayName("정상 케이스 - 순서대로 사용자 이름 조회")
    void shouldFindUserNamesInOrder_WhenSocialIdsProvided() {
        // Given: 조회할 소셜 ID 목록과 예상 결과
        List<String> socialIds = Arrays.asList("kakao123", "kakao456", "kakao789");
        List<String> expectedUserNames = Arrays.asList("user1", "user2", "user3");
        
        given(userRepository.findUserNamesInOrder(socialIds)).willReturn(expectedUserNames);

        // When: 순서대로 사용자 이름 조회 실행
        List<String> result = userQueryAdapter.findUserNamesInOrder(socialIds);

        // Then: 올바른 순서로 사용자 이름이 조회되는지 검증
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("user1", "user2", "user3");
        verify(userRepository).findUserNamesInOrder(eq(socialIds));
    }

    @Test
    @DisplayName("정상 케이스 - ID로 사용자 참조 가져오기")
    void shouldGetUserReference_WhenValidIdProvided() {
        // Given: 참조할 사용자 ID와 예상 참조
        Long userId = 1L;
        User userReference = User.builder()
                .id(userId)
                .build();
                
        given(userRepository.getReferenceById(userId)).willReturn(userReference);

        // When: 사용자 참조 가져오기 실행
        User result = userQueryAdapter.getReferenceById(userId);

        // Then: 올바른 사용자 참조가 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).getReferenceById(eq(userId));
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 사용자 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentId = 999L;
        given(userRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // When: 존재하지 않는 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findById(nonExistentId);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
        verify(userRepository).findById(eq(nonExistentId));
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 닉네임 존재 여부 확인")
    void shouldReturnFalse_WhenNonExistentUserNameProvided() {
        // Given: 존재하지 않는 닉네임
        String nonExistentUserName = "nonExistentUser";
        given(userRepository.existsByUserName(nonExistentUserName)).willReturn(false);

        // When: 존재하지 않는 닉네임 존재 여부 확인 실행
        boolean result = userQueryAdapter.existsByUserName(nonExistentUserName);

        // Then: false가 반환되는지 검증
        assertThat(result).isFalse();
        verify(userRepository).existsByUserName(eq(nonExistentUserName));
    }

    @Test
    @DisplayName("경계값 - 빈 소셜 ID 목록으로 사용자 이름 조회")
    void shouldReturnEmptyList_WhenEmptySocialIdsProvided() {
        // Given: 빈 소셜 ID 목록
        List<String> emptySocialIds = List.of();
        List<String> emptyUserNames = List.of();
        
        given(userRepository.findUserNamesInOrder(emptySocialIds)).willReturn(emptyUserNames);

        // When: 빈 목록으로 사용자 이름 조회 실행
        List<String> result = userQueryAdapter.findUserNamesInOrder(emptySocialIds);

        // Then: 빈 목록이 반환되는지 검증
        assertThat(result).isEmpty();
        verify(userRepository).findUserNamesInOrder(eq(emptySocialIds));
    }

    @Test
    @DisplayName("경계값 - null ID로 사용자 조회")
    void shouldHandleNullId_WhenNullIdProvided() {
        // Given: null ID
        Long nullId = null;
        given(userRepository.findById(any())).willReturn(Optional.empty());

        // When: null ID로 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findById(nullId);

        // Then: Repository에 null이 전달되는지 검증

        assertThat(result).isEmpty();
        verify(userRepository).findById(nullId);
    }

    @Test
    @DisplayName("경계값 - null 닉네임으로 사용자 조회")
    void shouldHandleNullUserName_WhenNullUserNameProvided() {
        // Given: null 닉네임
        String nullUserName = null;
        given(userRepository.findByUserName(any())).willReturn(Optional.empty());

        // When: null 닉네임으로 사용자 조회 실행
        Optional<User> result = userQueryAdapter.findByUserName(nullUserName);

        // Then: Repository에 null이 전달되는지 검증

        assertThat(result).isEmpty();
        verify(userRepository).findByUserName(nullUserName);
    }
}
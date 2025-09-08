package jaeik.bimillog.infrastructure.outadapter.user.persistence;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserCommandAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * <h2>UserCommandAdapter 테스트</h2>
 * <p>사용자 명령 어댑터의 persistence 동작 검증</p>
 * <p>User, Setting 엔티티의 CRUD 작업 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserCommandAdapterTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SettingRepository settingRepository;
    

    @InjectMocks
    private UserCommandAdapter userCommandAdapter;

    @Test
    @DisplayName("정상 케이스 - 사용자 저장")
    void shouldSaveUser_WhenValidUserProvided() {
        // Given: 저장할 사용자와 예상 결과
        User inputUser = User.builder()
                .userName("testUser")
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .build();
        
        User savedUser = User.builder()
                .id(1L)
                .userName("testUser")
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .build();
                
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // When: 사용자 저장 실행
        User result = userCommandAdapter.save(inputUser);

        // Then: 저장이 올바르게 수행되고 결과가 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("testUser");
        assertThat(result.getSocialId()).isEqualTo("social123");
        verify(userRepository).save(eq(inputUser));
    }

    @Test
    @DisplayName("정상 케이스 - 설정 저장")
    void shouldSaveSetting_WhenValidSettingProvided() {
        // Given: 저장할 설정과 예상 결과
        Setting inputSetting = Setting.builder()
                .commentNotification(true)
                .messageNotification(false)
                .build();
                
        Setting savedSetting = Setting.builder()
                .id(1L)
                .commentNotification(true)
                .messageNotification(false)
                .build();
                
        given(settingRepository.save(any(Setting.class))).willReturn(savedSetting);

        // When: 설정 저장 실행
        Setting result = userCommandAdapter.save(inputSetting);

        // Then: 저장이 올바르게 수행되고 결과가 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isCommentNotification()).isTrue();
        assertThat(result.isMessageNotification()).isFalse();
        verify(settingRepository).save(eq(inputSetting));
    }



    @Test
    @DisplayName("예외 케이스 - null 사용자 저장 시 예외 발생")
    void shouldThrowException_WhenNullUserProvided() {
        // TODO: 테스트 실패 - 메인 로직 버그 의심
        // 기존: null 허용하는 비논리적 테스트
        // 수정: null 사용자는 저장될 수 없으므로 적절한 예외 처리 필요
        
        // Given: null 사용자
        User nullUser = null;

        // When & Then: null 사용자 저장 시 예외 발생 확인
        assertThatThrownBy(() -> userCommandAdapter.save(nullUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User cannot be null");

        // 검증 실패로 repository 호출되지 않아야 함
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("예외 케이스 - null 설정 저장 시 예외 발생")
    void shouldThrowException_WhenNullSettingProvided() {
        // TODO: 테스트 실패 - 메인 로직 버그 의심
        // 기존: null 설정 허용하는 비논리적 테스트
        // 수정: null 설정은 저장될 수 없으므로 적절한 예외 처리 필요
        
        // Given: null 설정
        Setting nullSetting = null;

        // When & Then: null 설정 저장 시 예외 발생 확인
        assertThatThrownBy(() -> userCommandAdapter.save(nullSetting))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Setting cannot be null");

        // 검증 실패로 repository 호출되지 않아야 함
        verify(settingRepository, never()).save(any());
    }


}
package jaeik.growfarm.infrastructure.adapter.user.out.persistence;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.blacklist.BlackListRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserCommandAdapter;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserCommandAdapter 테스트</h2>
 * <p>사용자 명령 어댑터의 persistence 동작 검증</p>
 * <p>User, Setting, BlackList 엔티티의 CRUD 작업 테스트</p>
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
    
    @Mock
    private BlackListRepository blackListRepository;

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
    @DisplayName("정상 케이스 - 사용자 ID로 삭제")
    void shouldDeleteUser_WhenValidIdProvided() {
        // Given: 삭제할 사용자 ID
        Long userId = 1L;

        // When: 사용자 삭제 실행
        userCommandAdapter.deleteById(userId);

        // Then: Repository의 deleteById가 올바른 ID로 호출되었는지 검증
        verify(userRepository).deleteById(eq(userId));
    }

    @Test
    @DisplayName("정상 케이스 - 블랙리스트 저장")
    void shouldSaveBlackList_WhenValidBlackListProvided() {
        // Given: 저장할 블랙리스트
        BlackList blackList = BlackList.builder()
                .socialId("bannedSocialId")
                .provider(SocialProvider.KAKAO)
                .build();
                
        given(blackListRepository.save(any(BlackList.class))).willReturn(blackList);

        // When: 블랙리스트 저장 실행
        userCommandAdapter.save(blackList);

        // Then: Repository의 save가 올바른 객체로 호출되었는지 검증
        verify(blackListRepository).save(eq(blackList));
    }

    @Test
    @DisplayName("경계값 - null 사용자 저장")
    void shouldHandleNullUser_WhenNullUserProvided() {
        // Given: null 사용자
        User nullUser = null;
        
        given(userRepository.save(any())).willReturn(null);

        // When: null 사용자 저장 실행
        User result = userCommandAdapter.save(nullUser);

        // Then: Repository에 null이 전달되고 결과도 null인지 검증
        assertThat(result).isNull();
        verify(userRepository).save(nullUser);
    }

    @Test
    @DisplayName("경계값 - null 설정 저장")
    void shouldHandleNullSetting_WhenNullSettingProvided() {
        // Given: null 설정
        Setting nullSetting = null;
        
        given(settingRepository.save(any())).willReturn(null);

        // When: null 설정 저장 실행
        Setting result = userCommandAdapter.save(nullSetting);

        // Then: Repository에 null이 전달되고 결과도 null인지 검증
        assertThat(result).isNull();
        verify(settingRepository).save(nullSetting);
    }

    @Test
    @DisplayName("경계값 - null ID로 사용자 삭제")
    void shouldHandleNullId_WhenNullIdProvided() {
        // Given: null ID
        Long nullId = null;

        // When: null ID로 삭제 실행
        userCommandAdapter.deleteById(nullId);

        // Then: Repository에 null이 전달되는지 검증

        verify(userRepository).deleteById(nullId);
    }

    @Test
    @DisplayName("경계값 - null 블랙리스트 저장")
    void shouldHandleNullBlackList_WhenNullBlackListProvided() {
        // Given: null 블랙리스트
        BlackList nullBlackList = null;

        // When: null 블랙리스트 저장 실행
        userCommandAdapter.save(nullBlackList);

        // Then: Repository에 null이 전달되는지 검증

        verify(blackListRepository).save(nullBlackList);
    }
}
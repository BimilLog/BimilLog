package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * <h2>단위 테스트 베이스 클래스</h2>
 * <p>모든 단위 테스트가 상속받아 사용하는 기본 클래스</p>
 * <p>Mockito 설정과 공통 테스트 데이터를 lazy 초기화로 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>MockitoExtension 자동 적용</li>
 *   <li>공통 테스트 사용자 (testUser, adminUser, otherUser) - lazy 초기화</li>
 *   <li>공통 테스트 설정 (defaultSetting, customSetting) - lazy 초기화</li>
 *   <li>필요한 데이터만 생성하여 테스트 성능 향상</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    // Lazy 초기화를 위한 필드들 (실제 사용 시점에 초기화)
    private User cachedTestUser;
    private User cachedAdminUser;
    private User cachedOtherUser;
    private User cachedThirdUser;
    private Setting cachedDefaultSetting;

    /**
     * 기본 테스트 사용자 획득 (일반 권한)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getTestUser() {
        if (cachedTestUser == null) {
            cachedTestUser = TestUsers.copyWithId(TestUsers.USER1, 1L);
        }
        return cachedTestUser;
    }

    /**
     * 관리자 권한 테스트 사용자 획득
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getAdminUser() {
        if (cachedAdminUser == null) {
            cachedAdminUser = TestUsers.copyWithId(TestUsers.withRole(UserRole.ADMIN), 999L);
        }
        return cachedAdminUser;
    }

    /**
     * 추가 테스트 사용자 획득 (다른 사용자 시나리오용)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getOtherUser() {
        if (cachedOtherUser == null) {
            cachedOtherUser = TestUsers.copyWithId(TestUsers.USER2, 2L);
        }
        return cachedOtherUser;
    }

    /**
     * 세 번째 테스트 사용자 획득 (복잡한 시나리오용)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getThirdUser() {
        if (cachedThirdUser == null) {
            cachedThirdUser = TestUsers.copyWithId(TestUsers.USER3, 3L);
        }
        return cachedThirdUser;
    }

    /**
     * 기본 설정 객체 획득 (모든 알림 활성화)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected Setting getDefaultSetting() {
        if (cachedDefaultSetting == null) {
            cachedDefaultSetting = TestUsers.createAllEnabledSetting();
        }
        return cachedDefaultSetting;
    }

    /**
     * ID가 포함된 테스트 사용자 생성 헬퍼 메서드
     * @param userId 사용자 ID
     * @return ID가 설정된 테스트 사용자
     */
    protected User createTestUserWithId(Long userId) {
        return TestUsers.copyWithId(getTestUser(), userId);
    }

    /**
     * 커스텀 설정 생성 헬퍼 메서드
     * @param messageNotification 메시지 알림 활성화 여부
     * @param commentNotification 댓글 알림 활성화 여부
     * @param postFeaturedNotification 게시글 추천 알림 활성화 여부
     * @return 커스터마이징된 설정 객체
     */
    protected Setting createCustomSetting(boolean messageNotification,
                                         boolean commentNotification,
                                         boolean postFeaturedNotification) {
        return TestUsers.createSetting(messageNotification, commentNotification, postFeaturedNotification);
    }

    /**
     * ID가 포함된 설정 생성 헬퍼 메서드
     * @param setting 원본 설정
     * @param settingId 설정 ID
     * @return ID가 설정된 설정 객체
     */
    protected Setting createSettingWithId(Setting setting, Long settingId) {
        return Setting.builder()
                .id(settingId)
                .messageNotification(setting.isMessageNotification())
                .commentNotification(setting.isCommentNotification())
                .postFeaturedNotification(setting.isPostFeaturedNotification())
                .build();
    }

    // ==================== SecurityContext Mock 헬퍼 메서드 ====================

    /**
     * 익명 사용자로 SecurityContext를 Mock 설정
     * <p>인증되지 않은 사용자 테스트 시 사용</p>
     * @param mockedSecurityContext MockedStatic SecurityContextHolder
     */
    protected void mockAnonymousAuthentication(MockedStatic<SecurityContextHolder> mockedSecurityContext) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = new AnonymousAuthenticationToken(
            "anonymous",
            "anonymous",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
    }

    /**
     * 인증된 사용자로 SecurityContext를 Mock 설정 (기본 테스트 사용자)
     * <p>기본 테스트 사용자와 USER 권한으로 설정</p>
     * @param mockedSecurityContext MockedStatic SecurityContextHolder
     */
    protected void mockAuthenticatedUser(MockedStatic<SecurityContextHolder> mockedSecurityContext) {
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(getTestUser());
        mockAuthenticatedUser(mockedSecurityContext, userDetails, UserRole.USER);
    }

    /**
     * 특정 사용자와 권한으로 SecurityContext를 Mock 설정
     * <p>가장 유연한 SecurityContext Mock 메서드</p>
     * @param mockedSecurityContext MockedStatic SecurityContextHolder
     * @param userDetails CustomUserDetails
     * @param role 사용자 권한
     */
    protected void mockAuthenticatedUser(MockedStatic<SecurityContextHolder> mockedSecurityContext,
                                        CustomUserDetails userDetails,
                                        UserRole role) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );

        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
    }
}
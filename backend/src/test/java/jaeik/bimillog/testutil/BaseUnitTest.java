package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * <h3>중요: 필드 접근은 더 이상 지원되지 않습니다</h3>
 * <p>모든 테스트 데이터는 getter 메서드를 통해서만 접근 가능합니다.</p>
 * <p>필드 직접 접근 (testUser, adminUser 등)은 제거되었으며, </p>
 * <p>getTestUser(), getAdminUser() 등의 getter 메서드를 사용해야 합니다.</p>
 *
 * <h3>사용 예시:</h3>
 * <pre>
 * class UserServiceTest extends BaseUnitTest {
 *     {@literal @}Mock UserRepository repository;
 *     {@literal @}InjectMocks UserService service;
 *
 *     {@literal @}Test
 *     void test() {
 *         // ✅ 올바른 사용 - getter 메서드
 *         given(repository.findById(1L)).willReturn(Optional.of(getTestUser()));
 *         
 *         // ❌ 잘못된 사용 - 필드 직접 접근 (더 이상 지원 안함)
 *         // given(repository.findById(1L)).willReturn(Optional.of(testUser));
 *     }
 * }
 * </pre>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2025-01 Lazy getter 메서드만 지원, 필드 직접 접근 제거
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
            cachedTestUser = TestUsers.USER1;
        }
        return cachedTestUser;
    }

    /**
     * 관리자 권한 테스트 사용자 획득
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getAdminUser() {
        if (cachedAdminUser == null) {
            cachedAdminUser = TestUsers.withRole(UserRole.ADMIN);
        }
        return cachedAdminUser;
    }

    /**
     * 추가 테스트 사용자 획득 (다른 사용자 시나리오용)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getOtherUser() {
        if (cachedOtherUser == null) {
            cachedOtherUser = TestUsers.USER2;
        }
        return cachedOtherUser;
    }

    /**
     * 세 번째 테스트 사용자 획득 (복잡한 시나리오용)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected User getThirdUser() {
        if (cachedThirdUser == null) {
            cachedThirdUser = TestUsers.USER3;
        }
        return cachedThirdUser;
    }

    /**
     * 기본 설정 객체 획득 (모든 알림 활성화)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected Setting getDefaultSetting() {
        if (cachedDefaultSetting == null) {
            cachedDefaultSetting = TestUsers.createSetting(true, true, true);
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
}
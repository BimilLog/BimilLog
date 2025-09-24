package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <h2>단위 테스트 베이스 클래스</h2>
 * <p>모든 단위 테스트가 상속받아 사용하는 기본 클래스</p>
 * <p>Mockito 설정과 공통 테스트 데이터를 자동으로 제공</p>
 * 
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>MockitoExtension 자동 적용</li>
 *   <li>공통 테스트 사용자 (testUser, adminUser, otherUser)</li>
 *   <li>공통 테스트 설정 (defaultSetting, customSetting)</li>
 *   <li>각 테스트 메서드 실행 전 자동 초기화</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * class UserServiceTest extends BaseUnitTest {
 *     {@literal @}Mock UserRepository repository;
 *     {@literal @}InjectMocks UserService service;
 *     
 *     {@literal @}Test
 *     void test() {
 *         // testUser, adminUser 등이 이미 준비되어 있음
 *         given(repository.findById(1L)).willReturn(Optional.of(testUser));
 *     }
 * }
 * </pre>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    /**
     * 기본 테스트 사용자 (일반 권한)
     * TestUsers.USER1의 복사본으로 매 테스트마다 새로 생성
     */
    protected User testUser;

    /**
     * 관리자 권한 테스트 사용자
     * TestUsers.withRole(UserRole.ADMIN)으로 매 테스트마다 새로 생성
     */
    protected User adminUser;

    /**
     * 추가 테스트 사용자 (다른 사용자 시나리오용)
     * TestUsers.USER2의 복사본으로 매 테스트마다 새로 생성
     */
    protected User otherUser;

    /**
     * 세 번째 테스트 사용자 (복잡한 시나리오용)
     * TestUsers.USER3의 복사본으로 매 테스트마다 새로 생성
     */
    protected User thirdUser;

    /**
     * 기본 설정 객체 (모든 알림 활성화)
     */
    protected Setting defaultSetting;

    /**
     * 커스텀 설정 객체 (테스트 시나리오별 변경 가능, 모든 알림 비활성화)
     */
    protected Setting customSetting;

    /**
     * 메시지 알림만 활성화된 설정
     */
    protected Setting messageOnlySetting;

    /**
     * 댓글 알림만 활성화된 설정
     */
    protected Setting commentOnlySetting;

    /**
     * 게시글 추천 알림만 활성화된 설정
     */
    protected Setting postFeaturedOnlySetting;

    /**
     * 각 테스트 메서드 실행 전 공통 데이터 초기화
     * 매 테스트마다 새로운 객체를 생성하여 테스트 간 격리 보장
     */
    @BeforeEach
    protected void setUpBase() {
        // 사용자 초기화 (매번 새로운 인스턴스 생성)
        this.testUser = TestUsers.USER1;
        this.adminUser = TestUsers.withRole(UserRole.ADMIN);
        this.otherUser = TestUsers.USER2;
        this.thirdUser = TestUsers.USER3;
        
        // 설정 초기화 (매번 새로운 인스턴스 생성)
        this.defaultSetting = TestUsers.createSetting(true, true, true);
        this.customSetting = TestUsers.createAllDisabledSetting();
        this.messageOnlySetting = TestUsers.createMessageOnlySetting();
        this.commentOnlySetting = TestUsers.createCommentOnlySetting();
        this.postFeaturedOnlySetting = TestUsers.createPostFeaturedOnlySetting();
        
        // 하위 클래스의 추가 설정을 위한 hook
        setUpChild();
    }

    /**
     * 하위 클래스에서 추가 설정이 필요한 경우 오버라이드
     * 기본 구현은 비어있음
     */
    protected void setUpChild() {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * ID가 포함된 테스트 사용자 생성 헬퍼 메서드
     * @param userId 사용자 ID
     * @return ID가 설정된 테스트 사용자
     */
    protected User createTestUserWithId(Long userId) {
        return TestUsers.copyWithId(testUser, userId);
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
        Setting copiedSetting = Setting.builder()
                .id(settingId)
                .messageNotification(setting.isMessageNotification())
                .commentNotification(setting.isCommentNotification())
                .postFeaturedNotification(setting.isPostFeaturedNotification())
                .build();
        return copiedSetting;
    }
}
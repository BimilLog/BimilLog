package jaeik.bimillog.testutil.annotation;

import jaeik.bimillog.domain.user.entity.UserRole;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

/**
 * <h2>Mock 사용자 인증 애노테이션</h2>
 * <p>테스트에서 인증된 사용자를 자동으로 설정하는 애노테이션</p>
 * <p>CustomUserDetails를 자동으로 생성하여 SecurityContext에 설정</p>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * {@literal @}Test
 * {@literal @}WithMockCustomUser  // 기본 USER 권한
 * void testWithAuthenticatedUser() {
 *     // 인증된 사용자로 테스트 실행
 * }
 * 
 * {@literal @}Test
 * {@literal @}WithMockCustomUser(role = UserRole.ADMIN)  // ADMIN 권한
 * void testWithAdmin() {
 *     // 관리자 권한으로 테스트 실행
 * }
 * 
 * {@literal @}Test
 * {@literal @}WithMockCustomUser(
 *     userId = 123L,
 *     userName = "customUser",
 *     socialId = "kakao_456"
 * )
 * void testWithCustomUser() {
 *     // 커스터마이징된 사용자로 테스트
 * }
 * </pre>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

    /**
     * 사용자 ID
     * @return 사용자 ID (기본값: 1)
     */
    long userId() default 1L;

    /**
     * 사용자명
     * @return 사용자명 (기본값: "testUser")
     */
    String userName() default "testUser";

    /**
     * 소셜 ID
     * @return 소셜 ID (기본값: "kakao_123456")
     */
    String socialId() default "kakao_123456";

    /**
     * 소셜 닉네임
     * @return 소셜 닉네임 (기본값: "테스트유저")
     */
    String socialNickname() default "테스트유저";

    /**
     * 프로필 이미지 URL
     * @return 프로필 이미지 URL
     */
    String thumbnailImage() default "http://example.com/profile.jpg";

    /**
     * 사용자 권한
     * @return UserRole (기본값: USER)
     */
    UserRole role() default UserRole.USER;

    /**
     * 설정 ID
     * @return 설정 ID (기본값: 1)
     */
    long settingId() default 1L;
}
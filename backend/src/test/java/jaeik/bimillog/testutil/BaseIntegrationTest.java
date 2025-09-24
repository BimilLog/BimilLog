package jaeik.bimillog.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * <h2>통합 테스트 베이스 클래스</h2>
 * <p>모든 통합 테스트가 상속받아 사용하는 기본 클래스</p>
 * <p>TestContainers, MockMvc, Spring Security 설정을 자동으로 제공</p>
 * 
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>TestContainers를 통한 실제 MySQL, Redis 환경</li>
 *   <li>MockMvc 자동 설정 및 헬퍼 메서드</li>
 *   <li>Spring Security 통합</li>
 *   <li>트랜잭션 롤백</li>
 *   <li>공통 테스트 사용자 및 설정</li>
 *   <li>CustomUserDetails 생성 유틸리티</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * {@literal @}IntegrationTest
 * class UserControllerIntegrationTest extends BaseIntegrationTest {
 *     
 *     {@literal @}Test
 *     void test() throws Exception {
 *         // testUser가 이미 DB에 저장되어 있음
 *         // mockMvc가 이미 설정되어 있음
 *         
 *         performGet("/api/user/me", testUserDetails)
 *             .andExpect(status().isOk());
 *     }
 * }
 * </pre>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import(TestContainersConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected UserRepository userRepository;

    @Autowired(required = false)
    protected TestEntityManager entityManager;

    /**
     * MockMvc 인스턴스 (자동 설정됨)
     */
    protected MockMvc mockMvc;

    /**
     * 기본 테스트 사용자 (DB에 저장됨)
     */
    protected User testUser;

    /**
     * 관리자 권한 테스트 사용자 (DB에 저장됨)
     */
    protected User adminUser;

    /**
     * 추가 테스트 사용자 (DB에 저장됨)
     */
    protected User otherUser;

    /**
     * 테스트 사용자의 CustomUserDetails
     */
    protected CustomUserDetails testUserDetails;

    /**
     * 관리자 사용자의 CustomUserDetails
     */
    protected CustomUserDetails adminUserDetails;

    /**
     * 추가 사용자의 CustomUserDetails
     */
    protected CustomUserDetails otherUserDetails;

    /**
     * 기본 설정 객체
     */
    protected Setting defaultSetting;

    /**
     * 커스텀 설정 객체
     */
    protected Setting customSetting;

    /**
     * 각 테스트 메서드 실행 전 설정 초기화
     */
    @BeforeEach
    protected void setUpBase() {
        // MockMvc 설정
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 설정 초기화
        this.defaultSetting = TestUsers.createSetting(true, true, true);
        this.customSetting = TestUsers.createAllDisabledSetting();

        // 사용자 생성 및 저장
        setupTestUsers();

        // CustomUserDetails 생성
        this.testUserDetails = createCustomUserDetails(testUser);
        this.adminUserDetails = createCustomUserDetails(adminUser);
        this.otherUserDetails = createCustomUserDetails(otherUser);

        // 하위 클래스의 추가 설정
        setUpChild();
    }

    /**
     * 테스트 사용자들을 DB에 저장
     */
    protected void setupTestUsers() {
        if (userRepository != null) {
            // 고유한 사용자 생성하여 충돌 방지
            this.testUser = userRepository.save(TestUsers.createUniqueWithPrefix("test"));
            this.adminUser = userRepository.save(TestUsers.createUniqueWithPrefix("admin"));
            this.otherUser = userRepository.save(TestUsers.createUniqueWithPrefix("other"));
        } else {
            // UserRepository가 없는 경우 기본 사용자 사용
            this.testUser = TestUsers.USER1;
            this.adminUser = TestUsers.withRole(jaeik.bimillog.domain.user.entity.UserRole.ADMIN);
            this.otherUser = TestUsers.USER2;
        }
    }

    /**
     * 하위 클래스에서 추가 설정이 필요한 경우 오버라이드
     */
    protected void setUpChild() {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * CustomUserDetails 생성 유틸리티
     * @param user 사용자 엔티티
     * @return CustomUserDetails 인스턴스
     */
    protected CustomUserDetails createCustomUserDetails(User user) {
        ExistingUserDetail userDetail = ExistingUserDetail.builder()
                .userId(user.getId())
                .settingId(user.getSetting() != null ? user.getSetting().getId() : null)
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .tokenId(null)
                .fcmTokenId(null)
                .build();

        return new CustomUserDetails(userDetail);
    }

    /**
     * 인증된 GET 요청 수행
     * @param url 요청 URL
     * @param userDetails 인증 정보
     * @return ResultActions
     */
    protected ResultActions performGet(String url, CustomUserDetails userDetails) throws Exception {
        return mockMvc.perform(get(url)
                .with(user(userDetails))
                .with(csrf()));
    }

    /**
     * 인증 없는 GET 요청 수행
     * @param url 요청 URL
     * @return ResultActions
     */
    protected ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                .with(csrf()));
    }

    /**
     * 인증된 POST 요청 수행 (JSON)
     * @param url 요청 URL
     * @param content 요청 본문
     * @param userDetails 인증 정보
     * @return ResultActions
     */
    protected ResultActions performPost(String url, Object content, CustomUserDetails userDetails) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(content))
                .with(user(userDetails))
                .with(csrf()));
    }

    /**
     * 인증 없는 POST 요청 수행 (JSON)
     * @param url 요청 URL
     * @param content 요청 본문
     * @return ResultActions
     */
    protected ResultActions performPost(String url, Object content) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(content))
                .with(csrf()));
    }

    /**
     * 인증 없는 DELETE 요청 수행
     * @param url 요청 URL
     * @return ResultActions
     */
    protected ResultActions performDelete(String url) throws Exception {
        return mockMvc.perform(delete(url)
                .with(csrf()));
    }

    /**
     * 엔티티를 영속화하고 플러시
     * @param entity 저장할 엔티티
     * @return 저장된 엔티티
     */
    protected <T> T saveAndFlush(T entity) {
        if (entityManager != null) {
            entityManager.persist(entity);
            entityManager.flush();
            return entity;
        }
        throw new UnsupportedOperationException("TestEntityManager is not available");
    }

    /**
     * 엔티티 새로고침 (DB에서 다시 조회)
     * @param entity 새로고침할 엔티티
     * @param id 엔티티 ID
     * @param entityClass 엔티티 클래스
     * @return 새로고침된 엔티티
     */
    protected <T> T refresh(T entity, Object id, Class<T> entityClass) {
        if (entityManager != null) {
            entityManager.clear();
            return entityManager.find(entityClass, id);
        }
        return entity;
    }
}
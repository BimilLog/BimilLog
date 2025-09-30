package jaeik.bimillog.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.MemberRole;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.member.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * <h2>통합 테스트 베이스 클래스</h2>
 * <p>모든 SpringBootTest가 상속받아 사용하는 기본 클래스</p>
 * <p>MockMvc, Spring Security 설정을 자동으로 제공</p>
 * 
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>MockMvc 자동 설정 및 헬퍼 메서드</li>
 *   <li>Spring Security 통합</li>
 *   <li>트랜잭션 롤백</li>
 *   <li>공통 테스트 사용자 및 설정</li>
 *   <li>CustomUserDetails 생성 유틸리티</li>
 * </ul>
 * 
 * <h3>DB 환경 선택:</h3>
 * <p>테스트 클래스에서 필요에 따라 DB 환경을 선택:</p>
 * <ul>
 *   <li>TestContainers: @IntegrationTest 메타 어노테이션 사용</li>
 *   <li>H2 Database: @ActiveProfiles("h2test") @Import(H2TestConfiguration.class) 사용</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Transactional
@Tag("integration")
public abstract class BaseIntegrationTest {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected UserRepository userRepository;

    @Autowired(required = false)
    protected TestEntityManager entityManager;

    @PersistenceContext
    protected EntityManager entityManagerDelegate;

    /**
     * MockMvc 인스턴스 (자동 설정됨)
     */
    protected MockMvc mockMvc;

    /**
     * 기본 테스트 사용자 (DB에 저장됨)
     */
    protected Member testMember;

    /**
     * 관리자 권한 테스트 사용자 (DB에 저장됨)
     */
    protected Member adminMember;

    /**
     * 추가 테스트 사용자 (DB에 저장됨)
     */
    protected Member otherMember;

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
        this.defaultSetting = TestUsers.createAllEnabledSetting();
        this.customSetting = TestUsers.createAllDisabledSetting();

        // 사용자 생성 및 저장
        setupTestUsers();

        // CustomUserDetails 생성
        this.testUserDetails = createCustomUserDetails(testMember);
        this.adminUserDetails = createCustomUserDetails(adminMember);
        this.otherUserDetails = createCustomUserDetails(otherMember);

        // 하위 클래스의 추가 설정
        setUpChild();
    }

    /**
     * 테스트 사용자들을 DB에 저장
     */
    protected void setupTestUsers() {
        if (userRepository != null) {
            // 고유한 사용자 생성하여 충돌 방지
            this.testMember = userRepository.save(TestUsers.createUniqueWithPrefix("test"));
            this.adminMember = userRepository.save(TestUsers.createUniqueWithPrefix("admin", builder -> {
                builder.role(MemberRole.ADMIN);
                builder.setting(TestUsers.createAllDisabledSetting());
            }));
            this.otherMember = userRepository.save(TestUsers.createUniqueWithPrefix("other"));
        } else {
            // UserRepository가 없는 경우 기본 사용자 사용
            this.testMember = TestUsers.MEMBER_1;
            this.adminMember = TestUsers.withRole(MemberRole.ADMIN);
            this.otherMember = TestUsers.MEMBER_2;
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
     * @param member 사용자 엔티티
     * @return CustomUserDetails 인스턴스
     */
    protected CustomUserDetails createCustomUserDetails(Member member) {
        return AuthTestFixtures.createCustomUserDetails(member);
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
}

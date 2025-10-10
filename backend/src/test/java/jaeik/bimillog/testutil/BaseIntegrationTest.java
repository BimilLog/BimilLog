package jaeik.bimillog.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberRole;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.member.MemberRepository;
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
 *   <li>공통 테스트 회원 및 설정</li>
 *   <li>CustomUserDetails 생성 유틸리티</li>
 * </ul>
 * 
 * <h3>DB 환경 선택:</h3>
 * <p>테스트 클래스에서 필요에 따라 DB 환경을 선택:</p>
 * <ul>
 *   <li>로컬 MySQL/Redis: @IntegrationTest 메타 어노테이션 사용</li>
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
    protected MemberRepository memberRepository;

    @Autowired(required = false)
    protected TestEntityManager entityManager;

    @PersistenceContext
    protected EntityManager entityManagerDelegate;

    /**
     * MockMvc 인스턴스 (자동 설정됨)
     */
    protected MockMvc mockMvc;

    /**
     * 기본 테스트 회원 (DB에 저장됨)
     */
    protected Member testMember;

    /**
     * 관리자 권한 테스트 회원 (DB에 저장됨)
     */
    protected Member adminMember;

    /**
     * 추가 테스트 회원 (DB에 저장됨)
     */
    protected Member otherMember;

    /**
     * 테스트 회원의 CustomUserDetails
     */
    protected CustomUserDetails testUserDetails;

    /**
     * 관리자 회원의 CustomUserDetails
     */
    protected CustomUserDetails adminUserDetails;

    /**
     * 추가 회원의 CustomUserDetails
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
        this.defaultSetting = TestMembers.createAllEnabledSetting();
        this.customSetting = TestMembers.createAllDisabledSetting();

        // 회원 생성 및 저장
        setupTestMembers();

        // CustomUserDetails 생성
        this.testUserDetails = createCustomUserDetails(testMember);
        this.adminUserDetails = createCustomUserDetails(adminMember);
        this.otherUserDetails = createCustomUserDetails(otherMember);

        // 하위 클래스의 추가 설정
        setUpChild();
    }

    /**
     * 테스트 회원들을 DB에 저장
     */
    protected void setupTestMembers() {
        if (memberRepository != null) {
            // 고유한 회원 생성하여 충돌 방지
            Member testMemberToSave = TestMembers.createUniqueWithPrefix("test");
            Member adminMemberToSave = TestMembers.withRole(MemberRole.ADMIN);
            Member otherMemberToSave = TestMembers.createUniqueWithPrefix("other");

            // Member를 저장하기 전에 연관된 Setting과 KakaoToken을 먼저 persist
            persistMemberDependencies(testMemberToSave);
            persistMemberDependencies(adminMemberToSave);
            persistMemberDependencies(otherMemberToSave);

            // Member 저장
            this.testMember = memberRepository.save(testMemberToSave);
            this.adminMember = memberRepository.save(adminMemberToSave);
            this.otherMember = memberRepository.save(otherMemberToSave);
        } else {
            // MemberRepository가 없는 경우 기본 회원 사용
            this.testMember = TestMembers.MEMBER_1;
            this.adminMember = TestMembers.withRole(MemberRole.ADMIN);
            this.otherMember = TestMembers.MEMBER_2;
        }
    }

    /**
     * Member의 연관 엔티티(Setting, KakaoToken)만 먼저 persist (Member는 제외)
     * Member는 이후 memberRepository.save()로 저장됩니다.
     */
    private void persistMemberDependencies(Member member) {
        if (member.getSetting() != null) {
            entityManagerDelegate.persist(member.getSetting());
        }
        if (member.getKakaoToken() != null) {
            entityManagerDelegate.persist(member.getKakaoToken());
        }
    }

    /**
     * Member를 안전하게 저장하는 헬퍼 메서드
     * Member의 연관 엔티티(Setting, KakaoToken)를 자동으로 persist한 후 Member를 저장
     *
     * @param member 저장할 Member 엔티티
     * @return 저장된 Member 엔티티
     */
    protected Member saveMember(Member member) {
        persistMemberDependencies(member);
        return memberRepository.save(member);
    }

    /**
     * 여러 Member를 안전하게 저장하는 헬퍼 메서드
     * 각 Member의 연관 엔티티(Setting, KakaoToken)를 자동으로 persist한 후 Member들을 저장
     *
     * @param members 저장할 Member 엔티티들
     * @return 저장된 Member 엔티티들
     */
    protected java.util.List<Member> saveMembers(java.util.List<Member> members) {
        members.forEach(this::persistMemberDependencies);
        return memberRepository.saveAll(members);
    }

    /**
     * 하위 클래스에서 추가 설정이 필요한 경우 오버라이드
     */
    protected void setUpChild() {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * CustomUserDetails 생성 유틸리티
     * @param member 회원 엔티티
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

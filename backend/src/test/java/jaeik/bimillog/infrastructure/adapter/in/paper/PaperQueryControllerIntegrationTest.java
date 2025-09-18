package jaeik.bimillog.infrastructure.adapter.in.paper;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.entity.UserDetail;
import jaeik.bimillog.infrastructure.adapter.out.paper.MessageRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
import jaeik.bimillog.testutil.TestSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>페이퍼 Query 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 페이퍼 Query API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Import({TestContainersConfiguration.class, TestSocialLoginPortConfig.class})
@Transactional
class PaperQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
    
    /**
     * 테스트용 사용자 엔티티 생성 및 저장
     */
    private User createAndSaveUser(String userName, String socialId, String socialNickname) {
        User baseUser = TestUsers.USER1;
        User user = User.builder()
                .userName(userName)
                .socialId(socialId)
                .socialNickname(socialNickname)
                .provider(baseUser.getProvider())
                .role(baseUser.getRole())
                .setting(baseUser.getSetting())
                .build();
        return userRepository.save(user);
    }
    
    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails(User user) {
        UserDetail userDetail = UserDetail.builder()
                .userId(user.getId())
                .socialId(user.getSocialId())
                .provider(user.getProvider())
                .settingId(1L)
                .socialNickname(user.getSocialNickname())
                .userName(user.getUserName())
                .role(user.getRole())
                .tokenId(1L)
                .fcmTokenId(1L)
                .build();
        return new CustomUserDetails(userDetail);
    }
    
    /**
     * 테스트용 메시지 생성 및 저장
     */
    private Message createAndSaveMessage(User paperOwner, String anonymity, String content, int x, int y) {
        Message message = Message.builder()
                .user(paperOwner)
                .decoType(DecoType.APPLE)
                .anonymity(anonymity)
                .content(content)
                .x(x)
                .y(y)
                .build();
        return messageRepository.save(message);
    }
    
    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공 (메시지 있음)")
    void myPaper_WithMessages_Success() throws Exception {
        // Given - 사용자와 해당 사용자의 메시지 생성
        User paperOwner = createAndSaveUser("paperOwner", "owner123", "페이퍼주인");
        
        createAndSaveMessage(paperOwner, "친구1", "생일 축하해!", 1, 1);
        createAndSaveMessage(paperOwner, "친구2", "항상 행복하세요", 2, 1);
        createAndSaveMessage(paperOwner, "익명", "응원합니다", 3, 1);
        
        CustomUserDetails userDetails = createUserDetails(paperOwner);
        
        // When & Then
        mockMvc.perform(get("/api/paper")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                // 최신 생성일 기준 내림차순으로 정렬됨: "익명" -> "친구2" -> "친구1"
                .andExpect(jsonPath("$[0].anonymity").value("익명"))
                .andExpect(jsonPath("$[0].content").value("응원합니다"))
                .andExpect(jsonPath("$[0].x").value(3))
                .andExpect(jsonPath("$[0].y").value(1))
                .andExpect(jsonPath("$[0].decoType").value("APPLE"));
    }
    
    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공 (메시지 없음)")
    void myPaper_WithoutMessages_Success() throws Exception {
        // Given - 메시지가 없는 사용자
        User paperOwner = createAndSaveUser("emptyPaper", "empty123", "빈페이퍼");
        CustomUserDetails userDetails = createUserDetails(paperOwner);
        
        // When & Then
        mockMvc.perform(get("/api/paper")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 내 롤링페이퍼 조회 - 실패")
    void myPaper_Unauthenticated_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/paper"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 성공 (메시지 있음)")
    void visitPaper_WithMessages_Success() throws Exception {
        // Given - 방문할 사용자와 메시지들 생성
        User paperOwner = createAndSaveUser("visitTarget", "visit123", "방문대상");
        
        createAndSaveMessage(paperOwner, "방문자1", "좋은 메시지", 1, 1);
        createAndSaveMessage(paperOwner, "방문자2", "또 다른 메시지", 2, 2);
        
        // When & Then - 익명 사용자도 방문 가능
        mockMvc.perform(get("/api/paper/{userName}", paperOwner.getUserName()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].x").value(1))
                .andExpect(jsonPath("$[0].y").value(1))
                .andExpect(jsonPath("$[0].decoType").value("APPLE"))
                .andExpect(jsonPath("$[1].x").value(2))
                .andExpect(jsonPath("$[1].y").value(2));
    }
    
    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 성공 (메시지 없음)")
    void visitPaper_WithoutMessages_Success() throws Exception {
        // Given - 메시지가 없는 사용자
        User paperOwner = createAndSaveUser("emptyVisit", "empty456", "빈방문대상");
        
        // When & Then
        mockMvc.perform(get("/api/paper/{userName}", paperOwner.getUserName()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 롤링페이퍼 방문 - 실패")
    void visitPaper_NonExistentUser_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/paper/{userName}", "nonexistentuser"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("인증된 사용자도 다른 사용자 롤링페이퍼 방문 - 성공")
    void visitPaper_AuthenticatedUser_Success() throws Exception {
        // Given - 방문할 사용자와 방문하는 사용자
        User paperOwner = createAndSaveUser("visitTarget2", "visit789", "방문대상2");
        User visitor = createAndSaveUser("visitor", "visitor123", "방문자");
        
        createAndSaveMessage(paperOwner, "테스트", "방문용 메시지", 1, 2);
        
        CustomUserDetails visitorDetails = createUserDetails(visitor);
        
        // When & Then
        mockMvc.perform(get("/api/paper/{userName}", paperOwner.getUserName())
                        .with(user(visitorDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].x").value(1))
                .andExpect(jsonPath("$[0].y").value(2))
                .andExpect(jsonPath("$[0].decoType").value("APPLE"));
    }
    
    @Test
    @DisplayName("자신의 롤링페이퍼를 userName으로 방문 - 성공")
    void visitPaper_OwnPaper_Success() throws Exception {
        // Given - 자신의 페이퍼에 메시지 생성
        User paperOwner = createAndSaveUser("selfVisit", "self123", "자기방문");
        createAndSaveMessage(paperOwner, "나에게", "자기 메시지", 3, 1);
        
        CustomUserDetails userDetails = createUserDetails(paperOwner);
        
        // When & Then - 자신의 페이퍼도 userName으로 방문 가능
        mockMvc.perform(get("/api/paper/{userName}", paperOwner.getUserName())
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].x").value(3))
                .andExpect(jsonPath("$[0].y").value(1));
    }
}
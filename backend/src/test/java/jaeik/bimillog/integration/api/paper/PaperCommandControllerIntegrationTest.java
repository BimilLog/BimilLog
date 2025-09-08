package jaeik.bimillog.integration.api.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.paper.out.persistence.paper.MessageRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.UserRepository;
import jaeik.bimillog.global.entity.UserDetail;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>페이퍼 Command 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 페이퍼 Command API 통합 테스트</p>
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
class PaperCommandControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
     * 테스트용 사용자 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails() {
        UserDetail userDetail = UserDetail.builder()
                .userId(1L)
                .socialId("user123")
                .provider(SocialProvider.KAKAO)
                .settingId(1L)
                .socialNickname("테스트사용자")
                .userName("testuser")
                .role(UserRole.USER)
                .tokenId(1L)
                .fcmTokenId(1L)
                .build();
        return new CustomUserDetails(userDetail);
    }
    
    /**
     * 테스트용 사용자 엔티티 생성 및 저장
     */
    private User createAndSaveUser(String userName, String socialId) {
        Setting setting = Setting.createSetting();
        User user = User.builder()
                .userName(userName)
                .socialId(socialId)
                .socialNickname("테스트사용자")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        return userRepository.save(user);
    }
    
    @Test
    @DisplayName("익명 사용자 메시지 작성 - 성공")
    void writeMessage_AnonymousUser_Success() throws Exception {
        // Given - 메시지를 받을 사용자 생성
        User targetUser = createAndSaveUser("targetUser", "target123");
        
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setAnonymity("익명사용자");
        messageDTO.setContent("따뜻한 메시지입니다.");
        messageDTO.setWidth(1);
        messageDTO.setHeight(1);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        
        // When & Then
        mockMvc.perform(post("/api/paper/{userName}", targetUser.getUserName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 작성되었습니다."));
    }
    
    @Test
    @DisplayName("인증된 사용자 메시지 작성 - 성공")
    void writeMessage_AuthenticatedUser_Success() throws Exception {
        // Given - 메시지를 받을 사용자와 작성하는 사용자 생성
        User targetUser = createAndSaveUser("targetUser", "target123");
        User writerUser = createAndSaveUser("writerUser", "writer123");
        
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setUserId(writerUser.getId());
        messageDTO.setDecoType(DecoType.STAR);
        messageDTO.setAnonymity("친구1");
        messageDTO.setContent("생일 축하해!");
        messageDTO.setWidth(2);
        messageDTO.setHeight(2);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        CustomUserDetails userDetails = createUserDetails();
        
        // When & Then
        mockMvc.perform(post("/api/paper/{userName}", targetUser.getUserName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 작성되었습니다."));
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자에게 메시지 작성 - 실패")
    void writeMessage_NonExistentUser_NotFound() throws Exception {
        // Given
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setAnonymity("익명");
        messageDTO.setContent("메시지");
        messageDTO.setWidth(1);
        messageDTO.setHeight(1);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        
        // When & Then
        mockMvc.perform(post("/api/paper/{userName}", "nonexistentuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("잘못된 MessageDTO로 메시지 작성 - 실패")
    void writeMessage_InvalidMessageDTO_BadRequest() throws Exception {
        // Given - 타겟 사용자 생성
        User targetUser = createAndSaveUser("targetUser", "target123");
        
        // 유효성 검증 실패 - 너무 긴 익명 이름
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setAnonymity("매우긴익명사용자이름입니다"); // 8자 초과
        messageDTO.setContent("메시지");
        messageDTO.setWidth(1);
        messageDTO.setHeight(1);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        
        // When & Then
        mockMvc.perform(post("/api/paper/{userName}", targetUser.getUserName())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("내 페이퍼에서 메시지 삭제 - 성공")
    void deleteMessage_MyPaper_Success() throws Exception {
        // Given - Admin 패턴을 따라 사용자와 메시지 생성
        Setting setting = Setting.createSetting();
        User paperOwner = User.builder()
                .userName("paperOwner")
                .socialId("owner123")
                .socialNickname("페이퍼주인")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .setting(setting)
                .build();
        User savedUser = userRepository.save(paperOwner);
        
        Message message = Message.builder()
                .user(savedUser)
                .decoType(DecoType.APPLE)
                .anonymity("테스트")
                .content("삭제될 메시지")
                .width(1)
                .height(1)
                .build();
        Message savedMessage = messageRepository.save(message);
        
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(savedMessage.getId());
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setAnonymity("테스트");
        messageDTO.setContent("삭제될 메시지");
        messageDTO.setWidth(1);
        messageDTO.setHeight(1);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        
        // savedUser로 로그인
        UserDetail ownerUserDetail = UserDetail.builder()
                .userId(savedUser.getId())
                .socialId(savedUser.getSocialId())
                .provider(savedUser.getProvider())
                .settingId(savedUser.getSetting().getId())
                .socialNickname(savedUser.getSocialNickname())
                .userName(savedUser.getUserName())
                .role(savedUser.getRole())
                .tokenId(1L)
                .fcmTokenId(1L)
                .build();
        CustomUserDetails ownerDetails = new CustomUserDetails(ownerUserDetail);
        
        // When & Then
        mockMvc.perform(post("/api/paper/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(user(ownerDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("메시지가 삭제되었습니다."));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자의 메시지 삭제 - 실패")
    void deleteMessage_Unauthenticated_Unauthorized() throws Exception {
        // Given
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(1L);
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setAnonymity("테스트");
        messageDTO.setContent("메시지");
        messageDTO.setWidth(1);
        messageDTO.setHeight(1);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        
        // When & Then - 인증되지 않은 요청시 403 Forbidden 반환
        mockMvc.perform(post("/api/paper/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("존재하지 않는 메시지 삭제 - 실패")
    void deleteMessage_NonExistentMessage_NotFound() throws Exception {
        // Given
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(99999L); // 존재하지 않는 메시지 ID
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setAnonymity("테스트");
        messageDTO.setContent("메시지");
        messageDTO.setWidth(1);
        messageDTO.setHeight(1);
        
        String requestBody = objectMapper.writeValueAsString(messageDTO);
        CustomUserDetails userDetails = createUserDetails();
        
        // When & Then
        mockMvc.perform(post("/api/paper/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
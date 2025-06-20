package jaeik.growfarm.integration;

import jaeik.growfarm.controller.PaperController;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.message.DecoType;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.message.MessageRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>PaperController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 PaperController의 전체 API를 테스트합니다.
 * </p>
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
@Transactional
public class PaperControllerIntegrationTest {

    private final PaperController paperController;
    private final MessageRepository messageRepository;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    private Message testMessage;
    private CustomUserDetails userDetails;

    public PaperControllerIntegrationTest(PaperController paperController,
            MessageRepository messageRepository,
            SettingRepository settingRepository,
            TokenRepository tokenRepository,
            UserRepository userRepository) {
        this.paperController = paperController;
        this.messageRepository = messageRepository;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @BeforeAll
    void setUp() {
        // 사용자 설정 생성
        Setting setting1 = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting1);

        Setting setting2 = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting2);

        // 토큰 생성
        Token token = Token.builder()
                .jwtRefreshToken("testRefreshToken")
                .kakaoAccessToken("testKakaoAccessToken")
                .kakaoRefreshToken("testKakaoRefreshToken")
                .build();
        tokenRepository.save(token);

        // 사용자들 생성
        Users user1 = Users.builder()
                .kakaoId(1234567890L)
                .kakaoNickname("testNickname")
                .thumbnailImage("testImage")
                .userName("testUser")
                .role(UserRole.USER)
                .setting(setting1)
                .build();
        Users testUser = userRepository.save(user1);

        Users user2 = Users.builder()
                .kakaoId(9876543210L)
                .kakaoNickname("targetNickname")
                .thumbnailImage("targetImage")
                .userName("targetUser")
                .role(UserRole.USER)
                .setting(setting2)
                .build();
        Users targetUser = userRepository.save(user2);

        // 메시지 생성
        Message message = Message.builder()
                .anonymity("testCrop")
                .content("testMessage")
                .decoType(DecoType.APPLE)
                .width(100)
                .height(100)
                .users(targetUser)
                .build();
        testMessage = messageRepository.save(message);

        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 통합 테스트")
    void testMyPaper() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // When
        ResponseEntity<List<MessageDTO>> response = paperController.myPaper(userDetails);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("다른 사람 롤링페이퍼 방문 통합 테스트")
    void testVisitPaper() {
        // When
        ResponseEntity<List<VisitMessageDTO>> response = paperController.visitPaper("targetUser");

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("메시지 작성 통합 테스트")
    void testPlantCrop() {
        // Given
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setAnonymity("testCrop");
        messageDTO.setContent("testMessage");
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setWidth(100);
        messageDTO.setHeight(100);

        // When
        ResponseEntity<String> response = paperController.plantCrop("targetUser", messageDTO);

        // Then
        assertEquals("메시지가 작성되었습니다.", response.getBody());
    }

    @Test
    @DisplayName("메시지 삭제 통합 테스트")
    void testDeleteCrop() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(testMessage.getId());

        // When
        ResponseEntity<String> response = paperController.deleteCrop(userDetails, messageDTO);

        // Then
        assertEquals("메시지가 삭제되었습니다.", response.getBody());
    }
}
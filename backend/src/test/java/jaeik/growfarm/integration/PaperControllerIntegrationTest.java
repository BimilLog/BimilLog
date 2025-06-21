package jaeik.growfarm.integration;

import jaeik.growfarm.controller.PaperController;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.message.DecoType;
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
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>PaperController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 PaperController의 전체 API를 테스트합니다.
 * </p>
 * <p>
 * 롤링페이퍼 메시지 작성, 조회, 삭제 기능을 검증합니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Transactional
public class PaperControllerIntegrationTest {

    private final PaperController paperController;
    private final MessageRepository messageRepository;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    private CustomUserDetails userDetails;
    private Users user;

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
        // 고유한 값 생성을 위한 랜덤 값
        int uniqueId = random.nextInt(1000000);
        long timestamp = System.currentTimeMillis();

        // 사용자 설정 생성
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        // 사용자 생성 (고유한 값 사용)
        Users testUser = Users.builder()
                .kakaoId(timestamp + uniqueId)
                .kakaoNickname("testNickname")
                .thumbnailImage("testImage")
                .userName("testUser")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        user = userRepository.save(testUser);
        // 토큰 생성
        Token token = Token.builder()
                .users(testUser)
                .jwtRefreshToken("testRefreshToken" + uniqueId)
                .kakaoAccessToken("testKakaoAccessToken" + uniqueId)
                .kakaoRefreshToken("testKakaoRefreshToken" + uniqueId)
                .build();
        tokenRepository.save(token);


        // ClientDTO 생성
        ClientDTO clientDTO = new ClientDTO(testUser, token.getId(), null);
        userDetails = new CustomUserDetails(clientDTO);
    }

    @AfterAll
    void tearDown() {
        // 별도 정리 로직 없음
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 통합 테스트")
    void testMyPaper() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null,
                        userDetails.getAuthorities()));

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
        ResponseEntity<List<VisitMessageDTO>> response = paperController.visitPaper("testUser");

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("메시지 작성 통합 테스트")
    void testPlantCrop() {

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setUserId(user.getId());
        messageDTO.setAnonymity("익명");
        messageDTO.setContent("testMessage");
        messageDTO.setDecoType(DecoType.APPLE);
        messageDTO.setWidth(100);
        messageDTO.setHeight(100);

        // When
        ResponseEntity<String> response = paperController.writeMessage("testUser", messageDTO);

        // Then
        assertEquals("메시지가 작성되었습니다.", response.getBody());
    }

    @Test
    @DisplayName("메시지 삭제 통합 테스트")
    void testDeleteCrop() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null,
                        userDetails.getAuthorities()));

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(1L);
        messageDTO.setUserId(user.getId());

        // When
        ResponseEntity<String> response = paperController.deleteMessage(userDetails, messageDTO);

        // Then
        assertEquals("메시지가 삭제되었습니다.", response.getBody());
    }
}
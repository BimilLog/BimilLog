package jaeik.growfarm.integration;

import jaeik.growfarm.controller.FarmController;
import jaeik.growfarm.dto.farm.CropDTO;
import jaeik.growfarm.dto.farm.VisitCropDTO;
import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.crop.CropType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>FarmController 통합 테스트</h2>
 * 실제 데이터베이스와 서비스를 사용하여 FarmController의 전체 API를 테스트합니다.
 *
 * @since 2025.05.17
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
@Transactional
public class FarmControllerIntegrationTest {

    private final FarmController farmController;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;

    private Users testUser;

    public FarmControllerIntegrationTest(FarmController farmController, SettingRepository settingRepository, TokenRepository tokenRepository, UserRepository userRepository, UserUtil userUtil) {
        this.farmController = farmController;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
    }

    /**
     * <h3>테스트 데이터 초기화</h3>
     * 사용자 데이터 생성
     *
     * @since 2025.05.17
     */
    @BeforeAll
    void setUp() {
        Setting setting = Setting.builder()
                .isFarmNotification(true)
                .isCommentNotification(true)
                .isPostFeaturedNotification(true)
                .isCommentFeaturedNotification(true)
                .build();
        settingRepository.save(setting);

        Token token = Token.builder()
                .jwtRefreshToken("testRefreshToken")
                .kakaoAccessToken("testKakaoAccessToken")
                .kakaoRefreshToken("testKakaoRefreshToken")
                .build();
        tokenRepository.save(token);

        Users user = Users.builder()
                .kakaoId(1234567890L)
                .kakaoNickname("testNickname")
                .thumbnailImage("testImage")
                .farmName("testFarm")
                .role(UserRole.USER)
                .setting(setting)
                .token(token)
                .build();
        testUser = userRepository.save(user);
    }


    /**
     * <h3>농작물 심기 통합 테스트</h3>
     *
     * @since 2025.05.17
     */
    @Test
    @Order(1)
    @DisplayName("농작물 심기 통합 테스트")
    void testPlantCrop() throws IOException {
        // Given
        String farmName = "testFarm";

        CropDTO cropDTO = new CropDTO();
        cropDTO.setCropType(CropType.TOMATO);
        cropDTO.setNickname("testCrop");
        cropDTO.setMessage("testMessage");
        cropDTO.setWidth(1);
        cropDTO.setHeight(2);

        // When
        ResponseEntity<String> response = farmController.plantCrop(farmName, cropDTO);

        // Then
        assertEquals("농작물이 심어졌습니다.", response.getBody());
    }

    /**
     * <h3>다른 농장 방문 통합 테스트</h3>
     *
     * @since 2025.05.17
     */
    @Test
    @Order(2)
    @DisplayName("다른 농장 방문 통합 테스트")
    void testVisitFarm() {
        // Given
        String farmName = "testFarm";

        // When
        ResponseEntity<List<VisitCropDTO>> response = farmController.visitFarm(farmName);
        List<VisitCropDTO> visitCropList = response.getBody();

        // Then
        assertNotNull(visitCropList);
        assertEquals(1, visitCropList.size());

        VisitCropDTO visitCrop = visitCropList.getFirst();
        assertEquals(CropType.TOMATO, visitCrop.getCropType());
        assertEquals(1, visitCrop.getWidth());
        assertEquals(2, visitCrop.getHeight());
    }

    /**
     * <h3>내 농장 조회 통합 테스트</h3>
     *
     * @since 2025.05.17
     */
    @Test
    @Order(3)
    @DisplayName("내 농장 조회 통합 테스트")
    void testMyFarm() {
        // Given
        UserDTO userDTO = userUtil.UserToDTO(testUser);
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<List<CropDTO>> response = farmController.myFarm(userDetails);
        List<CropDTO> cropList = response.getBody();

        // Then
        assertNotNull(cropList);
        assertEquals(1, cropList.size());

        CropDTO visitCrop = cropList.getFirst();
        assertEquals(CropType.TOMATO, visitCrop.getCropType());
        assertEquals(1, visitCrop.getWidth());
        assertEquals(2, visitCrop.getHeight());
        assertEquals("testCrop", visitCrop.getNickname());
        assertEquals("testMessage", visitCrop.getMessage());
    }

    /**
     * <h3>농작물 삭제 통합 테스트</h3>
     *
     * @since 2025.05.17
     */
    @Test
    @Order(4)
    @DisplayName("농작물 삭제 통합 테스트")
    void testDeleteCrop() {
        // Given
        UserDTO userDTO = userUtil.UserToDTO(testUser);
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<String> response = farmController.deleteCrop(userDetails, 1L);

        // Then
        assertEquals("농작물이 삭제되었습니다.", response.getBody());
    }


}
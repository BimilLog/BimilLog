package jaeik.growfarm.integration;

import jaeik.growfarm.controller.AdminController;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>AdminController 통합 테스트</h2>
 * <p>실제 데이터베이스와 서비스를 사용하여 AdminController의 전체 API를 테스트합니다.</p>
 * @since 1.0.0
 * @author Jaeik
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Commit
@Transactional
public class AdminControllerIntegrationTest {

    private final AdminController adminController;
    private final ReportRepository reportRepository;
    private final SettingRepository settingRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;

    private Users adminUser;
    private Users normalUser;
    private Report testReport;

    public AdminControllerIntegrationTest(AdminController adminController,
                                          ReportRepository reportRepository,
                                          SettingRepository settingRepository,
                                          TokenRepository tokenRepository,
                                          UserRepository userRepository,
                                          UserUtil userUtil) {
        this.adminController = adminController;
        this.reportRepository = reportRepository;
        this.settingRepository = settingRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
    }

    /**
     * <h3>테스트 데이터 초기화</h3>
     * 관리자 사용자, 일반 사용자, 신고 데이터 생성
     *
     * @since 2025.05.17
     */
    @BeforeAll
    void setUp() {
        // 관리자 사용자 설정 생성
        Setting adminSetting = Setting.builder()
                .farmNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .commentFeaturedNotification(true)
                .build();
        settingRepository.save(adminSetting);

        // 관리자 사용자 생성
        Users adminUser = Users.builder()
                .kakaoId(9876543210L)
                .kakaoNickname("adminNickname")
                .thumbnailImage("adminImage")
                .farmName("adminFarm")
                .role(UserRole.ADMIN)
                .setting(adminSetting)
                .build();
        userRepository.save(adminUser);

        // 관리자 토큰 생성
        Token adminToken = Token.builder()
                .jwtRefreshToken("adminRefreshToken")
                .kakaoAccessToken("adminKakaoAccessToken")
                .kakaoRefreshToken("adminKakaoRefreshToken")
                .users(adminUser)
                .build();
        tokenRepository.save(adminToken);


        // 일반 사용자 설정 생성
        Setting userSetting = Setting.builder()
                .farmNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .commentFeaturedNotification(true)
                .build();
        settingRepository.save(userSetting);

        // 일반 사용자 토큰 생성
        Token userToken = Token.builder()
                .jwtRefreshToken("userRefreshToken")
                .kakaoAccessToken("userKakaoAccessToken")
                .kakaoRefreshToken("userKakaoRefreshToken")
                .build();
        tokenRepository.save(userToken);

        // 일반 사용자 생성
        Users user = Users.builder()
                .kakaoId(1234567890L)
                .kakaoNickname("userNickname")
                .thumbnailImage("userImage")
                .farmName("userFarm")
                .role(UserRole.USER)
                .setting(userSetting)
                .token(userToken)
                .build();
        normalUser = userRepository.save(user);

        // 신고 데이터 생성
        Report report = Report.builder()
                .reportType(ReportType.POST)
                .users(normalUser)
                .targetId(1L)
                .content("Test Report Content")
                .build();
        testReport = reportRepository.save(report);
    }

    /**
     * <h3>신고 목록 조회 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("신고 목록 조회 통합 테스트")
    void testGetReportList() {
        // 인증 설정 (관리자 권한)
        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(adminUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<Page<ReportDTO>> response = adminController.getReportList(0, 10, null);

        // Then
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }

    /**
     * <h3>신고 상세 조회 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("신고 상세 조회 통합 테스트")
    void testGetReportDetail() {
        // 인증 설정 (관리자 권한)
        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(adminUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<ReportDTO> response = adminController.getReportDetail(testReport.getId());

        // Then
        assertNotNull(response.getBody());
        assertEquals(testReport.getId(), response.getBody().getReportId());
        assertEquals(testReport.getReportType(), response.getBody().getReportType());
        assertEquals(testReport.getUsers().getId(), response.getBody().getUserId());
        assertEquals(testReport.getTargetId(), response.getBody().getTargetId());
        assertEquals(testReport.getContent(), response.getBody().getContent());
    }

    /**
     * <h3>유저 차단 통합 테스트</h3>
     * @since 2025.05.17
     */
    @Test
    @DisplayName("유저 차단 통합 테스트")
    void testBanUser() {
        // 인증 설정 (관리자 권한)
        CustomUserDetails userDetails = new CustomUserDetails(userUtil.UserToDTO(adminUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        // When
        ResponseEntity<String> response = adminController.banUser(normalUser.getId());

        // Then
        assertEquals("유저를 성공적으로 차단했습니다.", response.getBody());
    }
}

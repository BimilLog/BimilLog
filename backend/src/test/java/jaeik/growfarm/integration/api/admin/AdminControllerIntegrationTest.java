package jaeik.growfarm.integration.api.admin;

import jaeik.growfarm.controller.AdminController;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * <h2>AdminController 통합 테스트</h2>
 * <p>
 * 실제 데이터베이스와 서비스를 사용하여 AdminController의 전체 API를 테스트합니다.
 * </p>
 * <p>
 * 카카오 서버와 통신이 필요한 API는 테스트에서 제외함.
 * </p>
 * <p>
 * 이후에 카카오 Mock 서버를 만들어 테스트에 추가 필요.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(PER_CLASS)
@Transactional
public class AdminControllerIntegrationTest {

        private final AdminController adminController;
        private final ReportRepository reportRepository;
        private final SettingRepository settingRepository;
        private final TokenRepository tokenRepository;
        private final UserRepository userRepository;

        private Report testReport;
        private CustomUserDetails userDetails;

        private final Random random = new Random();

        public AdminControllerIntegrationTest(AdminController adminController,
                        ReportRepository reportRepository,
                        SettingRepository settingRepository,
                        TokenRepository tokenRepository,
                        UserRepository userRepository) {
                this.adminController = adminController;
                this.reportRepository = reportRepository;
                this.settingRepository = settingRepository;
                this.tokenRepository = tokenRepository;
                this.userRepository = userRepository;
        }

        @BeforeAll
        void setUp() {
                // 고유한 값 생성을 위한 랜덤 값
                int uniqueId = random.nextInt(1000000);
                long timestamp = System.currentTimeMillis();

                // 관리자 설정 생성
                Setting adminSetting = Setting.builder()
                                .messageNotification(true)
                                .commentNotification(true)
                                .postFeaturedNotification(true)
                                .build();
                settingRepository.save(adminSetting);

                // 관리자 생성
                Users admin = Users.builder()
                                .kakaoId(timestamp + uniqueId)
                                .kakaoNickname("admin" + uniqueId)
                                .thumbnailImage("adminImage")
                                .userName("관리자" + uniqueId)
                                .role(UserRole.ADMIN)
                                .setting(adminSetting)
                                .build();
                Users testUser = userRepository.save(admin);

                // 토큰 생성
                Token adminToken = Token.builder()
                                .users(testUser)
                                .jwtRefreshToken("adminRefreshToken" + uniqueId)
                                .kakaoAccessToken("adminKakaoAccessToken" + uniqueId)
                                .kakaoRefreshToken("adminKakaoRefreshToken" + uniqueId)
                                .build();
                tokenRepository.save(adminToken);

                // 신고 생성
                Report report = Report.builder()
                                .reportType(ReportType.POST)
                                .users(testUser)
                                .targetId(1L)
                                .content("Test Report " + uniqueId)
                                .build();
                testReport = reportRepository.save(report);

                // ClientDTO 생성
                ClientDTO clientDTO = new ClientDTO(testUser, adminToken.getId(), null);
                userDetails = new CustomUserDetails(clientDTO);
        }

        @Test
        @DisplayName("신고 목록 조회 통합 테스트")
        void testGetReportList() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<Page<ReportDTO>> response = adminController.getReportList(0, 10, null);

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("신고 상세 조회 통합 테스트")
        void testGetReportDetail() {
                // Given
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                // When
                ResponseEntity<ReportDTO> response = adminController.getReportDetail(testReport.getId());

                // Then
                assertEquals(200, response.getStatusCodeValue());
                assertNotNull(response.getBody());
                assertEquals(testReport.getContent(), response.getBody().getContent());
        }

        /*
         * 카카오 API가 필요한 테스트는 Mock 서버 구축 후 추가 예정:
         * - testBanUser() - 사용자 차단 (카카오 연결 해제 필요)
         */
}
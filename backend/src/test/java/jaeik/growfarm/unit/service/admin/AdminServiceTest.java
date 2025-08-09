package jaeik.growfarm.unit.service.admin;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.BlackList;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.kakao.KakaoService;
import jaeik.growfarm.service.admin.AdminService;
import jaeik.growfarm.service.admin.AdminUpdateService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>AdminService 단위 테스트</h2>
 * <p>
 * 관리자 서비스의 비즈니스 로직을 테스트합니다.
 * </p>
 * @version 1.0.0
 * @author Jaeik
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdminServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KakaoService kakaoService;

    @Mock
    private AdminUpdateService adminUpdateService;

    @Mock
    private jaeik.growfarm.repository.comment.CommentRepository commentRepository;

    @Mock
    private jaeik.growfarm.repository.post.PostRepository postRepository;

    private AdminService adminService;

    private Report mockReport;
    private Users mockUser;
    private ReportDTO mockReportDTO;
    private MockedStatic<ReportDTO> mockedReportDTO;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(reportRepository, userRepository, kakaoService, adminUpdateService, commentRepository, postRepository);
        // Mock Report 설정
        mockReport = mock(Report.class);
        when(mockReport.getId()).thenReturn(1L);
        when(mockReport.getReportType()).thenReturn(ReportType.POST);
        when(mockReport.getContent()).thenReturn("Test report content");

        // Mock User 설정
        mockUser = mock(Users.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getKakaoId()).thenReturn(123456789L);

        // Mock ReportDTO 설정
        mockReportDTO = mock(ReportDTO.class);
        when(mockReportDTO.getReportId()).thenReturn(1L);
        when(mockReportDTO.getReportType()).thenReturn(ReportType.POST);
        when(mockReportDTO.getContent()).thenReturn("Test report content");

        mockedReportDTO = Mockito.mockStatic(ReportDTO.class);
    }

    @AfterEach
    void tearDown() {
        mockedReportDTO.close();
    }

    @Test
    @DisplayName("신고 목록 조회 테스트 - 전체 조회")
    void testGetReportListAll() {
        // Given
        List<ReportDTO> mockReports = List.of(mockReportDTO);
        Page<ReportDTO> mockPage = new PageImpl<>(mockReports);
        when(reportRepository.findReportsWithPaging(any(), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<ReportDTO> result = adminService.getReportList(0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(reportRepository, times(1)).findReportsWithPaging(eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("신고 목록 조회 테스트 - 타입별 조회")
    void testGetReportListByType() {
        // Given
        List<ReportDTO> mockReports = List.of(mockReportDTO);
        Page<ReportDTO> mockPage = new PageImpl<>(mockReports);
        when(reportRepository.findReportsWithPaging(eq(ReportType.POST), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<ReportDTO> result = adminService.getReportList(0, 10, ReportType.POST);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(reportRepository, times(1)).findReportsWithPaging(eq(ReportType.POST), any(Pageable.class));
    }

    @Test
    @DisplayName("신고 상세 조회 성공 테스트")
    void testGetReportDetailSuccess() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(mockReport));
        mockedReportDTO.when(() -> ReportDTO.createReportDTO(mockReport)).thenReturn(mockReportDTO);

        // When
        ReportDTO result = adminService.getReportDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(mockReportDTO.getReportId(), result.getReportId());
        verify(reportRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("신고 상세 조회 실패 테스트 - 신고를 찾을 수 없음")
    void testGetReportDetailNotFound() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CustomException.class, () -> adminService.getReportDetail(1L));
        verify(reportRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("사용자 차단 성공 테스트")
    @Disabled("카카오 연결 해제 기능이 외부 API에 의존하므로 테스트를 비활성화합니다.")
    void testBanUserSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(mockUser.getKakaoId()).thenReturn(123456789L);
        doNothing().when(adminUpdateService).banUserProcess(eq(1L), any(BlackList.class));
        when(kakaoService.unlinkByAdmin(eq(123456789L))).thenReturn("OK");

        // When
        ReportDTO reportDTO = ReportDTO.builder()
                .reportId(1L)
                .targetId(1L)
                .reportType(ReportType.POST)
                .build();
        adminService.banUser(reportDTO);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(adminUpdateService, times(1)).banUserProcess(eq(1L), any(BlackList.class));
        verify(kakaoService, times(1)).unlinkByAdmin(eq(123456789L));
    }

    @Test
    @DisplayName("사용자 차단 실패 테스트 - 사용자를 찾을 수 없음")
    void testBanUserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ReportDTO reportDTO = ReportDTO.builder()
                .reportId(1L)
                .targetId(1L)
                .reportType(ReportType.POST)
                .build();
        assertThrows(CustomException.class, () -> adminService.banUser(reportDTO));
        verify(userRepository, times(1)).findById(1L);
        verify(adminUpdateService, never()).banUserProcess(anyLong(), any(BlackList.class));
        verify(kakaoService, never()).unlinkByAdmin(anyLong());
    }

    @Test
    @DisplayName("사용자 차단 실패 테스트 - 카카오 연결 해제 실패")
    void testBanUserKakaoUnlinkFail() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        doNothing().when(adminUpdateService).banUserProcess(anyLong(), any(BlackList.class));
        doThrow(new RuntimeException("Kakao Unlink Failed")).when(kakaoService).unlinkByAdmin(anyLong());

        // When & Then
        ReportDTO reportDTO = ReportDTO.builder()
                .reportId(1L)
                .targetId(1L)
                .reportType(ReportType.POST)
                .build();
        CustomException exception = assertThrows(CustomException.class, () -> adminService.banUser(reportDTO));
        assertEquals(ErrorCode.BAN_USER_ERROR.getMessage(), exception.getMessage());

        verify(userRepository, times(1)).findById(1L);
        verify(adminUpdateService, times(1)).banUserProcess(anyLong(), any(BlackList.class));
        verify(kakaoService, times(1)).unlinkByAdmin(anyLong());
    }
}
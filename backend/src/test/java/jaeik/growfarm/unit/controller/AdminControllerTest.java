package jaeik.growfarm.unit.controller;

import jaeik.growfarm.controller.AdminController;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.service.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private ReportDTO mockReportDTO;
    private List<ReportDTO> mockReportList;

    @BeforeEach
    void setUp() {
        // Setup mock data
        mockReportDTO = ReportDTO.builder()
                .reportId(1L)
                .reportType(ReportType.POST)
                .userId(1L)
                .targetId(2L)
                .content("Test report content")
                .build();

        mockReportList = new ArrayList<>();
        mockReportList.add(mockReportDTO);
    }

    @Test
    @DisplayName("신고 목록 조회 테스트")
    @WithMockUser(roles = "ADMIN")
    void testGetReportList() {
        // Given
        Page<ReportDTO> reportPage = new PageImpl<>(mockReportList);
        when(adminService.getReportList(anyInt(), anyInt(), any())).thenReturn(reportPage);

        // When
        ResponseEntity<Page<ReportDTO>> response = adminController.getReportList(0, 10, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals(1L, response.getBody().getContent().getFirst().getReportId());
        assertEquals(ReportType.POST, response.getBody().getContent().getFirst().getReportType());
    }

    @Test
    @DisplayName("신고 상세 조회 테스트")
    @WithMockUser(roles = "ADMIN")
    void testGetReportDetail() {
        // Given
        when(adminService.getReportDetail(anyLong())).thenReturn(mockReportDTO);

        // When
        ResponseEntity<ReportDTO> response = adminController.getReportDetail(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getReportId());
        assertEquals(ReportType.POST, response.getBody().getReportType());
        assertEquals("Test report content", response.getBody().getContent());
    }

    @Test
    @DisplayName("유저 차단 테스트")
    @WithMockUser(roles = "ADMIN")
    void testBanUser() {
        // When
        ResponseEntity<String> response = adminController.banUser(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("유저를 성공적으로 차단했습니다.", response.getBody());
    }
}

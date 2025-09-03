package jaeik.bimillog.infrastructure.adapter.user.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>UserCommandController 단위 테스트</h2>
 * <p>UserCommandController의 신고/건의사항 제출 API에 대한 단위 테스트</p>
 * <p>Given-When-Then 패턴을 사용하여 테스트를 구조화합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@WebMvcTest(UserCommandController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCommandUseCase userCommandUseCase;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("댓글 신고 제출 - 성공")
    void submitCommentReport_Success() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(123L)
                .content("부적절한 댓글입니다")
                .build();

        UserDTO userDTO = UserDTO.builder()
                .userId(1L)
                .userName("testuser")
                .role(UserRole.USER)
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .tokenId(100L)
                .fcmTokenId(null)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));

        // 이벤트 발행 검증
        ArgumentCaptor<ReportSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ReportSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ReportSubmittedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.reporterId()).isEqualTo(1L);
        assertThat(capturedEvent.reporterName()).isEqualTo("testuser");
        assertThat(capturedEvent.reportVO().reportType()).isEqualTo(ReportType.COMMENT);
        assertThat(capturedEvent.reportVO().targetId()).isEqualTo(123L);
        assertThat(capturedEvent.reportVO().content()).isEqualTo("부적절한 댓글입니다");
    }

    @Test
    @DisplayName("게시글 신고 제출 - 성공")
    void submitPostReport_Success() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(456L)
                .content("스팸 게시글입니다")
                .build();

        UserDTO userDTO = UserDTO.builder()
                .userId(2L)
                .userName("reporter")
                .role(UserRole.USER)
                .socialId("social456")
                .provider(SocialProvider.KAKAO)
                .tokenId(101L)
                .fcmTokenId(null)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));

        // 이벤트 발행 검증
        ArgumentCaptor<ReportSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ReportSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ReportSubmittedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.reporterId()).isEqualTo(2L);
        assertThat(capturedEvent.reporterName()).isEqualTo("reporter");
        assertThat(capturedEvent.reportVO().reportType()).isEqualTo(ReportType.POST);
        assertThat(capturedEvent.reportVO().targetId()).isEqualTo(456L);
        assertThat(capturedEvent.reportVO().content()).isEqualTo("스팸 게시글입니다");
    }

    @Test
    @DisplayName("건의사항 제출 - 성공 (targetId 없음)")
    void submitSuggestion_Success() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.IMPROVEMENT)
                .targetId(null) // 건의사항은 targetId가 필요 없음
                .content("새로운 기능을 건의합니다")
                .build();

        UserDTO userDTO = UserDTO.builder()
                .userId(3L)
                .userName("suggester")
                .role(UserRole.USER)
                .socialId("social789")
                .provider(SocialProvider.KAKAO)
                .tokenId(102L)
                .fcmTokenId(null)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));

        // 이벤트 발행 검증
        ArgumentCaptor<ReportSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ReportSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ReportSubmittedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.reporterId()).isEqualTo(3L);
        assertThat(capturedEvent.reporterName()).isEqualTo("suggester");
        assertThat(capturedEvent.reportVO().reportType()).isEqualTo(ReportType.IMPROVEMENT);
        assertThat(capturedEvent.reportVO().targetId()).isNull();
        assertThat(capturedEvent.reportVO().content()).isEqualTo("새로운 기능을 건의합니다");
    }

    @Test
    @DisplayName("익명 신고 제출 - 성공 (비로그인 사용자)")
    void submitAnonymousReport_Success() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(123L)
                .content("부적절한 댓글입니다")
                .build();

        // When & Then - 인증 정보 없이 요청
        mockMvc.perform(post("/api/user/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));

        // 익명 신고 이벤트 발행 검증
        ArgumentCaptor<ReportSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ReportSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ReportSubmittedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.reporterId()).isNull(); // 익명 사용자는 ID가 null
        assertThat(capturedEvent.reporterName()).isEqualTo("익명");
        assertThat(capturedEvent.reportVO().reportType()).isEqualTo(ReportType.COMMENT);
        assertThat(capturedEvent.reportVO().targetId()).isEqualTo(123L);
        assertThat(capturedEvent.reportVO().content()).isEqualTo("부적절한 댓글입니다");
    }

    @Test
    @DisplayName("신고 제출 - 실패 (잘못된 요청 데이터)")
    void submitReport_Fail_InvalidData() throws Exception {
        // Given - reportType이 누락된 잘못된 데이터
        String invalidJson = "{\"targetId\":123,\"content\":\"내용\"}";
        
        UserDTO userDTO = UserDTO.builder()
                .userId(1L)
                .userName("testuser")
                .role(UserRole.USER)
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .tokenId(100L)
                .fcmTokenId(null)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // 이벤트가 발행되지 않았는지 검증
        verify(eventPublisher, times(0)).publishEvent(any(ReportSubmittedEvent.class));
    }

    @Test
    @DisplayName("신고 제출 - 실패 (빈 내용)")
    void submitReport_Fail_EmptyContent() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.COMMENT)
                .targetId(123L)
                .content("") // 빈 내용
                .build();

        UserDTO userDTO = UserDTO.builder()
                .userId(1L)
                .userName("testuser")
                .role(UserRole.USER)
                .socialId("social123")
                .provider(SocialProvider.KAKAO)
                .tokenId(100L)
                .fcmTokenId(null)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userDTO);

        // When & Then
        mockMvc.perform(post("/api/user/report")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO)))
                .andExpect(status().isBadRequest());

        // 이벤트가 발행되지 않았는지 검증
        verify(eventPublisher, times(0)).publishEvent(any(ReportSubmittedEvent.class));
    }
}
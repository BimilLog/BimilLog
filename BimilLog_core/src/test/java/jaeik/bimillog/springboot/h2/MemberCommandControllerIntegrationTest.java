package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.admin.dto.ReportDTO;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.dto.MemberNameDTO;
import jaeik.bimillog.domain.member.dto.SettingDTO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.config.TestSocialLoginAdapterConfig;
import jaeik.bimillog.testutil.fixtures.AuthTestFixtures;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>사용자 명령 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest를 사용한 실제 Member Command API 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("사용자 명령 컨트롤러 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("springboot-h2")
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginAdapterConfig.class})
class MemberCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberRepository userRepository;

    @Test
    @DisplayName("닉네임 변경 통합 테스트 - 성공")
    void updateUserName_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);
        
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(testMember);
        
        MemberNameDTO memberNameDTO = new MemberNameDTO();
        memberNameDTO.setMemberName("새로운닉네임");

        // When & Then
        mockMvc.perform(post("/api/member/username")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberNameDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임이 변경되었습니다."));
    }

    @Test
    @DisplayName("닉네임 변경 통합 테스트 - 유효성 검증 실패 (8글자 초과)")
    void updateUserName_IntegrationTest_ValidationFail() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);
        
        var userDetails = createCustomUserDetails(testMember);
        
        MemberNameDTO memberNameDTO = new MemberNameDTO();
        memberNameDTO.setMemberName("아주긴닉네임이라서8글자초과"); // 8글자 초과

        // When & Then
        mockMvc.perform(post("/api/member/username")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberNameDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("설정 수정 통합 테스트 - 성공")
    void updateSetting_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);
        
        var userDetails = createCustomUserDetails(testMember);
        
        SettingDTO settingDTO = SettingDTO.builder()
                .messageNotification(Boolean.FALSE)
                .commentNotification(Boolean.TRUE)
                .postFeaturedNotification(Boolean.FALSE)
                .build();

        // When & Then
        mockMvc.perform(post("/api/member/setting")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("설정 수정 완료"));
    }





    @Test
    @DisplayName("닉네임 변경 - 빈 문자열 검증 실패 - 400 Bad Request")
    void updateUserName_BlankValidation_BadRequest() throws Exception {
        // Given
        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);
        
        var userDetails = createCustomUserDetails(testMember);
        
        MemberNameDTO memberNameDTO = new MemberNameDTO();
        memberNameDTO.setMemberName(""); // 빈 문자열

        // When & Then
        mockMvc.perform(post("/api/member/username")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberNameDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }




    @Test
    @DisplayName("신고 제출 통합 테스트 - 성공")
    void submitReport_Success() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("테스트 신고 내용입니다.")
                .build();

        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);
        var userDetails = createCustomUserDetails(testMember);

        // When & Then
        mockMvc.perform(post("/api/member/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("신고/건의사항이 접수되었습니다."));
    }

    @Test
    @DisplayName("신고 제출 - 빈 내용 - 400 Bad Request")
    void submitReport_EmptyContent_BadRequest() throws Exception {
        // Given
        ReportDTO reportDTO = ReportDTO.builder()
                .reportType(ReportType.POST)
                .targetId(123L)
                .content("") // 빈 내용
                .build();

        Member testMember = TestMembers.createUnique();
        testMember = saveMember(testMember);
        var userDetails = createCustomUserDetails(testMember);

        // When & Then
        mockMvc.perform(post("/api/member/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDTO))
                        .with(user(userDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }



    @Test
    @DisplayName("회원 탈퇴 통합 테스트 - 성공")
    void withdraw_Success() throws Exception {
        var result = mockMvc.perform(delete("/api/member/withdraw")
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andReturn();

        var cookies = result.getResponse().getHeaders("Set-Cookie");

        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie).startsWith("jwt_access_token="));
        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie).startsWith("jwt_refresh_token="));
        entityManagerDelegate.flush();

        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> {
                    entityManagerDelegate.clear();
                    assertThat(userRepository.findById(testMember.getId())).isEmpty();
                });
    }

}

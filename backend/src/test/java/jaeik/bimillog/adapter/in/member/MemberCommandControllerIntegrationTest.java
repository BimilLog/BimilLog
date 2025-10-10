package jaeik.bimillog.adapter.in.member;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.adapter.in.admin.dto.ReportDTO;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.infrastructure.adapter.in.member.dto.MemberNameDTO;
import jaeik.bimillog.infrastructure.adapter.in.member.dto.SettingDTO;
import jaeik.bimillog.infrastructure.adapter.in.member.dto.SignUpRequestDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.member.MemberRepository;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.Optional;

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
@IntegrationTest
@Import(TestSocialLoginPortConfig.class)
@DisplayName("사용자 명령 컨트롤러 통합 테스트")
class MemberCommandControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberRepository userRepository;

    @Test
    @DisplayName("회원가입 통합 테스트 - 성공")
    void signUp_IntegrationTest_Success() throws Exception {
        // Given - 먼저 소셜 로그인으로 temp 데이터를 생성
        SocialLoginRequestDTO socialRequest = new SocialLoginRequestDTO("KAKAO", "new_user_code", "integration-test-fcm-TemporaryToken");

        // 1. 소셜 로그인으로 임시 데이터 생성
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socialRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // 2. 응답에서 temp_user_id 쿠키 추출 (UUID는 HttpOnly 쿠키로 전달됨)
        var tempCookie = loginResult.getResponse().getCookie("temp_user_id");
        assertThat(tempCookie).isNotNull();

        // 3. 회원가입 수행 (UUID는 요청 본문이 아닌 쿠키로 전달)
        SignUpRequestDTO signUpRequest = new SignUpRequestDTO("통합테스트사용자");

        mockMvc.perform(post("/api/member/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest))
                        .cookie(tempCookie)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(cookie().value("temp_user_id", ""))
                .andExpect(cookie().maxAge("temp_user_id", 0))
                .andExpect(content().string(""));
    }

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

        long start = System.currentTimeMillis();
        Optional<Member> remaining;
        do {
            entityManagerDelegate.clear();
            remaining = userRepository.findById(testMember.getId());
            if (remaining.isEmpty()) {
                break;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() - start < 2000);

        assertThat(remaining).isEmpty();
    }

}

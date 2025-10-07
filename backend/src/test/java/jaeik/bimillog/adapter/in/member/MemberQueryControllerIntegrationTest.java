package jaeik.bimillog.adapter.in.member;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthTokenRepository;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.member.MemberRepository;
import jaeik.bimillog.testutil.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>사용자 조회 컨트롤러 통합 테스트</h2>
 * <p>@SpringBootTest + H2 인메모리 데이터베이스 환경에서 사용자 조회 API를 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ActiveProfiles("h2test")
@Import({H2TestConfiguration.class, TestSocialLoginPortConfig.class})
@DisplayName("사용자 조회 컨트롤러 통합 테스트")
@Tag("integration")
class MemberQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Test
    @DisplayName("닉네임 중복 확인 통합 테스트 - 사용 가능한 닉네임")
    void checkUserName_Available_IntegrationTest() throws Exception {
        // Given
        String availableUserName = "사용가능한닉네임";

        // When & Then
        mockMvc.perform(get("/api/member/username/check")
                        .param("memberName", availableUserName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("닉네임 중복 확인 통합 테스트 - 중복된 닉네임")
    void checkUserName_Duplicate_IntegrationTest() throws Exception {
        // Given - 기존 사용자 생성 및 저장
        Member existingMember = TestMembers.createUnique();
        saveMember(existingMember);

        // When & Then
        mockMvc.perform(get("/api/member/username/check")
                        .param("memberName", existingMember.getMemberName()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("사용자 설정 조회 통합 테스트 - 성공")
    void getSetting_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        Member testMember = TestMembers.createUnique();
        userRepository.save(testMember);
        
        var userDetails = createCustomUserDetails(testMember);

        // When & Then
        mockMvc.perform(get("/api/member/setting")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageNotification").value(true))
                .andExpect(jsonPath("$.commentNotification").value(true))
                .andExpect(jsonPath("$.postFeaturedNotification").value(true));
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 정상 케이스 (Mock 환경)")
    void getKakaoFriendList_Success() throws Exception {
        // Given - 테스트용 사용자 생성 및 저장
        Member member = TestMembers.createUnique();
        Member savedMember = saveMember(member);
        
        // 테스트용 토큰 생성 및 저장
        AuthToken authToken = AuthToken.createToken("test-refresh-TemporaryToken", savedMember);
        AuthToken savedAuthToken = authTokenRepository.save(authToken);
        
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(savedMember, savedAuthToken.getId(), null);

        // When & Then
        mockMvc.perform(get("/api/member/friendlist")
                        .param("offset", "0")
                        .param("limit", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk());
                // Note: 실제 카카오 API 호출은 Mock으로 처리되므로 응답 구조 검증은 제한적
    }

    @Test
    @DisplayName("카카오 친구 목록 조회 - 동의 필요 시 401 응답")
    void getKakaoFriendList_ConsentRequired() throws Exception {
        // Given
        Member member = TestMembers.createUnique();
        Member savedMember = saveMember(member);

        AuthToken authToken = AuthToken.createToken("test-refresh-TemporaryToken", savedMember);
        AuthToken savedAuthToken = authTokenRepository.save(authToken);
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(savedMember, savedAuthToken.getId(), null);

        TestSocialLoginPortConfig.setFriendConsentRequired(true);
        try {
            mockMvc.perform(get("/api/member/friendlist")
                            .param("offset", "0")
                            .param("limit", "10")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(MemberErrorCode.KAKAO_FRIEND_CONSENT_FAIL.getMessage()));
        } finally {
            TestSocialLoginPortConfig.setFriendConsentRequired(false);
        }
    }
}

package jaeik.bimillog.adapter.in.user;

import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.TokenRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.testutil.AuthTestFixtures;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestSocialLoginPortConfig;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
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
class UserQueryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Test
    @DisplayName("닉네임 중복 확인 통합 테스트 - 사용 가능한 닉네임")
    void checkUserName_Available_IntegrationTest() throws Exception {
        // Given
        String availableUserName = "사용가능한닉네임";

        // When & Then
        mockMvc.perform(get("/api/user/username/check")
                        .param("userName", availableUserName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("닉네임 중복 확인 통합 테스트 - 중복된 닉네임")
    void checkUserName_Duplicate_IntegrationTest() throws Exception {
        // Given - 기존 사용자 생성 및 저장
        User existingUser = TestUsers.createUnique();
        userRepository.save(existingUser);

        // When & Then
        mockMvc.perform(get("/api/user/username/check")
                        .param("userName", existingUser.getUserName()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("사용자 설정 조회 통합 테스트 - 성공")
    void getSetting_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        var userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/setting")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageNotification").value(true))
                .andExpect(jsonPath("$.commentNotification").value(true))
                .andExpect(jsonPath("$.postFeaturedNotification").value(true));
    }

    @Test
    @DisplayName("사용자 작성 게시글 목록 조회 통합 테스트 - 성공")
    void getUserPosts_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        var userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/posts")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("사용자 추천한 게시글 목록 조회 통합 테스트 - 성공")
    void getUserLikedPosts_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        var userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/likeposts")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회 통합 테스트 - 성공")
    void getUserComments_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        var userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/comments")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("사용자 추천한 댓글 목록 조회 통합 테스트 - 성공")
    void getUserLikedComments_IntegrationTest_Success() throws Exception {
        // Given - 테스트 사용자 생성 및 저장
        User testUser = TestUsers.createUnique();
        userRepository.save(testUser);
        
        var userDetails = createCustomUserDetails(testUser);

        // When & Then
        mockMvc.perform(get("/api/user/likecomments")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }




    @Test
    @DisplayName("카카오 친구 목록 조회 - 정상 케이스 (Mock 환경)")
    void getKakaoFriendList_Success() throws Exception {
        // Given - 테스트용 사용자 생성 및 저장
        User user = TestUsers.createUnique();
        User savedUser = userRepository.save(user);
        
        // 테스트용 토큰 생성 및 저장
        Token token = Token.createToken("test-access-TemporaryToken", "test-refresh-TemporaryToken", savedUser);
        Token savedToken = tokenRepository.save(token);
        
        CustomUserDetails userDetails = AuthTestFixtures.createCustomUserDetails(savedUser, savedToken.getId(), null);

        // When & Then
        mockMvc.perform(get("/api/user/friendlist")
                        .param("offset", "0")
                        .param("limit", "10")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk());
                // Note: 실제 카카오 API 호출은 Mock으로 처리되므로 응답 구조 검증은 제한적
    }





}
package jaeik.growfarm.controller;


import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class AuthControllerTest {

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthController authController;

    private String savedRefreshToken;


    public AuthControllerTest(TokenRepository tokenRepository,
                              UserRepository userRepository,
                              UserUtil userUtil,
                              JwtTokenProvider jwtTokenProvider,
                              AuthController authController) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authController = authController;
    }

    @BeforeEach
    void setUp() {
//        Token token = tokenRepository.save(Token.builder()
//                .kakaoAccessToken("kakaoAccessToken")
//                .kakaoRefreshToken("kakaoRefreshToken")
//                .build());
//
//        Users user = userRepository.save(Users.builder()
//                .token(token)
//                .farmName("farmName")
//                .kakaoId(12345L)
//                .kakaoNickname("kakaoNickname")
//                .thumbnailImage("thumbnailImage")
//                .role(UserRole.USER)
//                .modifiedAt(LocalDateTime.now())
//                .createdAt(LocalDateTime.now())
//                .build());
//
//        UserDTO userDTO = userUtil.UserToDTO(user);
//
//        String accessToken = jwtTokenProvider.generateAccessToken(userDTO);
//        String refreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
//        this.savedRefreshToken = refreshToken;
//        token.updateJwtRefreshToken(refreshToken);
//
//        Users user1 = userRepository.findByKakaoId(12345L);
//        UserDTO userDTO1 = userUtil.UserToDTO(user1);
//
//        CustomUserDetails customUserDetails = new CustomUserDetails(userDTO1);
//        Authentication authentication = new UsernamePasswordAuthenticationToken(
//                customUserDetails,
//                null,
//                customUserDetails.getAuthorities());
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("사용자 정보 가져오기")
    void getUserInfo() {
//        // Given
//
//        // When
//        //ResponseEntity<?> responseEntity = authController.getCurrentUser();
//
//        // Then
//        HttpStatusCode code = responseEntity.getStatusCode();
//        assert code.is2xxSuccessful() : "사용자 정보 가져오기 실패";
//        assert responseEntity.getBody() instanceof UserDTO : "사용자 정보 DTO가 아닙니다.";
//        UserDTO userDTO = (UserDTO) responseEntity.getBody();
//        assert userDTO.getFarmName().equals("farmName") : "사용자 farmName이 다릅니다.";
//        assert userDTO.getKakaoNickname().equals("kakaoNickname") : "사용자 kakaoNickname이 다릅니다.";
//        assert userDTO.getThumbnailImage().equals("thumbnailImage") : "사용자 thumbnailImage가 다릅니다.";
//        assert userDTO.getKakaoId().equals(12345L) : "사용자 kakaoId가 다릅니다.";
//        assert userDTO.getRole().equals(UserRole.USER) : "사용자 role이 다릅니다.";
//        Token token = tokenRepository.findById(userDTO.getUserId()).orElseThrow();
//        assert token.getKakaoAccessToken().equals("kakaoAccessToken") : "사용자 kakaoAccessToken이 다릅니다.";
//        assert token.getKakaoRefreshToken().equals("kakaoRefreshToken") : "사용자 kakaoRefreshToken이 다릅니다.";
//        assert token.getJwtRefreshToken().equals(savedRefreshToken) : "사용자 jwtRefreshToken이 다릅니다.";
    }
}

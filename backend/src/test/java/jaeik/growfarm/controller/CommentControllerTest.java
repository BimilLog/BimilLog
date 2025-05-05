package jaeik.growfarm.controller;

import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class CommentControllerTest {

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final JwtTokenProvider jwtTokenProvider;

    public CommentControllerTest(TokenRepository tokenRepository,
                              UserRepository userRepository,
                              UserUtil userUtil,
                              JwtTokenProvider jwtTokenProvider) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.userUtil = userUtil;
        this.jwtTokenProvider = jwtTokenProvider;
    }

//    @BeforeEach
//    void setUp() {
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
//    }
}

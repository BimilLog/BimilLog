package jaeik.growfarm.controller;

import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.global.jwt.JwtTokenProvider;
import jaeik.growfarm.repository.user.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.UserUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDateTime;

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

    @BeforeEach
    void setUp() {
        Token token = tokenRepository.save(Token.builder()
                .kakaoAccessToken("kakaoAccessToken")
                .kakaoRefreshToken("kakaoRefreshToken")
                .build());

        Users user = userRepository.save(Users.builder()
                .token(token)
                .farmName("farmName")
                .kakaoId(12345L)
                .kakaoNickname("kakaoNickname")
                .thumbnailImage("thumbnailImage")
                .role(UserRole.USER)
                .modifiedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        UserDTO userDTO = userUtil.UserToDTO(user);

        String accessToken = jwtTokenProvider.generateAccessToken(userDTO);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDTO);
        token.updateJwtRefreshToken(refreshToken);

        Users user1 = userRepository.findByKakaoId(12345L);
        UserDTO userDTO1 = userUtil.UserToDTO(user1);

        CustomUserDetails customUserDetails = new CustomUserDetails(userDTO1);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

package jaeik.growfarm.infrastructure.adapter.auth.in.web;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.auth.in.web.dto.UserInfoResponseDTO;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.UserDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;

/**
 * <h2>인증 조회 컨트롤러 단위 테스트</h2>
 * <p>AuthQueryController의 비즈니스 로직 단위 테스트</p>
 * <p>@ExtendWith(MockitoExtension.class)를 사용한 순수 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("인증 조회 컨트롤러 단위 테스트")
class AuthQueryControllerUnitTest {

    @InjectMocks
    private AuthQueryController authQueryController;

    @Test
    @DisplayName("현재 사용자 정보 조회 - 성공")
    void getCurrentUser_Success() {
        // Given
        Long userId = 1L;
        CustomUserDetails userDetails = createMockUserDetails(userId);
        
        // When
        ResponseEntity<UserInfoResponseDTO> response = authQueryController.getCurrentUser(userDetails);
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        UserInfoResponseDTO responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.userId()).isEqualTo(userId);
        assertThat(responseBody.settingId()).isEqualTo(101L);
        assertThat(responseBody.socialNickname()).isEqualTo("테스트사용자_소셜닉네임");
        assertThat(responseBody.thumbnailImage()).isEqualTo("http://example.com/image1.jpg");
        assertThat(responseBody.userName()).isEqualTo("테스트사용자");
        assertThat(responseBody.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 - null CustomUserDetails")
    void getCurrentUser_NullUserDetails_ThrowsException() {
        // Given
        CustomUserDetails userDetails = null;

        // When & Then
        assertThatThrownBy(() -> authQueryController.getCurrentUser(userDetails))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NULL_SECURITY_CONTEXT.getMessage());
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 - 관리자 사용자")
    void getCurrentUser_AdminUser_Success() {
        // Given
        Long adminUserId = 999L;
        CustomUserDetails adminUserDetails = createMockAdminUserDetails(adminUserId);
        
        // When
        ResponseEntity<UserInfoResponseDTO> response = authQueryController.getCurrentUser(adminUserDetails);
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        UserInfoResponseDTO responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.userId()).isEqualTo(adminUserId);
        assertThat(responseBody.role()).isEqualTo(UserRole.ADMIN);
        assertThat(responseBody.userName()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("서버 헬스체크 - 성공")
    void healthCheck_Success() {
        // When
        ResponseEntity<String> response = authQueryController.healthCheck();
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("OK");
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 - 다양한 소셜 제공자")
    void getCurrentUser_VariousSocialProviders_Success() {
        // Given - NAVER 사용자
        CustomUserDetails naverUser = createMockUserDetails(2L, SocialProvider.NAVER, "네이버사용자");

        // When
        ResponseEntity<UserInfoResponseDTO> response = authQueryController.getCurrentUser(naverUser);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        UserInfoResponseDTO responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.userId()).isEqualTo(2L);
        assertThat(responseBody.userName()).isEqualTo("네이버사용자");
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 - 빈 프로필 이미지")
    void getCurrentUser_EmptyThumbnailImage_Success() {
        // Given
        CustomUserDetails userWithoutImage = createMockUserDetailsWithoutImage(3L);

        // When
        ResponseEntity<UserInfoResponseDTO> response = authQueryController.getCurrentUser(userWithoutImage);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        UserInfoResponseDTO responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.userId()).isEqualTo(3L);
        assertThat(responseBody.thumbnailImage()).isEmpty();
        assertThat(responseBody.userName()).isEqualTo("이미지없는사용자");
    }

    @Test
    @DisplayName("UserInfoResponseDTO 변환 검증")
    void userInfoResponseDTO_Conversion_Success() {
        // Given
        CustomUserDetails userDetails = createMockUserDetails(123L);
        UserDTO userDTO = userDetails.getUserDTO();

        // When
        UserInfoResponseDTO responseDTO = UserInfoResponseDTO.from(userDTO);

        // Then
        assertThat(responseDTO.userId()).isEqualTo(userDTO.getUserId());
        assertThat(responseDTO.settingId()).isEqualTo(userDTO.getSettingId());
        assertThat(responseDTO.socialNickname()).isEqualTo(userDTO.getSocialNickname());
        assertThat(responseDTO.thumbnailImage()).isEqualTo(userDTO.getThumbnailImage());
        assertThat(responseDTO.userName()).isEqualTo(userDTO.getUserName());
        assertThat(responseDTO.role()).isEqualTo(userDTO.getRole());
    }

    @Test
    @DisplayName("헬스체크 응답 검증")
    void healthCheck_ResponseVerification() {
        // When
        ResponseEntity<String> response = authQueryController.healthCheck();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody()).isEqualTo("OK");
    }

    /**
     * 테스트용 Mock CustomUserDetails 생성 (일반 사용자)
     */
    private CustomUserDetails createMockUserDetails(Long userId) {
        return createMockUserDetails(userId, SocialProvider.KAKAO, "테스트사용자");
    }

    /**
     * 테스트용 Mock CustomUserDetails 생성 (소셜 제공자별)
     */
    private CustomUserDetails createMockUserDetails(Long userId, SocialProvider provider, String userName) {
        UserDTO userDTO = UserDTO.builder()
                .userId(userId)
                .settingId(100L + userId)
                .socialId("social" + userId)
                .socialNickname(userName + "_소셜닉네임")
                .thumbnailImage("http://example.com/image" + userId + ".jpg")
                .userName(userName)
                .provider(provider)
                .role(UserRole.USER)
                .build();
        
        return new CustomUserDetails(userDTO);
    }

    /**
     * 테스트용 Mock CustomUserDetails 생성 (관리자)
     */
    private CustomUserDetails createMockAdminUserDetails(Long userId) {
        UserDTO userDTO = UserDTO.builder()
                .userId(userId)
                .settingId(999L)
                .socialId("admin_social")
                .socialNickname("관리자소셜닉네임")
                .thumbnailImage("http://example.com/admin.jpg")
                .userName("관리자")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.ADMIN)
                .build();
        
        return new CustomUserDetails(userDTO);
    }

    /**
     * 테스트용 Mock CustomUserDetails 생성 (프로필 이미지 없음)
     */
    private CustomUserDetails createMockUserDetailsWithoutImage(Long userId) {
        UserDTO userDTO = UserDTO.builder()
                .userId(userId)
                .settingId(100L + userId)
                .socialId("social" + userId)
                .socialNickname("이미지없는사용자_소셜닉네임")
                .thumbnailImage("") // 빈 이미지
                .userName("이미지없는사용자")
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .build();
        
        return new CustomUserDetails(userDTO);
    }
}
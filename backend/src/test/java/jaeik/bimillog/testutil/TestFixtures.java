package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.NewUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.PostCreateDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 * <h2>테스트 데이터 Fixtures</h2>
 * <p>테스트에서 자주 사용되는 데이터 생성 유틸리티</p>
 * <p>엔티티, DTO, 인증 객체 등 다양한 테스트 데이터 생성 메서드 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>엔티티 생성 (Post, Comment, Notification, RollingPaper 등)</li>
 *   <li>DTO 생성 (요청/응답 DTO)</li>
 *   <li>인증 객체 생성 (CustomUserDetails, ExistingUserDetail)</li>
 *   <li>쿠키 및 토큰 생성</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestFixtures {

    // ==================== Auth Test Constants ====================
    public static final String TEST_SOCIAL_ID = "kakao123456";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_USERNAME = "testUser";
    public static final String TEST_SOCIAL_NICKNAME = "테스트유저";
    public static final String TEST_PROFILE_IMAGE = "http://example.com/profile.jpg";
    public static final String TEST_ACCESS_TOKEN = "access-test-token";
    public static final String TEST_REFRESH_TOKEN = "refresh-test-token";
    public static final String TEST_AUTH_CODE = "auth-code-123";
    public static final String TEST_FCM_TOKEN = "fcm-token-123";
    public static final SocialProvider TEST_PROVIDER = SocialProvider.KAKAO;

    // ==================== Entity Creation ====================



    // ==================== DTO Creation ====================

    /**
     * 게시글 작성 요청 DTO 생성
     * @param title 제목
     * @param content 내용
     * @return PostCreateDTO
     */
    public static PostCreateDTO createPostRequest(String title, String content) {
        return PostCreateDTO.builder()
                .title(title)
                .content(content)
                .password("1234")
                .build();
    }



    /**
     * 게시글 수정 요청 DTO 생성
     * @param title 제목
     * @param content 내용
     * @return PostUpdateDTO
     */
    public static jaeik.bimillog.infrastructure.adapter.in.post.dto.PostUpdateDTO createPostUpdateDTO(String title, String content) {
        return jaeik.bimillog.infrastructure.adapter.in.post.dto.PostUpdateDTO.builder()
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 기본 게시글 수정 요청 DTO 생성
     * @return PostUpdateDTO
     */
    public static jaeik.bimillog.infrastructure.adapter.in.post.dto.PostUpdateDTO createPostUpdateDTO() {
        return createPostUpdateDTO("수정된 제목", "수정된 내용입니다. 10자 이상으로 작성합니다.");
    }

    /**
     * 롤링페이퍼 메시지 요청 DTO 생성
     * @param content 메시지 내용
     * @param positionX X 위치
     * @param positionY Y 위치
     * @return MessageDTO
     */
    public static MessageDTO createPaperMessageRequest(String content, int positionX, int positionY) {
        MessageDTO dto = new MessageDTO();
        dto.setDecoType(jaeik.bimillog.domain.paper.entity.DecoType.POTATO);
        dto.setContent(content);
        dto.setAnonymity("테스트사용자");
        dto.setX(positionX);
        dto.setY(positionY);
        return dto;
    }



    // ==================== Authentication Objects ====================

    /**
     * CustomUserDetails 생성
     * @param user 사용자 엔티티
     * @return CustomUserDetails
     */
    public static CustomUserDetails createCustomUserDetails(User user) {
        ExistingUserDetail userDetail = createExistingUserDetail(user);
        return new CustomUserDetails(userDetail);
    }

    /**
     * CustomUserDetails 생성 (토큰 ID 지정 가능)
     * @param user 사용자 엔티티
     * @param tokenId 토큰 ID (null 가능)
     * @param fcmTokenId FCM 토큰 ID (null 가능)
     * @return CustomUserDetails
     */
    public static CustomUserDetails createCustomUserDetails(User user, Long tokenId, Long fcmTokenId) {
        ExistingUserDetail userDetail = createExistingUserDetail(user, tokenId, fcmTokenId);
        return new CustomUserDetails(userDetail);
    }

    /**
     * ExistingUserDetail 생성 (기본 메서드)
     * @param user 사용자 엔티티
     * @return ExistingUserDetail
     */
    public static ExistingUserDetail createExistingUserDetail(User user) {
        return createExistingUserDetail(user, null, null);
    }

    /**
     * ExistingUserDetail 생성 (토큰 ID 지정 가능)
     * @param user 사용자 엔티티
     * @param tokenId 토큰 ID (null 가능)
     * @param fcmTokenId FCM 토큰 ID (null 가능)
     * @return ExistingUserDetail
     */
    public static ExistingUserDetail createExistingUserDetail(User user, Long tokenId, Long fcmTokenId) {
        Long settingId = null;
        if (user.getSetting() != null && user.getSetting().getId() != null) {
            settingId = user.getSetting().getId();
        } else {
            settingId = 1L; // 기본값
        }

        return ExistingUserDetail.builder()
                .userId(user.getId() != null ? user.getId() : 1L)
                .settingId(settingId)
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .tokenId(tokenId)
                .fcmTokenId(fcmTokenId)
                .build();
    }


    // ==================== Cookie & Token Creation ====================

    /**
     * 임시 UUID 쿠키 생성
     * @param uuid UUID 값
     * @return 임시 쿠키
     */
    public static ResponseCookie createTempCookie(String uuid) {
        return ResponseCookie.from("temp", uuid)
            .path("/")
            .maxAge(60 * 10) // 10분
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .build();
    }

    // ==================== Utility Methods ====================


    /**
     * 리플렉션을 통한 private 필드 값 설정 (테스트 전용)
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

}

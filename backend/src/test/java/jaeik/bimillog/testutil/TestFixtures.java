package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.auth.dto.SocialLoginRequestDTO;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentReqDTO;
import jaeik.bimillog.infrastructure.adapter.in.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.PostCreateDTO;
import jaeik.bimillog.infrastructure.adapter.in.user.dto.SignUpRequestDTO;
import jaeik.bimillog.infrastructure.adapter.in.user.dto.UserNameDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // ==================== Entity Creation ====================

    /**
     * 테스트용 게시글 생성
     * @param author 작성자
     * @param title 제목
     * @param content 내용
     * @return Post 엔티티
     */
    public static Post createPost(User author, String title, String content) {
        return Post.createPost(author, title, content, 1234);
    }

    /**
     * 기본 테스트 게시글 생성 (TestUsers.USER1 사용)
     * @return Post 엔티티
     */
    public static Post createDefaultPost() {
        return createPost(TestUsers.USER1, "테스트 게시글", "테스트 게시글 내용입니다.");
    }

    /**
     * 특정 사용자로 기본 게시글 생성
     * @param author 작성자
     * @return Post 엔티티
     */
    public static Post createPostWithUser(User author) {
        return createPost(author, "테스트 게시글", "테스트 게시글 내용입니다.");
    }

    /**
     * 세부 정보를 지정한 게시글 생성
     * @param author 작성자
     * @param title 제목
     * @param content 내용
     * @param password 비밀번호
     * @return Post 엔티티
     */
    public static Post createPostWithDetails(User author, String title, String content, int password) {
        return Post.createPost(author, title, content, password);
    }

    /**
     * ID가 포함된 게시글 생성
     * @param id 게시글 ID
     * @param author 작성자
     * @param title 제목
     * @param content 내용
     * @return Post 엔티티
     */
    public static Post createPostWithId(Long id, User author, String title, String content) {
        Post post = createPost(author, title, content);
        // 리플렉션을 통한 ID 설정 (테스트 전용)
        setFieldValue(post, "id", id);
        return post;
    }



    /**
     * 테스트용 알림 생성
     * @param receiver 수신자
     * @param type 알림 타입
     * @param relatedId 관련 ID
     * @param message 알림 메시지
     * @return Notification 엔티티
     */
    public static Notification createNotification(User receiver, NotificationType type, Long relatedId, String message) {
        return Notification.create(
                receiver,
                type,
                message,
                relatedId != null ? "/post/" + relatedId : null
        );
    }

    /**
     * 테스트용 롤링페이퍼 메시지 생성
     * @param receiver 수신자
     * @param content 메시지 내용
     * @param color 색상
     * @param font 폰트
     * @param positionX X 위치
     * @param positionY Y 위치
     * @return Message 엔티티
     */
    public static Message createRollingPaper(User receiver, String content,
                                                 String color, String font,
                                                 int positionX, int positionY) {
        // Message uses DecoType enum instead of color/font strings
        // Using POTATO as default for testing
        return Message.createMessage(
                receiver,
                jaeik.bimillog.domain.paper.entity.DecoType.POTATO,
                "테스트사용자",
                content,
                positionX,
                positionY
        );
    }

    /**
     * 테스트용 게시글 좋아요 생성
     * @param post 게시글
     * @param user 사용자
     * @return PostLike 엔티티
     */
    public static PostLike createPostLike(Post post, User user) {
        return PostLike.builder()
                .post(post)
                .user(user)
                .build();
    }

    /**
     * 테스트용 PostSearchResult 생성
     * @param id 게시글 ID
     * @param title 제목
     * @return PostSearchResult
     */
    public static jaeik.bimillog.domain.post.entity.PostSearchResult createPostSearchResult(Long id, String title) {
        return jaeik.bimillog.domain.post.entity.PostSearchResult.builder()
                .id(id)
                .title(title)
                .build();
    }

    /**
     * 테스트용 PostDetail 생성 (기본값 사용)
     * @param id 게시글 ID
     * @param title 제목
     * @param content 내용
     * @return PostDetail
     */
    public static jaeik.bimillog.domain.post.entity.PostDetail createPostDetail(Long id, String title, String content) {
        return createPostDetail(id, title, content, 1L, false);
    }

    /**
     * 테스트용 PostDetail 생성 (상세 설정)
     * @param id 게시글 ID
     * @param title 제목
     * @param content 내용
     * @param userId 사용자 ID
     * @param isLiked 좋아요 여부
     * @return PostDetail
     */
    public static jaeik.bimillog.domain.post.entity.PostDetail createPostDetail(Long id, String title, String content,
                                                                                  Long userId, boolean isLiked) {
        return jaeik.bimillog.domain.post.entity.PostDetail.builder()
                .id(id)
                .title(title)
                .content(content)
                .viewCount(10)
                .likeCount(5)
                .postCacheFlag(null)
                .createdAt(java.time.Instant.now())
                .userId(userId)
                .userName("testUser")
                .commentCount(3)
                .isNotice(false)
                .isLiked(isLiked)
                .build();
    }

    /**
     * 테스트용 PostDetail Mock 생성 (JOIN 쿼리 테스트용)
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return PostDetail
     */
    public static jaeik.bimillog.domain.post.entity.PostDetail createMockPostDetail(Long postId, Long userId) {
        return createPostDetail(postId, "Test Title", "Test Content", 1L, userId != null);
    }

    // ==================== DTO Creation ====================

    /**
     * 회원가입 요청 DTO 생성
     * @param uuid UUID
     * @param userName 사용자명
     * @return SignUpRequestDTO
     */
    public static SignUpRequestDTO createSignUpRequest(String uuid, String userName) {
        return new SignUpRequestDTO(uuid, userName);
    }

    /**
     * 소셜 로그인 요청 DTO 생성
     * @param provider 소셜 제공자
     * @param code 인증 코드
     * @param fcmToken FCM 토큰
     * @return SocialLoginRequestDTO
     */
    public static SocialLoginRequestDTO createSocialLoginRequest(String provider, String code, String fcmToken) {
        return new SocialLoginRequestDTO(provider, code, fcmToken);
    }

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
     * 기본 게시글 작성 요청 DTO 생성
     * @return PostCreateDTO
     */
    public static PostCreateDTO createPostCreateDTO() {
        return createPostRequest("테스트 게시글 제목", "테스트 게시글 내용입니다.");
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
     * @param color 색상
     * @param font 폰트
     * @param positionX X 위치
     * @param positionY Y 위치
     * @return MessageDTO
     */
    public static MessageDTO createPaperMessageRequest(String content, String color,
                                                                  String font, int positionX, int positionY) {
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
     * DB에 저장된 사용자로부터 CustomUserDetails 생성
     * @param savedUser DB에 저장된 사용자 엔티티 (ID 필수)
     * @return CustomUserDetails
     */
    public static CustomUserDetails createCustomUserDetailsFromSavedUser(User savedUser) {
        ExistingUserDetail userDetail = ExistingUserDetail.builder()
                .userId(savedUser.getId())
                .settingId(savedUser.getSetting() != null ? savedUser.getSetting().getId() : null)
                .socialId(savedUser.getSocialId())
                .socialNickname(savedUser.getSocialNickname())
                .thumbnailImage(savedUser.getThumbnailImage())
                .userName(savedUser.getUserName())
                .provider(savedUser.getProvider())
                .role(savedUser.getRole())
                .tokenId(1L) // 테스트용 토큰 ID
                .fcmTokenId(null)
                .build();
        return new CustomUserDetails(userDetail);
    }

    /**
     * ExistingUserDetail 생성
     * @param user 사용자 엔티티
     * @return ExistingUserDetail
     */
    public static ExistingUserDetail createExistingUserDetail(User user) {
        return ExistingUserDetail.builder()
                .userId(user.getId())
                .settingId(user.getSetting() != null ? user.getSetting().getId() : null)
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .tokenId(null)
                .fcmTokenId(null)
                .build();
    }

    /**
     * SocialUserProfile 생성
     * @param socialId 소셜 ID
     * @param email 이메일
     * @param provider 소셜 제공자
     * @param nickname 닉네임
     * @param profileImage 프로필 이미지
     * @return SocialUserProfile
     */
    public static SocialUserProfile createSocialUserProfile(String socialId, String email,
                                                           SocialProvider provider, String nickname,
                                                           String profileImage) {
        Token token = Token.createTemporaryToken("access-TemporaryToken", "refresh-TemporaryToken");
        return new SocialUserProfile(socialId, email, provider, nickname, profileImage, token);
    }

    // ==================== Cookie & Token Creation ====================

    /**
     * 인증 쿠키 생성
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @return ResponseCookie
     */
    public static ResponseCookie createAuthCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
    }

    /**
     * 임시 UUID 쿠키 생성
     * @param uuid UUID 값
     * @return ResponseCookie
     */
    public static ResponseCookie createTempCookie(String uuid) {
        return ResponseCookie.from("temp", uuid)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .sameSite("Lax")
                .build();
    }
    
    /**
     * 로그아웃 쿠키 생성 (Auth 테스트용)
     * @return 로그아웃용 쿠키 리스트 (값이 비어있고 만료시간 0)
     */
    public static List<ResponseCookie> createLogoutCookies() {
        return List.of(
            ResponseCookie.from("jwt_access_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build(),
            ResponseCookie.from("jwt_refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build()
        );
    }
    
    /**
     * JWT 인증 쿠키 생성 (Auth 테스트용)
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return JWT 쿠키 리스트
     */
    public static List<ResponseCookie> createJwtCookies(String accessToken, String refreshToken) {
        return List.of(
            ResponseCookie.from("jwt_access_token", accessToken)
                .path("/")
                .maxAge(60 * 60) // 1시간
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build(),
            ResponseCookie.from("jwt_refresh_token", refreshToken)
                .path("/")
                .maxAge(60 * 60 * 24 * 7) // 7일
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build()
        );
    }

    /**
     * LoginResult.ExistingUser 생성
     * @param cookies 인증 쿠키들
     * @return LoginResult.ExistingUser
     */
    public static LoginResult.ExistingUser createExistingUserLoginResult(List<ResponseCookie> cookies) {
        return new LoginResult.ExistingUser(cookies);
    }

    /**
     * LoginResult.NewUser 생성
     * @param uuid UUID
     * @param tempCookie 임시 쿠키
     * @return LoginResult.NewUser
     */
    public static LoginResult.NewUser createNewUserLoginResult(String uuid, ResponseCookie tempCookie) {
        return new LoginResult.NewUser(uuid, tempCookie);
    }
    

    
    /**
     * 여러 토큰 생성 (Auth 테스트용)
     * @param count 생성할 토큰 개수
     * @return 토큰 리스트
     */
    public static List<Token> createMultipleTokens(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> Token.createTemporaryToken(
                "access-token-" + i,
                "refresh-token-" + i
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    // ==================== Utility Methods ====================

    /**
     * 랜덤 UUID 생성
     * @return UUID 문자열
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 타임스탬프 기반 고유 ID 생성
     * @param prefix 접두사
     * @return 고유 ID
     */
    public static String generateUniqueId(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    /**
     * 리플렉션을 통한 private 필드 값 설정 (테스트 전용)
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }

    // Private constructor to prevent instantiation
    private TestFixtures() {}
}
package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.PostCreateDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;

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

    // ==================== Entity Creation ====================

    /**
     * 테스트용 게시글 생성 (기본 메서드)
     * @param author 작성자
     * @param title 제목
     * @param content 내용
     * @return Post 엔티티
     */
    public static Post createPost(User author, String title, String content) {
        return Post.createPost(author, title, content, 1234);
    }

    /**
     * 테스트용 게시글 생성 (모든 매개변수 지정)
     * @param author 작성자  
     * @param title 제목
     * @param content 내용
     * @param password 비밀번호
     * @return Post 엔티티
     */
    public static Post createPost(User author, String title, String content, int password) {
        return Post.createPost(author, title, content, password);
    }


    /**
     * ID가 포함된 엔티티 생성 (테스트 전용)
     * @param <T> 엔티티 타입
     * @param id 엔티티 ID
     * @param entity 기존 엔티티
     * @return ID가 설정된 엔티티
     */
    public static <T> T withId(Long id, T entity) {
        setFieldValue(entity, "id", id);
        return entity;
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
    public static PostDetail createPostDetail(Long id, String title, String content) {
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
    public static PostDetail createPostDetail(Long id, String title, String content, Long userId, boolean isLiked) {
        return PostDetail.builder()
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
    public static PostDetail createMockPostDetail(Long postId, Long userId) {
        return createPostDetail(postId, "Test Title", "Test Content", 1L, userId != null);
    }

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

    // ==================== Lazy Initialization Support ====================

    /**
     * <h2>테스트 데이터 Lazy Holder</h2>
     * <p>자주 사용되는 테스트 데이터를 lazy 초기화로 제공</p>
     * <p>실제 사용될 때만 생성하여 테스트 성능 향상</p>
     */
    public static class LazyTestData {
        // User 관련 lazy suppliers
        private static volatile User testUser;

        /**
         * 테스트 사용자 lazy 획득 (USER1)
         */
        public static User getTestUser() {
            if (testUser == null) {
                synchronized (LazyTestData.class) {
                    if (testUser == null) {
                        testUser = TestUsers.USER1;
                    }
                }
            }
            return testUser;
        }
    }

    // ==================== Utility Methods ====================


    /**
     * 리플렉션을 통한 private 필드 값 설정 (테스트 전용)
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            // 먼저 현재 클래스에서 필드를 찾기
            java.lang.reflect.Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + target.getClass());
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }

    /**
     * 클래스 계층 구조를 따라 필드를 찾기 (부모 클래스 포함)
     */
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 부모 클래스에서 계속 찾기
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
package jaeik.bimillog.testutil;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.Token;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

/**
 * <h2>Redis 테스트 헬퍼</h2>
 * <p>Redis 관련 테스트에서 반복되는 보일러플레이트 코드를 제거하기 위한 유틸리티</p>
 * <p>Mock 설정, 테스트 데이터 생성, Redis 초기화 등의 공통 기능 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class RedisTestHelper {

    /**
     * RedisTemplate Mock 설정 헬퍼
     * ValueOperations와 ZSetOperations Mock을 함께 설정
     * 
     * @param redisTemplate Mock RedisTemplate
     * @param valueOperations Mock ValueOperations
     * @param zSetOperations Mock ZSetOperations
     */
    public static void setupRedisTemplateMocks(RedisTemplate<String, Object> redisTemplate,
                                                ValueOperations<String, Object> valueOperations,
                                                ZSetOperations<String, Object> zSetOperations) {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
    }

    /**
     * Redis 데이터 초기화 (TestContainers 통합 테스트용)
     * 
     * @param redisTemplate 실제 RedisTemplate
     */
    public static void flushRedis(RedisTemplate<String, Object> redisTemplate) {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            if (connection != null) {
                connection.serverCommands().flushAll();
            }
        } catch (Exception e) {
            System.err.println("Redis flush warning: " + e.getMessage());
        }
    }

    /**
     * JPAUpdateClause Mock 설정 헬퍼
     *
     * @param jpaQueryFactory Mock JPAQueryFactory
     * @param expectedUpdateCount 예상되는 업데이트 건수
     * @return 설정된 JPAUpdateClause Mock
     */
    public static JPAUpdateClause setupJpaUpdateClauseMock(JPAQueryFactory jpaQueryFactory, long expectedUpdateCount) {
        JPAUpdateClause updateClause = mock(JPAUpdateClause.class, RETURNS_SELF);
        given(jpaQueryFactory.update(any())).willReturn(updateClause);
        given(updateClause.where(any())).willReturn(updateClause);
        given(updateClause.execute()).willReturn(expectedUpdateCount);
        return updateClause;
    }

    /**
     * 테스트용 PostDetail 생성 빌더
     */
    public static class PostDetailBuilder {
        private Long id = 1L;
        private String title = "테스트 게시글";
        private String content = "테스트 내용";
        private final PostCacheFlag postCacheFlag = PostCacheFlag.REALTIME;
        private final Instant createdAt = Instant.now();
        private Long userId = 1L;
        private String userName = "testUser";
        private Boolean isNotice = false;

        public PostDetailBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PostDetailBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostDetailBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostDetailBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PostDetailBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public PostDetailBuilder isNotice(Boolean isNotice) {
            this.isNotice = isNotice;
            return this;
        }

        public PostDetail build() {
            Integer viewCount = 100;
            Integer likeCount = 50;
            Integer commentCount = 10;
            boolean isLiked = false;
            return PostDetail.builder()
                .id(id)
                .title(title)
                .content(content)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .postCacheFlag(postCacheFlag)
                .createdAt(createdAt)
                .userId(userId)
                .userName(userName)
                .commentCount(commentCount)
                .isNotice(isNotice)
                .isLiked(isLiked)
                .build();
        }
    }

    /**
     * PostDetail 빌더 시작
     * @return PostDetailBuilder
     */
    public static PostDetailBuilder postDetail() {
        return new PostDetailBuilder();
    }

    /**
     * 기본 PostDetail 생성
     * @return 기본값이 설정된 PostDetail
     */
    public static PostDetail defaultPostDetail() {
        return postDetail().build();
    }

    /**
     * 테스트용 SocialUserProfile 생성
     * @param socialId 소셜 ID
     * @param email 이메일
     * @return SocialUserProfile
     */
    public static SocialUserProfile createTestSocialUserProfile(String socialId, String email) {
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
        return new SocialUserProfile(
            socialId,
            email,
            SocialProvider.KAKAO,
            "testUser",
            "https://example.com/profile.jpg",
            token
        );
    }

    /**
     * 기본 SocialUserProfile 생성
     * @return 기본값이 설정된 SocialUserProfile
     */
    public static SocialUserProfile defaultSocialUserProfile() {
        return createTestSocialUserProfile("123456789", "test@example.com");
    }


    /**
     * Redis 키 생성 헬퍼
     */
    public static class RedisKeys {
        public static String postDetail(Long postId) {
            return "cache:post:" + postId;
        }

        public static String postList(PostCacheFlag cacheType) {
            return "cache:posts:" + cacheType.name().toLowerCase();
        }

        public static String tempUserData(String uuid) {
            return "temp:user:" + uuid;
        }

    }
}
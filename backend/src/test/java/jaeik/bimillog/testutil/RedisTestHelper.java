package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

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
     * 테스트용 SocialMemberProfile 생성
     * @param socialId 소셜 ID
     * @param email 이메일
     * @return SocialMemberProfile
     */
    public static SocialMemberProfile createTestSocialMemberProfile(String socialId, String email) {
        return new SocialMemberProfile(
                socialId,
                email,
                SocialProvider.KAKAO,
                "testMember",
                "https://example.com/profile.jpg",
                "access-token",
                "refresh-token",
                null
        );
    }

    /**
     * 기본 SocialMemberProfile 생성
     * @return 기본값이 설정된 SocialMemberProfile
     */
    public static SocialMemberProfile defaultSocialMemberProfile() {
        return createTestSocialMemberProfile("123456789", "test@example.com");
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

        public static String tempMemberData(String uuid) {
            return "temp:member:" + uuid;
        }

    }
}

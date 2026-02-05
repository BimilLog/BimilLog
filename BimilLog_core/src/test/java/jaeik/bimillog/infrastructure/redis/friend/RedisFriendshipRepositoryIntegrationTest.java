package jaeik.bimillog.infrastructure.redis.friend;

import jaeik.bimillog.testutil.RedisTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jaeik.bimillog.infrastructure.redis.friend.RedisFriendKeys.FRIEND_SHIP_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("local-integration")
class RedisFriendshipRepositoryIntegrationTest {

    @Autowired
    private RedisFriendshipRepository redisFriendshipRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        RedisTestHelper.flushRedis(redisTemplate);
    }

    @Test
    @DisplayName("탈퇴 회원 삭제 시 친구 테이블에서만 타겟 정리")
    void shouldRemoveWithdrawnMemberFromFriendSets() {
        Long withdrawMemberId = 1L;
        Long friendA = 2L;
        Long friendB = 3L;
        Long unrelatedA = 4L;
        Long unrelatedB = 5L;

        redisFriendshipRepository.addFriend(withdrawMemberId, friendA);
        redisFriendshipRepository.addFriend(withdrawMemberId, friendB);
        redisFriendshipRepository.addFriend(unrelatedA, unrelatedB);

        redisFriendshipRepository.deleteWithdrawFriendTargeted(withdrawMemberId);

        assertThat(redisTemplate.hasKey(FRIEND_SHIP_PREFIX + withdrawMemberId)).isFalse();
        Set<Object> friendASet = redisTemplate.opsForSet().members(FRIEND_SHIP_PREFIX + friendA);
        Set<Object> friendBSet = redisTemplate.opsForSet().members(FRIEND_SHIP_PREFIX + friendB);
        assertThat(friendASet).noneMatch(value -> value.toString().equals(withdrawMemberId.toString()));
        assertThat(friendBSet).noneMatch(value -> value.toString().equals(withdrawMemberId.toString()));

        Set<Object> unrelatedASet = redisTemplate.opsForSet().members(FRIEND_SHIP_PREFIX + unrelatedA);
        Set<Object> unrelatedBSet = redisTemplate.opsForSet().members(FRIEND_SHIP_PREFIX + unrelatedB);
        assertThat(unrelatedASet).anyMatch(value -> value.toString().equals(unrelatedB.toString()));
        assertThat(unrelatedBSet).anyMatch(value -> value.toString().equals(unrelatedA.toString()));
    }

    @Test
    @DisplayName("배치 조회 - 대량 요청도 모두 반환")
    void shouldReturnAllFriendsWhenBatchExceedsLimit() {
        int totalMembers = 550;
        Long commonFriend = 999L;
        List<Long> memberIdList = new ArrayList<>();

        for (long i = 1; i <= totalMembers; i++) {
            memberIdList.add(i);
            redisFriendshipRepository.addFriend(i, commonFriend);
        }

        List<Object> results = redisFriendshipRepository.getFriendsBatch(memberIdList, 30);

        assertThat(results).hasSize(totalMembers);
        for (int i = 0; i < memberIdList.size(); i++) {
            assertThat(results.get(i)).isInstanceOf(List.class);
            List<?> friendList = (List<?>) results.get(i);
            assertThat(friendList).anyMatch(f -> f.toString().equals(commonFriend.toString()));
        }
    }
}

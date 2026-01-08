package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.builder.FriendTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("FriendshipRedisRebuildService 단위 테스트")
@Tag("unit")
class FriendshipRedisRebuildServiceTest extends BaseUnitTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private RedisFriendshipRepository redisFriendshipRepository;

    @InjectMocks
    private FriendshipRedisRebuildService friendshipRedisRebuildService;

    @Test
    @DisplayName("DB 친구 관계를 Redis로 전체 재구성한다")
    void shouldRebuildRedisFriendshipCacheFromDb() {
        Member member1 = TestMembers.copyWithId(TestMembers.MEMBER_1, 1L);
        Member member2 = TestMembers.copyWithId(TestMembers.MEMBER_2, 2L);
        Member member3 = TestMembers.copyWithId(TestMembers.MEMBER_3, 3L);

        Friendship friendship1 = FriendTestDataBuilder.createFriendshipWithId(10L, member1, member2);
        Friendship friendship2 = FriendTestDataBuilder.createFriendshipWithId(11L, member2, member3);

        // 첫 페이지: 2개 항목, 총 2개 (마지막 페이지)
        Page<Friendship> page1 = new PageImpl<>(List.of(friendship1, friendship2), PageRequest.of(0, 500), 2);

        given(friendshipRepository.findAll(any(Pageable.class))).willReturn(page1);

        friendshipRedisRebuildService.rebuildRedisFriendshipCache();

        verify(redisFriendshipRepository, times(1)).clearAllFriendshipKeys();
        verify(redisFriendshipRepository, times(1)).addFriend(member1.getId(), member2.getId());
        verify(redisFriendshipRepository, times(1)).addFriend(member2.getId(), member3.getId());
        verify(friendshipRepository, times(1)).findAll(any(Pageable.class));
    }
}

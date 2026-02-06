package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.adapter.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipQueryRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>FriendshipQueryService 단위 테스트</h2>
 * <p>친구 관계 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 */
@DisplayName("FriendshipQueryService 단위 테스트")
@Tag("unit")
class FriendshipQueryServiceTest extends BaseUnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long FRIEND_ID_1 = 2L;
    private static final Long FRIEND_ID_2 = 3L;

    @Mock private FriendshipQueryRepository friendshipQueryRepository;
    @Mock private FriendToMemberAdapter friendToMemberAdapter;

    @InjectMocks
    private FriendshipQueryService friendshipQueryService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    // ==================== getMyFriendList ====================

    /**
     * <h3>친구 목록 조회 시나리오 제공</h3>
     * <p>다양한 페이지네이션 상황을 테스트합니다.</p>
     *
     * @return Pageable, 친구 리스트, 친구 정보 리스트, 총 개수의 조합
     */
    static Stream<Arguments> provideFriendListScenarios() {
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 5);

        return Stream.of(
                // 친구가 존재하는 경우
                Arguments.of(
                        page0,
                        List.of(
                                new Friend(100L, FRIEND_ID_1, java.time.Instant.now()),
                                new Friend(101L, FRIEND_ID_2, java.time.Instant.now())
                        ),
                        List.of(FRIEND_ID_1, FRIEND_ID_2),
                        List.of(
                                new Friend.FriendInfo(FRIEND_ID_1, "테스트회원2", "http://example.com/2.jpg"),
                                new Friend.FriendInfo(FRIEND_ID_2, "테스트회원3", "http://example.com/3.jpg")
                        ),
                        2L
                ),
                // 친구가 없는 경우
                Arguments.of(page0, List.of(), List.of(), List.of(), 0L),
                // 페이지네이션 (2페이지)
                Arguments.of(
                        page1,
                        List.of(new Friend(105L, 6L, java.time.Instant.now())),
                        List.of(6L),
                        List.of(new Friend.FriendInfo(6L, "테스트회원6", "http://example.com/6.jpg")),
                        10L
                )
        );
    }

    @ParameterizedTest(name = "page={0}, totalElements={4}")
    @MethodSource("provideFriendListScenarios")
    @DisplayName("친구 목록 조회 - 다양한 시나리오")
    void shouldGetFriendList(Pageable pageable, List<Friend> friends, List<Long> friendIds,
                              List<Friend.FriendInfo> friendInfos, long total) {
        // Given
        Page<Friend> friendPage = new PageImpl<>(friends, pageable, total);
        given(friendshipQueryRepository.getMyFriendIds(MEMBER_ID, pageable)).willReturn(friendPage);
        given(friendToMemberAdapter.addMyFriendInfo(friendIds)).willReturn(friendInfos);

        // When
        Page<Friend> result = friendshipQueryService.getMyFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(total);
        assertThat(result.getContent()).hasSize(friends.size());
    }

    @Test
    @DisplayName("친구 목록 조회 성공 - FriendInfo 주입 확인")
    void shouldInjectFriendInfo() {
        // Given
        Friend friend = new Friend(100L, FRIEND_ID_1, java.time.Instant.now());
        Page<Friend> friendPage = new PageImpl<>(List.of(friend), pageable, 1);

        Friend.FriendInfo friendInfo = new Friend.FriendInfo(
                FRIEND_ID_1,
                "친구이름",
                "http://example.com/profile.jpg"
        );

        given(friendshipQueryRepository.getMyFriendIds(MEMBER_ID, pageable)).willReturn(friendPage);
        given(friendToMemberAdapter.addMyFriendInfo(List.of(FRIEND_ID_1))).willReturn(List.of(friendInfo));

        // When
        Page<Friend> result = friendshipQueryService.getMyFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        Friend resultFriend = result.getContent().get(0);
        assertThat(resultFriend.memberName()).isNotNull();
        assertThat(resultFriend.memberName()).isEqualTo("친구이름");
        assertThat(resultFriend.thumbnailImage()).isEqualTo("http://example.com/profile.jpg");
    }
}

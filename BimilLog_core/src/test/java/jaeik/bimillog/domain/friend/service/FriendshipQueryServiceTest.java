package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipQueryRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>FriendshipQueryService 단위 테스트</h2>
 * <p>친구 관계 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
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

    @Test
    @DisplayName("친구 목록 조회 성공 - 친구가 존재하는 경우")
    void shouldGetFriendList_WhenFriendsExist() {
        // Given
        Friend friend1 = new Friend(100L, FRIEND_ID_1, java.time.Instant.now());
        Friend friend2 = new Friend(101L, FRIEND_ID_2, java.time.Instant.now());
        Page<Friend> friendPage = new PageImpl<>(List.of(friend1, friend2), pageable, 2);

        Friend.FriendInfo friendInfo1 = new Friend.FriendInfo(FRIEND_ID_1, "테스트회원2", "http://example.com/2.jpg");
        Friend.FriendInfo friendInfo2 = new Friend.FriendInfo(FRIEND_ID_2, "테스트회원3", "http://example.com/3.jpg");

        given(friendshipQueryRepository.getMyFriendIds(MEMBER_ID, pageable)).willReturn(friendPage);
        given(friendToMemberAdapter.addMyFriendInfo(List.of(FRIEND_ID_1, FRIEND_ID_2)))
                .willReturn(List.of(friendInfo1, friendInfo2));

        // When
        Page<Friend> result = friendshipQueryService.getMyFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFriendMemberId()).isEqualTo(FRIEND_ID_1);
        assertThat(result.getContent().get(0).getMemberName()).isNotNull();
        assertThat(result.getContent().get(0).getMemberName()).isEqualTo("테스트회원2");
    }

    @Test
    @DisplayName("친구 목록 조회 성공 - 친구가 없는 경우 (빈 페이지)")
    void shouldReturnEmptyPage_WhenNoFriends() {
        // Given
        Page<Friend> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(friendshipQueryRepository.getMyFriendIds(MEMBER_ID, pageable)).willReturn(emptyPage);
        given(friendToMemberAdapter.addMyFriendInfo(List.of())).willReturn(List.of());

        // When
        Page<Friend> result = friendshipQueryService.getMyFriendList(MEMBER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("친구 목록 조회 성공 - 페이지네이션 확인")
    void shouldSupportPagination() {
        // Given
        Pageable secondPage = PageRequest.of(1, 5);
        Friend friend = new Friend(105L, 6L, java.time.Instant.now());
        Page<Friend> friendPage = new PageImpl<>(List.of(friend), secondPage, 10);

        Friend.FriendInfo friendInfo = new Friend.FriendInfo(6L, "테스트회원6", "http://example.com/6.jpg");

        given(friendshipQueryRepository.getMyFriendIds(MEMBER_ID, secondPage)).willReturn(friendPage);
        given(friendToMemberAdapter.addMyFriendInfo(List.of(6L))).willReturn(List.of(friendInfo));

        // When
        Page<Friend> result = friendshipQueryService.getMyFriendList(MEMBER_ID, secondPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getNumber()).isEqualTo(1);  // 페이지 번호
        assertThat(result.getSize()).isEqualTo(5);    // 페이지 크기
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
        assertThat(resultFriend.getMemberName()).isNotNull();
        assertThat(resultFriend.getMemberName()).isEqualTo("친구이름");
        assertThat(resultFriend.getThumbnailImage()).isEqualTo("http://example.com/profile.jpg");
    }
}

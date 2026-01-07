package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.event.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendshipDeletedEvent;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.FriendTestDataBuilder;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>FriendshipCommandService 단위 테스트</h2>
 * <p>친구 관계 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("FriendshipCommandService 단위 테스트")
@Tag("unit")
class FriendshipCommandServiceTest extends BaseUnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long FRIEND_ID = 2L;
    private static final Long FRIEND_REQUEST_ID = 100L;
    private static final Long FRIENDSHIP_ID = 200L;

    @Mock private FriendToMemberAdapter friendToMemberAdapter;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendshipCommandService friendshipCommandService;

    private Member member;
    private Member friend;
    private Friendship friendship;

    @BeforeEach
    void setUp() {
        member = getTestMember();
        TestFixtures.setFieldValue(member, "id", MEMBER_ID);

        friend = getOtherMember();
        TestFixtures.setFieldValue(friend, "id", FRIEND_ID);

        friendship = FriendTestDataBuilder.createFriendshipWithId(FRIENDSHIP_ID, member, friend);
    }

    // ==================== createFriendship ====================

    @Test
    @DisplayName("친구 관계 생성 성공 - 정상적인 요청 수락")
    void shouldCreateFriendship_WhenValidRequest() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(Optional.of(friend));
        given(friendToMemberAdapter.findById(MEMBER_ID)).willReturn(Optional.of(member));
        doNothing().when(friendToMemberAdapter).checkMemberBlacklist(MEMBER_ID, FRIEND_ID);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(false);
        given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(false);
        given(friendshipRepository.save(any(Friendship.class))).willReturn(friendship);

        // When
        friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID);

        // Then
        verify(friendshipRepository, times(1)).save(any(Friendship.class));
        verify(friendRequestRepository, times(1)).deleteById(FRIEND_REQUEST_ID);
        verify(eventPublisher, times(1)).publishEvent(any(FriendshipCreatedEvent.class));
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 친구가 존재하지 않음")
    void shouldThrowException_WhenFriendNotFound() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(friendRequestRepository, never()).deleteById(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 블랙리스트 관계")
    void shouldThrowException_WhenBlacklisted() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(Optional.of(friend));
        doThrow(new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN))
                .when(friendToMemberAdapter).checkMemberBlacklist(MEMBER_ID, FRIEND_ID);

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 이미 친구 관계 존재 (member -> friend)")
    void shouldThrowException_WhenFriendshipAlreadyExists() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(Optional.of(friend));
        doNothing().when(friendToMemberAdapter).checkMemberBlacklist(MEMBER_ID, FRIEND_ID);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_SHIP_ALREADY_EXIST);

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 이미 친구 관계 존재 (friend -> member)")
    void shouldThrowException_WhenReverseFriendshipExists() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(Optional.of(friend));
        doNothing().when(friendToMemberAdapter).checkMemberBlacklist(MEMBER_ID, FRIEND_ID);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(false);
        given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_SHIP_ALREADY_EXIST);

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 회원이 존재하지 않음")
    void shouldThrowException_WhenMemberNotFound() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(Optional.of(friend));
        doNothing().when(friendToMemberAdapter).checkMemberBlacklist(MEMBER_ID, FRIEND_ID);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(false);
        given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(false);
        given(friendToMemberAdapter.findById(MEMBER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ==================== deleteFriendship ====================

    @Test
    @DisplayName("친구 관계 삭제 성공 - member가 삭제 요청")
    void shouldDeleteFriendship_WhenMemberRequestsDelete() {
        // Given
        given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.of(friendship));

        // When
        friendshipCommandService.deleteFriendship(MEMBER_ID, FRIENDSHIP_ID);

        // Then
        verify(friendshipRepository, times(1)).delete(friendship);
        verify(eventPublisher, times(1)).publishEvent(any(FriendshipDeletedEvent.class));
    }

    @Test
    @DisplayName("친구 관계 삭제 성공 - friend가 삭제 요청")
    void shouldDeleteFriendship_WhenFriendRequestsDelete() {
        // Given
        given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.of(friendship));

        // When
        friendshipCommandService.deleteFriendship(FRIEND_ID, FRIENDSHIP_ID);

        // Then
        verify(friendshipRepository, times(1)).delete(friendship);
        verify(eventPublisher, times(1)).publishEvent(any(FriendshipDeletedEvent.class));
    }

    @Test
    @DisplayName("친구 관계 삭제 실패 - 존재하지 않는 친구 관계")
    void shouldThrowException_WhenFriendshipNotFound() {
        // Given
        given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.deleteFriendship(MEMBER_ID, FRIENDSHIP_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_SHIP_NOT_FOUND);

        verify(friendshipRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 삭제 실패 - 친구 관계에 참여하지 않은 사용자")
    void shouldThrowException_WhenNotParticipant() {
        // Given
        Long outsiderId = 999L;
        given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.of(friendship));

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.deleteFriendship(outsiderId, FRIENDSHIP_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_SHIP_DELETE_FORBIDDEN);

        verify(friendshipRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

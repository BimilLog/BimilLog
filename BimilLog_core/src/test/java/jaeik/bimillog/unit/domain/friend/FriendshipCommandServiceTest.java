package jaeik.bimillog.unit.domain.friend;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.friend.adapter.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.friend.service.FriendshipCommandService;
import jaeik.bimillog.domain.friend.service.FriendshipRedisUpdate;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private FriendshipRedisUpdate friendshipRedisUpdate;

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

        friendship = createFriendship(FRIENDSHIP_ID, member, friend);
    }

    // ==================== 테스트 헬퍼 메서드 ====================

    private Friendship createFriendship(Long id, Member member, Member friend) {
        Friendship friendship = Friendship.createFriendship(member, friend);
        if (id != null) {
            TestFixtures.setFieldValue(friendship, "id", id);
        }
        return friendship;
    }

    // ==================== createFriendship ====================

    @Test
    @DisplayName("친구 관계 생성 성공 - 정상적인 요청 수락")
    void shouldCreateFriendship_WhenValidRequest() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(friend);
        given(friendToMemberAdapter.findById(MEMBER_ID)).willReturn(member);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(false);
        given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(false);
        given(friendshipRepository.save(any(Friendship.class))).willReturn(friendship);

        // When
        friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID);

        // Then
        verify(friendshipRepository, times(1)).save(any(Friendship.class));
        verify(friendRequestRepository, times(1)).deleteById(FRIEND_REQUEST_ID);
        verify(friendshipRedisUpdate, times(1)).addFriendToRedis(MEMBER_ID, FRIEND_ID);
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 친구가 존재하지 않음")
    void shouldThrowException_WhenFriendNotFound() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willThrow(new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(friendRequestRepository, never()).deleteById(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 생성 - 블랙리스트 체크 이벤트 발행")
    void shouldPublishCheckBlacklistEvent_WhenCreatingFriendship() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(friend);
        given(friendToMemberAdapter.findById(MEMBER_ID)).willReturn(member);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(false);
        given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(false);
        given(friendshipRepository.save(any(Friendship.class))).willReturn(friendship);

        // When
        friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID);

        // Then - CheckBlacklistEvent 발행 확인
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        boolean foundEvent = eventCaptor.getAllValues().stream()
                .anyMatch(event -> event instanceof jaeik.bimillog.domain.global.event.CheckBlacklistEvent);
        assertThat(foundEvent).isTrue();
        verify(friendshipRepository, times(1)).save(any(Friendship.class));
    }

    @ParameterizedTest(name = "친구 관계 생성 실패 - 이미 존재 ({0})")
    @MethodSource("provideFriendshipExistsScenarios")
    @DisplayName("이미 친구 관계 존재 예외 - 정방향/역방향 공통")
    void shouldThrowException_WhenFriendshipAlreadyExists(String direction, boolean memberToFriendExists, boolean friendToMemberExists) {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(friend);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(memberToFriendExists);
        if (!memberToFriendExists) {
            given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(friendToMemberExists);
        }

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_SHIP_ALREADY_EXIST);

        verify(friendshipRepository, never()).save(any(Friendship.class));
    }

    private static Stream<Arguments> provideFriendshipExistsScenarios() {
        return Stream.of(
                Arguments.of("member -> friend", true, false),
                Arguments.of("friend -> member", false, true)
        );
    }

    @Test
    @DisplayName("친구 관계 생성 실패 - 회원이 존재하지 않음")
    void shouldThrowException_WhenMemberNotFound() {
        // Given
        given(friendToMemberAdapter.findById(FRIEND_ID)).willReturn(friend);
        given(friendshipRepository.existsByMemberIdAndFriendId(MEMBER_ID, FRIEND_ID)).willReturn(false);
        given(friendshipRepository.existsByMemberIdAndFriendId(FRIEND_ID, MEMBER_ID)).willReturn(false);
        given(friendToMemberAdapter.findById(MEMBER_ID)).willThrow(new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.createFriendship(MEMBER_ID, FRIEND_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(friendshipRepository, never()).save(any(Friendship.class));
    }

    // ==================== deleteFriendship ====================

    @ParameterizedTest(name = "친구 관계 삭제 성공 - {0}가 삭제 요청")
    @MethodSource("provideFriendshipDeleteParticipants")
    @DisplayName("친구 관계 삭제 성공 - 참여자별")
    void shouldDeleteFriendship_WhenParticipantRequests(String role, Long requesterId) {
        // Given
        given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.of(friendship));

        // When
        friendshipCommandService.deleteFriendship(requesterId, FRIENDSHIP_ID);

        // Then
        verify(friendshipRepository, times(1)).delete(friendship);
        verify(friendshipRedisUpdate, times(1)).deleteFriendToRedis(MEMBER_ID, FRIEND_ID);
    }

    @ParameterizedTest(name = "친구 관계 삭제 실패 - {0}")
    @MethodSource("provideFriendshipDeleteFailureScenarios")
    @DisplayName("친구 관계 삭제 실패 - 예외 케이스")
    void shouldThrowException_WhenDeleteFails(String scenario, Long requesterId, boolean friendshipExists, ErrorCode expectedErrorCode) {
        // Given
        if (friendshipExists) {
            given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.of(friendship));
        } else {
            given(friendshipRepository.findById(FRIENDSHIP_ID)).willReturn(Optional.empty());
        }

        // When & Then
        assertThatThrownBy(() -> friendshipCommandService.deleteFriendship(requesterId, FRIENDSHIP_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);

        verify(friendshipRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static Stream<Arguments> provideFriendshipDeleteParticipants() {
        return Stream.of(
                Arguments.of("member", 1L),
                Arguments.of("friend", 2L)
        );
    }

    private static Stream<Arguments> provideFriendshipDeleteFailureScenarios() {
        return Stream.of(
                Arguments.of("존재하지 않는 친구 관계", 1L, false, ErrorCode.FRIEND_SHIP_NOT_FOUND),
                Arguments.of("참여하지 않은 사용자", 999L, true, ErrorCode.FRIEND_SHIP_DELETE_FORBIDDEN)
        );
    }
}

package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.event.FriendEvent;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.friend.adapter.FriendToMemberAdapter;
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
 * <h2>FriendRequestCommandService 단위 테스트</h2>
 * <p>친구 요청 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("FriendRequestCommandService 단위 테스트")
@Tag("unit")
class FriendRequestCommandServiceTest extends BaseUnitTest {

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long FRIEND_REQUEST_ID = 100L;

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendToMemberAdapter friendToMemberAdapter;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendRequestCommandService friendRequestCommandService;

    private Member sender;
    private Member receiver;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        sender = getTestMember();
        TestFixtures.setFieldValue(sender, "id", SENDER_ID);

        receiver = getOtherMember();
        TestFixtures.setFieldValue(receiver, "id", RECEIVER_ID);

        friendRequest = FriendTestDataBuilder.createFriendRequest(FRIEND_REQUEST_ID, sender, receiver);
    }

    // ==================== sendFriendRequest ====================

    @Test
    @DisplayName("친구 요청 전송 성공 - 정상적인 요청")
    void shouldSendFriendRequest_WhenValidInput() {
        // Given
        given(friendToMemberAdapter.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
        given(friendToMemberAdapter.findById(SENDER_ID)).willReturn(Optional.of(sender));
        given(friendRequestRepository.existsBySenderIdAndReceiverId(SENDER_ID, RECEIVER_ID)).willReturn(false);
        given(friendRequestRepository.existsBySenderIdAndReceiverId(RECEIVER_ID, SENDER_ID)).willReturn(false);
        given(friendRequestRepository.save(any(FriendRequest.class))).willReturn(friendRequest);

        // When
        friendRequestCommandService.sendFriendRequest(SENDER_ID, RECEIVER_ID);

        // Then
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
        ArgumentCaptor<FriendEvent> eventCaptor = ArgumentCaptor.forClass(FriendEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        FriendEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getReceiveMemberId()).isEqualTo(RECEIVER_ID);
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 자기 자신에게 요청")
    void shouldThrowException_WhenSendingToSelf() {
        // When & Then
        assertThatThrownBy(() -> friendRequestCommandService.sendFriendRequest(SENDER_ID, SENDER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SELF_FRIEND_REQUEST_FORBIDDEN);

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
        verify(eventPublisher, never()).publishEvent(any(FriendEvent.class));
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 존재하지 않는 수신자")
    void shouldThrowException_WhenReceiverNotFound() {
        // Given
        given(friendToMemberAdapter.findById(RECEIVER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> friendRequestCommandService.sendFriendRequest(SENDER_ID, RECEIVER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_USER_NOT_FOUND);

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    @DisplayName("친구 요청 전송 - 블랙리스트 체크 이벤트 발행")
    void shouldPublishCheckBlacklistEvent_WhenSendingFriendRequest() {
        // Given
        given(friendToMemberAdapter.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
        given(friendToMemberAdapter.findById(SENDER_ID)).willReturn(Optional.of(sender));
        given(friendRequestRepository.existsBySenderIdAndReceiverId(SENDER_ID, RECEIVER_ID)).willReturn(false);
        given(friendRequestRepository.existsBySenderIdAndReceiverId(RECEIVER_ID, SENDER_ID)).willReturn(false);
        given(friendRequestRepository.save(any(FriendRequest.class))).willReturn(friendRequest);

        // When
        friendRequestCommandService.sendFriendRequest(SENDER_ID, RECEIVER_ID);

        // Then - CheckBlacklistEvent 발행 확인
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        boolean foundEvent = eventCaptor.getAllValues().stream()
                .anyMatch(event -> event instanceof jaeik.bimillog.domain.global.event.CheckBlacklistEvent);
        assertThat(foundEvent).isTrue();
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 이미 보낸 요청 존재 (sender -> receiver)")
    void shouldThrowException_WhenRequestAlreadySent() {
        // Given
        given(friendToMemberAdapter.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.existsBySenderIdAndReceiverId(SENDER_ID, RECEIVER_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> friendRequestCommandService.sendFriendRequest(SENDER_ID, RECEIVER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_ALREADY_SEND);

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 이미 받은 요청 존재 (receiver -> sender)")
    void shouldThrowException_WhenReverseRequestExists() {
        // Given
        given(friendToMemberAdapter.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.existsBySenderIdAndReceiverId(SENDER_ID, RECEIVER_ID)).willReturn(false);
        given(friendRequestRepository.existsBySenderIdAndReceiverId(RECEIVER_ID, SENDER_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> friendRequestCommandService.sendFriendRequest(SENDER_ID, RECEIVER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_ALREADY_RECEIVE);

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    // ==================== cancelFriendRequest ====================

    @Test
    @DisplayName("보낸 친구 요청 취소 성공")
    void shouldCancelFriendRequest_WhenValidSender() {
        // Given
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.of(friendRequest));

        // When
        friendRequestCommandService.cancelFriendRequest(SENDER_ID, FRIEND_REQUEST_ID);

        // Then
        verify(friendRequestRepository, times(1)).deleteById(FRIEND_REQUEST_ID);
    }

    @ParameterizedTest(name = "친구 요청 {0} 실패 - 존재하지 않는 요청")
    @MethodSource("provideFriendRequestOperations")
    @DisplayName("친구 요청 없음 예외 - 취소/거절 공통")
    void shouldThrowException_WhenRequestNotFound(String operation, Long memberId) {
        // Given
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.empty());

        // When & Then
        if ("취소".equals(operation)) {
            assertThatThrownBy(() -> friendRequestCommandService.cancelFriendRequest(memberId, FRIEND_REQUEST_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        } else {
            assertThatThrownBy(() -> friendRequestCommandService.deleteFriendRequest(memberId, FRIEND_REQUEST_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        verify(friendRequestRepository, never()).deleteById(any());
    }

    @ParameterizedTest(name = "친구 요청 {0} 실패 - 권한 없음")
    @MethodSource("provideUnauthorizedOperations")
    @DisplayName("권한 없음 예외 - 취소/거절 공통")
    void shouldThrowException_WhenUnauthorized(String operation, ErrorCode expectedErrorCode) {
        // Given
        Long wrongMemberId = 999L;
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.of(friendRequest));

        // When & Then
        if ("취소".equals(operation)) {
            assertThatThrownBy(() -> friendRequestCommandService.cancelFriendRequest(wrongMemberId, FRIEND_REQUEST_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
        } else {
            assertThatThrownBy(() -> friendRequestCommandService.deleteFriendRequest(wrongMemberId, FRIEND_REQUEST_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
        }

        verify(friendRequestRepository, never()).deleteById(any());
    }

    private static Stream<Arguments> provideFriendRequestOperations() {
        return Stream.of(
                Arguments.of("취소", 1L),
                Arguments.of("거절", 2L)
        );
    }

    private static Stream<Arguments> provideUnauthorizedOperations() {
        return Stream.of(
                Arguments.of("취소", ErrorCode.FRIEND_REQUEST_CANCEL_FORBIDDEN),
                Arguments.of("거절", ErrorCode.FRIEND_REQUEST_REJECT_FORBIDDEN)
        );
    }

    // ==================== deleteFriendRequest ====================

    @Test
    @DisplayName("받은 친구 요청 거절 성공")
    void shouldDeleteFriendRequest_WhenValidReceiver() {
        // Given
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.of(friendRequest));

        // When
        friendRequestCommandService.deleteFriendRequest(RECEIVER_ID, FRIEND_REQUEST_ID);

        // Then
        verify(friendRequestRepository, times(1)).deleteById(FRIEND_REQUEST_ID);
    }
}

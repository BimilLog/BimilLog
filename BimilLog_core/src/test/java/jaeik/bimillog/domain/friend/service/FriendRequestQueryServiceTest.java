package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.repository.FriendRequestQueryRepository;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * <h2>FriendRequestQueryService 단위 테스트</h2>
 * <p>친구 요청 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("FriendRequestQueryService 단위 테스트")
@Tag("unit")
class FriendRequestQueryServiceTest extends BaseUnitTest {

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long FRIEND_REQUEST_ID = 100L;

    @Mock private FriendRequestQueryRepository friendRequestQueryRepository;
    @Mock private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private FriendRequestQueryService friendRequestQueryService;

    private Member sender;
    private Member receiver;
    private FriendRequest friendRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sender = getTestMember();
        TestFixtures.setFieldValue(sender, "id", SENDER_ID);

        receiver = getOtherMember();
        TestFixtures.setFieldValue(receiver, "id", RECEIVER_ID);

        friendRequest = FriendTestDataBuilder.createFriendRequestWithId(FRIEND_REQUEST_ID, sender, receiver);
        pageable = PageRequest.of(0, 10);
    }

    // ==================== getFriendSendRequest ====================

    @Test
    @DisplayName("보낸 친구 요청 조회 성공 - 요청이 존재하는 경우")
    void shouldGetSentRequests_WhenRequestsExist() {
        // Given
        FriendSenderRequest request1 = new FriendSenderRequest(100L, 2L, "테스트회원2");
        FriendSenderRequest request2 = new FriendSenderRequest(101L, 3L, "테스트회원3");
        Page<FriendSenderRequest> expectedPage = new PageImpl<>(List.of(request1, request2), pageable, 2);

        given(friendRequestQueryRepository.findAllBySenderId(SENDER_ID, pageable)).willReturn(expectedPage);

        // When
        Page<FriendSenderRequest> result = friendRequestQueryService.getFriendSendRequest(SENDER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFriendRequestId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getReceiverMemberId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("보낸 친구 요청 조회 성공 - 요청이 없는 경우 (빈 페이지)")
    void shouldReturnEmptyPage_WhenNoSentRequests() {
        // Given
        Page<FriendSenderRequest> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(friendRequestQueryRepository.findAllBySenderId(SENDER_ID, pageable)).willReturn(emptyPage);

        // When
        Page<FriendSenderRequest> result = friendRequestQueryService.getFriendSendRequest(SENDER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("보낸 친구 요청 조회 성공 - 페이지네이션 확인")
    void shouldSupportPagination_ForSentRequests() {
        // Given
        Pageable secondPage = PageRequest.of(1, 5);
        FriendSenderRequest request = new FriendSenderRequest(106L, 7L, "테스트회원7");
        Page<FriendSenderRequest> expectedPage = new PageImpl<>(List.of(request), secondPage, 10);

        given(friendRequestQueryRepository.findAllBySenderId(SENDER_ID, secondPage)).willReturn(expectedPage);

        // When
        Page<FriendSenderRequest> result = friendRequestQueryService.getFriendSendRequest(SENDER_ID, secondPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getNumber()).isEqualTo(1);  // 페이지 번호
        assertThat(result.getSize()).isEqualTo(5);    // 페이지 크기
    }

    // ==================== getFriendReceiveRequest ====================

    @Test
    @DisplayName("받은 친구 요청 조회 성공 - 요청이 존재하는 경우")
    void shouldGetReceivedRequests_WhenRequestsExist() {
        // Given
        FriendReceiverRequest request1 = new FriendReceiverRequest(100L, 1L, "테스트회원1");
        FriendReceiverRequest request2 = new FriendReceiverRequest(101L, 3L, "테스트회원3");
        Page<FriendReceiverRequest> expectedPage = new PageImpl<>(List.of(request1, request2), pageable, 2);

        given(friendRequestQueryRepository.findAllByReceiveId(RECEIVER_ID, pageable)).willReturn(expectedPage);

        // When
        Page<FriendReceiverRequest> result = friendRequestQueryService.getFriendReceiveRequest(RECEIVER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFriendRequestId()).isEqualTo(100L);
        assertThat(result.getContent().get(0).getSenderMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("받은 친구 요청 조회 성공 - 요청이 없는 경우 (빈 페이지)")
    void shouldReturnEmptyPage_WhenNoReceivedRequests() {
        // Given
        Page<FriendReceiverRequest> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(friendRequestQueryRepository.findAllByReceiveId(RECEIVER_ID, pageable)).willReturn(emptyPage);

        // When
        Page<FriendReceiverRequest> result = friendRequestQueryService.getFriendReceiveRequest(RECEIVER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("받은 친구 요청 조회 성공 - 페이지네이션 확인")
    void shouldSupportPagination_ForReceivedRequests() {
        // Given
        Pageable secondPage = PageRequest.of(1, 5);
        FriendReceiverRequest request = new FriendReceiverRequest(106L, 7L, "테스트회원7");
        Page<FriendReceiverRequest> expectedPage = new PageImpl<>(List.of(request), secondPage, 10);

        given(friendRequestQueryRepository.findAllByReceiveId(RECEIVER_ID, secondPage)).willReturn(expectedPage);

        // When
        Page<FriendReceiverRequest> result = friendRequestQueryService.getFriendReceiveRequest(RECEIVER_ID, secondPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getNumber()).isEqualTo(1);  // 페이지 번호
        assertThat(result.getSize()).isEqualTo(5);    // 페이지 크기
    }

    // ==================== getSenderId ====================

    @Test
    @DisplayName("요청 보낸 사람 ID 조회 성공")
    void shouldGetSenderId_WhenValidRequest() {
        // Given
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.of(friendRequest));

        // When
        Long result = friendRequestQueryService.getSenderId(RECEIVER_ID, FRIEND_REQUEST_ID);

        // Then
        assertThat(result).isEqualTo(SENDER_ID);
    }

    @Test
    @DisplayName("요청 보낸 사람 ID 조회 실패 - 존재하지 않는 요청")
    void shouldThrowException_WhenRequestNotFound() {
        // Given
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> friendRequestQueryService.getSenderId(RECEIVER_ID, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("요청 보낸 사람 ID 조회 실패 - 요청 받은 사람이 아님")
    void shouldThrowException_WhenNotReceiver() {
        // Given
        Long wrongReceiverId = 999L;
        given(friendRequestRepository.findById(FRIEND_REQUEST_ID)).willReturn(Optional.of(friendRequest));

        // When & Then
        assertThatThrownBy(() -> friendRequestQueryService.getSenderId(wrongReceiverId, FRIEND_REQUEST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_REJECT_FORBIDDEN);
    }
}

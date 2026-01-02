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
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * <h2>FriendRequestQueryService 단위 테스트</h2>
 * <p>친구 요청 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
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

    /**
     * <h3>보낸 친구 요청 조회 시나리오 제공</h3>
     * <p>다양한 페이지네이션 상황을 테스트합니다.</p>
     *
     * @return Pageable, 요청 리스트, 총 개수의 조합
     */
    static Stream<Arguments> provideSentRequestScenarios() {
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 5);

        return Stream.of(
                // 요청이 존재하는 경우
                Arguments.of(page0, List.of(
                        new FriendSenderRequest(100L, 2L, "테스트회원2"),
                        new FriendSenderRequest(101L, 3L, "테스트회원3")
                ), 2L),
                // 요청이 없는 경우
                Arguments.of(page0, List.of(), 0L),
                // 페이지네이션 (2페이지)
                Arguments.of(page1, List.of(
                        new FriendSenderRequest(106L, 7L, "테스트회원7")
                ), 10L)
        );
    }

    @ParameterizedTest(name = "page={0}, totalElements={2}")
    @MethodSource("provideSentRequestScenarios")
    @DisplayName("보낸 친구 요청 조회 - 다양한 시나리오")
    void shouldGetSentRequests(Pageable pageable, List<FriendSenderRequest> requests, long total) {
        // Given
        Page<FriendSenderRequest> page = new PageImpl<>(requests, pageable, total);
        given(friendRequestQueryRepository.findAllBySenderId(SENDER_ID, pageable)).willReturn(page);

        // When
        Page<FriendSenderRequest> result = friendRequestQueryService.getFriendSendRequest(SENDER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(total);
        assertThat(result.getContent()).hasSize(requests.size());
    }

    // ==================== getFriendReceiveRequest ====================

    /**
     * <h3>받은 친구 요청 조회 시나리오 제공</h3>
     * <p>다양한 페이지네이션 상황을 테스트합니다.</p>
     *
     * @return Pageable, 요청 리스트, 총 개수의 조합
     */
    static Stream<Arguments> provideReceivedRequestScenarios() {
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 5);

        return Stream.of(
                // 요청이 존재하는 경우
                Arguments.of(page0, List.of(
                        new FriendReceiverRequest(100L, 1L, "테스트회원1"),
                        new FriendReceiverRequest(101L, 3L, "테스트회원3")
                ), 2L),
                // 요청이 없는 경우
                Arguments.of(page0, List.of(), 0L),
                // 페이지네이션 (2페이지)
                Arguments.of(page1, List.of(
                        new FriendReceiverRequest(106L, 7L, "테스트회원7")
                ), 10L)
        );
    }

    @ParameterizedTest(name = "page={0}, totalElements={2}")
    @MethodSource("provideReceivedRequestScenarios")
    @DisplayName("받은 친구 요청 조회 - 다양한 시나리오")
    void shouldGetReceivedRequests(Pageable pageable, List<FriendReceiverRequest> requests, long total) {
        // Given
        Page<FriendReceiverRequest> page = new PageImpl<>(requests, pageable, total);
        given(friendRequestQueryRepository.findAllByReceiveId(RECEIVER_ID, pageable)).willReturn(page);

        // When
        Page<FriendReceiverRequest> result = friendRequestQueryService.getFriendReceiveRequest(RECEIVER_ID, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(total);
        assertThat(result.getContent()).hasSize(requests.size());
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

package jaeik.bimillog.springboot.h2;

import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.builder.FriendTestDataBuilder;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>FriendRequestCommandController 통합 테스트</h2>
 * <p>친구 요청 명령 컨트롤러의 API 엔드포인트를 검증하는 통합 테스트</p>
 * <p>H2 데이터베이스를 사용하여 실제 HTTP 요청/응답 검증</p>
 *
 * @author Jaeik
 */
@DisplayName("FriendRequestCommandController 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("springboot-h2")
@ActiveProfiles("h2test")
@Import(H2TestConfiguration.class)
class FriendRequestCommandControllerTest extends BaseIntegrationTest {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private Member receiver;
    private FriendRequest friendRequest;

    @Override
    protected void setUpChild() {
        // 추가 회원 생성 및 저장
        receiver = saveMember(TestMembers.createUniqueWithPrefix("receiver"));

        // testMember → receiver로의 친구 요청 생성
        friendRequest = FriendTestDataBuilder.createFriendRequest(null, testMember, receiver);
        friendRequest = friendRequestRepository.save(friendRequest);
    }

    // ==================== POST /api/friend/send ====================

    @Test
    @DisplayName("친구 요청 전송 성공 - 200 OK 및 보낸 요청 목록 반환")
    void shouldSendFriendRequest_AndReturnSentRequests() throws Exception {
        // Given
        Member newReceiver = saveMember(TestMembers.createUniqueWithPrefix("newReceiver"));
        FriendSenderRequest request = new FriendSenderRequest(null, newReceiver.getId(), null);

        // When & Then
        mockMvc.perform(post("/api/friend/send")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))  // setUpChild()에서 1개 + 테스트에서 1개 = 총 2개
                .andExpect(jsonPath("$.content[0].receiverMemberId").value(newReceiver.getId()));  // 최신순 정렬로 새 요청이 첫 번째
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 자기 자신에게 요청 (403 Forbidden)")
    void shouldReturn403_WhenSendingToSelf() throws Exception {
        // Given
        FriendSenderRequest request = new FriendSenderRequest(null, testMember.getId(), null);

        // When & Then
        mockMvc.perform(post("/api/friend/send")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("친구 요청 전송 실패 - 인증되지 않은 사용자 (403 Forbidden)")
    void shouldReturn403_WhenUnauthorized() throws Exception {
        // Given
        FriendSenderRequest request = new FriendSenderRequest(null, receiver.getId(), null);

        // When & Then
        mockMvc.perform(post("/api/friend/send")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());  // Spring Security 기본 동작: 인증 없이 인증 필요 엔드포인트 접근 시 403 반환
    }

    // ==================== DELETE /api/friend/send/{id} ====================

    @Test
    @DisplayName("보낸 친구 요청 취소 성공 - 200 OK 및 업데이트된 보낸 요청 목록 반환")
    void shouldCancelRequest_AndReturnUpdatedList() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/friend/send/{friendRequestId}", friendRequest.getId())
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 요청이 삭제되었는지 확인
        assert friendRequestRepository.findById(friendRequest.getId()).isEmpty();
    }

    @Test
    @DisplayName("보낸 친구 요청 취소 실패 - 존재하지 않는 요청 (400 Bad Request)")
    void shouldReturn400_WhenCancelingNonExistentRequest() throws Exception {
        // Given
        Long nonExistentId = 999999L;

        // When & Then
        mockMvc.perform(delete("/api/friend/send/{friendRequestId}", nonExistentId)
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("보낸 친구 요청 취소 실패 - 요청 보낸 사람이 아님 (403 Forbidden)")
    void shouldReturn403_WhenNotSender() throws Exception {
        // When & Then (otherMember가 testMember의 요청을 취소하려고 시도)
        mockMvc.perform(delete("/api/friend/send/{friendRequestId}", friendRequest.getId())
                        .with(user(otherUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/friend/receive/{id} ====================

    @Test
    @DisplayName("받은 친구 요청 거절 성공 - 200 OK 및 업데이트된 받은 요청 목록 반환")
    void shouldRejectRequest_AndReturnUpdatedList() throws Exception {
        // Given - receiver가 받은 요청 거절
        var receiverUserDetails = createCustomUserDetails(receiver);

        // When & Then
        mockMvc.perform(delete("/api/friend/receive/{friendRequestId}", friendRequest.getId())
                        .with(user(receiverUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 요청이 삭제되었는지 확인
        assert friendRequestRepository.findById(friendRequest.getId()).isEmpty();
    }

    @Test
    @DisplayName("받은 친구 요청 거절 실패 - 존재하지 않는 요청 (400 Bad Request)")
    void shouldReturn400_WhenRejectingNonExistentRequest() throws Exception {
        // Given
        Long nonExistentId = 999999L;
        var receiverUserDetails = createCustomUserDetails(receiver);

        // When & Then
        mockMvc.perform(delete("/api/friend/receive/{friendRequestId}", nonExistentId)
                        .with(user(receiverUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("받은 친구 요청 거절 실패 - 요청 받은 사람이 아님 (403 Forbidden)")
    void shouldReturn403_WhenNotReceiver() throws Exception {
        // When & Then (otherMember가 receiver의 요청을 거절하려고 시도)
        mockMvc.perform(delete("/api/friend/receive/{friendRequestId}", friendRequest.getId())
                        .with(user(otherUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/friend/receive/{id} ====================

    @Test
    @DisplayName("받은 친구 요청 수락 성공 - 200 OK 및 친구 목록 반환")
    void shouldAcceptRequest_AndReturnFriendList() throws Exception {
        // Given - receiver가 요청 수락
        var receiverUserDetails = createCustomUserDetails(receiver);

        // When & Then
        mockMvc.perform(post("/api/friend/receive/{friendRequestId}", friendRequest.getId())
                        .with(user(receiverUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].friendMemberId").value(testMember.getId()));

        // 요청이 삭제되었는지 확인
        assert friendRequestRepository.findById(friendRequest.getId()).isEmpty();
    }

    @Test
    @DisplayName("받은 친구 요청 수락 실패 - 존재하지 않는 요청 (400 Bad Request)")
    void shouldReturn400_WhenAcceptingNonExistentRequest() throws Exception {
        // Given
        Long nonExistentId = 999999L;
        var receiverUserDetails = createCustomUserDetails(receiver);

        // When & Then
        mockMvc.perform(post("/api/friend/receive/{friendRequestId}", nonExistentId)
                        .with(user(receiverUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("받은 친구 요청 수락 실패 - 요청 받은 사람이 아님 (403 Forbidden)")
    void shouldReturn403_WhenAcceptingAsNonReceiver() throws Exception {
        // When & Then (otherMember가 receiver의 요청을 수락하려고 시도)
        mockMvc.perform(post("/api/friend/receive/{friendRequestId}", friendRequest.getId())
                        .with(user(otherUserDetails))
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }
}

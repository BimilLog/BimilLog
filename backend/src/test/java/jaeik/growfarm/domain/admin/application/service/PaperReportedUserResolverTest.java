package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>PaperReportedUserResolver 단위 테스트</h2>
 * <p>롤링페이퍼 신고 사용자 해결사의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>모든 외부 의존성을 모킹하여 순수한 비즈니스 로직만 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaperReportedUserResolver 단위 테스트")
class PaperReportedUserResolverTest {

    @Mock
    private PaperQueryUseCase paperQueryUseCase;

    @InjectMocks
    private PaperReportedUserResolver paperReportedUserResolver;

    private Message testMessage;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .userName("testUser")
                .socialId("kakao123")
                .build();

        testMessage = Message.builder()
                .id(300L)
                .content("테스트 메시지")
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("유효한 메시지 ID로 사용자 해결 시 성공")
    void shouldResolveUser_WhenValidMessageId() {
        // Given
        Long messageId = 300L;
        given(paperQueryUseCase.findMessageById(messageId)).willReturn(Optional.of(testMessage));

        // When
        User result = paperReportedUserResolver.resolve(messageId);

        // Then
        assertThat(result).isEqualTo(testUser);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserName()).isEqualTo("testUser");
        verify(paperQueryUseCase).findMessageById(messageId);
    }

    @Test
    @DisplayName("존재하지 않는 메시지 ID로 해결 시 MESSAGE_NOT_FOUND 예외 발생")
    void shouldThrowException_WhenMessageNotFound() {
        // Given
        Long nonExistentMessageId = 999L;
        given(paperQueryUseCase.findMessageById(nonExistentMessageId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paperReportedUserResolver.resolve(nonExistentMessageId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_NOT_FOUND);

        verify(paperQueryUseCase).findMessageById(nonExistentMessageId);
    }

    @Test
    @DisplayName("지원하는 신고 유형이 PAPER인지 확인")
    void shouldSupportPaperReportType() {
        // When
        ReportType supportedType = paperReportedUserResolver.supports();

        // Then
        assertThat(supportedType).isEqualTo(ReportType.PAPER);
    }

    @Test
    @DisplayName("메시지가 존재하지만 사용자가 null인 경우 처리")
    void shouldHandleMessageWithNullUser() {
        // Given
        Long messageId = 300L;
        Message messageWithNullUser = Message.builder()
                .id(messageId)
                .content("사용자가 없는 메시지")
                .user(null)
                .build();
        
        given(paperQueryUseCase.findMessageById(messageId)).willReturn(Optional.of(messageWithNullUser));

        // When
        User result = paperReportedUserResolver.resolve(messageId);

        // Then
        assertThat(result).isNull();
        verify(paperQueryUseCase).findMessageById(messageId);
    }

    @Test
    @DisplayName("다양한 메시지 ID로 사용자 해결 테스트")
    void shouldResolveUsersForDifferentMessageIds() {
        // Given
        Long[] messageIds = {1L, 2L, 3L};
        User[] users = {
                User.builder().id(101L).userName("user1").build(),
                User.builder().id(102L).userName("user2").build(),
                User.builder().id(103L).userName("user3").build()
        };

        for (int i = 0; i < messageIds.length; i++) {
            Message message = Message.builder()
                    .id(messageIds[i])
                    .content("메시지 " + (i + 1))
                    .user(users[i])
                    .build();
            given(paperQueryUseCase.findMessageById(messageIds[i])).willReturn(Optional.of(message));
        }

        // When & Then
        for (int i = 0; i < messageIds.length; i++) {
            User result = paperReportedUserResolver.resolve(messageIds[i]);
            assertThat(result).isEqualTo(users[i]);
            assertThat(result.getId()).isEqualTo(users[i].getId());
            assertThat(result.getUserName()).isEqualTo(users[i].getUserName());
        }

        // 모든 호출이 이루어졌는지 검증
        for (Long messageId : messageIds) {
            verify(paperQueryUseCase).findMessageById(messageId);
        }
    }

    @Test
    @DisplayName("익명 사용자 메시지 처리")
    void shouldHandleAnonymousMessage() {
        // Given
        Long messageId = 400L;
        User anonymousUser = User.builder()
                .id(null)
                .userName("Anonymous")
                .socialId("anonymous")
                .build();

        Message anonymousMessage = Message.builder()
                .id(messageId)
                .content("익명 메시지")
                .user(anonymousUser)
                .build();

        given(paperQueryUseCase.findMessageById(messageId)).willReturn(Optional.of(anonymousMessage));

        // When
        User result = paperReportedUserResolver.resolve(messageId);

        // Then
        assertThat(result).isEqualTo(anonymousUser);
        assertThat(result.getUserName()).isEqualTo("Anonymous");
        assertThat(result.getSocialId()).isEqualTo("anonymous");
        verify(paperQueryUseCase).findMessageById(messageId);
    }
}
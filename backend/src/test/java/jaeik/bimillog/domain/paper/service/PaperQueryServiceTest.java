package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PaperQueryService 테스트</h2>
 * <p>롤링페이퍼 조회 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaperQueryService 테스트")
class PaperQueryServiceTest {

    @Mock
    private PaperQueryPort paperQueryPort;

    @Mock
    private GlobalUserQueryPort globalUserQueryPort;


    @Mock
    private User user;

    @Mock
    private Message message;

    @InjectMocks
    private PaperQueryService paperQueryService;

    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공")
    void shouldGetMyPaper_WhenValidUser() {
        // Given
        Long userId = 1L;
        List<Message> messages = Arrays.asList(
                createMessage(1L, userId, "첫 번째 메시지"),
                createMessage(2L, userId, "두 번째 메시지"),
                createMessage(3L, userId, "세 번째 메시지")
        );

        given(paperQueryPort.findMessagesByUserId(userId)).willReturn(messages);

        // When
        List<MessageDetail> result = paperQueryService.getMyPaper(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).content()).isEqualTo("첫 번째 메시지");
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).content()).isEqualTo("두 번째 메시지");
        assertThat(result.get(2).id()).isEqualTo(3L);
        assertThat(result.get(2).content()).isEqualTo("세 번째 메시지");

        verify(paperQueryPort, times(1)).findMessagesByUserId(userId);
        verifyNoMoreInteractions(paperQueryPort);
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 - 빈 목록")
    void shouldGetMyPaper_WhenNoMessages() {
        // Given
        Long userId = 1L;
        List<Message> emptyList = Collections.emptyList();

        given(paperQueryPort.findMessagesByUserId(userId)).willReturn(emptyList);

        // When
        List<MessageDetail> result = paperQueryService.getMyPaper(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(paperQueryPort, times(1)).findMessagesByUserId(userId);
        verifyNoMoreInteractions(paperQueryPort);
    }


    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 성공")
    void shouldVisitPaper_WhenValidUserName() {
        // Given
        String userName = "testuser";
        List<Message> messages = Arrays.asList(
                createMessage(1L, 100L, "메시지1"),
                createMessage(2L, 200L, "메시지2")
        );

        given(globalUserQueryPort.existsByUserName(userName)).willReturn(true);
        given(paperQueryPort.findMessagesByUserName(userName)).willReturn(messages);

        // When
        List<VisitMessageDetail> result = paperQueryService.visitPaper(userName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).userId()).isEqualTo(100L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).userId()).isEqualTo(200L);

        verify(globalUserQueryPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, times(1)).findMessagesByUserName(userName);
        verifyNoMoreInteractions(globalUserQueryPort, paperQueryPort);
    }

    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 사용자 없음")
    void shouldThrowException_WhenUserNotExists() {
        // Given
        String userName = "nonexistentuser";

        given(globalUserQueryPort.existsByUserName(userName)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> paperQueryService.visitPaper(userName))
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", PaperErrorCode.USERNAME_NOT_FOUND);

        verify(globalUserQueryPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, never()).findMessagesByUserName(any());
    }

    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 빈 목록")
    void shouldVisitPaper_WhenNoMessages() {
        // Given
        String userName = "testuser";
        List<Message> emptyList = Collections.emptyList();

        given(globalUserQueryPort.existsByUserName(userName)).willReturn(true);
        given(paperQueryPort.findMessagesByUserName(userName)).willReturn(emptyList);

        // When
        List<VisitMessageDetail> result = paperQueryService.visitPaper(userName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(globalUserQueryPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, times(1)).findMessagesByUserName(userName);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - 성공")
    void shouldFindMessageById_WhenValidId() {
        // Given
        Long messageId = 123L;
        Message expectedMessage = createMessage(messageId, 1L, "테스트 메시지");

        given(paperQueryPort.findMessageById(messageId)).willReturn(Optional.of(expectedMessage));

        // When
        Optional<Message> result = paperQueryService.findMessageById(messageId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(messageId);
        assertThat(result.get().getContent()).isEqualTo("테스트 메시지");

        verify(paperQueryPort, times(1)).findMessageById(messageId);
        verifyNoMoreInteractions(paperQueryPort);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - 메시지 없음")
    void shouldFindMessageById_WhenMessageNotFound() {
        // Given
        Long messageId = 999L;

        given(paperQueryPort.findMessageById(messageId)).willReturn(Optional.empty());

        // When
        Optional<Message> result = paperQueryService.findMessageById(messageId);

        // Then
        assertThat(result).isEmpty();

        verify(paperQueryPort, times(1)).findMessageById(messageId);
        verifyNoMoreInteractions(paperQueryPort);
    }

    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - null 또는 빈 사용자명 예외")
    void shouldThrowException_WhenInvalidUserName() {
        // Given - null userName
        String userName = null;

        // When & Then - null case
        assertThatThrownBy(() -> paperQueryService.visitPaper(userName))
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", PaperErrorCode.INVALID_INPUT_VALUE);

        // Given - empty userName
        String emptyUserName = "   ";

        // When & Then - empty case
        assertThatThrownBy(() -> paperQueryService.visitPaper(emptyUserName))
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", PaperErrorCode.INVALID_INPUT_VALUE);

        verify(globalUserQueryPort, never()).existsByUserName(any());
        verify(paperQueryPort, never()).findMessagesByUserName(any());
    }

    private Message createMessage(Long id, Long userId, String content) {
        User mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn(userId);
        
        return Message.builder()
                .id(id)
                .user(mockUser)
                .decoType(DecoType.APPLE)
                .anonymity("익명")
                .content(content)
                .x(100)
                .y(50)
                .createdAt(Instant.now())
                .build();
    }
}
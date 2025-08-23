package jaeik.growfarm.infrastructure.adapter.paper.out.persistence.paper;

import jaeik.growfarm.domain.paper.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>PaperCommandAdapter 테스트</h2>
 * <p>PaperCommandAdapter의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PaperCommandAdapterTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private PaperCommandAdapter paperCommandAdapter;

    private Message testMessage;

    @BeforeEach
    void setUp() {
        testMessage = Message.builder()
                .id(1L)
                .content("테스트 메시지")
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 저장")
    void shouldSaveMessage_WhenValidMessageProvided() {
        // Given
        given(messageRepository.save(any(Message.class))).willReturn(testMessage);

        // When
        Message savedMessage = paperCommandAdapter.save(testMessage);

        // Then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getId()).isEqualTo(1L);
        assertThat(savedMessage.getContent()).isEqualTo("테스트 메시지");
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 ID로 삭제")
    void shouldDeleteMessage_WhenValidIdProvided() {
        // Given
        Long messageId = 1L;

        // When
        paperCommandAdapter.deleteById(messageId);

        // Then
        verify(messageRepository, times(1)).deleteById(messageId);
    }

    @Test
    @DisplayName("경계값 - null 메시지 저장 시도")
    void shouldThrowException_WhenSavingNullMessage() {
        // Given
        Message nullMessage = null;
        given(messageRepository.save(nullMessage)).willThrow(new IllegalArgumentException("Message cannot be null"));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            paperCommandAdapter.save(nullMessage);
        });
        verify(messageRepository, times(1)).save(nullMessage);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 메시지 삭제 시도")
    void shouldHandleDeletion_WhenNonExistentIdProvided() {
        // Given
        Long nonExistentMessageId = 999L;
        // deleteById는 반환값이 없으므로 예외 발생 여부만 확인 (mock은 기본적으로 아무것도 하지 않음)

        // When
        paperCommandAdapter.deleteById(nonExistentMessageId);

        // Then
        verify(messageRepository, times(1)).deleteById(nonExistentMessageId);
        // 특정 예외가 발생하지 않는다는 것을 확인하려면 추가적인 설정이 필요하지만, 현재 deleteById의 동작 방식으로는 예외가 발생하지 않음
    }
}

package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.paper.application.port.out.PaperQueryPort;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.infrastructure.adapter.paper.in.web.dto.MessageDTO;
import jaeik.growfarm.infrastructure.adapter.paper.in.web.dto.VisitMessageDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * 
 * TODO: 다른 도메인 테스트의 컴파일 오류로 인해 현재 실행 불가.
 *       post, notification 도메인 컴파일 이슈 해결 후 테스트 실행 필요.
 *       VisitMessageDTO 구조 맞춤, 엣지케이스 및 성능 테스트 추가 완료.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaperQueryService 테스트")
class PaperQueryServiceTest {

    @Mock
    private PaperQueryPort paperQueryPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private CustomUserDetails userDetails;

    @Mock
    private Message message;

    @InjectMocks
    private PaperQueryService paperQueryService;

    @Test
    @DisplayName("내 롤링페이퍼 조회 - 성공")
    void shouldGetMyPaper_WhenValidUser() {
        // Given
        Long userId = 1L;
        List<MessageDTO> expectedMessages = Arrays.asList(
                createMessageDTO(1L, "첫 번째 메시지"),
                createMessageDTO(2L, "두 번째 메시지"),
                createMessageDTO(3L, "세 번째 메시지")
        );

        given(userDetails.getUserId()).willReturn(userId);
        given(paperQueryPort.findMessageDTOsByUserId(userId)).willReturn(expectedMessages);

        // When
        List<MessageDTO> result = paperQueryService.getMyPaper(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getContent()).isEqualTo("첫 번째 메시지");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getContent()).isEqualTo("두 번째 메시지");
        assertThat(result.get(2).getId()).isEqualTo(3L);
        assertThat(result.get(2).getContent()).isEqualTo("세 번째 메시지");

        verify(userDetails, times(1)).getUserId();
        verify(paperQueryPort, times(1)).findMessageDTOsByUserId(userId);
        verifyNoMoreInteractions(paperQueryPort);
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 - 빈 목록")
    void shouldGetMyPaper_WhenNoMessages() {
        // Given
        Long userId = 1L;
        List<MessageDTO> emptyList = Collections.emptyList();

        given(userDetails.getUserId()).willReturn(userId);
        given(paperQueryPort.findMessageDTOsByUserId(userId)).willReturn(emptyList);

        // When
        List<MessageDTO> result = paperQueryService.getMyPaper(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userDetails, times(1)).getUserId();
        verify(paperQueryPort, times(1)).findMessageDTOsByUserId(userId);
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 성공")
    void shouldVisitPaper_WhenValidUserName() {
        // Given
        String userName = "testuser";
        List<VisitMessageDTO> expectedMessages = Arrays.asList(
                createVisitMessageDTO(1L, "방문 메시지 1"),
                createVisitMessageDTO(2L, "방문 메시지 2")
        );

        given(loadUserPort.existsByUserName(userName)).willReturn(true);
        given(paperQueryPort.findVisitMessageDTOsByUserName(userName)).willReturn(expectedMessages);

        // When
        List<VisitMessageDTO> result = paperQueryService.visitPaper(userName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(loadUserPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, times(1)).findVisitMessageDTOsByUserName(userName);
        verifyNoMoreInteractions(loadUserPort, paperQueryPort);
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 사용자 없음 예외")
    void shouldThrowException_WhenUserNameNotFound() {
        // Given
        String userName = "nonexistentuser";

        given(loadUserPort.existsByUserName(userName)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> paperQueryService.visitPaper(userName))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USERNAME_NOT_FOUND);

        verify(loadUserPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, never()).findVisitMessageDTOsByUserName(any());
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 빈 목록")
    void shouldVisitPaper_WhenNoMessages() {
        // Given
        String userName = "testuser";
        List<VisitMessageDTO> emptyList = Collections.emptyList();

        given(loadUserPort.existsByUserName(userName)).willReturn(true);
        given(paperQueryPort.findVisitMessageDTOsByUserName(userName)).willReturn(emptyList);

        // When
        List<VisitMessageDTO> result = paperQueryService.visitPaper(userName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(loadUserPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, times(1)).findVisitMessageDTOsByUserName(userName);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - 성공")
    void shouldFindMessageById_WhenValidId() {
        // Given
        Long messageId = 123L;

        given(paperQueryPort.findMessageById(messageId)).willReturn(Optional.of(message));

        // When
        Optional<Message> result = paperQueryService.findMessageById(messageId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(message);

        verify(paperQueryPort, times(1)).findMessageById(messageId);
        verifyNoMoreInteractions(paperQueryPort);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - 메시지 없음")
    void shouldFindMessageById_WhenNotFound() {
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
    @DisplayName("롤링페이퍼 방문 - null 사용자명")
    void shouldThrowException_WhenUserNameIsNull() {
        // Given
        String userName = null;

        given(loadUserPort.existsByUserName(userName)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> paperQueryService.visitPaper(userName))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USERNAME_NOT_FOUND);

        verify(loadUserPort, times(1)).existsByUserName(userName);
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 빈 사용자명")
    void shouldThrowException_WhenUserNameIsEmpty() {
        // Given
        String userName = "";

        given(loadUserPort.existsByUserName(userName)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> paperQueryService.visitPaper(userName))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USERNAME_NOT_FOUND);

        verify(loadUserPort, times(1)).existsByUserName(userName);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - null ID")
    void shouldFindMessageById_WhenNullId() {
        // Given
        Long messageId = null;

        given(paperQueryPort.findMessageById(messageId)).willReturn(Optional.empty());

        // When
        Optional<Message> result = paperQueryService.findMessageById(messageId);

        // Then
        assertThat(result).isEmpty();

        verify(paperQueryPort, times(1)).findMessageById(messageId);
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 - null 사용자 ID")
    void shouldGetMyPaper_WhenNullUserId() {
        // Given
        Long userId = null;
        List<MessageDTO> emptyList = Collections.emptyList();

        given(userDetails.getUserId()).willReturn(userId);
        given(paperQueryPort.findMessageDTOsByUserId(userId)).willReturn(emptyList);

        // When
        List<MessageDTO> result = paperQueryService.getMyPaper(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userDetails, times(1)).getUserId();
        verify(paperQueryPort, times(1)).findMessageDTOsByUserId(userId);
    }

    @Test
    @DisplayName("대용량 메시지 목록 조회")
    void shouldGetMyPaper_WithLargeMessageList() {
        // Given
        Long userId = 1L;
        List<MessageDTO> largeMessageList = Arrays.asList(
                createMessageDTO(1L, "메시지 1"),
                createMessageDTO(2L, "메시지 2"),
                createMessageDTO(3L, "메시지 3"),
                createMessageDTO(4L, "메시지 4"),
                createMessageDTO(5L, "메시지 5")
        );

        given(userDetails.getUserId()).willReturn(userId);
        given(paperQueryPort.findMessageDTOsByUserId(userId)).willReturn(largeMessageList);

        // When
        List<MessageDTO> result = paperQueryService.getMyPaper(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(4).getId()).isEqualTo(5L);

        verify(userDetails, times(1)).getUserId();
        verify(paperQueryPort, times(1)).findMessageDTOsByUserId(userId);
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 대소문자 포함 사용자명")
    void shouldVisitPaper_WithMixedCaseUserName() {
        // Given
        String userName = "TestUser123";
        List<VisitMessageDTO> expectedMessages = Arrays.asList(
                createVisitMessageDTO(1L, "메시지"),
                createVisitMessageDTO(2L, "메시지")
        );

        given(loadUserPort.existsByUserName(userName)).willReturn(true);
        given(paperQueryPort.findVisitMessageDTOsByUserName(userName)).willReturn(expectedMessages);

        // When
        List<VisitMessageDTO> result = paperQueryService.visitPaper(userName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(loadUserPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, times(1)).findVisitMessageDTOsByUserName(userName);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - 음수 ID")
    void shouldFindMessageById_WhenNegativeId() {
        // Given
        Long negativeId = -1L;

        given(paperQueryPort.findMessageById(negativeId)).willReturn(Optional.empty());

        // When
        Optional<Message> result = paperQueryService.findMessageById(negativeId);

        // Then
        assertThat(result).isEmpty();

        verify(paperQueryPort, times(1)).findMessageById(negativeId);
    }

    @Test
    @DisplayName("메시지 ID로 조회 - 매우 큰 ID")
    void shouldFindMessageById_WhenVeryLargeId() {
        // Given
        Long largeId = Long.MAX_VALUE;

        given(paperQueryPort.findMessageById(largeId)).willReturn(Optional.empty());

        // When
        Optional<Message> result = paperQueryService.findMessageById(largeId);

        // Then
        assertThat(result).isEmpty();

        verify(paperQueryPort, times(1)).findMessageById(largeId);
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 특수문자 포함 사용자명")
    void shouldVisitPaper_WithSpecialCharacterUserName() {
        // Given
        String userName = "user@test.com";
        List<VisitMessageDTO> expectedMessages = Arrays.asList(
                createVisitMessageDTO(10L, "메시지")
        );

        given(loadUserPort.existsByUserName(userName)).willReturn(true);
        given(paperQueryPort.findVisitMessageDTOsByUserName(userName)).willReturn(expectedMessages);

        // When
        List<VisitMessageDTO> result = paperQueryService.visitPaper(userName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);

        verify(loadUserPort, times(1)).existsByUserName(userName);
        verify(paperQueryPort, times(1)).findVisitMessageDTOsByUserName(userName);
    }

    @Test
    @DisplayName("메시지 조회 성능 테스트 - 여러 번 연속 호출")
    void shouldHandleMultipleConsecutiveCalls() {
        // Given
        Long[] messageIds = {100L, 200L, 300L, 400L, 500L};
        
        for (Long messageId : messageIds) {
            given(paperQueryPort.findMessageById(messageId)).willReturn(Optional.of(message));
        }

        // When & Then
        for (Long messageId : messageIds) {
            Optional<Message> result = paperQueryService.findMessageById(messageId);
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(message);
        }

        // 모든 호출이 이루어졌는지 확인
        for (Long messageId : messageIds) {
            verify(paperQueryPort, times(1)).findMessageById(messageId);
        }
    }

    @Test
    @DisplayName("내 롤링페이퍼 조회 - 메시지 정렬 순서 확인")
    void shouldGetMyPaper_WithCorrectMessageOrder() {
        // Given
        Long userId = 1L;
        List<MessageDTO> orderedMessages = Arrays.asList(
                createMessageDTO(3L, "최신 메시지"),
                createMessageDTO(2L, "두 번째 메시지"),
                createMessageDTO(1L, "첫 번째 메시지")
        );

        given(userDetails.getUserId()).willReturn(userId);
        given(paperQueryPort.findMessageDTOsByUserId(userId)).willReturn(orderedMessages);

        // When
        List<MessageDTO> result = paperQueryService.getMyPaper(userDetails);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        // 반환된 순서가 그대로 유지되는지 확인
        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(2).getId()).isEqualTo(1L);

        verify(userDetails, times(1)).getUserId();
        verify(paperQueryPort, times(1)).findMessageDTOsByUserId(userId);
    }

    @Test
    @DisplayName("롤링페이퍼 방문 - 한글 사용자명")
    void shouldVisitPaper_WithKoreanUserName() {
        // Given
        String koreanUserName = "한글사용자";
        List<VisitMessageDTO> expectedMessages = Arrays.asList(
                createVisitMessageDTO(1L, "메시지")
        );

        given(loadUserPort.existsByUserName(koreanUserName)).willReturn(true);
        given(paperQueryPort.findVisitMessageDTOsByUserName(koreanUserName)).willReturn(expectedMessages);

        // When
        List<VisitMessageDTO> result = paperQueryService.visitPaper(koreanUserName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(loadUserPort, times(1)).existsByUserName(koreanUserName);
        verify(paperQueryPort, times(1)).findVisitMessageDTOsByUserName(koreanUserName);
    }

    private MessageDTO createMessageDTO(Long id, String content) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(id);
        messageDTO.setContent(content);
        return messageDTO;
    }

    private VisitMessageDTO createVisitMessageDTO(Long id, String content) {
        VisitMessageDTO visitMessageDTO = new VisitMessageDTO();
        visitMessageDTO.setId(id);
        return visitMessageDTO;
    }
}
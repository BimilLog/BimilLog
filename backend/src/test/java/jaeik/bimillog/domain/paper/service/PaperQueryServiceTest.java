package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalMemberQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.VisitMessageDetail;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.PaperTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

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
@DisplayName("PaperQueryService 테스트")
@Tag("unit")
class PaperQueryServiceTest extends BaseUnitTest {

    @Mock
    private PaperQueryPort paperQueryPort;

    @Mock
    private GlobalMemberQueryPort globalMemberQueryPort;

    @InjectMocks
    private PaperQueryService paperQueryService;


    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 성공")
    void shouldVisitPaper_WhenValidUserName() {
        // Given
        String memberName = getTestMember().getMemberName();
        List<Message> messages = Arrays.asList(
                PaperTestDataBuilder.createRollingPaper(getTestMember(), "메시지1", 5, 5),
                PaperTestDataBuilder.createRollingPaper(getOtherMember(), "메시지2", 10, 10)
        );

        given(globalMemberQueryPort.existsByMemberName(memberName)).willReturn(true);
        given(paperQueryPort.findMessagesByMemberName(memberName)).willReturn(messages);

        // When
        List<VisitMessageDetail> result = paperQueryService.visitPaper(memberName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).memberId()).isEqualTo(getTestMember().getId());
        assertThat(result.get(1).memberId()).isEqualTo(getOtherMember().getId());

        verify(globalMemberQueryPort, times(1)).existsByMemberName(memberName);
        verify(paperQueryPort, times(1)).findMessagesByMemberName(memberName);
        verifyNoMoreInteractions(globalMemberQueryPort, paperQueryPort);
    }

    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 사용자 없음")
    void shouldThrowException_WhenUserNotExists() {
        // Given
        String memberName = "nonexistentuser";

        given(globalMemberQueryPort.existsByMemberName(memberName)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> paperQueryService.visitPaper(memberName))
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", PaperErrorCode.USERNAME_NOT_FOUND);

        verify(globalMemberQueryPort, times(1)).existsByMemberName(memberName);
        verify(paperQueryPort, never()).findMessagesByMemberName(any());
    }



    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - null 또는 빈 사용자명 예외")
    void shouldThrowException_WhenInvalidUserName() {
        // Given - null memberName
        String memberName = null;

        // When & Then - null case
        assertThatThrownBy(() -> paperQueryService.visitPaper(memberName))
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", PaperErrorCode.INVALID_INPUT_VALUE);

        // Given - empty memberName
        String emptyUserName = "   ";

        // When & Then - empty case
        assertThatThrownBy(() -> paperQueryService.visitPaper(emptyUserName))
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", PaperErrorCode.INVALID_INPUT_VALUE);

        verify(globalMemberQueryPort, never()).existsByMemberName(any());
        verify(paperQueryPort, never()).findMessagesByMemberName(any());
    }

}
package jaeik.bimillog.domain.paper.service;

import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.service.PaperQueryService.VisitPaperResult;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.paper.out.PaperQueryRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PaperTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

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
@DisplayName("PaperQueryService 테스트")
@Tag("unit")
class PaperQueryServiceTest extends BaseUnitTest {

    @Mock
    private PaperQueryRepository paperQueryRepository;

    @Mock
    private GlobalMemberQueryAdapter globalMemberQueryAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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

        given(globalMemberQueryAdapter.findByMemberName(memberName)).willReturn(Optional.of(getTestMember()));
        given(paperQueryRepository.findMessagesByMemberName(memberName)).willReturn(messages);

        // When
        VisitPaperResult result = paperQueryService.visitPaper(getTestMember().getId(), memberName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).getMemberId()).isEqualTo(getTestMember().getId());
        assertThat(result.messages().get(1).getMemberId()).isEqualTo(getOtherMember().getId());

        verify(globalMemberQueryAdapter, times(1)).findByMemberName(memberName);
        verify(paperQueryRepository, times(1)).findMessagesByMemberName(memberName);
    }

    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - 메시지 없는 경우")
    void shouldReturnEmptyList_WhenNoMessages() {
        // Given
        String memberName = "userWithNoMessages";

        given(globalMemberQueryAdapter.findByMemberName(memberName)).willReturn(Optional.of(getTestMember()));
        given(paperQueryRepository.findMessagesByMemberName(memberName)).willReturn(Collections.emptyList());

        // When
        VisitPaperResult result = paperQueryService.visitPaper(getTestMember().getId(), memberName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.messages()).isEmpty();

        verify(globalMemberQueryAdapter, times(1)).findByMemberName(memberName);
        verify(paperQueryRepository, times(1)).findMessagesByMemberName(memberName);
    }



    @Test
    @DisplayName("다른 사용자 롤링페이퍼 방문 - null 또는 빈 사용자명 예외")
    void shouldThrowException_WhenInvalidUserName() {
        // Given - null memberName
        String memberName = null;

        // When & Then - null case
        assertThatThrownBy(() -> paperQueryService.visitPaper(getTestMember().getId(), memberName))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_INVALID_INPUT_VALUE);

        // Given - empty memberName
        String emptyUserName = "   ";

        // When & Then - empty case
        assertThatThrownBy(() -> paperQueryService.visitPaper(1L, emptyUserName))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAPER_INVALID_INPUT_VALUE);

        verify(paperQueryRepository, never()).findMessagesByMemberName(any());
    }

}
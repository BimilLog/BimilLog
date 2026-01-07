package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * <h2>CheckBlacklistListener 단위 테스트</h2>
 * <p>블랙리스트 체크 이벤트 리스너의 동작을 검증하는 단위 테스트</p>
 *
 * @version 2.4.0
 */
@DisplayName("CheckBlacklistListener 단위 테스트")
@Tag("unit")
class CheckBlacklistListenerTest extends BaseUnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long TARGET_MEMBER_ID = 2L;

    @Mock
    private MemberBlacklistService memberBlacklistService;

    @InjectMocks
    private CheckBlacklistListener checkBlacklistListener;

    @Test
    @DisplayName("블랙리스트 체크 - 블랙리스트 관계가 아닌 경우 정상 처리")
    void shouldCheckBlacklist_WhenNotBlacklisted() {
        // Given
        CheckBlacklistEvent event = new CheckBlacklistEvent(MEMBER_ID, TARGET_MEMBER_ID);
        doNothing().when(memberBlacklistService).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);

        // When
        checkBlacklistListener.checkBlacklist(event);

        // Then
        verify(memberBlacklistService, times(1)).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);
    }

    @Test
    @DisplayName("블랙리스트 체크 - 블랙리스트 관계인 경우 예외 발생")
    void shouldThrowException_WhenBlacklisted() {
        // Given
        CheckBlacklistEvent event = new CheckBlacklistEvent(MEMBER_ID, TARGET_MEMBER_ID);
        willThrow(new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN))
                .given(memberBlacklistService).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);

        // When & Then
        assertThatThrownBy(() -> checkBlacklistListener.checkBlacklist(event))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);

        verify(memberBlacklistService, times(1)).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);
    }

    @Test
    @DisplayName("블랙리스트 체크 - A가 B를 차단한 경우 예외 발생")
    void shouldThrowException_WhenABlockedB() {
        // Given
        CheckBlacklistEvent event = new CheckBlacklistEvent(MEMBER_ID, TARGET_MEMBER_ID);
        willThrow(new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN))
                .given(memberBlacklistService).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);

        // When & Then
        assertThatThrownBy(() -> checkBlacklistListener.checkBlacklist(event))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);

        verify(memberBlacklistService, times(1)).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);
    }

    @Test
    @DisplayName("블랙리스트 체크 - B가 A를 차단한 경우 예외 발생")
    void shouldThrowException_WhenBBlockedA() {
        // Given
        CheckBlacklistEvent event = new CheckBlacklistEvent(MEMBER_ID, TARGET_MEMBER_ID);
        willThrow(new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN))
                .given(memberBlacklistService).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);

        // When & Then
        assertThatThrownBy(() -> checkBlacklistListener.checkBlacklist(event))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);

        verify(memberBlacklistService, times(1)).checkMemberBlacklist(MEMBER_ID, TARGET_MEMBER_ID);
    }
}

package jaeik.bimillog.domain.paper.entity;

import jaeik.bimillog.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>Message 엔티티 테스트</h2>
 * <p>도메인 규칙이 개입되는 핵심 동작만 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Message 엔티티 테스트")
@Tag("unit")
class MessageTest {

    @Mock
    private Member member;

    @Test
    @DisplayName("팩토리 메서드가 필수 필드를 채운다")
    void shouldCreateMessageWithEssentialFields() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 3;
        int y = 4;

        // When
        Message message = Message.createMessage(member, decoType, anonymity, content, x, y);

        // Then
        assertThat(message.getMember()).isEqualTo(member);
        assertThat(message.getDecoType()).isEqualTo(decoType);
        assertThat(message.getAnonymity()).isEqualTo(anonymity);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getX()).isEqualTo(x);
        assertThat(message.getY()).isEqualTo(y);
    }

    @Test
    @DisplayName("소유자가 삭제를 요청하면 true")
    void shouldAllowDeleteWhenOwnerMatches() {
        // Given
        Long ownerId = 123L;
        given(member.getId()).willReturn(ownerId);
        Message message = Message.createMessage(member, DecoType.APPLE, "익명", "내용", 1, 1);

        // When
        boolean result = message.canBeDeletedBy(ownerId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("소유자가 아니면 삭제가 거부된다")
    void shouldRejectDeleteWhenOwnerDiffers() {
        // Given
        Long ownerId = 123L;
        Long requesterId = 999L;
        given(member.getId()).willReturn(ownerId);
        Message message = Message.createMessage(member, DecoType.APPLE, "익명", "내용", 1, 1);

        // When
        boolean result = message.canBeDeletedBy(requesterId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자 또는 요청 ID가 없으면 삭제가 거부된다")
    void shouldRejectDeleteWhenUserOrRequesterMissing() {
        // Given
        Message messageWithoutUser = Message.createMessage(null, DecoType.APPLE, "익명", "내용", 1, 1);
        Message messageWithUser = Message.createMessage(member, DecoType.APPLE, "익명", "내용", 1, 1);
        given(member.getId()).willReturn(10L);

        // When
        boolean withoutUser = messageWithoutUser.canBeDeletedBy(10L);
        boolean requesterNull = messageWithUser.canBeDeletedBy(null);

        // Then
        assertThat(withoutUser).isFalse();
        assertThat(requesterNull).isFalse();
    }

    @Test
    @DisplayName("getUserId는 사용자 ID를 그대로 반환한다")
    void shouldReturnUserIdWhenPresent() {
        // Given
        Long memberId = 456L;
        given(member.getId()).willReturn(memberId);
        Message message = Message.createMessage(member, DecoType.BANANA, "익명", "내용", 1, 1);

        // When
        Long actual = message.getMemberId();

        // Then
        assertThat(actual).isEqualTo(memberId);
    }

    @Test
    @DisplayName("사용자가 없으면 getUserId는 null")
    void shouldReturnNullWhenUserNotPresent() {
        // Given
        Message message = Message.createMessage(null, DecoType.BANANA, "익명", "내용", 1, 1);

        // When
        Long actual = message.getMemberId();

        // Then
        assertThat(actual).isNull();
    }
}

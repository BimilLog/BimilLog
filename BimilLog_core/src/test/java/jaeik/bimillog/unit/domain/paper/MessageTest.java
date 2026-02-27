package jaeik.bimillog.unit.domain.paper;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
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
    @DisplayName("빌더가 필수 필드를 채운다")
    void shouldCreateMessageWithEssentialFields() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 3;
        int y = 4;

        // When
        Message message = Message.builder()
                .member(member)
                .decoType(decoType)
                .anonymity(anonymity)
                .content(content)
                .x(x)
                .y(y)
                .build();

        // Then
        assertThat(message.getMember()).isEqualTo(member);
        assertThat(message.getDecoType()).isEqualTo(decoType);
        assertThat(message.getAnonymity()).isEqualTo(anonymity);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getX()).isEqualTo(x);
        assertThat(message.getY()).isEqualTo(y);
    }

    @Test
    @DisplayName("getMember().getId()는 사용자 ID를 그대로 반환한다")
    void shouldReturnUserIdWhenPresent() {
        // Given
        Long memberId = 456L;
        given(member.getId()).willReturn(memberId);
        Message message = Message.builder()
                .member(member)
                .decoType(DecoType.BANANA)
                .anonymity("익명")
                .content("내용")
                .x(1)
                .y(1)
                .build();

        // When
        Long actual = message.getMember().getId();

        // Then
        assertThat(actual).isEqualTo(memberId);
    }

    @Test
    @DisplayName("사용자가 없으면 getMember()는 null")
    void shouldReturnNullWhenUserNotPresent() {
        // Given
        Message message = Message.builder()
                .member(null)
                .decoType(DecoType.BANANA)
                .anonymity("익명")
                .content("내용")
                .x(1)
                .y(1)
                .build();

        // When & Then
        assertThat(message.getMember()).isNull();
    }
}

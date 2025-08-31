package jaeik.bimillog.domain.paper.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * <h2>MessageCommand 도메인 비즈니스 규칙 검증 테스트</h2>
 * <p>MessageCommand의 생성자에서 수행하는 비즈니스 규칙 검증을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
class MessageCommandTest {

    @Test
    @DisplayName("정상 케이스 - 유효한 데이터로 MessageCommand 생성")
    void shouldCreateMessageCommand_WhenValidDataProvided() {
        // Given: 유효한 데이터
        String validAnonymity = "테스트유저";
        String validContent = "안녕하세요! 테스트 메시지입니다.";

        // When & Then: 정상 생성
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                validAnonymity,
                validContent,
                2,
                3
        ));
    }

    @Test
    @DisplayName("경계값 테스트 - 익명 이름 8글자 정확히")
    void shouldCreateMessageCommand_WhenAnonymityExactly8Characters() {
        // Given: 정확히 8글자인 익명 이름
        String exactly8Chars = "12345678";

        // When & Then: 정상 생성
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                exactly8Chars,
                "테스트 내용",
                2,
                3
        ));
    }

    @Test
    @DisplayName("비즈니스 규칙 위반 - 익명 이름 8글자 초과")
    void shouldThrowException_WhenAnonymityExceeds8Characters() {
        // Given: 8글자를 초과하는 익명 이름
        String over8Chars = "123456789"; // 9글자

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                over8Chars,
                "테스트 내용",
                2,
                3
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("익명 이름은 최대 8글자까지 입력 가능합니다.");
    }

    @Test
    @DisplayName("경계값 테스트 - 내용 255자 정확히")
    void shouldCreateMessageCommand_WhenContentExactly255Characters() {
        // Given: 정확히 255자인 내용
        String exactly255Chars = "a".repeat(255);

        // When & Then: 정상 생성
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                "테스트유저",
                exactly255Chars,
                2,
                3
        ));
    }

    @Test
    @DisplayName("비즈니스 규칙 위반 - 내용 255자 초과")
    void shouldThrowException_WhenContentExceeds255Characters() {
        // Given: 255자를 초과하는 내용
        String over255Chars = "a".repeat(256); // 256글자

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                "테스트유저",
                over255Chars,
                2,
                3
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("내용은 최대 255자까지 입력 가능합니다.");
    }

    @Test
    @DisplayName("Null 처리 - null 익명 이름 허용")
    void shouldCreateMessageCommand_WhenAnonymityIsNull() {
        // Given: null 익명 이름
        String nullAnonymity = null;

        // When & Then: 정상 생성 (null은 검증하지 않음)
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                nullAnonymity,
                "테스트 내용",
                2,
                3
        ));
    }

    @Test
    @DisplayName("Null 처리 - null 내용 허용")
    void shouldCreateMessageCommand_WhenContentIsNull() {
        // Given: null 내용
        String nullContent = null;

        // When & Then: 정상 생성 (null은 검증하지 않음)
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                "테스트유저",
                nullContent,
                2,
                3
        ));
    }

    @Test
    @DisplayName("팩토리 메서드 - ofCreate 정상 동작")
    void shouldCreateProperMessageCommand_WhenUsingOfCreateFactory() {
        // Given: 팩토리 메서드 파라미터
        Long userId = 100L;
        DecoType decoType = DecoType.TOMATO;
        String anonymity = "팩토리테스트";
        String content = "팩토리 메서드로 생성된 메시지입니다.";
        int width = 5;
        int height = 7;

        // When: ofCreate 팩토리 메서드 사용
        MessageCommand command = MessageCommand.ofCreate(
                userId, decoType, anonymity, content, width, height
        );

        // Then: 모든 필드가 올바르게 설정됨
        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.decoType()).isEqualTo(decoType);
        assertThat(command.anonymity()).isEqualTo(anonymity);
        assertThat(command.content()).isEqualTo(content);
        assertThat(command.width()).isEqualTo(width);
        assertThat(command.height()).isEqualTo(height);
        assertThat(command.id()).isNull(); // 생성 시에는 ID 없음
    }

    @Test
    @DisplayName("팩토리 메서드 - ofDelete 정상 동작")
    void shouldCreateProperMessageCommand_WhenUsingOfDeleteFactory() {
        // Given: 삭제용 팩토리 메서드 파라미터
        Long id = 200L;
        Long userId = 300L;

        // When: ofDelete 팩토리 메서드 사용
        MessageCommand command = MessageCommand.ofDelete(id, userId);

        // Then: 삭제용 필드만 설정됨
        assertThat(command.id()).isEqualTo(id);
        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.decoType()).isNull();
        assertThat(command.anonymity()).isNull();
        assertThat(command.content()).isNull();
        assertThat(command.width()).isEqualTo(0);
        assertThat(command.height()).isEqualTo(0);
    }

    @Test
    @DisplayName("복합 검증 - 익명 이름과 내용 모두 초과")
    void shouldThrowException_WhenBothAnonymityAndContentExceedLimits() {
        // Given: 둘 다 길이 초과
        String over8CharsAnonymity = "123456789";
        String over255CharsContent = "a".repeat(256);

        // When & Then: 익명 이름 검증이 먼저 실행되어 해당 예외 발생
        assertThatThrownBy(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                over8CharsAnonymity,
                over255CharsContent,
                2,
                3
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("익명 이름은 최대 8글자까지 입력 가능합니다.");
    }

    @Test
    @DisplayName("한글 문자 처리 - 한글 익명 이름 8자")
    void shouldHandleKoreanCharacters_InAnonymityField() {
        // Given: 한글 8글자 익명 이름
        String koreanAnonymity = "가나다라마바사아"; // 8글자

        // When & Then: 정상 생성
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                koreanAnonymity,
                "한글 내용 테스트",
                2,
                3
        ));
    }

    @Test
    @DisplayName("한글 문자 처리 - 한글 내용 255자")
    void shouldHandleKoreanCharacters_InContentField() {
        // Given: 한글 255자 내용
        String koreanContent = "가".repeat(255);

        // When & Then: 정상 생성
        assertDoesNotThrow(() -> MessageCommand.ofCreate(
                1L,
                DecoType.POTATO,
                "테스트유저",
                koreanContent,
                2,
                3
        ));
    }
}
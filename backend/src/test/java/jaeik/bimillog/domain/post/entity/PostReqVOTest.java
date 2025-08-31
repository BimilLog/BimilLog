package jaeik.bimillog.domain.post.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>PostReqVO 테스트</h2>
 * <p>게시글 요청 값 객체의 도메인 규칙을 검증하는 단위 테스트</p>
 * <p>서비스 테스트와 분리하여 단일 책임 원칙을 준수</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("PostReqVO 테스트")
class PostReqVOTest {

    @Test
    @DisplayName("정상적인 게시글 요청 값 객체 생성")
    void shouldCreatePostReqVO_WhenValidInput() {
        // Given
        String title = "테스트 제목";
        String content = "테스트 내용";
        Integer password = 1234;

        // When
        PostReqVO postReqVO = PostReqVO.builder()
                .title(title)
                .content(content)
                .password(password)
                .build();

        // Then
        assertThat(postReqVO.title()).isEqualTo(title);
        assertThat(postReqVO.content()).isEqualTo(content);
        assertThat(postReqVO.password()).isEqualTo(password);
    }

    @Test
    @DisplayName("비밀번호 없는 게시글 요청 값 객체 생성")
    void shouldCreatePostReqVO_WhenNoPassword() {
        // Given
        String title = "테스트 제목";
        String content = "테스트 내용";

        // When
        PostReqVO postReqVO = PostReqVO.builder()
                .title(title)
                .content(content)
                .build();

        // Then
        assertThat(postReqVO.title()).isEqualTo(title);
        assertThat(postReqVO.content()).isEqualTo(content);
        assertThat(postReqVO.password()).isNull();
    }

    @Test
    @DisplayName("정적 팩토리 메서드 - of(title, content)")
    void shouldCreatePostReqVO_UsingFactoryMethod() {
        // Given
        String title = "팩토리 제목";
        String content = "팩토리 내용";

        // When
        PostReqVO postReqVO = PostReqVO.of(title, content);

        // Then
        assertThat(postReqVO.title()).isEqualTo(title);
        assertThat(postReqVO.content()).isEqualTo(content);
        assertThat(postReqVO.password()).isNull();
    }

    @Test
    @DisplayName("정적 팩토리 메서드 - of(title, content, password)")
    void shouldCreatePostReqVO_UsingFactoryMethodWithPassword() {
        // Given
        String title = "팩토리 제목";
        String content = "팩토리 내용";
        Integer password = 5678;

        // When
        PostReqVO postReqVO = PostReqVO.of(title, content, password);

        // Then
        assertThat(postReqVO.title()).isEqualTo(title);
        assertThat(postReqVO.content()).isEqualTo(content);
        assertThat(postReqVO.password()).isEqualTo(password);
    }

    @Test
    @DisplayName("빈 제목 예외")
    void shouldThrowException_WhenEmptyTitle() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.builder()
                .title("")
                .content("내용")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 제목은 필수입니다.");
    }

    @Test
    @DisplayName("null 제목 예외")
    void shouldThrowException_WhenNullTitle() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.builder()
                .title(null)
                .content("내용")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 제목은 필수입니다.");
    }

    @Test
    @DisplayName("공백만 있는 제목 예외")
    void shouldThrowException_WhenWhitespaceTitle() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.builder()
                .title("   ")
                .content("내용")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 제목은 필수입니다.");
    }

    @Test
    @DisplayName("빈 내용 예외")
    void shouldThrowException_WhenEmptyContent() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.builder()
                .title("제목")
                .content("")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 내용은 필수입니다.");
    }

    @Test
    @DisplayName("null 내용 예외")
    void shouldThrowException_WhenNullContent() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.builder()
                .title("제목")
                .content(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 내용은 필수입니다.");
    }

    @Test
    @DisplayName("공백만 있는 내용 예외")
    void shouldThrowException_WhenWhitespaceContent() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.builder()
                .title("제목")
                .content("   ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 내용은 필수입니다.");
    }

    @Test
    @DisplayName("긴 제목과 내용 처리")
    void shouldHandleLongTitleAndContent() {
        // Given
        String longTitle = "A".repeat(255);
        String longContent = "B".repeat(5000);

        // When
        PostReqVO postReqVO = PostReqVO.builder()
                .title(longTitle)
                .content(longContent)
                .build();

        // Then
        assertThat(postReqVO.title()).isEqualTo(longTitle);
        assertThat(postReqVO.content()).isEqualTo(longContent);
        assertThat(postReqVO.title()).hasSize(255);
        assertThat(postReqVO.content()).hasSize(5000);
    }

    @Test
    @DisplayName("정적 팩토리 메서드 - 빈 제목 예외")
    void shouldThrowException_WhenEmptyTitleInFactoryMethod() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.of("", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 제목은 필수입니다.");
    }

    @Test
    @DisplayName("정적 팩토리 메서드 - 빈 내용 예외")
    void shouldThrowException_WhenEmptyContentInFactoryMethod() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.of("제목", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 내용은 필수입니다.");
    }

    @Test
    @DisplayName("비밀번호 포함 정적 팩토리 메서드 - 빈 제목 예외")
    void shouldThrowException_WhenEmptyTitleInFactoryMethodWithPassword() {
        // When & Then
        assertThatThrownBy(() -> PostReqVO.of("", "내용", 1234))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 제목은 필수입니다.");
    }

    @Test
    @DisplayName("record 불변성 검증")
    void shouldBeImmutable() {
        // Given
        PostReqVO postReqVO = PostReqVO.of("제목", "내용", 1234);

        // When & Then - record는 기본적으로 불변이므로 getter만 존재
        assertThat(postReqVO.title()).isNotNull();
        assertThat(postReqVO.content()).isNotNull();
        assertThat(postReqVO.password()).isNotNull();

        // 값 변경 불가능 (setter가 없음을 암시적으로 검증)
        assertThat(PostReqVO.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().startsWith("set"));
    }

    @Test
    @DisplayName("equals와 hashCode 검증")
    void shouldHaveCorrectEqualsAndHashCode() {
        // Given
        PostReqVO postReqVO1 = PostReqVO.of("제목", "내용", 1234);
        PostReqVO postReqVO2 = PostReqVO.of("제목", "내용", 1234);
        PostReqVO postReqVO3 = PostReqVO.of("다른제목", "내용", 1234);

        // When & Then
        assertThat(postReqVO1).isEqualTo(postReqVO2);
        assertThat(postReqVO1).isNotEqualTo(postReqVO3);
        assertThat(postReqVO1.hashCode()).isEqualTo(postReqVO2.hashCode());
    }

    @Test
    @DisplayName("toString 검증")
    void shouldHaveCorrectToString() {
        // Given
        PostReqVO postReqVO = PostReqVO.of("제목", "내용", 1234);

        // When
        String toString = postReqVO.toString();

        // Then
        assertThat(toString).contains("제목");
        assertThat(toString).contains("내용");
        assertThat(toString).contains("1234");
        assertThat(toString).contains("PostReqVO");
    }
}
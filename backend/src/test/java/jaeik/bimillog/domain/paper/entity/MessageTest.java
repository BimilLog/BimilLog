package jaeik.bimillog.domain.paper.entity;

import jaeik.bimillog.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

/**
 * <h2>Message 엔티티 테스트</h2>
 * <p>Message 엔티티의 도메인 비즈니스 규칙 검증을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Message 엔티티 테스트")
class MessageTest {

    @Mock
    private User user;

    @Test
    @DisplayName("정상 케이스 - 유효한 데이터로 Message 생성 및 필드 검증")
    void shouldCreateMessage_WhenValidDataProvided() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        // When
        Message message = Message.createMessage(user, decoType, anonymity, content, x, y);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getUser()).isEqualTo(user);
        assertThat(message.getDecoType()).isEqualTo(decoType);
        assertThat(message.getAnonymity()).isEqualTo(anonymity);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getX()).isEqualTo(x);
        assertThat(message.getY()).isEqualTo(y);
    }

    @Test
    @DisplayName("정상 케이스 - 익명 이름 정확히 8글자")
    void shouldCreateMessage_WhenAnonymityExactly8Characters() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = "12345678"; // 정확히 8글자
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        // When & Then
        assertDoesNotThrow(() -> Message.createMessage(
                user, decoType, anonymity, content, x, y));
    }


    @Test
    @DisplayName("정상 케이스 - 내용 정확히 255글자")
    void shouldCreateMessage_WhenContentExactly255Characters() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = "A".repeat(255); // 정확히 255글자
        int x = 2;
        int y = 2;

        // When & Then
        assertDoesNotThrow(() -> Message.createMessage(
                user, decoType, anonymity, content, x, y));
    }


    @Test
    @DisplayName("정상 케이스 - 익명 이름이 null")
    void shouldCreateMessage_WhenAnonymityIsNull() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = null;
        String content = "테스트 메시지";
        int x = 2;
        int y = 2;

        // When & Then
        assertDoesNotThrow(() -> Message.createMessage(
                user, decoType, anonymity, content, x, y));
    }

    @Test
    @DisplayName("정상 케이스 - 내용이 null")
    void shouldCreateMessage_WhenContentIsNull() {
        // Given
        DecoType decoType = DecoType.APPLE;
        String anonymity = "익명";
        String content = null;
        int x = 2;
        int y = 2;

        // When & Then
        assertDoesNotThrow(() -> Message.createMessage(
                user, decoType, anonymity, content, x, y));
    }

    @Test
    @DisplayName("삭제 권한 확인 - 소유자인 경우 true")
    void shouldReturnTrue_WhenUserIsOwner() {
        // Given
        Long userId = 123L;
        given(user.getId()).willReturn(userId);
        
        Message message = Message.builder()
                .user(user)
                .decoType(DecoType.APPLE)
                .anonymity("익명")
                .content("테스트")
                .x(2)
                .y(2)
                .build();

        // When
        boolean canDelete = message.canBeDeletedBy(userId);

        // Then
        assertThat(canDelete).isTrue();
    }

    @Test
    @DisplayName("삭제 권한 확인 - 소유자가 아닌 경우 false")
    void shouldReturnFalse_WhenUserIsNotOwner() {
        // Given
        Long ownerId = 123L;
        Long requesterId = 456L;
        given(user.getId()).willReturn(ownerId);
        
        Message message = Message.builder()
                .user(user)
                .decoType(DecoType.APPLE)
                .anonymity("익명")
                .content("테스트")
                .x(2)
                .y(2)
                .build();

        // When
        boolean canDelete = message.canBeDeletedBy(requesterId);

        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    @DisplayName("삭제 권한 확인 - 사용자가 null인 경우 false")
    void shouldReturnFalse_WhenUserIsNull() {
        // Given
        Message message = Message.builder()
                .user(null)
                .decoType(DecoType.APPLE)
                .anonymity("익명")
                .content("테스트")
                .x(2)
                .y(2)
                .build();

        // When
        boolean canDelete = message.canBeDeletedBy(123L);

        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    @DisplayName("삭제 권한 확인 - 요청 사용자 ID가 null인 경우 false")
    void shouldReturnFalse_WhenRequesterIdIsNull() {
        // Given
        Long userId = 123L;
        given(user.getId()).willReturn(userId);
        
        Message message = Message.builder()
                .user(user)
                .decoType(DecoType.APPLE)
                .anonymity("익명")
                .content("테스트")
                .x(2)
                .y(2)
                .build();

        // When
        boolean canDelete = message.canBeDeletedBy(null);

        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    @DisplayName("다양한 DecoType으로 Message 생성 성공")
    void shouldCreateMessage_WithVariousDecoTypes() {
        // Given & When & Then
        for (DecoType decoType : DecoType.values()) {
            Message message = Message.createMessage(user, decoType, "테스트", "내용", 1, 1);
            
            assertThat(message).isNotNull();
            assertThat(message.getDecoType()).isEqualTo(decoType);
            assertThat(message.getUser()).isEqualTo(user);
        }
    }

    @Test
    @DisplayName("경계값 테스트 - 최소 크기 (1x1)")
    void shouldCreateMessage_WithMinimumSize() {
        // Given
        int minX = 1;
        int minY = 1;

        // When
        Message message = Message.createMessage(user, DecoType.APPLE, "익명", "테스트", minX, minY);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getX()).isEqualTo(minX);
        assertThat(message.getY()).isEqualTo(minY);
    }

    @Test
    @DisplayName("경계값 테스트 - 큰 크기")
    void shouldCreateMessage_WithLargeSize() {
        // Given
        int largeX = 100;
        int largeY = 100;

        // When
        Message message = Message.createMessage(user, DecoType.APPLE, "익명", "테스트", largeX, largeY);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getX()).isEqualTo(largeX);
        assertThat(message.getY()).isEqualTo(largeY);
    }

    @Test
    @DisplayName("빈 문자열로 Message 생성")
    void shouldCreateMessage_WithEmptyStrings() {
        // Given
        String emptyAnonymity = "";
        String emptyContent = "";

        // When
        Message message = Message.createMessage(user, DecoType.APPLE, emptyAnonymity, emptyContent, 2, 2);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getAnonymity()).isEqualTo(emptyAnonymity);
        assertThat(message.getContent()).isEqualTo(emptyContent);
    }

    @Test
    @DisplayName("getUserId 메서드 - 정상적으로 사용자 ID 반환")
    void shouldReturnUserId_WhenUserExists() {
        // Given
        Long expectedUserId = 456L;
        given(user.getId()).willReturn(expectedUserId);
        
        Message message = Message.builder()
                .user(user)
                .decoType(DecoType.BANANA)
                .anonymity("익명")
                .content("테스트")
                .x(3)
                .y(3)
                .build();

        // When
        Long actualUserId = message.getUserId();

        // Then
        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("getUserId 메서드 - 사용자가 null인 경우 null 반환")
    void shouldReturnNull_WhenUserIsNull() {
        // Given
        Message message = Message.builder()
                .user(null)
                .decoType(DecoType.GRAPE)
                .anonymity("익명")
                .content("테스트")
                .x(2)
                .y(2)
                .build();

        // When
        Long userId = message.getUserId();

        // Then
        assertThat(userId).isNull();
    }
}
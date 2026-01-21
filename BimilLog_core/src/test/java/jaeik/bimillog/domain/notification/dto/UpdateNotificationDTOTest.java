package jaeik.bimillog.domain.notification.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>UpdateNotificationDTO 테스트</h2>
 * <p>알림 업데이트 요청 DTO의 검증 및 필터링 로직을 테스트합니다.</p>
 * <p>DTO 계층에서 수행하는 ID 검증, 중복 제거, 삭제 우선 정책을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("UpdateNotificationDTO 테스트")
@Tag("unit")
class UpdateNotificationDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("JSON 역직렬화 시 ID 필터링 및 중복 제거")
    void shouldFilterAndDeduplicateIds_WhenDeserializing() throws Exception {
        // Given
        String json = """
                {
                    "readIds": [1, 2, 2, 3, null, 0, -1],
                    "deletedIds": [4, 5, 5, null, 0, -2]
                }
                """;

        // When
        UpdateNotificationDTO dto = objectMapper.readValue(json, UpdateNotificationDTO.class);

        // Then
        assertThat(dto.getReadIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(dto.getDeletedIds()).containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    @DisplayName("삭제 우선 정책 - 동일 ID가 있을 경우 읽음에서 제거")
    void shouldApplyDeletionPriority_WhenOverlappingIds() throws Exception {
        // Given
        String json = """
                {
                    "readIds": [1, 2, 3],
                    "deletedIds": [2, 3, 4]
                }
                """;

        // When
        UpdateNotificationDTO dto = objectMapper.readValue(json, UpdateNotificationDTO.class);

        // Then
        assertThat(dto.getReadIds()).containsExactly(1L);
        assertThat(dto.getDeletedIds()).containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @ParameterizedTest
    @MethodSource("provideEdgeCaseScenarios")
    @DisplayName("다양한 입력 처리 - 빈 리스트, null, 대량 데이터")
    void shouldHandleVariousInputs_WhenDeserializing(String json, int expectedReadSize, int expectedDeletedSize) throws Exception {
        // When
        UpdateNotificationDTO dto = objectMapper.readValue(json, UpdateNotificationDTO.class);

        // Then
        assertThat(dto.getReadIds()).hasSize(expectedReadSize);
        assertThat(dto.getDeletedIds()).hasSize(expectedDeletedSize);
    }

    static Stream<Arguments> provideEdgeCaseScenarios() {
        String emptyListJson = """
                {
                    "readIds": [],
                    "deletedIds": []
                }
                """;

        String nullFieldsJson = """
                {
                    "readIds": null,
                    "deletedIds": null
                }
                """;

        List<Long> largeReadIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        List<Long> largeDeletedIds = Arrays.asList(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
        String largeDataJson = String.format("""
                {
                    "readIds": [%s],
                    "deletedIds": [%s]
                }
                """,
                String.join(", ", largeReadIds.stream().map(String::valueOf).toArray(String[]::new)),
                String.join(", ", largeDeletedIds.stream().map(String::valueOf).toArray(String[]::new)));

        return Stream.of(
            Arguments.of(emptyListJson, 0, 0),      // 빈 리스트
            Arguments.of(nullFieldsJson, 0, 0),     // null 필드
            Arguments.of(largeDataJson, 10, 10)     // 대량 데이터
        );
    }

    @Test
    @DisplayName("최소 하나의 작업 필수 검증 - 성공")
    void shouldPassValidation_WhenAtLeastOneOperationRequested() {
        // Given
        UpdateNotificationDTO dto = new UpdateNotificationDTO();
        dto.setReadIds(Arrays.asList(1L, 2L));
        dto.setDeletedIds(null);

        // When & Then
        assertThat(dto.isAtLeastOneOperationRequested()).isTrue();
    }

    @Test
    @DisplayName("최소 하나의 작업 필수 검증 - 실패")
    void shouldFailValidation_WhenNoOperationRequested() {
        // Given
        UpdateNotificationDTO dto = new UpdateNotificationDTO();
        dto.setReadIds(null);
        dto.setDeletedIds(null);

        // When & Then
        assertThat(dto.isAtLeastOneOperationRequested()).isFalse();
    }
}
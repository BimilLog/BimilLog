package jaeik.bimillog.infrastructure.adapter.out.sse;

import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.SseTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>EmitterRepositoryImpl 테스트</h2>
 * <p>SSE Emitter Repository 구현체의 동작을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("EmitterRepositoryImpl 테스트")
class EmitterRepositoryImplTest extends BaseUnitTest {

    private EmitterRepositoryImpl emitterRepository;
    private SseEmitter emitter1;
    private SseEmitter emitter2;

    @BeforeEach
    void setUp() {
        emitterRepository = new EmitterRepositoryImpl();
        emitter1 = SseTestHelper.createMockEmitter();
        emitter2 = SseTestHelper.createMockEmitter();
    }

    @Test
    @DisplayName("Emitter 저장 - 성공")
    void shouldSaveEmitter_WhenValidInput() {
        // Given
        String emitterId = SseTestHelper.createEmitterId(1L, 100L, 1234567890L);

        // When
        SseEmitter savedEmitter = emitterRepository.save(emitterId, emitter1);

        // Then
        assertThat(savedEmitter).isEqualTo(emitter1);
    }

    @Test
    @DisplayName("사용자 ID로 모든 Emitter 조회 - 성공")
    void shouldFindAllEmitterByUserId_WhenEmittersExist() {
        // Given
        Long userId = 1L;
        String emitterId1 = SseTestHelper.createEmitterId(userId, 100L);
        String emitterId2 = SseTestHelper.createEmitterId(userId, 101L);
        
        emitterRepository.save(emitterId1, emitter1);
        emitterRepository.save(emitterId2, emitter2);

        // When
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);

        // Then
        assertThat(emitters).hasSize(2);
        assertThat(emitters).containsEntry(emitterId1, emitter1);
        assertThat(emitters).containsEntry(emitterId2, emitter2);
    }

    @Test
    @DisplayName("사용자 ID로 모든 Emitter 조회 - 해당하는 Emitter가 없는 경우")
    void shouldReturnEmptyMap_WhenNoEmittersForUser() {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;
        String emitterId = SseTestHelper.createEmitterId(otherUserId, 100L);
        
        emitterRepository.save(emitterId, emitter1);

        // When
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);

        // Then
        assertThat(emitters).isEmpty();
    }

    @Test
    @DisplayName("Emitter ID로 삭제 - 성공")
    void shouldDeleteById_WhenEmitterExists() {
        // Given
        Long userId = 1L;
        String emitterId = SseTestHelper.defaultEmitterId(userId);
        emitterRepository.save(emitterId, emitter1);

        // When
        emitterRepository.deleteById(emitterId);

        // Then
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);
        assertThat(emitters).isEmpty();
    }

    @Test
    @DisplayName("Emitter ID로 삭제 - 존재하지 않는 Emitter")
    void shouldNotThrowException_WhenDeletingNonExistentEmitter() {
        // Given
        String nonExistentEmitterId = "999_999_9999999999";

        // When & Then
        // 예외가 발생하지 않아야 함
        emitterRepository.deleteById(nonExistentEmitterId);
    }

    @Test
    @DisplayName("사용자 ID로 모든 Emitter 삭제 - 성공")
    void shouldDeleteAllEmitterByUserId_WhenEmittersExist() {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;
        Map<String, SseEmitter> userEmitters = SseTestHelper.createMultiDeviceEmitters(userId, 2);
        String otherUserEmitterId = SseTestHelper.createEmitterId(otherUserId, 100L);
        
        // 사용자 emitter 저장
        userEmitters.forEach((id, emitter) -> emitterRepository.save(id, emitter));
        emitterRepository.save(otherUserEmitterId, SseTestHelper.createMockEmitter());

        // When
        emitterRepository.deleteAllEmitterByUserId(userId);

        // Then
        Map<String, SseEmitter> remainingUserEmitters = emitterRepository.findAllEmitterByUserId(userId);
        Map<String, SseEmitter> otherUserEmitters = emitterRepository.findAllEmitterByUserId(otherUserId);
        
        assertThat(remainingUserEmitters).isEmpty();
        assertThat(otherUserEmitters).hasSize(1); // 다른 사용자의 Emitter는 삭제되지 않아야 함
    }

    @Test
    @DisplayName("사용자 ID로 모든 Emitter 삭제 - 해당하는 Emitter가 없는 경우")
    void shouldNotThrowException_WhenDeletingEmittersForUserWithNoEmitters() {
        // Given
        Long userId = 1L;

        // When & Then
        // 예외가 발생하지 않아야 함
        emitterRepository.deleteAllEmitterByUserId(userId);
    }

    @Test
    @DisplayName("Emitter ID 패턴 검증 - 다양한 사용자 ID 패턴")
    void shouldHandleDifferentUserIdPatterns() {
        // Given
        String emitterId1 = "123_100_1234567890";
        String emitterId2 = "456_101_1234567891";
        String emitterId3 = "123_102_1234567892";
        
        emitterRepository.save(emitterId1, emitter1);
        emitterRepository.save(emitterId2, emitter2);
        emitterRepository.save(emitterId3, new SseEmitter());

        // When
        Map<String, SseEmitter> user123Emitters = emitterRepository.findAllEmitterByUserId(123L);
        Map<String, SseEmitter> user456Emitters = emitterRepository.findAllEmitterByUserId(456L);

        // Then
        assertThat(user123Emitters).hasSize(2);
        assertThat(user456Emitters).hasSize(1);
    }

    @Test
    @DisplayName("동일한 Emitter ID로 재저장 시 덮어쓰기")
    void shouldOverwriteEmitter_WhenSameEmitterIdUsed() {
        // Given
        String emitterId = "1_100_1234567890";
        SseEmitter newEmitter = new SseEmitter();
        
        emitterRepository.save(emitterId, emitter1);

        // When
        SseEmitter savedEmitter = emitterRepository.save(emitterId, newEmitter);

        // Then
        assertThat(savedEmitter).isEqualTo(newEmitter);
        
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(1L);
        assertThat(emitters).hasSize(1);
        assertThat(emitters.get(emitterId)).isEqualTo(newEmitter);
    }

    @Test
    @DisplayName("경계값 테스트 - 유사한 사용자 ID로 잘못 매칭되지 않는지 검증")
    void shouldNotMatchSimilarUserIds() {
        // Given
        String emitterId1 = "1_100_1234567890";    // userId = 1
        String emitterId10 = "10_100_1234567890";  // userId = 10
        String emitterId11 = "11_100_1234567890";  // userId = 11
        String emitterId123 = "123_100_1234567890"; // userId = 123
        
        emitterRepository.save(emitterId1, new SseEmitter());
        emitterRepository.save(emitterId10, new SseEmitter());
        emitterRepository.save(emitterId11, new SseEmitter());
        emitterRepository.save(emitterId123, new SseEmitter());

        // When & Then
        Map<String, SseEmitter> user1Emitters = emitterRepository.findAllEmitterByUserId(1L);
        Map<String, SseEmitter> user10Emitters = emitterRepository.findAllEmitterByUserId(10L);
        Map<String, SseEmitter> user11Emitters = emitterRepository.findAllEmitterByUserId(11L);
        Map<String, SseEmitter> user123Emitters = emitterRepository.findAllEmitterByUserId(123L);

        // 각 사용자는 자신의 Emitter만 조회되어야 함
        assertThat(user1Emitters).hasSize(1);
        assertThat(user1Emitters).containsKey(emitterId1);

        assertThat(user10Emitters).hasSize(1);
        assertThat(user10Emitters).containsKey(emitterId10);

        assertThat(user11Emitters).hasSize(1);
        assertThat(user11Emitters).containsKey(emitterId11);

        assertThat(user123Emitters).hasSize(1);
        assertThat(user123Emitters).containsKey(emitterId123);
    }

    @Test
    @DisplayName("경계값 테스트 - 사용자 ID 삭제 시 유사한 ID 영향받지 않음")
    void shouldNotDeleteSimilarUserIds_WhenDeletingByUserId() {
        // Given
        String emitterId1 = "1_100_1234567890";    // userId = 1
        String emitterId10 = "10_100_1234567890";  // userId = 10
        String emitterId11 = "11_100_1234567890";  // userId = 11
        String emitterId123 = "123_100_1234567890"; // userId = 123
        
        emitterRepository.save(emitterId1, new SseEmitter());
        emitterRepository.save(emitterId10, new SseEmitter());
        emitterRepository.save(emitterId11, new SseEmitter());
        emitterRepository.save(emitterId123, new SseEmitter());

        // When - userId=1의 Emitter만 삭제
        emitterRepository.deleteAllEmitterByUserId(1L);

        // Then - userId=1만 삭제되고 나머지는 유지되어야 함
        Map<String, SseEmitter> user1Emitters = emitterRepository.findAllEmitterByUserId(1L);
        Map<String, SseEmitter> user10Emitters = emitterRepository.findAllEmitterByUserId(10L);
        Map<String, SseEmitter> user11Emitters = emitterRepository.findAllEmitterByUserId(11L);
        Map<String, SseEmitter> user123Emitters = emitterRepository.findAllEmitterByUserId(123L);

        assertThat(user1Emitters).isEmpty(); // userId=1은 삭제됨
        assertThat(user10Emitters).hasSize(1); // userId=10은 유지됨
        assertThat(user11Emitters).hasSize(1); // userId=11은 유지됨
        assertThat(user123Emitters).hasSize(1); // userId=123은 유지됨
    }

    @Test
    @DisplayName("특정 사용자 ID와 토큰 ID로 Emitter 삭제 - 성공")
    void shouldDeleteEmitterByUserIdAndTokenId_WhenEmitterExists() {
        // Given
        Long userId = 1L;
        Long tokenId = 100L;
        String targetEmitterId = "1_100_1234567890";
        String otherEmitterId = "1_101_1234567891";
        
        emitterRepository.save(targetEmitterId, emitter1);
        emitterRepository.save(otherEmitterId, emitter2);

        // When
        emitterRepository.deleteEmitterByUserIdAndTokenId(userId, tokenId);

        // Then
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);
        assertThat(emitters).hasSize(1);
        assertThat(emitters).containsKey(otherEmitterId); // 다른 토큰의 Emitter는 유지
        assertThat(emitters).doesNotContainKey(targetEmitterId); // 특정 토큰의 Emitter는 삭제
    }

    @Test
    @DisplayName("특정 사용자 ID와 토큰 ID로 Emitter 삭제 - 다중 기기 시나리오")
    void shouldDeleteEmitterByUserIdAndTokenId_MultiDeviceScenario() {
        // Given
        Long userId = 1L;
        Long tokenIdA = 100L; // A 기기
        Long tokenIdB = 101L; // B 기기
        Long tokenIdC = 102L; // C 기기
        
        String emitterIdA1 = SseTestHelper.createEmitterId(userId, tokenIdA, 1234567890L); // A 기기 - 연결1
        String emitterIdA2 = SseTestHelper.createEmitterId(userId, tokenIdA, 1234567891L); // A 기기 - 연결2
        String emitterIdB = SseTestHelper.createEmitterId(userId, tokenIdB, 1234567892L);  // B 기기
        String emitterIdC = SseTestHelper.createEmitterId(userId, tokenIdC, 1234567893L);  // C 기기
        
        emitterRepository.save(emitterIdA1, SseTestHelper.createMockEmitter());
        emitterRepository.save(emitterIdA2, SseTestHelper.createMockEmitter());
        emitterRepository.save(emitterIdB, SseTestHelper.createMockEmitter());
        emitterRepository.save(emitterIdC, SseTestHelper.createMockEmitter());

        // When - A 기기(tokenId=100)만 로그아웃
        emitterRepository.deleteEmitterByUserIdAndTokenId(userId, tokenIdA);

        // Then
        Map<String, SseEmitter> remainingEmitters = emitterRepository.findAllEmitterByUserId(userId);
        
        assertThat(remainingEmitters).hasSize(2); // B, C 기기만 남음
        assertThat(remainingEmitters).containsKey(emitterIdB); // B 기기 유지
        assertThat(remainingEmitters).containsKey(emitterIdC); // C 기기 유지
        assertThat(remainingEmitters).doesNotContainKey(emitterIdA1); // A 기기 연결1 삭제
        assertThat(remainingEmitters).doesNotContainKey(emitterIdA2); // A 기기 연결2 삭제
    }

    @Test
    @DisplayName("특정 사용자 ID와 토큰 ID로 Emitter 삭제 - 해당하는 Emitter가 없는 경우")
    void shouldNotThrowException_WhenDeletingNonExistentEmitterByUserIdAndTokenId() {
        // Given
        Long userId = 1L;
        Long tokenId = 999L; // 존재하지 않는 토큰 ID
        
        String existingEmitterId = "1_100_1234567890";
        emitterRepository.save(existingEmitterId, emitter1);

        // When & Then
        // 예외가 발생하지 않아야 함
        emitterRepository.deleteEmitterByUserIdAndTokenId(userId, tokenId);
        
        // 기존 Emitter는 유지되어야 함
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterByUserId(userId);
        assertThat(emitters).hasSize(1);
    }

    @Test
    @DisplayName("특정 사용자 ID와 토큰 ID로 Emitter 삭제 - 다른 사용자에게 영향 없음")
    void shouldNotAffectOtherUsers_WhenDeletingEmitterByUserIdAndTokenId() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long tokenId = 100L;
        
        String user1EmitterId = "1_100_1234567890";
        String user2EmitterId = "2_100_1234567890"; // 동일한 토큰 ID, 다른 사용자
        
        emitterRepository.save(user1EmitterId, emitter1);
        emitterRepository.save(user2EmitterId, emitter2);

        // When - user1의 특정 토큰만 삭제
        emitterRepository.deleteEmitterByUserIdAndTokenId(userId1, tokenId);

        // Then
        Map<String, SseEmitter> user1Emitters = emitterRepository.findAllEmitterByUserId(userId1);
        Map<String, SseEmitter> user2Emitters = emitterRepository.findAllEmitterByUserId(userId2);
        
        assertThat(user1Emitters).isEmpty(); // user1의 Emitter는 삭제됨
        assertThat(user2Emitters).hasSize(1); // user2의 Emitter는 유지됨
        assertThat(user2Emitters).containsKey(user2EmitterId);
    }
}
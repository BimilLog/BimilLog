package jaeik.bimillog.domain.friend.algorithm;

import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BreadthFirstSearchTest {

    @Mock
    private RedisFriendshipRepository redisFriendshipRepository;

    @Test
    @DisplayName("1촌이 200명을 초과하면 샘플링 후 BFS 수행 및 2촌 계산 정확성 검증")
    void shouldSampleFirstDegreeAndCalculateSecondDegreeCorrectly() {
        BreadthFirstSearch bfs = new BreadthFirstSearch(redisFriendshipRepository);

        // Given: 250명의 1촌 (샘플링 임계값 200 초과)
        Set<Long> firstDegree = new HashSet<>();
        for (long i = 1; i <= 250; i++) {
            firstDegree.add(i);
        }

        // Mock: 각 1촌의 친구 목록 설정
        // 1촌 ID 1-100: 각각 친구 1001, 1002를 가짐
        // 1촌 ID 101-200: 각각 친구 2001, 2002를 가짐
        given(redisFriendshipRepository.getFriendsBatch(org.mockito.ArgumentMatchers.anySet()))
                .willAnswer(invocation -> {
                    Set<Long> input = invocation.getArgument(0);
                    Map<Long, Set<Long>> result = new HashMap<>();
                    for (Long id : input) {
                        if (id <= 100) {
                            result.put(id, Set.of(1001L, 1002L));
                        } else if (id <= 200) {
                            result.put(id, Set.of(2001L, 2002L));
                        } else {
                            result.put(id, Set.of());
                        }
                    }
                    return result;
                });

        // When: 친구 관계 탐색 (샘플링으로 200명만 사용됨)
        var relation = bfs.findFriendRelation(0L, firstDegree);

        // Then 1: 샘플링이 정확히 200명으로 제한되었는지 검증
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(redisFriendshipRepository).getFriendsBatch(captor.capture());
        Set<Long> sampled = captor.getValue();
        assertThat(sampled).hasSize(200);

        // Then 2: 2촌 후보가 존재해야 함
        assertThat(relation.getSecondDegreeCandidates()).isNotEmpty();

        // Then 3: 2촌 후보는 1001, 1002, 2001, 2002 중 일부
        Set<Long> secondDegreeIds = relation.getSecondDegreeIds();
        assertThat(secondDegreeIds).allMatch(id ->
            id == 1001L || id == 1002L || id == 2001L || id == 2002L
        );

        // Then 4: 각 2촌 후보는 최소 1명 이상의 중개 친구를 가져야 함
        for (var candidate : relation.getSecondDegreeCandidates()) {
            assertThat(candidate.getBridgeFriendIds()).isNotEmpty();
        }
    }
}

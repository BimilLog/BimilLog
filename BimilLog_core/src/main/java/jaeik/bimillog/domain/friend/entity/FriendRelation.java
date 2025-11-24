package jaeik.bimillog.domain.friend.entity;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <h2>친구 관계 도메인 객체</h2>
 * <p>특정 회원의 1촌/2촌/3촌 관계 정보를 담는 도메인 객체입니다.</p>
 * <p>친구 추천 알고리즘에서 사용되며, 각 촌수별 ID와 연결 관계 정보를 포함합니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Getter
@Setter
@AllArgsConstructor
public class FriendRelation {
    /**
     * 본인 회원 ID
     */
    private Long memberId;

    /**
     * 1촌 친구 ID 집합
     */
    private Set<Long> firstDegreeIds;

    /**
     * 2촌 후보자 ID 집합
     */
    private Set<Long> secondDegreeIds;

    /**
     * 3촌 후보자 ID 집합
     */
    private Set<Long> thirdDegreeIds;

    /**
     * 2촌 연결 관계 매핑
     * <p>Key: 2촌 후보자 ID, Value: 그 후보자와 연결된 1촌 친구들의 ID 집합 (공통 친구)</p>
     */
    private Map<Long, Set<Long>> secondDegreeConnections;

    /**
     * 3촌 연결 관계 매핑
     * <p>Key: 3촌 후보자 ID, Value: 그 후보자와 연결된 2촌의 ID 집합</p>
     */
    private Map<Long, Set<Long>> thirdDegreeConnections;

    /**
     * <h3>2촌 관계 생성 (연결 정보 포함)</h3>
     *
     * @param memberId               본인 ID
     * @param firstDegreeIds         1촌 ID 집합
     * @param secondDegreeConnections 2촌 연결 관계 (Key: 2촌 ID, Value: 연결된 1촌들)
     * @return FriendRelation
     */
    public static FriendRelation createWithConnections(
            Long memberId,
            Set<Long> firstDegreeIds,
            Map<Long, Set<Long>> secondDegreeConnections) {
        return new FriendRelation(
                memberId,
                firstDegreeIds,
                secondDegreeConnections.keySet(),
                Set.of(),
                secondDegreeConnections,
                new HashMap<>()
        );
    }

    /**
     * <h3>2촌 관계 생성 (하위 호환성)</h3>
     * <p>연결 정보 없이 ID 집합만 사용하는 기존 방식</p>
     *
     * @deprecated 연결 정보를 포함하는 {@link #createWithConnections} 사용 권장
     */
    @Deprecated
    public static FriendRelation createSecondRelation(Long memberId, Set<Long> firstDegreeIds, Set<Long> secondDegreeIds) {
        return new FriendRelation(
                memberId,
                firstDegreeIds,
                secondDegreeIds,
                Set.of(),
                new HashMap<>(),
                new HashMap<>()
        );
    }

    /**
     * <h3>3촌 관계 업데이트</h3>
     *
     * @param thirdDegreeConnections 3촌 연결 관계 (Key: 3촌 ID, Value: 연결된 2촌들)
     */
    public void updateThirdRelation(Map<Long, Set<Long>> thirdDegreeConnections) {
        this.thirdDegreeIds = thirdDegreeConnections.keySet();
        this.thirdDegreeConnections = thirdDegreeConnections;
    }
}

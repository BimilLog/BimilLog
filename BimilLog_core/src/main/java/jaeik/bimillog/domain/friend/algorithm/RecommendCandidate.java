package jaeik.bimillog.domain.friend.algorithm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * <h2>친구 추천 후보자</h2>
 * <p>친구 추천 알고리즘에서 사용하는 임시 데이터 클래스입니다.</p>
 * <p>추천 대상의 촌수, 공통 친구, 상호작용 점수, 총점을 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
public class RecommendCandidate {

    /**
     * 추천 대상 회원 ID
     */
    private Long memberId;

    /**
     * 촌수 (2 또는 3)
     */
    private int depth;

    /**
     * 공통 친구 ID 집합
     */
    @Builder.Default
    private Set<Long> commonFriends = new HashSet<>();

    /**
     * 상호작용 점수 (0.0 ~ 10.0)
     */
    @Builder.Default
    private Double interactionScore = 0.0;

    /**
     * 총점 (기본 점수 + 공통 친구 점수 + 상호작용 점수)
     */
    private double totalScore;

    /**
     * 대표 공통 친구 ID (2촌용)
     * <p>공통 친구 중 첫 번째 친구를 대표로 선택</p>
     */
    private Long acquaintanceId;

    /**
     * 공통 친구 2명 이상 여부
     */
    @Builder.Default
    private boolean manyAcquaintance = false;

    /**
     * 공통 친구 추가
     *
     * @param friendId 공통 친구 ID
     */
    public void addCommonFriend(Long friendId) {
        this.commonFriends.add(friendId);

        // 첫 번째 공통 친구를 대표로 설정
        if (this.acquaintanceId == null) {
            this.acquaintanceId = friendId;
        } else if (this.commonFriends.size() >= 2) {
            // 공통 친구가 2명 이상이면 플래그 설정
            this.manyAcquaintance = true;
        }
    }

    /**
     * 공통 친구 수 조회
     *
     * @return 공통 친구 수
     */
    public int getCommonFriendCount() {
        return this.commonFriends.size();
    }
}

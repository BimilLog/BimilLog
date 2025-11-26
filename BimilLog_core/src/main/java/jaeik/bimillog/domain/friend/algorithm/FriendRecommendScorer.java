package jaeik.bimillog.domain.friend.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <h2>친구 추천 점수 계산기</h2>
 * <p>2촌/3촌 추천 친구의 점수를 계산합니다.</p>
 * <p>점수 구성: 기본 점수 + 공통 친구 점수 + 상호작용 점수</p>
 *
 * <h3>점수 체계</h3>
 * <ul>
 *     <li><b>2촌</b>: 기본 50점 + 공통친구(1명당 2점, 최대 20점) + 상호작용(최대 10점) = <b>최대 80점</b></li>
 *     <li><b>3촌</b>: 기본 20점 + 공통친구(연결된2촌의공통친구수*0.5, 최대 5점) + 상호작용(최대 10점) = <b>최대 35점</b></li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@Slf4j
public class FriendRecommendScorer {

    // 2촌 기본 점수
    public static final int BASE_SCORE_SECOND_DEGREE = 50;

    // 3촌 기본 점수
    public static final int BASE_SCORE_THIRD_DEGREE = 20;

    // 2촌 공통 친구 1명당 점수
    public static final int COMMON_FRIEND_SCORE_PER_PERSON_2ND = 2;

    // 3촌 공통 친구 1명당 점수 (연결된 2촌의 공통친구 수 기반)
    public static final double COMMON_FRIEND_SCORE_PER_PERSON_3RD = 0.5;

    // 상호작용 점수 최대값
    public static final double INTERACTION_SCORE_MAX = 10.0;

    // 공통 친구 최대 카운트 (10명)
    public static final int COMMON_FRIEND_MAX_COUNT = 10;

    // 촌수 - 2촌
    public static final int DEPTH_SECOND = 2;

    // 촌수 - 3촌
    public static final int DEPTH_THIRD = 3;
    /**
     * <h3>총점 계산</h3>
     * <p>기본 점수 + 공통 친구 점수 + 상호작용 점수를 합산합니다.</p>
     *
     * @param candidate 추천 후보자
     * @return 총점
     */
    public double calculateTotalScore(RecommendCandidate candidate) {
        double baseScore = getBaseScore(candidate.getDepth());
        double commonFriendScore = getCommonFriendScore(candidate.getCommonFriendCount(), candidate.getDepth());
        double interactionScore = getInteractionScore(candidate.getInteractionScore());

        double totalScore = baseScore + commonFriendScore + interactionScore;

        log.debug("점수 계산 완료: memberId={}, depth={}, base={}, common={}, interaction={}, total={}",
                candidate.getMemberId(), candidate.getDepth(), baseScore, commonFriendScore, interactionScore, totalScore);

        return totalScore;
    }

    /**
     * <h3>기본 점수 조회</h3>
     * <p>2촌: 50점, 3촌: 20점, null: 0점</p>
     *
     * @param depth 촌수 (2, 3, 또는 null - null은 2촌/3촌이 아닌 경우)
     * @return 기본 점수
     */
    public int getBaseScore(Integer depth) {
        if (depth == null) {
            return 0;  // 최근 가입자 등 2촌/3촌이 아닌 경우
        }

        if (depth == DEPTH_SECOND) {
            return BASE_SCORE_SECOND_DEGREE;
        } else if (depth == DEPTH_THIRD) {
            return BASE_SCORE_THIRD_DEGREE;
        }
        return 0;
    }

    /**
     * <h3>공통 친구 점수 계산</h3>
     * <p><b>2촌</b>: 1명당 2점, 최대 10명 20점</p>
     * <p><b>3촌</b>: 연결된 2촌의 공통친구 수 * 0.5점, 최대 10명 5점</p>
     * <p><b>null</b>: 0점 (2촌/3촌이 아닌 경우)</p>
     *
     * @param commonCount 공통 친구 수
     * @param depth       촌수 (2, 3, 또는 null - null은 2촌/3촌이 아닌 경우)
     * @return 공통 친구 점수
     */
    public double getCommonFriendScore(int commonCount, Integer depth) {
        if (depth == null) {
            return 0.0;  // 최근 가입자 등 2촌/3촌이 아닌 경우
        }

        if (depth == DEPTH_SECOND) {
            // 2촌: 1명당 2점, 최대 10명 20점
            int count = Math.min(commonCount, COMMON_FRIEND_MAX_COUNT);
            return count * COMMON_FRIEND_SCORE_PER_PERSON_2ND;
        } else if (depth == DEPTH_THIRD) {
            // 3촌: 연결된 2촌의 공통친구 수 * 0.5점, 최대 10명 5점
            int count = Math.min(commonCount, COMMON_FRIEND_MAX_COUNT);
            return count * COMMON_FRIEND_SCORE_PER_PERSON_3RD;
        }
        return 0.0;
    }

    /**
     * <h3>상호작용 점수 조회</h3>
     * <p>최대 10점까지 적용됩니다.</p>
     *
     * @param score Redis에서 조회한 상호작용 점수
     * @return 제한된 상호작용 점수 (최대 10점)
     */
    public double getInteractionScore(Double score) {
        if (score == null) {
            return 0.0;
        }

        // 최대 10점
        return Math.min(score, INTERACTION_SCORE_MAX);
    }

    /**
     * <h3>후보자 점수 계산 및 설정</h3>
     * <p>계산된 총점을 후보자 객체에 설정합니다.</p>
     *
     * @param candidate 추천 후보자
     */
    public void calculateAndSetScore(RecommendCandidate candidate) {
        double totalScore = calculateTotalScore(candidate);
        candidate.setTotalScore(totalScore);
    }
}

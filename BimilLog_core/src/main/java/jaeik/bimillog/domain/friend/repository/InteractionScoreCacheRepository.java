package jaeik.bimillog.domain.friend.repository;

import java.util.Map;
import java.util.Set;

/**
 * <h2>상호작용 점수 캐시 리포지토리 인터페이스</h2>
 * <p>회원 간 상호작용 점수 데이터의 캐싱을 담당하는 리포지토리 인터페이스입니다.</p>
 * <p>Hexagonal Architecture 원칙에 따라 도메인 계층에 정의되며, 인프라 계층에서 구현됩니다.</p>
 * <p>구현체: {@link jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository}</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
public interface InteractionScoreCacheRepository {

    /**
     * <h3>후보자들의 상호작용 점수만 파이프라인으로 일괄 조회</h3>
     * <p>N+1 문제를 방지하기 위해 배치 조회를 수행합니다.</p>
     *
     * @param memberId  기준 회원 ID
     * @param targetIds 점수를 조회할 대상(후보자) ID 목록
     * @return Map<대상ID, 점수>
     */
    Map<Long, Double> getInteractionScoresBatch(Long memberId, Set<Long> targetIds);

    /**
     * <h3>상호작용 점수 추가</h3>
     * <p>Lua 스크립트로 원자적으로 점수를 증가시킵니다.</p>
     * <p>기존 점수가 10점 이상인 경우 증가하지 않습니다.</p>
     *
     * @param memberId            회원 ID
     * @param interactionMemberId 상호작용 대상 회원 ID
     */
    void addInteractionScore(Long memberId, Long interactionMemberId);

    /**
     * <h3>탈퇴 회원의 상호작용 점수 삭제 (SCAN 패턴 매칭)</h3>
     * <p>탈퇴한 회원의 모든 상호작용 점수를 캐시에서 제거합니다.</p>
     * <p>전체 회원 조회 없이 SCAN 명령으로 효율적으로 처리합니다.</p>
     *
     * @param withdrawMemberId 탈퇴한 회원 ID
     */
    void deleteInteractionKeyByWithdraw(Long withdrawMemberId);
}

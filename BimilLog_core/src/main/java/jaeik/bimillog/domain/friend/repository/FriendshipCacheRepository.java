package jaeik.bimillog.domain.friend.repository;

import java.util.Map;
import java.util.Set;

/**
 * <h2>친구 관계 캐시 리포지토리 인터페이스</h2>
 * <p>친구 관계 데이터의 캐싱을 담당하는 리포지토리 인터페이스입니다.</p>
 * <p>Hexagonal Architecture 원칙에 따라 도메인 계층에 정의되며, 인프라 계층에서 구현됩니다.</p>
 * <p>구현체: {@link jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository}</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
public interface FriendshipCacheRepository {

    /**
     * <h3>특정 회원의 친구 목록 조회</h3>
     *
     * @param memberId 회원 ID
     * @return 친구 ID 집합
     */
    Set<Long> getFriends(Long memberId);

    /**
     * <h3>여러 회원의 친구 목록을 파이프라인으로 일괄 조회</h3>
     * <p>N+1 문제를 방지하기 위해 배치 조회를 수행합니다.</p>
     *
     * @param memberIds 조회할 회원 ID 목록
     * @return Map<회원ID, 친구ID_Set>
     */
    Map<Long, Set<Long>> getFriendsBatch(Set<Long> memberIds);

    /**
     * <h3>친구 관계 추가</h3>
     * <p>양방향 친구 관계를 캐시에 추가합니다.</p>
     *
     * @param memberId 회원 ID
     * @param friendId 친구 ID
     */
    void addFriend(Long memberId, Long friendId);

    /**
     * <h3>친구 관계 삭제</h3>
     * <p>양방향 친구 관계를 캐시에서 삭제합니다.</p>
     *
     * @param memberId 회원 ID
     * @param friendId 친구 ID
     */
    void deleteFriend(Long memberId, Long friendId);

    /**
     * <h3>탈퇴 회원의 친구 관계 삭제 (SCAN 패턴 매칭)</h3>
     * <p>탈퇴한 회원의 모든 친구 관계를 캐시에서 제거합니다.</p>
     * <p>전체 회원 조회 없이 SCAN 명령으로 효율적으로 처리합니다.</p>
     *
     * @param withdrawFriendId 탈퇴한 회원 ID
     */
    void deleteWithdrawFriendByScan(Long withdrawFriendId);
}

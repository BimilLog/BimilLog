package jaeik.bimillog.domain.friend.service;

import com.querydsl.core.Tuple;
import jaeik.bimillog.domain.friend.entity.FriendRelation;
import jaeik.bimillog.domain.friend.entity.FriendUnion;
import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendRecommendScheduledService {
    private final FriendToMemberAdapter friendToMemberAdapter;
    private final FriendshipQueryRepository friendshipQueryRepository;
    private final FriendRecommendUnionFindService friendRecommendUnionFindService;

    /**
     * 추천친구는 depth3 까지 탐색한다.<br>
     * 추천친구는 점수 상위 10명을 잘라서 저장한다.<br>
     * 현재 페이징이 10개 단위다 (1페이징만 공개) 향후 페이징을 늘릴 필요가 있으면 늘리면된다.<br>
     * 자신의 친구(1촌) <- 무시<br>
     * 친구의 친구(2촌)<br>
     * 친구의 친구의 친구(3촌)<br>
     * 2촌은 기본점수 50점을 준다. 3촌은 기본점수 20점을 준다.<br>
     * 공통친구 자신의 친구 A와 B가 공통으로 친구를 하는 또 다른 친구 C(2촌)는 사람당 2점을준다. 상한은 총 10명 20점이다.<br>
     * 3촌의 공통친구는 곧 공통친구 점수를 받은2촌의 친구이다. 예를들어<br>
     * 나 → A → B → C<br>
     * 나 → D → B → C<br>
     * 이럴경우 해당 2촌이 받은 공통친구 사람 수 만큼 C(3촌)에 0.5점을 준다. 상한은 총 10명 5점이다.<br>
     * 서로 글,댓글 추천, 글에 댓글을 단 적이 있으면 행동당 1점을준다. 상한은 총 10건 10점이다.<br>
     * 기본이 익명이기 때문에 롤링페이퍼 메시지 점수는 없다.<br>
     * 만약 그렇게해서 나온 추천친구가 10명이 되지않는 경우 멤버중 랜덤으로 10명을 채운다.(id max 방식)<br>
     * 중간에 빈 id가 나오면 예외를 던지고 무시한다. (10명이 채워지지 않더라도 확률상 0명이 될 경우는 거의 없다.)<br>
     * 자신의 id가 나오면 안된다.<br>
     */
    @Scheduled(fixedRate = 60000 * 60) // 1시간 마다
    @Transactional
    public void friendRecommendUpdate() {
        // 1. 2촌계산 : 각 멤버의 2촌을 가져왔을때 10명이 이상이면 3으로간다. 되지않으면 2로간다. 아무도 없으면 4로 간다. (3촌의 경우 최대점수가 35점으로 2촌을 넘을 수 없다)
        List<Tuple> results = friendshipQueryRepository.findAllTwoDegreeRelations(); // 2촌까지 조회
        List<FriendRelation> friendRelationList = buildFriendRelations(results); // FriendRelation으로 매핑

        // 2촌이 10명 이상인 경우
        List<FriendRelation> secondRelationPass = friendRelationList.stream()
                .filter(f -> f.getSecondDegreeIds().size() >= 10)
                .toList();

        // 2촌이 10명 미만인 경우
        List<FriendRelation> requiredThirdRelation = friendRelationList.stream()
                .filter(f -> f.getSecondDegreeIds().size() < 10)
                .toList();

        // 2촌이 아무도 없는 경우
        List<FriendRelation> noRelation = friendRelationList.stream()
                .filter(f -> f.getSecondDegreeIds().isEmpty())
                .toList();

        // 2. 3촌계산 : 3촌까지 10명이 넘든 넘지않든 3으로 간다.
        Map<Long, FriendRelation> relationMap = friendRelationList.stream()
                .collect(Collectors.toMap(FriendRelation::getMemberId, r -> r));
        fillThirdRelations(requiredThirdRelation, relationMap);

        // 3. 공통친구계산 : 결과를 대상으로 유니온파인드를 실행하여 관계파악 후 점수를 부여 후 4로 간다.


        // 4-1. 상호작용계산 (10명 이상) : 결과를 대상으로 상호작용 계산을 시행하여 점수를 부여하고 상위 10명을 잘라 저장한다. (이 때 3촌은 acquaintanceId가 null이다)
        // 4-2. 상호작용계산 (10명 미만) : 전체 테이블 대상으로 상호작용 계산을 시행하여 점수를 부여하고 상위 10명을 잘라 저장한다. 10명 미만인 경우는 5로 간다.
        // 5. 멤버 채우기 : 멤버ID Max를 범위로 랜덤값을 뽑아 10명을 채우고 저장한다.

    }

    /**
     * 튜플을 FriendRelation에 매핑한다.
     */
    private List<FriendRelation> buildFriendRelations(List<Tuple> tuples) {
        // 누적용 Map(중간 계산 용도)
        Map<Long, Set<Long>> first = new HashMap<>();
        Map<Long, Set<Long>> second = new HashMap<>();

        for (Tuple t : tuples) {
            Long memberId = t.get(0, Long.class);   // 본인
            Long firstId  = t.get(1, Long.class);   // 1촌
            Long secondId = t.get(2, Long.class);   // 2촌

            first.computeIfAbsent(memberId, k -> new HashSet<>()).add(firstId);
            second.computeIfAbsent(memberId, k -> new HashSet<>()).add(secondId);
        }

        // Map을 기반으로 FriendRelation 리스트 생성
        List<FriendRelation> result = new ArrayList<>();

        for (Long memberId : first.keySet()) {
            FriendRelation relation = FriendRelation.createSecondRelation(
                    memberId,
                    first.get(memberId),
                    second.getOrDefault(memberId, Set.of())
            );
            result.add(relation);
        }

        return result;
    }

    /**
     * 이미 존재하는 데이터로 DB를 조회하지 않고 3촌을 조회한다.
     */
    private void fillThirdRelations(List<FriendRelation> targetList, Map<Long, FriendRelation> relationMap) {
        for (FriendRelation requiredThirdRelation : targetList) {
            Set<Long> third = new HashSet<>();

            // 2촌 순회
            for (Long secondId : requiredThirdRelation.getSecondDegreeIds()) {
                FriendRelation secondRel = relationMap.get(secondId);
                if (secondRel == null) continue;

                // 2촌의 1촌들 = 3촌 후보
                for (Long candidate : secondRel.getFirstDegreeIds()) {

                    if (!candidate.equals(requiredThirdRelation.getMemberId()) &&
                            !requiredThirdRelation.getFirstDegreeIds().contains(candidate) &&
                            !requiredThirdRelation.getSecondDegreeIds().contains(candidate)) {
                        third.add(candidate);
                    }
                }
            }

            // 3촌 정보 갱신
            requiredThirdRelation.updateThirdRelation(third);
        }
    }

    private FriendUnion convertToUnionFind(FriendRelation r) {

        FriendUnion uf = new FriendUnion();

        // 2촌 등록
        for (Long second : r.getSecondDegreeIds()) {
            friendRecommendUnionFindService.addNode(second, 2);  // 기본점수 50
        }

        // 3촌 등록
        for (Long third : r.getThirdDegreeIds()) {
            friendRecommendUnionFindService.addNode(third, 3);  // 기본점수 20
        }

        return uf;
    }

}
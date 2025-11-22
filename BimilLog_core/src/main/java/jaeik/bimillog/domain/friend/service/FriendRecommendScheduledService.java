package jaeik.bimillog.domain.friend.service;

import com.querydsl.core.Tuple;
import jaeik.bimillog.domain.friend.entity.FriendRelation;
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

        // 1단계에서 조회한 튜플 정보를 사용하여 (나의 1촌 친구 수)와 (2촌 친구) 간의 연결 경로를 다시 구성합니다.
        Map<Long, Map<Long, List<Long>>> memberToSecondDegreePaths = mapSecondDegreePaths(results);

        // 모든 멤버의 추천 관계에 대해 Union-Find를 초기화하고 점수를 부여합니다.
        for (FriendRelation relation : friendRelationList) {
            Long memberId = relation.getMemberId();

            // 3-1. 유니온-파인드 노드 초기화 및 기본 점수 (2촌 50, 3촌 20) 부여
            // Union-Find는 개별 멤버의 추천 그룹을 분석하므로, 매번 초기화합니다.
            friendRecommendUnionFindService.clearNodes();
            friendRecommendUnionFindService.initializeNodes(relation);

            // 3-2. 공통친구 점수 계산 및 부여
            Map<Long, List<Long>> secondPaths = memberToSecondDegreePaths.getOrDefault(memberId, Collections.emptyMap());
            applyCommonFriendScore(relation, secondPaths, relationMap); // relationMap 추가

            // Union-Find에 등록된 총 후보 수 (2촌 + 3촌)를 확인합니다.
            List<FriendRecommendUnionFindService.FriendUnion> currentCandidates = new ArrayList<>(friendRecommendUnionFindService.allNodes());
            int totalCandidatesCount = currentCandidates.size();
            List<FriendRecommendUnionFindService.FriendUnion> finalRecommendationList;

            if (totalCandidatesCount >= 10) {
                // 4-1. 상호작용계산 (10명 이상) : 결과를 대상으로 상호작용 계산을 시행하여 점수를 부여하고 상위 10명을 잘라 저장한다. (이 때 3촌은 acquaintanceId가 null이다)

                // 1. 상호작용 점수 부여 및 최종 정렬
                List<FriendRecommendUnionFindService.FriendUnion> topCandidates =
                        applyInteractionScoreAndSort(memberId, currentCandidates, false); // false: 전체 테이블 미참조

                // 2. 상위 10명만 최종 추천 목록으로 선정 및 저장
                finalRecommendationList = topCandidates.stream().limit(10).toList();

            } else {
                // 4-2. 상호작용계산 (10명 미만) : 전체 테이블 대상으로 상호작용 계산을 시행하여 점수를 부여하고 상위 10명을 잘라 저장한다. 10명 미만인 경우는 5로 간다.

                // 1. 전체 멤버 중 상호작용 점수를 부여할 후보를 추가하고, 상호작용 점수 부여 및 최종 정렬
                List<FriendRecommendUnionFindService.FriendUnion> extendedCandidates =
                        extendCandidatesWithAllMembersForInteraction(memberId, currentCandidates, relation); // Placeholder

                List<FriendRecommendUnionFindService.FriendUnion> topCandidates =
                        applyInteractionScoreAndSort(memberId, extendedCandidates, true); // true: 전체 테이블 참조

                // 5. 10명 미달 시 랜덤 멤버 채우기
                finalRecommendationList = new ArrayList<>(topCandidates);

                if (finalRecommendationList.size() < 10) {
                    // TODO: 최종적으로 10명이 되도록 랜덤 멤버를 채우는 로직 구현 필요
                    // finalRecommendationList.addAll(fillRandomMembers(memberId, 10 - finalRecommendationList.size(), relation));
                }

                // 3. 상위 10명만 최종 추천 목록으로 선정
                finalRecommendationList = finalRecommendationList.stream().limit(10).toList();
            }

            // 5. 멤버 채우기 : 멤버ID Max를 범위로 랜덤값을 뽑아 10명을 채우고 저장한다.
            // saveRecommendationList(memberId, finalRecommendationList); // TODO: 최종 저장 로직 필요
            // ----------------------------------------------------

        }
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

    /**
     * 튜플 결과를 재구성하여 각 2촌이 몇 명의 1촌을 통해 연결되었는지 (공통 친구 수)를 계산하기 위한 맵을 생성합니다.
     * Map<본인 ID, Map<2촌 ID, List<1촌 ID>>>
     */
    private Map<Long, Map<Long, List<Long>>> mapSecondDegreePaths(List<Tuple> tuples) {
        Map<Long, Map<Long, List<Long>>> memberToSecondDegreePaths = new HashMap<>();

        for (Tuple t : tuples) {
            Long memberId = t.get(0, Long.class);   // 본인
            Long firstId = t.get(1, Long.class);    // 1촌
            Long secondId = t.get(2, Long.class);   // 2촌

            if (memberId == null || firstId == null || secondId == null) continue;

            // 2촌이 1촌이나 본인이 아닌 경우에만 유효
            if (!secondId.equals(memberId) && !secondId.equals(firstId)) {
                memberToSecondDegreePaths
                        .computeIfAbsent(memberId, k -> new HashMap<>())
                        .computeIfAbsent(secondId, k -> new ArrayList<>())
                        .add(firstId);
            }
        }
        return memberToSecondDegreePaths;
    }

    /**
     * 공통 친구 점수 및 3촌의 파생 점수를 계산하여 Union-Find 서비스에 적용합니다.
     *
     * @param relation 현재 멤버의 FriendRelation
     * @param secondPaths 2촌 ID -> 연결된 1촌 ID 리스트
     * @param relationMap 모든 멤버의 FriendRelation 맵 (3촌 경로 확인용)
     */
    private void applyCommonFriendScore(
            FriendRelation relation,
            Map<Long, List<Long>> secondPaths,
            Map<Long, FriendRelation> relationMap
    ) {
        // 1. 2촌 공통친구 점수 계산 및 부여 (기본 50점 외 추가 점수)
        // 공통친구: 1촌을 통해 연결된 경로 수. 사람당 2점, 상한 10명(20점).
        Map<Long, Integer> secondDegreeCommonFriendCount = new HashMap<>();

        for (Map.Entry<Long, List<Long>> entry : secondPaths.entrySet()) {
            Long secondId = entry.getKey();
            List<Long> firstIds = entry.getValue(); // 연결된 1촌 리스트

            // 2촌의 공통 친구 수 (1촌을 통해 연결된 경로의 수)
            int commonFriendCount = firstIds.size();
            secondDegreeCommonFriendCount.put(secondId, commonFriendCount);

            // 점수 계산: Math.min(경로 수, 10) * 2점
            int scoreToAdd = Math.min(commonFriendCount, 10) * 2;

            friendRecommendUnionFindService.addScore(secondId, scoreToAdd);
        }

        // 2. 3촌 공통친구 파생 점수 계산 및 부여 (기본 20점 외 추가 점수)
        // 3촌: 해당 2촌이 받은 공통친구 사람 수 만큼 0.5점을 준다. 상한은 총 10명(5점).

        // 3촌을 순회하며 연결된 2촌들을 찾고, 해당 2촌이 받은 '공통친구 사람 수'를 합산합니다.
        for (Long thirdId : relation.getThirdDegreeIds()) {
            int totalConnectingPeopleCount = 0; // 3촌에게 점수를 주는 2촌의 '공통 친구 사람 수'의 총합

            for (Long secondId : relation.getSecondDegreeIds()) {
                // secondId가 3촌 thirdId의 1촌인지 확인 (즉, Me -> B -> C 경로의 B -> C 부분)
                // FriendRelation 활용: B (secondId)의 1촌 리스트에 C (thirdId)가 있는지 확인
                FriendRelation secondRel = relationMap.get(secondId);

                // 여기서 secondRel.getFirstDegreeIds()는 FriendRelation이 가지고 있는 1촌 정보입니다.
                // 3촌 계산 로직에서 이 정보가 유용하게 사용됩니다.
                if (secondRel != null && secondRel.getFirstDegreeIds().contains(thirdId)) {
                    // thirdId는 secondId의 친구(1촌)입니다.
                    // 이제 secondId(2촌)가 받은 공통친구 사람 수(N)를 가져와 0.5점을 부여합니다.
                    int connectingPeopleCount = secondDegreeCommonFriendCount.getOrDefault(secondId, 0);

                    // 2촌 한 명당 연결된 1촌 수(사람 수)가 3촌 점수에 기여.
                    totalConnectingPeopleCount += connectingPeopleCount;
                }
            }

            // 점수 계산: 총 사람 수 * 0.5, 상한 10명(5점)
            int cappedPeopleCount = Math.min(totalConnectingPeopleCount, 10);
            int scoreToAdd = (int) Math.floor(cappedPeopleCount * 0.5); // 0.5점은 정수화

            friendRecommendUnionFindService.addScore(thirdId, scoreToAdd);
        }
    }

    /**
     * 상호작용 점수를 계산하고 최종 점수를 기준으로 후보를 정렬합니다. (Placeholder)
     * @param isExtendedSearch 전체 멤버(테이블)를 대상으로 검색했는지 여부 (4-2단계)
     */
    private List<FriendRecommendUnionFindService.FriendUnion> applyInteractionScoreAndSort(
            Long memberId,
            List<FriendRecommendUnionFindService.FriendUnion> candidates,
            boolean isExtendedSearch) {

        // TODO: 같은 유니온 내의 ID끼리의 상호작용(글, 댓글 등) 상호작용 점수 추가 로직 필요
        // TODO: 예시: 랜덤으로 0~10점 추가 (실제 로직 대체 필요)
        Random random = new Random();
        for (FriendRecommendUnionFindService.FriendUnion candidate : candidates) {
            // isExtendedSearch 여부에 따라 점수 계산 범위가 달라질 수 있습니다.
            int interactionScore = random.nextInt(11); // 0 to 10
            candidate.addScore(interactionScore);

            // isExtendedSearch가 true일 때, 2, 3촌이 아닌 멤버는 기본 점수가 0이므로,
            // 상호작용 점수만으로 순위를 매기게 됩니다.
        }

        // 최종 점수(기본점수 + 공통친구점수 + 상호작용점수)를 기준으로 내림차순 정렬
        return candidates.stream()
                .sorted(Comparator.comparing(FriendRecommendUnionFindService.FriendUnion::getScore).reversed())
                .toList();
    }

    /**
     * 4-2 단계에서 10명 미만인 경우, 전체 멤버 중 상호작용이 있는 멤버를 추가로 가져오는 Placeholder.
     */
    private List<FriendRecommendUnionFindService.FriendUnion> extendCandidatesWithAllMembersForInteraction(
            Long memberId,
            List<FriendRecommendUnionFindService.FriendUnion> currentCandidates,
            FriendRelation relation) {

        // TODO: 특정 memberId와 상호작용 점수가 있는 모든 멤버를 가져와야 합니다.
        // 이때, 이미 친구인 (1촌) 및 이미 2/3촌으로 등록된 멤버는 제외해야 합니다.

        // 현재는 Placeholder로, 현재 후보만 반환합니다. (실제 4-2 로직을 위해 이 부분을 구현해야 합니다.)
        return new ArrayList<>(currentCandidates);
    }

}
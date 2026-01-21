package jaeik.bimillog.domain.friend.recommend;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h2>친구 추천 응답 조립기</h2>
 * <p>추천된 후보자(ID 기반) 정보를 바탕으로 실제 화면에 필요한 회원 정보(이름, 프로필 등)를 조회하여<br>
 * 최종 응답 객체(RecommendedFriend)로 변환하는 역할을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@RequiredArgsConstructor
public class FriendRecommendationAssembler {
    private final MemberQueryRepository memberQueryRepository;

    /**
     * 후보자 리스트를 응답 DTO 리스트로 변환합니다.
     */
    public List<RecommendedFriend> toResponse(List<RecommendCandidate> candidates) {
        if (candidates.isEmpty()) return Collections.emptyList();

        // 1. 필요한 ID 수집 (화면 표시용)
        List<Long> candidateIds = candidates.stream()
                .map(RecommendCandidate::getMemberId)
                .toList();

        // 2. 2촌의 경우, 연결고리가 되는 지인(Acquaintance) ID 수집
        Set<Long> acquaintanceIds = candidates.stream()
                .filter(c -> c.getDepth() != null && c.getDepth() == FriendRecommendScorer.DEPTH_SECOND && c.getAcquaintanceId() != null)
                .map(RecommendCandidate::getAcquaintanceId)
                .collect(Collectors.toSet());

        // 3. 회원 정보 및 지인 정보 일괄 조회 (Bulk Fetch)
        Map<Long, RecommendedFriend.RecommendedFriendInfo> friendInfoMap = memberQueryRepository.addRecommendedFriendInfo(candidateIds).stream()
                .collect(Collectors.toMap(RecommendedFriend.RecommendedFriendInfo::friendMemberId, Function.identity()));

        Map<Long, RecommendedFriend.AcquaintanceInfo> acquaintanceInfoMap = acquaintanceIds.isEmpty() ? Collections.emptyMap() :
                memberQueryRepository.addAcquaintanceInfo(new ArrayList<>(acquaintanceIds)).stream()
                        .collect(Collectors.toMap(RecommendedFriend.AcquaintanceInfo::acquaintanceId, Function.identity()));

        // 4. 조립 (DTO 매핑)
        return candidates.stream()
                .filter(candidate -> friendInfoMap.containsKey(candidate.getMemberId()))
                .map(candidate -> mapToRecommendedFriend(candidate, friendInfoMap, acquaintanceInfoMap))
                .collect(Collectors.toList());
    }

    private RecommendedFriend mapToRecommendedFriend(RecommendCandidate c,
                                                     Map<Long, RecommendedFriend.RecommendedFriendInfo> friendMap,
                                                     Map<Long, RecommendedFriend.AcquaintanceInfo> acquaintanceMap) {

        RecommendedFriend.RecommendedFriendInfo info = friendMap.get(c.getMemberId());

        boolean isSecondDegree = c.getDepth() != null && c.getDepth() == FriendRecommendScorer.DEPTH_SECOND;
        Long acquaintanceId = (isSecondDegree && c.getAcquaintanceId() != null) ? c.getAcquaintanceId() : null;
        boolean isMany = isSecondDegree && c.isManyAcquaintance();

        RecommendedFriend friend = new RecommendedFriend(c.getMemberId(), acquaintanceId, isMany, c.getDepth());

        // 위 filter에서 존재 여부를 확인했으므로 info는 null이 아님이 보장되지만, 방어적 코드로 남겨둡니다.
        if (info != null) {
            friend.setRecommendedFriendName(info);
        }

        if (acquaintanceId != null) {
            RecommendedFriend.AcquaintanceInfo acqInfo = acquaintanceMap.get(acquaintanceId);
            if (acqInfo != null) {
                friend.setAcquaintanceFriendName(acqInfo);
            }
        }

        return friend;
    }
}
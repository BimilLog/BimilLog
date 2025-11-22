package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.repository.FriendRecommendationQueryRepository;
import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendRecommendService {
    private final FriendRecommendationQueryRepository friendRecommendationQueryRepository;
    private final FriendToMemberAdapter friendToMemberAdapter;

    @Transactional(readOnly = true)
    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        Page<RecommendedFriend> recommendedFriendPages = friendRecommendationQueryRepository.getRecommendFriendList(memberId, pageable);
        List<Long> friendIds = recommendedFriendPages.getContent().stream().map(RecommendedFriend::getFriendMemberId).toList();
        List<RecommendedFriend.RecommendedFriendInfo> friendInfos = friendToMemberAdapter.addRecommendedFriendInfo(friendIds);
        Map<Long, RecommendedFriend.RecommendedFriendInfo> infoMap = friendInfos.stream()
                .collect(Collectors.toMap(RecommendedFriend.RecommendedFriendInfo::friendMemberId, info -> info));

        // 3. 기존 Page<RecommendedFriend> 내부 객체에 FriendInfo 주입
        recommendedFriendPages.getContent().forEach(recommendedFriends -> {
            RecommendedFriend.RecommendedFriendInfo info = infoMap.get(recommendedFriends.getFriendMemberId());
            if (info != null) {
                recommendedFriends.updateInfo(info);
            }
        });
        return recommendedFriendPages;
    }
}

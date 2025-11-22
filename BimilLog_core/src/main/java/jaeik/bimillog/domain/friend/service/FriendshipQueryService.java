package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipQueryRepository;
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
public class FriendshipQueryService {
    private final FriendshipQueryRepository friendshipQueryRepository;
    private final FriendToMemberAdapter friendToMemberAdapter;

    /**
     * 친구 조회
     */
    @Transactional(readOnly = true)
    public Page<Friend> getMyFriendList(Long memberId, Pageable pageable) {
        Page<Friend> myFriendPages = friendshipQueryRepository.getMyFriendIds(memberId, pageable);
        List<Long> friendIds = myFriendPages.getContent().stream().map(Friend::getFriendMemberId).toList();
        List<Friend.FriendInfo> friendInfos = friendToMemberAdapter.addMyFriendInfo(friendIds);

        Map<Long, Friend.FriendInfo> infoMap = friendInfos.stream()
                .collect(Collectors.toMap(Friend.FriendInfo::memberId, info -> info));

        // 3. 기존 Page<Friend> 내부 객체에 FriendInfo 주입
        myFriendPages.getContent().forEach(friend -> {
            Friend.FriendInfo info = infoMap.get(friend.getFriendMemberId());
            if (info != null) {
                friend.updateInfo(info);
            }
        });
        return myFriendPages;
    }
}

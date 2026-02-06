package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.adapter.FriendToMemberAdapter;
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

    /**
     * 친구 조회
     */
    @Transactional(readOnly = true)
    public Page<Friend> getMyFriendList(Long memberId, Pageable pageable) {
        return friendshipQueryRepository.getFriendPage(memberId, pageable);
    }
}

package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.member.service.MemberFriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class FriendToMemberAdapter {
    private final MemberFriendService memberFriendService;

    // 친구 추가 정보 조회
    public List<Friend.FriendInfo> addMyFriendInfo(List<Long> friendIds) {
        return memberFriendService.addMyFriendInfo(friendIds);
    }


}

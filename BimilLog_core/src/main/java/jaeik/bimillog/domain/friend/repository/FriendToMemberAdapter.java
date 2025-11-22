package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
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

    // 추천 친구 추가 정보 조회
    public List<RecommendedFriend.RecommendedFriendInfo> addRecommendedFriendInfo(List<Long> friendIds) {
        return memberFriendService.addRecommendedFriendInfo(friendIds);
    }

    // 추천 친구 아는 사람 추가 정보 조회
    public List<RecommendedFriend.AcquaintanceInfo> addAcquaintanceInfo(List<Long> acquaintanceIds) {
        return memberFriendService.addAcquaintanceInfo(acquaintanceIds);
    }

    // 추천 친구 저장 스케줄링
    public void friendRecommendUpdate() {
        memberFriendService.friendRecommendUpdate();
    }
}

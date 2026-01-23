package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.testutil.fixtures.TestFixtures;

/**
 * Friend 도메인 테스트 데이터 빌더
 * <p>
 * Friend 관련 테스트 데이터 생성 유틸리티
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>FriendRequest 생성 (ID 선택적 설정)</li>
 * </ul>
 */
public class FriendTestDataBuilder {

    // ==================== FriendRequest ====================

    /**
     * 친구 요청 엔티티 생성
     * @param id 친구 요청 ID (null이면 ID 설정 안함)
     * @param sender 요청 보낸 사람
     * @param receiver 요청 받은 사람
     * @return FriendRequest
     */
    public static FriendRequest createFriendRequest(Long id, Member sender, Member receiver) {
        FriendRequest friendRequest = FriendRequest.createFriendRequest(sender, receiver);
        if (id != null) {
            TestFixtures.setFieldValue(friendRequest, "id", id);
        }
        return friendRequest;
    }

}

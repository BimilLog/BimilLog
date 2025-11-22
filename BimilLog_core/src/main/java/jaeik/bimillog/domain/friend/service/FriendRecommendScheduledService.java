package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.repository.FriendToMemberAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendRecommendScheduledService {
    private final FriendToMemberAdapter friendToMemberAdapter;

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
        // 그룹바이로 유저별 2촌을 조회하여 메모리로 가지고 온다.
        // 1. 2촌계산 : 각 멤버의 2촌을 가져왔을때 10명이 넘으면 3으로간다. 되지않으면 2로간다. (3촌의 경우 최대점수가 35점으로 2촌을 넘을 수 없다)
        // 2. 3촌계산 : 3촌까지 10명이 넘든 넘지않든 3으로 간다.
        // 3. 공통친구계산 : 결과를 대상으로 유니온파인드를 실행하여 관계파악 후 점수를 부여 후 4로 간다.
        // 4-1. 상호작용계산 (10명 이상) : 결과를 대상으로 상호작용 계산을 시행하여 점수를 부여하고 상위 10명을 잘라 저장한다. (이 때 3촌은 acquaintanceId가 null이다)
        // 4-2. 상호작용계산 (10명 미만) : 전체 테이블 대상으로 상호작용 계산을 시행하여 점수를 부여하고 상위 10명을 잘라 저장한다. 10명 미만인 경우는 5로 간다.
        // 5. 멤버 채우기 : 멤버ID Max를 범위로 랜덤값을 뽑아 10명을 채우고 저장한다.
        friendToMemberAdapter.friendRecommendUpdate();
    }
}

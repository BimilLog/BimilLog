package jaeik.bimillog.domain.friend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendRecommendScheduledService {

    @Scheduled(fixedRate = 60000 * 60) // 1시간 마다
    @Transactional
    public void friendRecommendUpdate() {

    }
}

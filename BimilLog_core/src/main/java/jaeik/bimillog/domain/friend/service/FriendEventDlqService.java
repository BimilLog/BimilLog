package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendEventDlq;
import jaeik.bimillog.domain.friend.repository.FriendEventDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>친구 이벤트 DLQ 서비스</h2>
 * <p>Redis 친구 이벤트 처리 실패 시 DLQ에 저장하는 서비스입니다.</p>
 * <p>REQUIRES_NEW 트랜잭션을 사용하여 호출자의 트랜잭션과 독립적으로 동작합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendEventDlqService {

    private final FriendEventDlqRepository repository;

    /**
     * 친구 추가 이벤트를 DLQ에 저장합니다.
     *
     * @param memberId 회원 ID
     * @param friendId 친구 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFriendAdd(Long memberId, Long friendId) {
        FriendEventDlq dlq = FriendEventDlq.createFriendAdd(memberId, friendId);
        repository.save(dlq);
        log.info("[DLQ] 친구 추가 이벤트 저장: memberId={}, friendId={}", memberId, friendId);
    }

    /**
     * 친구 삭제 이벤트를 DLQ에 저장합니다.
     *
     * @param memberId 회원 ID
     * @param friendId 친구 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFriendRemove(Long memberId, Long friendId) {
        FriendEventDlq dlq = FriendEventDlq.createFriendRemove(memberId, friendId);
        repository.save(dlq);
        log.info("[DLQ] 친구 삭제 이벤트 저장: memberId={}, friendId={}", memberId, friendId);
    }

    /**
     * 상호작용 점수 증가 이벤트를 DLQ에 저장합니다.
     *
     * @param memberId 회원 ID
     * @param targetId 상호작용 대상 ID
     * @param score 증가할 점수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveScoreUp(Long memberId, Long targetId, Double score) {
        FriendEventDlq dlq = FriendEventDlq.createScoreUp(memberId, targetId, score);
        repository.save(dlq);
        log.info("[DLQ] 상호작용 점수 증가 이벤트 저장: memberId={}, targetId={}, score={}", memberId, targetId, score);
    }
}

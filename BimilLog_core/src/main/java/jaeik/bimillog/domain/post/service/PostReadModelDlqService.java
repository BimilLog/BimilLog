package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostReadModelDlq;
import jaeik.bimillog.domain.post.entity.jpa.PostReadModelEventType;
import jaeik.bimillog.domain.post.repository.PostReadModelDlqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>Post Read Model DLQ 서비스</h2>
 * <p>PostReadModel 이벤트 처리 실패 시 DLQ에 저장하는 서비스입니다.</p>
 * <p>REQUIRES_NEW 트랜잭션을 사용하여 호출자의 트랜잭션과 독립적으로 동작합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostReadModelDlqService {

    private final PostReadModelDlqRepository repository;

    /**
     * 게시글 생성 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePostCreated(String eventId, Long postId, String title, Long memberId, String memberName) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createPostCreated(eventId, postId, title, memberId, memberName);
            repository.save(dlq);
            log.info("[DLQ] 게시글 생성 이벤트 저장: eventId={}, postId={}", eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 게시글 생성 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }

    /**
     * 게시글 수정 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePostUpdated(String eventId, Long postId, String newTitle) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createPostUpdated(eventId, postId, newTitle);
            repository.save(dlq);
            log.info("[DLQ] 게시글 수정 이벤트 저장: eventId={}, postId={}", eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 게시글 수정 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }

    /**
     * 단순 이벤트를 DLQ에 저장합니다. (좋아요 증감, 댓글 수 증감)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSimpleEvent(PostReadModelEventType eventType, String eventId, Long postId) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createSimpleEvent(eventType, eventId, postId);
            repository.save(dlq);
            log.info("[DLQ] {} 이벤트 저장: eventId={}, postId={}", eventType, eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] {} 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventType, eventId);
        }
    }

    /**
     * 조회수 증가 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveViewIncrement(String eventId, Long postId, Integer deltaValue) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createViewIncrement(eventId, postId, deltaValue);
            repository.save(dlq);
            log.info("[DLQ] 조회수 증가 이벤트 저장: eventId={}, postId={}, delta={}", eventId, postId, deltaValue);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 조회수 증가 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }
}

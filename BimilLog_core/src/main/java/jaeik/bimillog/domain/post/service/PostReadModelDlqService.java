package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.entity.jpa.PostReadModelDlq;
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
     * 좋아요 증가 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLikeIncrement(String eventId, Long postId) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createLikeIncrement(eventId, postId);
            repository.save(dlq);
            log.info("[DLQ] 좋아요 증가 이벤트 저장: eventId={}, postId={}", eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 좋아요 증가 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }

    /**
     * 좋아요 감소 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLikeDecrement(String eventId, Long postId) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createLikeDecrement(eventId, postId);
            repository.save(dlq);
            log.info("[DLQ] 좋아요 감소 이벤트 저장: eventId={}, postId={}", eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 좋아요 감소 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }

    /**
     * 좋아요 감소 이벤트를 DLQ에 저장합니다. (eventId 자동 생성)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLikeDecrement(Long postId) {
        String eventId = "LIKE_DEC:" + postId + ":" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        saveLikeDecrement(eventId, postId);
    }

    /**
     * 댓글 수 증가 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCommentIncrement(String eventId, Long postId) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createCommentIncrement(eventId, postId);
            repository.save(dlq);
            log.info("[DLQ] 댓글 수 증가 이벤트 저장: eventId={}, postId={}", eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 댓글 수 증가 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }

    /**
     * 댓글 수 감소 이벤트를 DLQ에 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCommentDecrement(String eventId, Long postId) {
        try {
            PostReadModelDlq dlq = PostReadModelDlq.createCommentDecrement(eventId, postId);
            repository.save(dlq);
            log.info("[DLQ] 댓글 수 감소 이벤트 저장: eventId={}, postId={}", eventId, postId);
        } catch (DataIntegrityViolationException e) {
            log.debug("[DLQ] 댓글 수 감소 이벤트 중복 저장 스킵 (멱등성): eventId={}", eventId);
        }
    }

    /**
     * 댓글 수 감소 이벤트를 DLQ에 저장합니다. (eventId 자동 생성)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCommentDecrement(Long postId) {
        String eventId = "CMT_DEC:" + postId + ":" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        saveCommentDecrement(eventId, postId);
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

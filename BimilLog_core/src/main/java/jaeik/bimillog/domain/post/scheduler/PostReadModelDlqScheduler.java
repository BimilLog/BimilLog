package jaeik.bimillog.domain.post.scheduler;

import jaeik.bimillog.domain.post.entity.jpa.PostReadModel;
import jaeik.bimillog.domain.post.entity.jpa.PostReadModelDlq;
import jaeik.bimillog.domain.post.repository.PostReadModelDlqRepository;
import jaeik.bimillog.domain.post.repository.PostReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>Post Read Model DLQ 재처리 스케줄러</h2>
 * <p>5분마다 DLQ에 저장된 이벤트를 배치 재처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostReadModelDlqScheduler {

    private final PostReadModelDlqRepository dlqRepository;
    private final PostReadModelRepository postReadModelRepository;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;

    /**
     * 5분마다 DLQ 이벤트를 재처리합니다.
     */
    @Scheduled(fixedRate = 300000)  // 5분마다
    @Transactional
    public void processDlq() {
        List<PostReadModelDlq> events = dlqRepository.findPendingEvents(BATCH_SIZE);

        if (events.isEmpty()) {
            return;
        }

        log.info("[PostReadModel DLQ] 재처리 시작: {}건", events.size());

        List<PostReadModelDlq> processedEvents = new ArrayList<>();
        List<PostReadModelDlq> failedEvents = new ArrayList<>();

        for (PostReadModelDlq event : events) {
            try {
                processEvent(event);
                event.markAsProcessed();
                processedEvents.add(event);
            } catch (Exception e) {
                event.incrementRetryCount();
                if (event.getRetryCount() >= MAX_RETRY) {
                    event.markAsFailed();
                    log.error("[PostReadModel DLQ] 최대 재시도 초과, FAILED 처리: id={}, type={}", event.getId(), event.getEventType());
                }
                failedEvents.add(event);
            }
        }

        dlqRepository.saveAll(processedEvents);
        dlqRepository.saveAll(failedEvents);

        log.info("[PostReadModel DLQ] 재처리 완료: 성공={}건, 실패={}건", processedEvents.size(), failedEvents.size());
    }

    private void processEvent(PostReadModelDlq event) {
        switch (event.getEventType()) {
            case POST_CREATED -> processPostCreated(event);
            case POST_UPDATED -> postReadModelRepository.findById(event.getPostId())
                    .ifPresent(readModel -> readModel.updateTitle(event.getTitle()));
            case LIKE_INCREMENT -> postReadModelRepository.incrementLikeCount(event.getPostId());
            case LIKE_DECREMENT -> postReadModelRepository.decrementLikeCount(event.getPostId());
            case COMMENT_INCREMENT -> postReadModelRepository.incrementCommentCount(event.getPostId());
            case COMMENT_DECREMENT -> postReadModelRepository.decrementCommentCount(event.getPostId());
            case VIEW_INCREMENT -> {
                int delta = event.getDeltaValue() != null ? event.getDeltaValue() : 1;
                postReadModelRepository.incrementViewCountByAmount(event.getPostId(), (long) delta);
            }
        }
    }

    private void processPostCreated(PostReadModelDlq event) {
        // 이미 존재하는지 확인
        if (postReadModelRepository.existsById(event.getPostId())) {
            log.debug("[PostReadModel DLQ] 이미 존재하는 PostReadModel 스킵: postId={}", event.getPostId());
            return;
        }

        PostReadModel readModel = PostReadModel.createNew(
                event.getPostId(), event.getTitle(), event.getMemberId(),
                event.getMemberName(), null
        );

        postReadModelRepository.save(readModel);
    }
}

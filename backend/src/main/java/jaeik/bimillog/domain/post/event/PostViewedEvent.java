package jaeik.bimillog.domain.post.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * <h2>게시글 조회 이벤트</h2>
 * <p>
 * 게시글이 조회되었을 때 발생하는 이벤트
 * 조회수 증가 로직을 비동기로 처리하기 위해 사용
 * </p>
 * <p>
 * 중복 조회 검증은 Controller에서 처리되며, 이벤트는 단순히 조회수 증가만 담당합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public class PostViewedEvent extends ApplicationEvent {

    /**
     * 조회된 게시글 ID
     */
    private final Long postId;

    /**
     * <h3>PostViewedEvent 생성자</h3>
     * <p>
     * 게시글 조회 이벤트를 생성합니다.
     * 중복 조회 검증은 이미 완료된 상태입니다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param postId 조회된 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public PostViewedEvent(Object source, Long postId) {
        super(source);
        this.postId = postId;
    }
}
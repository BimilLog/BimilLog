package jaeik.bimillog.domain.post.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <h2>게시글 조회 이벤트</h2>
 * <p>
 * 게시글이 조회되었을 때 발생하는 이벤트
 * 조회수 증가 로직을 비동기로 처리하기 위해 사용
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
public class PostViewedEvent extends ApplicationEvent {

    /**
     * 조회된 게시글 ID
     */
    private final Long postId;

    /**
     * HTTP 요청 (쿠키 확인용)
     */
    private final HttpServletRequest request;

    /**
     * HTTP 응답 (쿠키 설정용)
     */
    private final HttpServletResponse response;

    /**
     * <h3>PostViewedEvent 생성자</h3>
     * <p>
     * 게시글 조회 이벤트를 생성합니다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param postId 조회된 게시글 ID
     * @param request HTTP 요청 (쿠키 확인용)
     * @param response HTTP 응답 (쿠키 설정용)
     * @author Jaeik
     * @since 2.0.0
     */
    public PostViewedEvent(Object source, Long postId, HttpServletRequest request, HttpServletResponse response) {
        super(source);
        this.postId = postId;
        this.request = request;
        this.response = response;
    }
}
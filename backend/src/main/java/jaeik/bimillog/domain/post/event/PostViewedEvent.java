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
 * 헥사고날 아키텍처를 준수하여 도메인 레이어에서 인프라 의존성을 제거했습니다.
 * HTTP 요청/응답 객체 대신 필요한 데이터만 전달합니다.
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
     * 사용자 식별자 (쿠키 중복 방지용)
     * IP 주소, 세션 ID, 또는 기타 사용자를 식별할 수 있는 값
     */
    private final String userIdentifier;

    /**
     * 현재 사용자의 조회 이력 (쿠키에서 추출된 데이터)
     * key: "viewed_posts", value: 쉼표로 구분된 게시글 ID 목록
     */
    private final Map<String, String> viewHistory;

    /**
     * <h3>PostViewedEvent 생성자</h3>
     * <p>
     * 게시글 조회 이벤트를 생성합니다.
     * 인프라 레이어에서 HTTP 관련 데이터를 도메인 데이터로 변환하여 전달합니다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param postId 조회된 게시글 ID
     * @param userIdentifier 사용자 식별자 (IP, 세션 ID 등)
     * @param viewHistory 사용자의 조회 이력 맵
     * @author Jaeik
     * @version 2.0.0
     */
    public PostViewedEvent(Object source, Long postId, String userIdentifier, Map<String, String> viewHistory) {
        super(source);
        this.postId = postId;
        this.userIdentifier = userIdentifier;
        this.viewHistory = viewHistory;
    }
}
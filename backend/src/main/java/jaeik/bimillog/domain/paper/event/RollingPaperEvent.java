package jaeik.bimillog.domain.paper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <h2>메시지 작성 이벤트</h2>
 * <p>
 * 다른 사용자가 롤링페이퍼에 메시지를 남겼을 때 발생하는 이벤트
 * SSE와 FCM 알림을 트리거한다
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
public class RollingPaperEvent extends ApplicationEvent {

    /**
     * 롤링페이퍼 주인 ID (알림을 받을 사용자)
     */
    private final Long paperOwnerId;

    /**
     * 닉네임
     */
    private final String userName;

    /**
     * <h3>MessageEvent 생성자</h3>
     * <p>
     * 메시지 작성 이벤트를 생성한다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param paperOwnerId 롤링페이퍼 주인 ID (알림을 받을 사용자)
     * @param userName 닉네임
     * @author Jaeik
     * @since 2.0.0
     */
    public RollingPaperEvent(Object source, Long paperOwnerId, String userName) {
        super(source);
        this.paperOwnerId = paperOwnerId;
        this.userName = userName;
    }

}
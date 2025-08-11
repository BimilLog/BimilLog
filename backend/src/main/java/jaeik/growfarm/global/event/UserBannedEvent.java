package jaeik.growfarm.global.event;

import jaeik.growfarm.domain.user.domain.SocialProvider;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <h2>사용자 차단 이벤트</h2>
 * <p>
 * 사용자가 관리자에 의해 차단되었을 때 발생하는 이벤트
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
public class UserBannedEvent extends ApplicationEvent {

    private final Long userId;
    private final String socialId;
    private final SocialProvider provider;

    /**
     * <h3>UserBannedEvent 생성자</h3>
     * <p>
     * 사용자 차단 이벤트를 생성한다.
     * </p>
     *
     * @param source 이벤트를 발생시킨 객체
     * @param userId 차단된 사용자 ID
     * @param socialId 차단된 사용자 소셜 ID
     * @param provider 소셜 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    public UserBannedEvent(Object source, Long userId, String socialId, SocialProvider provider) {
        super(source);
        this.userId = userId;
        this.socialId = socialId;
        this.provider = provider;
    }
}

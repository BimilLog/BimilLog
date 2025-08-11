package jaeik.growfarm.global.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserWithdrawnEvent extends ApplicationEvent {

    private final Long userId;

    public UserWithdrawnEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}

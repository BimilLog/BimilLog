package jaeik.growfarm.domain.comment.application.handler;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserWithdrawnEventHandler {

    private final CommentCommandUseCase commentCommandUseCase;

    @Async
    @EventListener
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        commentCommandUseCase.anonymizeUserComments(event.getUserId());
    }
}

package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawListener {

    @Async
    @EventListener
    @Transactional
    public void userWithdraw() {

    }
}

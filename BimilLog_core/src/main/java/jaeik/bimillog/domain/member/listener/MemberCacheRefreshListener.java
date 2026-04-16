package jaeik.bimillog.domain.member.listener;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.event.MemberCacheRefreshEvent;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberCacheRefreshListener {
    private final MemberQueryRepository memberQueryRepository;
    private final RedisMemberAdapter redisMemberAdapter;

    @Async("memberPERexecutor")
    @EventListener
    public void RefreshMemberCache(MemberCacheRefreshEvent event) {
        Pageable pageable = event.getPageable();
        Page<SimpleMemberDTO> fresh = memberQueryRepository.findAllMembers(pageable);

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        redisMemberAdapter.saveMemberPage(page, size, fresh.getContent());
    }
}

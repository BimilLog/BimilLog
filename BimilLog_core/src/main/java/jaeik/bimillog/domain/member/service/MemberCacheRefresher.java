package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>멤버 페이지 캐시 비동기 갱신</h2>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberCacheRefresher {

    private final MemberQueryRepository memberQueryRepository;
    private final RedisMemberAdapter redisMemberAdapter;

    @Async("memberEventExecutor")
    public void refresh(int page, int size, Runnable cleanup) {
        try {
            Page<SimpleMemberDTO> result = memberQueryRepository.findAllMembers(PageRequest.of(page, size));
            redisMemberAdapter.saveMemberPage(page, size, result.getContent());
        } catch (Exception e) {
            log.warn("회원 페이지 캐시 비동기 갱신 실패: page={}, size={}, err={}", page, size, e.getMessage());
        } finally {
            cleanup.run();
        }
    }
}

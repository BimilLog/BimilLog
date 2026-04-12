package jaeik.bimillog.domain.member.scheduler;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>MemberCacheScheduler</h2>
 * <p>실험용 - 회원 목록 Redis 캐시 사전 적재 스케줄러</p>
 * <p>앱 기동 시 {@link PostConstruct}로 전체 회원 캐시를 1회 워밍하고,
 * TTL(1분) 만료 전에 50초마다 재적재하여 캐시를 항상 Warm 상태로 유지합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberCacheScheduler {

    private final MemberRepository memberRepository;
    private final RedisMemberAdapter redisMemberAdapter;

    @PostConstruct
    public void warmUpCache() {
        try {
            refreshMemberCache();
        } catch (Exception e) {
            log.warn("회원 캐시 워밍 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * TTL 1분보다 10초 빠른 50초 주기로 갱신하여 캐시를 항상 Warm 상태로 유지
     */
    @Scheduled(fixedDelay = 50_000)
    @Transactional(readOnly = true)
    public void refreshMemberCache() {
        List<SimpleMemberDTO> members = memberRepository.findAllSimpleMembersOrderByCreatedAtDesc();
        if (members.isEmpty()) {
            log.info("회원 데이터 없음 - 캐시 갱신 건너뜀");
            return;
        }
        redisMemberAdapter.saveMemberPage(0, 10, members);
        log.info("회원 캐시 갱신 완료. {}명 → {}개 페이지 적재", members.size(), (members.size() + 9) / 10);
    }
}

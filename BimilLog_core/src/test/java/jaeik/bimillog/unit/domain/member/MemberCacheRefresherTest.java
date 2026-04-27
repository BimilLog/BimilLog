package jaeik.bimillog.unit.domain.member;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.service.MemberCacheRefresher;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("MemberCacheRefresher 테스트")
@Tag("unit")
class MemberCacheRefresherTest extends BaseUnitTest {

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private RedisMemberAdapter redisMemberAdapter;

    @InjectMocks
    private MemberCacheRefresher memberCacheRefresher;

    @Test
    @DisplayName("refresh - 성공: DB 조회 → 캐시 저장 → cleanup 실행")
    void shouldSaveAndRunCleanup_onSuccess() {
        int page = 0, size = 20;
        List<SimpleMemberDTO> list = List.of(new SimpleMemberDTO(1L, "a"));
        given(memberQueryRepository.findAllMembers(PageRequest.of(page, size)))
                .willReturn(new PageImpl<>(list, PageRequest.of(page, size), list.size()));

        AtomicInteger cleanupCount = new AtomicInteger();
        memberCacheRefresher.refresh(page, size, cleanupCount::incrementAndGet);

        verify(redisMemberAdapter).saveMemberPage(page, size, list);
        assertThat(cleanupCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("refresh - 첫 호출이 아직 진행 중이면 두 번째 호출은 즉시 return")
    void shouldDedupeWhileInProgress() {
        int page = 0, size = 20;
        AtomicInteger cleanupCount = new AtomicInteger();

        // 첫 호출 중간에 두 번째 호출을 트리거하여 dedupe 검증
        given(memberQueryRepository.findAllMembers(any())).willAnswer(inv -> {
            memberCacheRefresher.refresh(page, size, cleanupCount::incrementAndGet);
            return new PageImpl<>(List.<SimpleMemberDTO>of(), PageRequest.of(page, size), 0);
        });

        memberCacheRefresher.refresh(page, size, cleanupCount::incrementAndGet);

        // 두 번째 호출은 inProgress 가드에 걸려 cleanup 실행 안 됨 (finally에 도달 못 함)
        assertThat(cleanupCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("refresh - DB 예외 발생해도 cleanup 실행")
    void shouldRunCleanup_onException() {
        int page = 0, size = 20;
        given(memberQueryRepository.findAllMembers(any()))
                .willThrow(new QueryTimeoutException("timeout"));

        AtomicInteger cleanupCount = new AtomicInteger();
        memberCacheRefresher.refresh(page, size, cleanupCount::incrementAndGet);

        assertThat(cleanupCount.get()).isEqualTo(1);
    }
}

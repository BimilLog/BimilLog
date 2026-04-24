package jaeik.bimillog.unit.domain.member;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.repository.SettingRepository;
import jaeik.bimillog.domain.member.service.MemberCacheRefresher;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter.CachedMemberPage;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * <h2>MemberQueryService 테스트</h2>
 * <p>사용자 조회 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>CLAUDE.md 테스트 철학에 따라 예외 처리 로직이 있는 findBySettingId만 테스트</p>
 * <p>단순 위임 메서드들은 테스트에서 제외 (테스트 불필요 카테고리)</p>
 *
 * @author Jaeik
 */
@DisplayName("MemberQueryService 테스트")
@Tag("unit")
class MemberQueryServiceTest extends BaseUnitTest {

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private RedisMemberAdapter redisMemberAdapter;

    @Mock
    private MemberCacheRefresher memberCacheRefresher;

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Test
    @DisplayName("설정 ID로 설정 조회 - 정상 케이스")
    void shouldFindSetting_WhenValidSettingId() {
        // Given
        Long settingId = 1L;
        Setting expectedSetting = Setting.builder()
                .id(settingId)
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        given(settingRepository.findById(settingId)).willReturn(Optional.of(expectedSetting));

        // When
        Setting result = memberQueryService.findBySettingId(settingId);

        // Then
        verify(settingRepository).findById(settingId);
        assertThat(result).isEqualTo(expectedSetting);
        assertThat(result.getId()).isEqualTo(settingId);
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isCommentNotification()).isFalse();
        assertThat(result.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("설정 ID로 설정 조회 - 설정이 존재하지 않는 경우")
    void shouldThrowException_WhenSettingNotFound() {
        // Given
        Long settingId = 999L;

        given(settingRepository.findById(settingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberQueryService.findBySettingId(settingId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_SETTINGS_NOT_FOUND.getMessage());

        verify(settingRepository).findById(settingId);
    }

    @Test
    @DisplayName("findAllMembers - 캐시 히트 + fresh: 캐시 반환, 리프레셔 호출 없음")
    void shouldReturnCached_whenFresh() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<SimpleMemberDTO> list = List.of(new SimpleMemberDTO(1L, "a"), new SimpleMemberDTO(2L, "b"));
        CachedMemberPage fresh = new CachedMemberPage(System.currentTimeMillis(), list);

        given(redisMemberAdapter.lookup(page, size)).willReturn(fresh);

        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        assertThat(result.getContent()).containsExactlyElementsOf(list);
        verify(memberCacheRefresher, never()).refresh(anyInt(), anyInt(), any());
        verify(memberQueryRepository, never()).findAllMembers(any());
        verify(redisMemberAdapter, never()).saveMemberPage(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("findAllMembers - 캐시 히트 + stale: 스테일 반환 후 비동기 리프레셔 1회 호출")
    void shouldReturnStaleAndTriggerRefresh_whenStale() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<SimpleMemberDTO> list = List.of(new SimpleMemberDTO(1L, "a"));
        CachedMemberPage stale = new CachedMemberPage(System.currentTimeMillis() - 55_000L, list);

        given(redisMemberAdapter.lookup(page, size)).willReturn(stale);

        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        assertThat(result.getContent()).containsExactlyElementsOf(list);
        verify(memberCacheRefresher, times(1)).refresh(eq(page), eq(size), any());
        verify(memberQueryRepository, never()).findAllMembers(any());
    }

    @Test
    @DisplayName("findAllMembers - stale 중복 호출 시 리프레셔는 1회만 호출 (inFlight 마커로 dedupe)")
    void shouldDedupeStaleRefresh_whenInFlight() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<SimpleMemberDTO> list = List.of(new SimpleMemberDTO(1L, "a"));
        CachedMemberPage stale = new CachedMemberPage(System.currentTimeMillis() - 55_000L, list);

        given(redisMemberAdapter.lookup(page, size)).willReturn(stale);

        memberQueryService.findAllMembers(pageable);
        memberQueryService.findAllMembers(pageable);
        memberQueryService.findAllMembers(pageable);

        // 리프레셔는 첫 호출만 트리거, 이후는 inFlight에 마커 있어서 skip
        verify(memberCacheRefresher, times(1)).refresh(eq(page), eq(size), any());
    }

    @Test
    @DisplayName("findAllMembers - 캐시 미스: DB 조회 + 캐시 저장")
    void shouldLoadFromDbAndSave_whenCacheMiss() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<SimpleMemberDTO> list = List.of(new SimpleMemberDTO(10L, "x"), new SimpleMemberDTO(11L, "y"));
        Page<SimpleMemberDTO> dbResult = new PageImpl<>(list, pageable, list.size());

        given(redisMemberAdapter.lookup(page, size)).willReturn(null);
        given(memberQueryRepository.findAllMembers(pageable)).willReturn(dbResult);

        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        assertThat(result.getContent()).containsExactlyElementsOf(list);
        verify(memberQueryRepository).findAllMembers(pageable);
        verify(redisMemberAdapter).saveMemberPage(page, size, list);
        verify(memberCacheRefresher, never()).refresh(anyInt(), anyInt(), any());
    }
}

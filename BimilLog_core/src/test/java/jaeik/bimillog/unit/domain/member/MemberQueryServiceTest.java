package jaeik.bimillog.unit.domain.member;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.domain.member.repository.SettingRepository;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.member.CacheMemberDTO;
import jaeik.bimillog.infrastructure.redis.member.RedisMemberAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * <h2>MemberQueryService 테스트</h2>
 * <p>사용자 조회 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 */
@DisplayName("MemberQueryService 테스트")
@Tag("unit")
class MemberQueryServiceTest extends BaseUnitTest {

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private RedisMemberAdapter redisMemberAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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

    // ==================== findAllMembers ====================

    @Test
    @DisplayName("findAllMembers - 캐시 히트, TTL 충분 → 캐시 반환, DB/저장 미호출")
    void shouldReturnFromCache_WhenCacheHit() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SimpleMemberDTO> cached = createSimpleMembers(10);
        Page<SimpleMemberDTO> cachedPage = new PageImpl<>(cached, pageable, cached.size());
        // TTL이 충분히 큰 값 → PER 조건 미충족 (갱신 이벤트 미발행)
        CacheMemberDTO cacheDto = CacheMemberDTO.from(cachedPage, 3600.0);

        given(redisMemberAdapter.getMemberByPageWithPER(0, 10)).willReturn(Optional.of(cacheDto));

        // When
        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getContent().get(0).getMemberId()).isEqualTo(1L);
        verify(redisMemberAdapter).getMemberByPageWithPER(0, 10);
        verify(memberQueryRepository, never()).findAllMembers(any(Pageable.class));
        verify(redisMemberAdapter, never()).saveMemberPage(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("findAllMembers - PER 조건 충족 (TTL 임박) → 캐시 반환 + 갱신 이벤트 발행")
    void shouldPublishRefreshEvent_WhenPERConditionMet() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SimpleMemberDTO> cached = createSimpleMembers(10);
        Page<SimpleMemberDTO> cachedPage = new PageImpl<>(cached, pageable, cached.size());
        // TTL이 0에 가까우면 PER 공식 gap > TTL 조건이 반드시 충족됨
        CacheMemberDTO cacheDto = CacheMemberDTO.from(cachedPage, 0.0);

        given(redisMemberAdapter.getMemberByPageWithPER(0, 10)).willReturn(Optional.of(cacheDto));

        // When
        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(10);
        verify(eventPublisher).publishEvent(any());
        verify(memberQueryRepository, never()).findAllMembers(any(Pageable.class));
    }

    @Test
    @DisplayName("findAllMembers - 캐시 미스 → 싱글플라이트로 DB 조회 후 캐시 저장")
    void shouldFallbackToDb_WhenCacheMiss() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SimpleMemberDTO> dbMembers = createSimpleMembers(5);
        Page<SimpleMemberDTO> dbPage = new PageImpl<>(dbMembers, pageable, dbMembers.size());

        given(redisMemberAdapter.getMemberByPageWithPER(0, 10)).willReturn(Optional.empty());
        given(memberQueryRepository.findAllMembers(pageable)).willReturn(dbPage);

        // When
        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(5);
        verify(memberQueryRepository).findAllMembers(pageable);
        verify(redisMemberAdapter).saveMemberPage(eq(0), eq(10), any());
    }

    @Test
    @DisplayName("findAllMembers - 캐시 미스 + DB 빈 결과 → 빈 페이지, 캐시 저장 시도")
    void shouldReturnEmptyPage_WhenCacheMissAndDbEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimpleMemberDTO> emptyPage = Page.empty(pageable);

        given(redisMemberAdapter.getMemberByPageWithPER(0, 10)).willReturn(Optional.empty());
        given(memberQueryRepository.findAllMembers(pageable)).willReturn(emptyPage);

        // When
        Page<SimpleMemberDTO> result = memberQueryService.findAllMembers(pageable);

        // Then
        assertThat(result.isEmpty()).isTrue();
        verify(memberQueryRepository).findAllMembers(pageable);
        verify(redisMemberAdapter).saveMemberPage(eq(0), eq(10), any());
    }

    @Test
    @DisplayName("findAllMembers - page=2, size=20 캐시 미스 → 올바른 page/size로 캐시 저장")
    void shouldSaveWithCorrectPageAndSize_WhenDifferentPagable() {
        // Given
        Pageable pageable = PageRequest.of(2, 20);
        List<SimpleMemberDTO> dbMembers = createSimpleMembers(5);
        Page<SimpleMemberDTO> dbPage = new PageImpl<>(dbMembers, pageable, dbMembers.size());

        given(redisMemberAdapter.getMemberByPageWithPER(2, 20)).willReturn(Optional.empty());
        given(memberQueryRepository.findAllMembers(pageable)).willReturn(dbPage);

        // When
        memberQueryService.findAllMembers(pageable);

        // Then: page=2, size=20 그대로 전달되는지 확인
        verify(redisMemberAdapter).saveMemberPage(eq(2), eq(20), any());
    }

    // ==================== 헬퍼 ====================

    private List<SimpleMemberDTO> createSimpleMembers(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new SimpleMemberDTO((long) i, "member" + i))
                .toList();
    }

    private List<Member> createMembersWithIds(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> createTestMemberWithId((long) i))
                .toList();
    }
}

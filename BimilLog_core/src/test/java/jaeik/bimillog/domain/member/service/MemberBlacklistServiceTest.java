package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import jaeik.bimillog.domain.member.repository.MemberBlacklistQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberBlacklistRepository;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("MemberBlacklistService 테스트")
@Tag("unit")
class MemberBlacklistServiceTest extends BaseUnitTest {

    private static final Long BLACKLIST_ID = 100L;
    private static final Long OWNER_MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 3L;

    @Mock
    private MemberBlacklistQueryRepository memberBlacklistQueryRepository;

    @Mock
    private MemberBlacklistRepository memberBlacklistRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberBlacklistService memberBlacklistService;

    @Test
    @DisplayName("블랙리스트 삭제 - 성공")
    void shouldDeleteBlacklist_WhenValidOwner() {
        // Given
        Member requestMember = getTestMember();  // ID=1L
        Member blackMember = getOtherMember();   // ID=2L

        MemberBlacklist blacklist = MemberBlacklist.createMemberBlacklist(requestMember, blackMember);
        TestFixtures.setFieldValue(blacklist, "id", BLACKLIST_ID);

        given(memberBlacklistRepository.findById(BLACKLIST_ID)).willReturn(Optional.of(blacklist));

        // When
        memberBlacklistService.deleteMemberFromMyBlacklist(BLACKLIST_ID, OWNER_MEMBER_ID);

        // Then
        verify(memberBlacklistRepository, times(1)).findById(BLACKLIST_ID);
        verify(memberBlacklistRepository, times(1)).deleteById(BLACKLIST_ID);
        verifyNoMoreInteractions(memberBlacklistRepository);
    }

    @Test
    @DisplayName("블랙리스트 삭제 - 블랙리스트 없음 예외")
    void shouldThrowException_WhenBlacklistNotFound() {
        // Given
        Long nonExistentBlacklistId = 999L;

        given(memberBlacklistRepository.findById(nonExistentBlacklistId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberBlacklistService.deleteMemberFromMyBlacklist(
                nonExistentBlacklistId, OWNER_MEMBER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_BLACKLIST_NOT_FOUND);

        verify(memberBlacklistRepository, times(1)).findById(nonExistentBlacklistId);
        verify(memberBlacklistRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("블랙리스트 삭제 - 권한 없음 예외 (타인의 블랙리스트)")
    void shouldThrowException_WhenNotOwner() {
        // Given
        Member requestMember = getTestMember();  // ID=1L (블랙리스트 소유자)
        Member blackMember = getOtherMember();   // ID=2L (차단된 회원)

        MemberBlacklist blacklist = MemberBlacklist.createMemberBlacklist(requestMember, blackMember);
        TestFixtures.setFieldValue(blacklist, "id", BLACKLIST_ID);

        given(memberBlacklistRepository.findById(BLACKLIST_ID)).willReturn(Optional.of(blacklist));

        // When & Then - User C(id=3)가 User A(id=1)의 블랙리스트 삭제 시도
        assertThatThrownBy(() -> memberBlacklistService.deleteMemberFromMyBlacklist(
                BLACKLIST_ID, OTHER_MEMBER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_BLACKLIST_FORBIDDEN);

        verify(memberBlacklistRepository, times(1)).findById(BLACKLIST_ID);
        verify(memberBlacklistRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("블랙리스트 체크 - 블랙리스트 관계가 아닌 경우 정상 처리")
    void shouldCheckMemberBlacklist_WhenNotBlacklisted() {
        // Given
        Long memberId = 1L;
        Long targetMemberId = 2L;

        given(memberBlacklistRepository.existsByRequestMemberIdAndBlackMemberId(memberId, targetMemberId))
                .willReturn(false);
        given(memberBlacklistRepository.existsByRequestMemberIdAndBlackMemberId(targetMemberId, memberId))
                .willReturn(false);

        // When & Then - 예외가 발생하지 않아야 함
        memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId);

        verify(memberBlacklistRepository, times(1))
                .existsByRequestMemberIdAndBlackMemberId(memberId, targetMemberId);
        verify(memberBlacklistRepository, times(1))
                .existsByRequestMemberIdAndBlackMemberId(targetMemberId, memberId);
    }

    @Test
    @DisplayName("블랙리스트 체크 - A가 B를 차단한 경우 예외 발생")
    void shouldThrowException_WhenABlockedB() {
        // Given
        Long memberId = 1L;
        Long targetMemberId = 2L;

        given(memberBlacklistRepository.existsByRequestMemberIdAndBlackMemberId(memberId, targetMemberId))
                .willReturn(true);
        given(memberBlacklistRepository.existsByRequestMemberIdAndBlackMemberId(targetMemberId, memberId))
                .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);

        // 실제 구현은 항상 두 번 모두 호출하므로 둘 다 검증
        verify(memberBlacklistRepository).existsByRequestMemberIdAndBlackMemberId(memberId, targetMemberId);
        verify(memberBlacklistRepository).existsByRequestMemberIdAndBlackMemberId(targetMemberId, memberId);
    }

    @Test
    @DisplayName("블랙리스트 체크 - B가 A를 차단한 경우 예외 발생")
    void shouldThrowException_WhenBBlockedA() {
        // Given
        Long memberId = 1L;
        Long targetMemberId = 2L;

        given(memberBlacklistRepository.existsByRequestMemberIdAndBlackMemberId(memberId, targetMemberId))
                .willReturn(false);
        given(memberBlacklistRepository.existsByRequestMemberIdAndBlackMemberId(targetMemberId, memberId))
                .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);

        verify(memberBlacklistRepository, times(1))
                .existsByRequestMemberIdAndBlackMemberId(memberId, targetMemberId);
        verify(memberBlacklistRepository, times(1))
                .existsByRequestMemberIdAndBlackMemberId(targetMemberId, memberId);
    }
}

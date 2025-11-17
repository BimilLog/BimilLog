package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import jaeik.bimillog.domain.member.out.MemberBlacklistQueryRepository;
import jaeik.bimillog.domain.member.out.MemberBlacklistRepository;
import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
        Pageable pageable = PageRequest.of(0, 10);

        MemberBlacklist blacklist = MemberBlacklist.createMemberBlacklist(requestMember, blackMember);
        TestFixtures.setFieldValue(blacklist, "id", BLACKLIST_ID);

        given(memberBlacklistRepository.findById(BLACKLIST_ID)).willReturn(Optional.of(blacklist));

        // When
        memberBlacklistService.deleteMemberFromMyBlacklist(BLACKLIST_ID, OWNER_MEMBER_ID, pageable);

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
        Pageable pageable = PageRequest.of(0, 10);

        given(memberBlacklistRepository.findById(nonExistentBlacklistId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberBlacklistService.deleteMemberFromMyBlacklist(
                nonExistentBlacklistId, OWNER_MEMBER_ID, pageable))
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
        Pageable pageable = PageRequest.of(0, 10);

        MemberBlacklist blacklist = MemberBlacklist.createMemberBlacklist(requestMember, blackMember);
        TestFixtures.setFieldValue(blacklist, "id", BLACKLIST_ID);

        given(memberBlacklistRepository.findById(BLACKLIST_ID)).willReturn(Optional.of(blacklist));

        // When & Then - User C(id=3)가 User A(id=1)의 블랙리스트 삭제 시도
        assertThatThrownBy(() -> memberBlacklistService.deleteMemberFromMyBlacklist(
                BLACKLIST_ID, OTHER_MEMBER_ID, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_BLACKLIST_FORBIDDEN);

        verify(memberBlacklistRepository, times(1)).findById(BLACKLIST_ID);
        verify(memberBlacklistRepository, never()).deleteById(any());
    }
}

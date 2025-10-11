package jaeik.bimillog.infrastructure.adapter.out.member;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>SaveMemberAdapter 단위 테스트</h2>
 * <p>회원 엔티티를 저장소에 위임하는 책임을 검증합니다.</p>
 */
@Tag("unit")
class SaveMemberAdapterTest extends BaseUnitTest {

    @Mock private MemberRepository memberRepository;

    @InjectMocks private SaveMemberAdapter saveMemberAdapter;

    @Test
    @DisplayName("엔티티를 저장소에 위임한다")
    void shouldDelegateMemberPersist() {
        Member member = TestMembers.createMember("social-id", "tester", "nickname");
        Member persisted = TestMembers.copyWithId(member, 1L);

        when(memberRepository.save(any(Member.class))).thenReturn(persisted);

        Member result = saveMemberAdapter.saveNewMember(member);

        assertThat(result).isSameAs(persisted);
        verify(memberRepository).save(member);
    }
}

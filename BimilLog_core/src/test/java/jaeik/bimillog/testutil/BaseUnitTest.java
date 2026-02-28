package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.member.entity.Member;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * <h2>단위 테스트 베이스 클래스</h2>
 * <p>모든 단위 테스트가 상속받아 사용하는 기본 클래스</p>
 * <p>Mockito 설정과 공통 테스트 데이터를 lazy 초기화로 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>MockitoExtension 자동 적용</li>
 *   <li>공통 테스트 회원 (testMember, otherMember) - lazy 초기화</li>
 *   <li>필요한 데이터만 생성하여 테스트 성능 향상</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    // Lazy 초기화를 위한 필드들 (실제 사용 시점에 초기화)
    private Member cachedTestMember;
    private Member cachedOtherMember;

    /**
     * 기본 테스트 회원 획득 (일반 권한)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected Member getTestMember() {
        if (cachedTestMember == null) {
            cachedTestMember = TestMembers.copyWithId(TestMembers.MEMBER_1, 1L);
        }
        return cachedTestMember;
    }

    /**
     * 추가 테스트 회원 획득 (다른 회원 시나리오용)
     * 첫 호출 시 생성, 이후 캐시된 인스턴스 반환
     */
    protected Member getOtherMember() {
        if (cachedOtherMember == null) {
            cachedOtherMember = TestMembers.copyWithId(TestMembers.MEMBER_2, 2L);
        }
        return cachedOtherMember;
    }

    /**
     * ID가 포함된 테스트 회원 생성 헬퍼 메서드
     * @param memberId 회원 ID
     * @return ID가 설정된 테스트 회원
     */
    protected Member createTestMemberWithId(Long memberId) {
        return TestMembers.copyWithId(getTestMember(), memberId);
    }

}

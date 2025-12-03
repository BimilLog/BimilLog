package jaeik.bimillog.domain.member.out;

import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface MemberBlacklistRepository extends JpaRepository<MemberBlacklist, Long> {

    boolean existsByRequestMemberIdAndBlackMemberId(Long requestMemberId, Long blackMemberId);

    /**
     * <h3>블랙리스트 회원 ID 조회</h3>
     * <p>특정 회원이 차단한 회원들의 ID만 조회합니다.</p>
     * <p>친구 추천 알고리즘에서 블랙리스트 사용자를 제외할 때 사용됩니다.</p>
     *
     * @param requestMemberId 차단을 요청한 회원 ID
     * @return 차단된 회원 ID 집합
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT bl.blackMember.id FROM MemberBlacklist bl WHERE bl.requestMember.id = :requestMemberId")
    Set<Long> findBlacklistIdsByRequestMemberId(@Param("requestMemberId") Long requestMemberId);
}

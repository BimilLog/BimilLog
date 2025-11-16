package jaeik.bimillog.domain.member.out;

import jaeik.bimillog.domain.member.entity.MemberBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberBlacklistRepository extends JpaRepository<MemberBlacklist, Long> {

}

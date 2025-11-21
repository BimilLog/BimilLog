package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByMemberIdAndFriendId(Long memberId, Long friendId);

}


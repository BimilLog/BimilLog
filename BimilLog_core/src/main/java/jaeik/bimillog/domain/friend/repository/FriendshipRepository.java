package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
}


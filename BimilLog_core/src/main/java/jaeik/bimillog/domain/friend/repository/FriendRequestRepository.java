package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
}

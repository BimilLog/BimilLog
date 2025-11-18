package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsBySenderIdAndReceiverId(Long requestMemberId, Long blackMemberId);

}

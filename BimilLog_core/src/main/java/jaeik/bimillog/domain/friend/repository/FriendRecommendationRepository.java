package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRecommendationRepository extends JpaRepository<FriendRecommendation, Long> {
}

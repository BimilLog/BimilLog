package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRecommendationRepository extends JpaRepository<FriendRecommendation, Long> {

    /**
     * 특정 멤버의 모든 추천친구 데이터를 삭제합니다.
     * 배치 스케줄러에서 추천친구를 갱신할 때 사용됩니다.
     *
     * @param memberId 삭제할 멤버 ID
     */
    @Modifying
    @Query("DELETE FROM FriendRecommendation fr WHERE fr.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}

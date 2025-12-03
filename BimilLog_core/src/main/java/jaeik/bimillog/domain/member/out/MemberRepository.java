package jaeik.bimillog.domain.member.out;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <h2>사용자 Repository</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 위한 Repository
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 조회</h3>
     * <p>주어진 소셜 제공자와 소셜 ID로 사용자 정보를 조회합니다.</p>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Member> findByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>주어진 닉네임으로 사용자 정보를 조회합니다.</p>
     *
     * @param memberName 조회할 닉네임
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Member> findByMemberName(String memberName);

    /**
     * <h3>닉네임 존재 여부 확인</h3>
     * <p>주어진 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     *
     * @param memberName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByMemberName(String memberName);

    /**
     * <h3>ID와 설정을 포함한 사용자 조회</h3>
     * <p>주어진 ID로 사용자 정보를 조회하며, 연관된 설정 정보도 함께 가져옵니다.</p>
     * <p>{@link MemberQueryService}에서 설정 정보가 필요한 조회 시 호출됩니다.</p>
     *
     * @param id 사용자 ID
     * @return Optional<Member> 조회된 사용자 객체 (설정 정보 포함). 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.setting WHERE m.id = :id")
    Optional<Member> findByIdWithSetting(@Param("id") Long id);

    /**
     * 여러 사용자 ID로 친구 추가 정보 조회
     * 친구 조회시 사용
     */
    List<Friend.FriendInfo> findFriendInfoByIdIn(List<Long> id);

    /**
     * 여러 사용자 ID로 추천 친구 추가 정보 조회
     */
    List<RecommendedFriend.RecommendedFriendInfo> findRecommendedFriendInfoByIdIn(List<Long> id);

    /**
     * 여러 사용자 ID로 추천 친구 아는 사람 추가 정보 조회
     */
    List<RecommendedFriend.AcquaintanceInfo> findAcquaintanceInfoByIdIn(List<Long> id);

    /**
     * <h3>접두사 검색 (인덱스 활용)</h3>
     * <p>LIKE 'query%' 조건으로 멤버명을 검색하여 인덱스를 활용합니다.</p>
     * <p>{@link MemberQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 검색된 멤버명 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleMemberDTO> findByMemberNameStartingWithOrderByMemberNameAsc(String memberName, Pageable pageable);

    /**
     * <h3>부분 문자열 검색 (인덱스 미활용)</h3>
     * <p>LIKE '%query%' 조건으로 멤버명 부분 검색을 수행합니다.</p>
     * <p>{@link MemberQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 검색된 멤버명 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleMemberDTO> findByMemberNameContainingOrderByMemberNameAsc(String memberName, Pageable pageable);

    /**
     * <h3>최근 가입자 조회</h3>
     * <p>생성 날짜(createdAt) 기준으로 최근 가입한 회원들의 ID를 조회합니다.</p>
     * <p>친구 추천 알고리즘에서 추천 인원이 부족할 때 사용됩니다.</p>
     *
     * @return 최근 가입자 ID 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> findIdByIdNotInOrderByCreatedAtDesc(Set<Long> id, Pageable pageable);
}
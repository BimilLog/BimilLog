package jaeik.growfarm.repository.user;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>사용자 커스텀 저장소 인터페이스</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 수행하며 커스텀한 쿼리메소드가 포함되어 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface UserCustomRepository {

    /**
     * <h3>카카오 ID 목록으로 농장 이름 조회</h3>
     * <p>
     * 카카오 ID 목록의 순서대로 농장 이름을 조회한다.
     * </p>
     *
     * @param ids 카카오 ID 목록
     * @return 농장 이름 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    List<String> findFarmNamesInOrder(List<Long> ids);

}

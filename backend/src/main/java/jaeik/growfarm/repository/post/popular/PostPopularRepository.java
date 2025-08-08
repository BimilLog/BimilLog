package jaeik.growfarm.repository.post.popular;

import jaeik.growfarm.dto.post.SimplePostDTO;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * <h2>게시글 인기글 저장소</h2>
 * <p>
 * ISP(Interface Segregation Principle) 적용으로 분리된 인터페이스
 * </p>
 * <p>
 * 인기글 선정 및 관리 기능만을 담당
 * </p>
 * 
 * @author jaeik
 * @version 1.0.21
 * @since 1.0.21
 */
@Repository
public interface PostPopularRepository {

    /**
     * <h3>실시간 인기글 선정</h3>
     * <p>
     * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
     * </p>
     * 
     * @return 실시간 인기글 목록
     * @author Jaeik
     * @since 1.0.21
     */
    List<SimplePostDTO> updateRealtimePopularPosts();

    /**
     * <h3>주간 인기글 선정</h3>
     * <p>
     * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
     * </p>
     * 
     * @return 주간 인기글 목록
     * @author Jaeik
     * @since 1.0.21
     */
    List<SimplePostDTO> updateWeeklyPopularPosts();

    /**
     * <h3>레전드 인기글 선정</h3>
     * <p>
     * 추천 수가 20개 이상인 글을 레전드 인기글로 선정한다.
     * </p>
     * 
     * @return 레전드 인기글 목록
     * @author Jaeik
     * @since 1.0.21
     */
    List<SimplePostDTO> updateLegendPosts();
}
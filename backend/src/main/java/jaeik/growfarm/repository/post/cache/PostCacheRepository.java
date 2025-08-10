package jaeik.growfarm.repository.post.cache;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * <h2>게시글 캐시 저장소</h2>
 * <p>
 * 캐시글 선정 및 관리 기능을 담당 (인기글, 공지사항 등)
 * </p>
 * 
 * @author jaeik
 * @version 2.0.0
 */
@Repository
public interface PostCacheRepository {

    /**
     * <h3>실시간 인기글 선정</h3>
     * <p>
     * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
     * </p>
     * 
     * @return 실시간 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> updateRealtimePopularPosts();

    /**
     * <h3>주간 인기글 선정</h3>
     * <p>
     * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
     * </p>
     * 
     * @return 주간 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> updateWeeklyPopularPosts();

    /**
     * <h3>레전드 인기글 선정</h3>
     * <p>
     * 추천 수가 20개 이상인 글을 레전드 인기글로 선정한다.
     * </p>
     * 
     * @return 레전드 인기글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> updateLegendPosts();

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>
     * 공지사항으로 설정된 게시글 목록을 최신순으로 조회한다.
     * </p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<SimplePostResDTO> findNoticePost();
}
package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;

import java.util.List;

/**
 * <h2>게시글 캐시 동기화 Port</h2>
 * <p>스케쥴러가 캐시 데이터를 생성하기 위해 데이터베이스에서 게시글을 조회하는 인터페이스입니다.</p>
 * <p>캐시 저장을 위한 데이터 소스 역할을 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheSyncPort {

    /**
     * <h3>실시간 인기 게시글 조회</h3>
     * <p>실시간 인기 게시글 목록을 데이터베이스에서 조회하여 스케쥴러에 반환합니다.</p>
     *
     * @return 실시간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> findRealtimePopularPosts();

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>주간 인기 게시글 목록을 데이터베이스에서 조회하여 스케쥴러에 반환합니다.</p>
     *
     * @return 주간 인기 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> findWeeklyPopularPosts();

    /**
     * <h3>전설의 게시글 조회</h3>
     * <p>전설의 게시글 목록을 데이터베이스에서 조회하여 스케쥴러에 반환합니다.</p>
     *
     * @return 전설의 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> findLegendaryPosts();
    
    /**
     * <h3>게시글 상세 정보 조회</h3>
     * <p>게시글 ID로 상세 정보를 조회하여 캐싱할 수 있는 형태로 반환합니다.</p>
     *
     * @param postId 게시글 ID
     * @return 게시글 상세 정보
     * @author Jaeik
     * @since 2.0.0
     */
    PostDetail findPostDetail(Long postId);
}

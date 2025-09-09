package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * <h2>PostQueryUseCase</h2>
 * <p>
 * Post 도메인의 조회 전용 비즈니스 로직을 처리하는 인바운드 포트입니다.
 * 헥사고날 아키텍처에서 CQRS 패턴의 Query 측면을 담당하며, 게시글 데이터의 모든 조회 작업을 처리합니다.
 * </p>
 * <p>
 * 이 유스케이스는 다양한 게시글 조회 기능을 제공합니다:
 * - 게시글 목록: 일반, 인기, 공지사항 목록 조회
 * - 게시글 상세: 게시글 내용과 연관 정보 통합 조회
 * - 게시글 검색: 제목, 내용, 작성자 기반 검색
 * - 사용자 활동: 작성글, 추천글 내역 조회
 * - 인기 콘텐츠: 실시간, 주간, 레전드 인기글 조회
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 사용자 경험 - 다양한 조회 옵션과 빠른 검색 제공
 * 2. 성능 최적화 - 페이지네이션과 JOIN을 통한 효율적 데이터 조회
 * 3. 개인화 서비스 - 사용자별 게시글 활동 내역 제공
 * 4. 커뮤니티 운영 - 검색과 분류를 통한 콘텐츠 관리
 * 5. 캐시 활용 - Redis 캐시와 DB 조회의 적절한 조합
 * </p>
 * <p>
 * PostQueryController에서 게시글 목록 조회 API 제공 시 호출됩니다.
 * UserController에서 사용자 활동 내역 조회 시 호출됩니다.
 * 웹 페이지에서 게시글 목록 및 검색 기능 제공 시 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostQueryUseCase {

    /**
     * <h3>게시판 메인 목록 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>일반 사용자가 커뮤니티 메인 페이지에서 전체 게시글을 통해 볼 때 사용됩니다.</p>
     * <p>PostQueryController에서 게시판 목록 API 요청을 처리할 때 호출됩니다.</p>
     * <p>페이지네이션을 통해 대량의 게시글도 효율적으로 조회할 수 있습니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 정렬 기준 포함)
     * @return Page<PostSearchResult> 최신순으로 정렬된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getBoard(Pageable pageable);

    /**
     * <h3>게시글 상세 페이지 조회</h3>
     * <p>특정 게시글의 상세 내용과 댓글수, 추천수, 사용자 추천여부 등을 통합 조회합니다.</p>
     * <p>JOIN 쿼리를 통해 게시글 상세 페이지에 필요한 모든 정보를 한 번에 조회합니다.</p>
     * <p>PostQueryController에서 게시글 상세 보기 API 요청을 처리할 때 호출됩니다.</p>
     * <p>웹 페이지에서 사용자가 게시글을 클릭하여 상세 페이지로 이동할 때 사용됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @param userId 현재 로그인한 사용자 ID (추천여부 확인용, null 가능)
     * @return PostDetail 게시글 상세 정보와 연관 데이터
     * @author Jaeik
     * @since 2.0.0
     */
    PostDetail getPost(Long postId, Long userId);

    /**
     * <h3>게시글 전문 검색</h3>
     * <p>검색 타입과 검색어를 기반으로 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     * <p>MySQL의 ngram parser를 활용한 전문 검색으로 한국어 검색을 지원합니다.</p>
     * <p>PostQueryController에서 게시글 검색 API 요청을 처리할 때 호출됩니다.</p>
     * <p>웹 페이지의 검색 박스에서 사용자의 검색 요청을 처리할 때 사용됩니다.</p>
     *
     * @param type 검색 대상 유형 (title: 제목, content: 내용, author: 작성자)
     * @param query 검색할 키워드 (한국어 단어 지원)
     * @param pageable 페이지 정보 (결과 개수 제한 및 정렬)
     * @return Page<PostSearchResult> 검색 조건에 맞는 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> searchPost(String type, String query, Pageable pageable);

    /**
     * <h3>실시간 및 주간 인기글 동시 조회</h3>
     * <p>실시간과 주간 인기 게시글을 한 번에 조회하여 페이지 로딩 시간을 단축합니다.</p>
     * <p>Redis 캐시에서 미리 계산된 인기글 데이터를 효율적으로 가져옵니다.</p>
     * <p>PostQueryController에서 메인 페이지 인기글 섹션 API 요청 시 호출됩니다.</p>
     * <p>웹 페이지의 인기글 탭에서 두 종류의 인기글을 동시에 표시할 때 사용됩니다.</p>
     *
     * @return Map<String, List<PostSearchResult>> 인기글 맵 ("realtime": 실시간, "weekly": 주간)
     * @author Jaeik
     * @since 2.0.0
     */
    Map<String, List<PostSearchResult>> getRealtimeAndWeeklyPosts();

    /**
     * <h3>레전드 인기글 목록 페이지네이션 조회</h3>
     * <p>역대 최고 인기글로 선정된 레전드 게시글 목록을 페이지 단위로 조회합니다.</p>
     * <p>Redis List 구조를 활용하여 대량의 레전드 게시글도 효율적으로 페이지네이션합니다.</p>
     * <p>PostQueryController에서 레전드 게시글 목록 API 요청 시 호출됩니다.</p>
     * <p>레전드 게시글 전용 페이지에서 사용자가 과거 명작들을 톨아보는 용도로 사용됩니다.</p>
     *
     * @param type 조회할 캐시 유형 (PostCacheFlag.LEGEND 고정값)
     * @param pageable 페이지 정보 (페이지 번호, 크기)
     * @return Page<PostSearchResult> 레전드 인기 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getPopularPostLegend(PostCacheFlag type, Pageable pageable);

    /**
     * <h3>공지사항 목록 전체 조회</h3>
     * <p>관리자가 지정한 공지사항 게시글 목록을 Redis 캐시에서 조회합니다.</p>
     * <p>공지사항은 페원며 개수가 많지 않으므로 전체 목록을 한 번에 반환합니다.</p>
     * <p>PostQueryController에서 공지사항 목록 API 요청 시 호출됩니다.</p>
     * <p>웹 페이지에서 게시판 상단에 공지사항을 노출할 때 사용됩니다.</p>
     *
     * @return List<PostSearchResult> 캐시된 공지사항 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<PostSearchResult> getNoticePosts();

    /**
     * <h3>크로스 도메인 게시글 엔티티 조회</h3>
     * <p>다른 도메인에서 Post 엔티티가 필요할 때 사용하는 단순 조회 메서드입니다.</p>
     * <p>예외 처리를 위해 Post 엔티티를 직접 반환하여 도메인 간 의존성을 최소화합니다.</p>
     * <p>CommentService에서 댓글 작성 시 게시글 존재성 검증을 위해 호출됩니다.</p>
     * <p>NotificationService에서 알림 발송 시 게시글 정보 획득을 위해 호출됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @return Post 게시글 엔티티 (예외 발생 시 처리)
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long postId);

    /**
     * <h3>사용자 작성 게시글 내역 조회</h3>
     * <p>특정 사용자가 작성한 모든 게시글 목록을 최신순으로 페이지네이션 조회합니다.</p>
     * <p>사용자 프로필 페이지에서 자신의 게시 활동 내역을 확인할 때 사용됩니다.</p>
     * <p>UserController에서 사용자 작성글 목록 API 요청 시 호출됩니다.</p>
     * <p>마이페이지에서 사용자가 자신의 과거 작성글들을 관리할 때 사용됩니다.</p>
     *
     * @param userId 작성글을 조회할 사용자의 식별자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return Page<PostSearchResult> 사용자가 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getUserPosts(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천 게시글 내역 조회</h3>
     * <p>특정 사용자가 추천한 모든 게시글 목록을 최신순으로 페이지네이션 조회합니다.</p>
     * <p>사용자 프로필 페이지에서 자신이 추천한 게시글 내역을 확인할 때 사용됩니다.</p>
     * <p>UserController에서 사용자 추천글 목록 API 요청 시 호출됩니다.</p>
     * <p>마이페이지에서 사용자가 과거에 관심을 보인 게시글들을 다시 찾을 때 사용됩니다.</p>
     *
     * @param userId 추천글을 조회할 사용자의 식별자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return Page<PostSearchResult> 사용자가 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSearchResult> getUserLikedPosts(Long userId, Pageable pageable);
}

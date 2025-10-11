package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.infrastructure.adapter.in.post.web.PostQueryController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>게시글 조회 유스케이스</h2>
 * <p>게시글 도메인의 조회 작업을 담당하는 유스케이스입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색</p>
 * <p>인기글 조회, 사용자별 활동 내역 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostQueryUseCase {

    /**
     * <h3>게시판 메인 목록 조회</h3>
     * <p>전체 게시글을 최신순으로 정렬하여 페이지 단위로 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryController}에서 GET /api/post 요청 처리 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보 (크기, 정렬 기준 포함)
     * @return Page<PostSimpleDetail> 최신순으로 정렬된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> getBoard(Pageable pageable);

    /**
     * <h3>게시글 상세 페이지 조회</h3>
     * <p>특정 게시글의 상세 내용과 댓글수, 추천수, 사용자 추천여부를 조회합니다.</p>
     * <p>JOIN으로 게시글 상세 페이지에 필요한 모든 정보를 한 번에 조회합니다.</p>
     * <p>{@link PostQueryController}에서 GET /api/post/{postId} 요청 처리 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @param memberId 현재 로그인한 사용자 ID (추천여부 확인용, null 가능)
     * @return PostDetail 게시글 상세 정보와 연관 데이터
     * @author Jaeik
     * @since 2.0.0
     */
    PostDetail getPost(Long postId, Long memberId);

    /**
     * <h3>게시글 전문 검색</h3>
     * <p>검색 타입과 검색어를 기반으로 게시글을 검색합니다.</p>
     * <p>MySQL ngram parser를 활용하여 한국어 검색을 지원합니다.</p>
     * <p>{@link PostQueryController}에서 GET /api/post/search 요청 처리 시 호출됩니다.</p>
     *
     * @param type 검색 대상 유형 (TITLE: 제목, WRITER: 작성자, TITLE_CONTENT: 제목+내용)
     * @param query 검색할 키워드 (한국어 단어 지원)
     * @param pageable 페이지 정보 (결과 개수 제한 및 정렬)
     * @return Page<PostSimpleDetail> 검색 조건에 맞는 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> searchPost(PostSearchType type, String query, Pageable pageable);

    /**
     * <h3>크로스 도메인 게시글 엔티티 조회</h3>
     * <p>다른 도메인에서 Post 엔티티가 필요할 때 사용하는 단순 조회 메서드입니다.</p>
     * <p>댓글 작성 시 게시글 존재성 검증, 알림 발송 시 게시글 정보 획득</p>
     * <p>Comment 도메인, Notification 도메인에서 도메인 간 조회 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글의 식별자 ID
     * @return Post 게시글 엔티티 (예외 발생 시 처리)
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long postId);

    /**
     * <h3>사용자 작성 게시글 내역 조회</h3>
     * <p>특정 사용자가 작성한 모든 게시글 목록을 최신순으로 조회합니다.</p>
     * <p>사용자 프로필 페이지에서 작성 활동 내역 확인 시 사용</p>
     * <p>Member 도메인에서 사용자 활동 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId 작성글을 조회할 사용자의 식별자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return Page<PostSimpleDetail> 사용자가 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> getMemberPosts(Long memberId, Pageable pageable);

    /**
     * <h3>사용자 추천 게시글 내역 조회</h3>
     * <p>특정 사용자가 추천한 모든 게시글 목록을 최신순으로 조회합니다.</p>
     * <p>사용자 프로필 페이지에서 추천한 게시글 내역 확인 시 사용</p>
     * <p>Member 도메인에서 사용자 활동 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId 추천글을 조회할 사용자의 식별자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return Page<PostSimpleDetail> 사용자가 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PostSimpleDetail> getMemberLikedPosts(Long memberId, Pageable pageable);
}

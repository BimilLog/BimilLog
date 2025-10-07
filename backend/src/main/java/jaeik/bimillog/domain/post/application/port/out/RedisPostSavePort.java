package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;

import java.util.List;

/**
 * <h2>게시글 캐시 명령 포트</h2>
 * <p>Post 도메인의 Redis 캐시 데이터 생성, 수정, 삭제 작업을 담당하는 포트입니다.</p>
 * <p>인기글 캐시 데이터 생성 및 업데이트</p>
 * <p>게시글 상태 변경에 따른 캐시 동기화</p>
 * <p>인기 플래그 설정 및 배치 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface RedisPostSavePort {

    /**
     * <h3>인기글 postId 영구 저장</h3>
     * <p>인기글 postId 목록만 Redis List에 영구 또는 긴 TTL로 저장합니다.</p>
     * <p>목록 캐시 TTL 만료 시 복구용으로 사용됩니다.</p>
     * <p>PostScheduledService에서 주기적인 인기글 데이터 업데이트 시 호출됩니다.</p>
     * <p>PostAdminService에서 공지사항 설정/해제 시 호출됩니다.</p>
     *
     * @param type 캐시할 인기글 유형 (WEEKLY, LEGEND, NOTICE)
     * @param postIds 캐시할 게시글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostIdsOnly(PostCacheFlag type, List<Long> postIds);

    /**
     * <h3>인기글 목록 캐싱 (Hash 구조)</h3>
     * <p>인기글 목록을 Redis Hash에 저장합니다 (TTL 5분)</p>
     * <p>Hash 구조: Field는 postId, Value는 PostSimpleDetail 객체</p>
     * <p>PostScheduledService에서 주기적인 인기글 데이터 업데이트 시 호출됩니다.</p>
     *
     * @param type 캐시할 인기글 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @param posts 캐시할 게시글 목록 (PostSimpleDetail)
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostList(PostCacheFlag type, List<PostSimpleDetail> posts);


    /**
     * <h3>단일 게시글 상세 정보 캐싱</h3>
     * <p>게시글 상세 정보를 Redis 캐시에 저장합니다.</p>
     * <p>PostQueryService에서 캐시 어사이드 패턴으로 DB 조회 후 캐시 저장 시 호출됩니다.</p>
     *
     * @param postDetail 캐시할 게시글 상세 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void cachePostDetail(PostDetail postDetail);



    /**
     * <h3>postIds 저장소에 단일 게시글 추가</h3>
     * <p>postIds 영구 저장소의 앞에 게시글 ID를 추가합니다 (LPUSH).</p>
     * <p>공지사항 설정 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 추가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void addPostIdToStorage(PostCacheFlag type, Long postId);



}

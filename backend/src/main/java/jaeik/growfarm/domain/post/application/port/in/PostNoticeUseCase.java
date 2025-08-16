package jaeik.growfarm.domain.post.application.port.in;

import jaeik.growfarm.infrastructure.exception.CustomException;

/**
 * <h2>게시글 공지사항 유스케이스</h2>
 * <p>게시글의 공지사항 설정/해제와 관련된 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 * <p>관리자 권한이 필요한 기능들을 포함합니다.</p>
 * 
 * <p><strong>TODO: 현재 구현의 문제점</strong></p>
 * <ul>
 *   <li>공지사항 설정/해제 시 캐시에서 삭제만 하고, 실시간으로 새로운 공지사항을 캐시에 추가하지 않음</li>
 *   <li>캐시 삭제 후 사용자가 공지사항 목록 요청 시 빈 리스트 반환됨</li>
 *   <li>실시간 캐시 업데이트 로직이 필요함 (DB 조회 → 캐시 저장)</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostNoticeUseCase {

    /**
     * <h3>게시글 공지 설정</h3>
     * <p>특정 게시글을 공지로 설정합니다.</p>
     * <p>관리자 권한이 필요하며, 설정 후 공지사항 캐시를 무효화합니다.</p>
     *
     * @param postId 공지로 설정할 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    void setPostAsNotice(Long postId);

    /**
     * <h3>게시글 공지 해제</h3>
     * <p>게시글의 공지 설정을 해제합니다.</p>
     * <p>관리자 권한이 필요하며, 해제 후 공지사항 캐시를 무효화합니다.</p>
     *
     * @param postId 공지 설정을 해제할 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    void unsetPostAsNotice(Long postId);
}
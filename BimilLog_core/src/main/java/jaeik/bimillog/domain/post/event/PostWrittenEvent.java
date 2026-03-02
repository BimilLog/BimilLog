package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;

/**
 * <h2>글 작성 캐시 이벤트</h2>
 * <p>게시글 작성 후 첫 페이지 캐시에 반영하기 위한 이벤트</p>
 *
 * @param postDetail 새로 작성된 게시글 요약 정보
 * @author Jaeik
 * @version 2.8.0
 */
public record PostWrittenEvent(PostSimpleDetail postDetail) {}

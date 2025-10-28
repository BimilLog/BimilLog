package jaeik.bimillog.domain.post.entity;

import lombok.Getter;

/**
 * <h2>게시글 검색 타입</h2>
 * <p>게시글 검색 시 사용되는 검색 유형을 정의하는 enum입니다.</p>
 * <p>제목 검색, 작성자 검색, 제목+내용 검색</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum PostSearchType {
    TITLE,
    WRITER,
    TITLE_CONTENT
}
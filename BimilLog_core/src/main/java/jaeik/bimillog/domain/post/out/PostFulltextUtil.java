package jaeik.bimillog.domain.post.out;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>게시글 전문검색 유틸 클래스</h2>
 * <p>FULLTEXT 검색 결과 매핑을 처리합니다.</p>
 *
 * @author Jaeik
 * @since 2.3.0
 */
@Component
@RequiredArgsConstructor
public class PostFulltextUtil {

    /**
     * <h3>FULLTEXT 검색 결과 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열 목록을 PostSimpleDetail 목록으로 변환합니다.</p>
     *
     * @param rows FULLTEXT 검색 결과 행 목록
     * @return 변환된 게시글 간략 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<PostSimpleDetail> mapFullTextRows(List<Object[]> rows) {
        return rows.stream()
                .map(this::mapFullTextRow)
                .collect(Collectors.toList());
    }

    /**
     * <h3>FULLTEXT 검색 단일 행 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열을 PostSimpleDetail 객체로 변환합니다.</p>
     * <p>좋아요 수와 댓글 수는 0으로 초기화되며, 이후 배치 조회로 채워집니다.</p>
     *
     * @param row FULLTEXT 검색 결과 행 (id, title, views, ?, createdAt, memberId, memberName)
     * @return 변환된 게시글 간략 정보
     * @author Jaeik
     * @since 2.0.0
     */
    private PostSimpleDetail mapFullTextRow(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String title = row[1] != null ? row[1].toString() : null;
        Integer views = row[2] != null ? ((Number) row[2]).intValue() : 0;
        Instant createdAt = toInstant(row[4]);
        Long memberId = row[5] != null ? ((Number) row[5]).longValue() : null;
        String memberName = row[6] != null ? row[6].toString() : null;

        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(views)
                .likeCount(0)
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .commentCount(0)
                .build();
    }

    /**
     * <h3>Object를 Instant로 변환</h3>
     * <p>다양한 날짜/시간 타입을 Instant로 변환합니다.</p>
     * <p>지원 타입: Instant, Timestamp, LocalDateTime</p>
     *
     * @param value 변환할 날짜/시간 객체
     * @return Instant 객체 (변환 실패 시 null)
     * @author Jaeik
     * @since 2.0.0
     */
    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        return null;
    }
}

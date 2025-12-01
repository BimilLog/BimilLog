package jaeik.bimillog.domain.comment.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.comment.entity.QCommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CommentLikeQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private static final QCommentLike commentLike = QCommentLike.commentLike;

    /**
     * 댓글의 총 추천 수 배치 조회
     */
    public Map<Long, Integer> findCommentLikeCountsMap(List<Long> commentIds) {
        List<Tuple> tuples = jpaQueryFactory
                .select(commentLike.comment.id, commentLike.id.count().intValue())
                .from(commentLike)
                .where(commentLike.comment.id.in(commentIds))
                .groupBy(commentLike.comment.id)
                .fetch();

        return tuples.stream()
                .collect(Collectors.toMap(
                        t -> t.get(commentLike.comment.id),
                        t -> Math.toIntExact(Optional.ofNullable(t.get(commentLike.id.count())).orElse(0L))
                ));
    }


}

package jaeik.bimillog.domain.post.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.QPostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.QPostReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>Post Read Model Query Repository</h2>
 * <p>기존 PostQueryRepository의 findBoardPostsByCursor를 대체합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReadModelQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private static final QPostReadModel postReadModel = QPostReadModel.postReadModel;

    /**
     * <h3>게시판 게시글 조회</h3>
     * <p>PostReadModel에서 커서 기반 페이지네이션으로 게시글 목록을 조회합니다.</p>
     * <p>hasNext 판단을 위해 size + 1개를 조회합니다.</p>
     *
     * @param cursor 마지막으로 조회한 게시글 ID (null이면 처음부터)
     * @param size   조회할 개수
     * @return 게시글 목록 (size + 1개까지 조회됨)
     */
    public List<PostSimpleDetail> findBoardPostsByCursor(Long cursor, int size) {
        // cursor가 있으면 해당 ID보다 작은 게시글을 size만큼 조회
        BooleanExpression cursorCondition = cursor != null ? postReadModel.postId.lt(cursor) : null;

        return jpaQueryFactory
                .select(new QPostSimpleDetail(
                        postReadModel.postId,
                        postReadModel.title,
                        postReadModel.viewCount,
                        postReadModel.likeCount,
                        postReadModel.createdAt,
                        postReadModel.memberId,
                        postReadModel.memberName,
                        postReadModel.commentCount))
                .from(postReadModel)
                .where(cursorCondition)
                .orderBy(postReadModel.postId.desc())  // ID 내림차순 (최신순)
                .limit(size + 1)                       // 다음 페이지 판단용
                .fetch();
    }
}

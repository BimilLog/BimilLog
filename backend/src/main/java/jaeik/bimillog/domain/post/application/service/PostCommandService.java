package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.infrastructure.adapter.in.post.web.PostCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * <h2>게시글 명령 서비스</h2>
 * <p>게시글 명령 유스케이스의 구현체입니다.</p>
 * <p>게시글 작성, 수정, 삭제 비즈니스 로직 처리</p>
 * <p>익명/회원 게시글 권한 검증</p>
 * <p>캐시 무효화 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCommandService implements PostCommandUseCase {

    private final PostCommandPort postCommandPort;
    private final GlobalPostQueryPort globalPostQueryPort;
    private final GlobalUserQueryPort globalUserQueryPort;
    private final RedisPostCommandPort redisPostCommandPort;
    private final PostLikeCommandPort postLikeCommandPort;
    private final PostToCommentPort postToCommentPort;


    /**
     * <h3>게시글 작성</h3>
     * <p>새로운 게시글을 생성하고 저장합니다.</p>
     * <p>익명/회원 구분 처리, Post 팩토리 메서드로 엔티티 생성</p>
     * <p>{@link PostCommandController}에서 게시글 작성 API 처리 시 호출됩니다.</p>
     *
     * @param userId   작성자 사용자 ID (null이면 익명 게시글)
     * @param title    게시글 제목
     * @param content  게시글 내용
     * @param password 게시글 비밀번호 (익명 게시글인 경우)
     * @return 저장된 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public Long writePost(Long userId, String title, String content, Integer password) {
        User user = (userId != null) ? globalUserQueryPort.getReferenceById(userId) : null;
        Post newPost = Post.createPost(user, title, content, password);
        Post savedPost = postCommandPort.create(newPost);
        return savedPost.getId();
    }


    /**
     * <h3>게시글 수정</h3>
     * <p>기존 게시글의 제목과 내용을 수정합니다.</p>
     * <p>작성자 권한 검증 후 게시글 업데이트, 관련 캐시 삭제</p>
     * <p>{@link PostCommandController}에서 게시글 수정 API 처리 시 호출됩니다.</p>
     *
     * @param userId  현재 로그인 사용자 ID
     * @param postId  수정할 게시글 ID
     * @param title   새로운 제목
     * @param content 새로운 내용
     * @throws PostCustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updatePost(Long userId, Long postId, String title, String content) {
        Post post = globalPostQueryPort.findById(postId);

        if (!post.isAuthor(userId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        post.updatePost(title, content);
        redisPostCommandPort.deleteCache(null, postId);
        
        log.info("게시글 수정 완료: postId={}, userId={}, title={}", postId, userId, title);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>작성자 권한 검증 후 게시글과 연관 데이터 삭제, 관련 캐시 제거</p>
     * <p>{@link PostCommandController}에서 게시글 삭제 API 처리 시 호출됩니다.</p>
     *
     * @param userId 현재 로그인 사용자 ID
     * @param postId 삭제할 게시글 ID
     * @throws PostCustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = globalPostQueryPort.findById(postId);

        if (!post.isAuthor(userId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        String postTitle = post.getTitle();

        postToCommentPort.deleteCommentInPost(postId);
        postLikeCommandPort.deletePostLikeByPostId(postId);
        postCommandPort.delete(post);
        redisPostCommandPort.deleteCache(null, postId);
        log.info("게시글 삭제 완료: postId={}, userId={}, title={}", postId, userId, postTitle);
    }

    @Override
    public void deleteAllPostsByUserId(Long userId) {

    }
}

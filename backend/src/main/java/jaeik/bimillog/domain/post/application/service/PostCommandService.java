package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.post.application.port.out.LoadUserInfoPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostReqVO;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * <h2>게시글 기본 명령 서비스</h2>
 * <p>PostCommandUseCase의 구현체로, 게시글의 기본적인 CRUD 비즈니스 로직을 처리합니다.</p>
 * <p>통합된 PostCommandPort를 사용하여 게시글 생성/수정/삭제 작업을 처리합니다.</p>
 * <p>ISP(Interface Segregation Principle)를 준수하여 Command 책임만 분리했습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostCommandService implements PostCommandUseCase {

    private final PostCommandPort postCommandPort;
    private final PostQueryPort postQueryPort;
    private final LoadUserInfoPort loadUserInfoPort;
    private final PostCacheCommandPort postCacheCommandPort;


    /**
     * <h3>게시글 작성</h3>
     * <p>로그인 사용자 또는 비로그인 사용자(익명)가 작성한 게시글을 저장하고, 해당 게시글의 ID를 반환합니다.</p>
     * <p>비로그인 사용자의 경우 userId가 null이며, 비밀번호를 통해 익명 게시글로 저장됩니다.</p>
     *
     * @param userId      게시글 작성자의 사용자 ID (null 허용 - 익명 작성자)
     * @param postReqVO  게시글 요청 값 객체
     * @return 저장된 게시글의 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Long writePost(Long userId, PostReqVO postReqVO) {
        User user = (userId != null) ? loadUserInfoPort.getReferenceById(userId) : null;
        Post newPost = Post.createPost(user, postReqVO);
        Post savedPost = postCommandPort.save(newPost);
        return savedPost.getId();
    }


    /**
     * <h3>게시글 수정</h3>
     * <p>게시글 작성자만 게시글을 수정할 수 있습니다.</p>
     * <p>헥사고날 아키텍처 원칙에 따라 모든 외부 의존성을 Port를 통해 처리합니다.</p>
     *
     * @param userId     현재 로그인한 사용자 ID
     * @param postId     수정할 게시글 ID
     * @param postReqVO 수정할 게시글 정보 값 객체
     * @throws PostCustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updatePost(Long userId, Long postId, PostReqVO postReqVO) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
        
        if (!post.isAuthor(userId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        post.updatePost(postReqVO);
        postCommandPort.save(post);
        postCacheCommandPort.deleteCache(null, postId, new PostCacheFlag[0]);
        
        log.info("게시글 수정 완료: postId={}, userId={}, title={}", postId, userId, postReqVO.title());
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글 작성자만 게시글을 삭제할 수 있습니다.</p>
     * <p>DB CASCADE 설정으로 댓글과 추천이 자동으로 삭제됩니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 삭제할 게시글 ID
     * @throws PostCustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
        
        if (!post.isAuthor(userId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        String postTitle = post.getTitle();

        // DB CASCADE로 댓글과 추천이 자동 삭제됨
        postCommandPort.delete(post);
        postCacheCommandPort.deleteCache(null, postId, new PostCacheFlag[0]);
        
        log.info("게시글 삭제 완료: postId={}, userId={}, title={}", postId, userId, postTitle);
    }

}

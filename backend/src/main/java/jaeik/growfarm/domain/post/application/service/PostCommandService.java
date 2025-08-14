package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.in.PostCommandUseCase;
import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.post.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.post.application.port.out.PostCommandPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.global.event.PostDeletedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
    private final LoadPostPort loadPostPort;
    private final LoadUserPort loadUserPort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>게시글 작성</h3>
     * <p>사용자가 작성한 게시글을 저장하고, 해당 게시글의 ID를 반환합니다.</p>
     *
     * @param userId      게시글 작성자의 사용자 ID
     * @param postReqDTO  게시글 요청 DTO
     * @return 저장된 게시글의 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Long writePost(Long userId, PostReqDTO postReqDTO) {
        User user = loadUserPort.getReferenceById(userId);
        Post newPost = Post.createPost(user, postReqDTO);
        Post savedPost = postCommandPort.save(newPost);
        return savedPost.getId();
    }


    /**
     * <h3>게시글 수정</h3>
     * <p>게시글 작성자만 게시글을 수정할 수 있습니다.</p>
     * <p>권한 검증 후 게시글 내용을 업데이트합니다.</p>
     *
     * @param userId     현재 로그인한 사용자 ID
     * @param postId     수정할 게시글 ID
     * @param postReqDTO 수정할 게시글 정보 DTO
     * @throws CustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void updatePost(Long userId, Long postId, PostReqDTO postReqDTO) {
        Post post = validatePostOwner(userId, postId);
        post.updatePost(postReqDTO);
        postCommandPort.save(post);
        log.info("Post updated: postId={}, userId={}, title={}", postId, userId, postReqDTO.getTitle());
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글 작성자만 게시글을 삭제할 수 있습니다.</p>
     * <p>권한 검증 후 게시글을 삭제하고 관련 데이터 정리 이벤트를 발행합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 삭제할 게시글 ID
     * @throws CustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = validatePostOwner(userId, postId);
        postCommandPort.delete(post);
        log.info("Post deleted: postId={}, userId={}, title={}", postId, userId, post.getTitle());
        eventPublisher.publishEvent(new PostDeletedEvent(postId));
    }


    /**
     * <h3>게시글 소유자 검증</h3>
     * <p>현재 사용자가 해당 게시글의 작성자인지 확인합니다.</p>
     *
     * @param userId 현재 사용자 ID
     * @param postId 검증할 게시글 ID
     * @return 검증된 게시글 엔티티
     * @throws CustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    private Post validatePostOwner(Long userId, Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(post.getUser().getId(), userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

}

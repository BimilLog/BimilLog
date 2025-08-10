package jaeik.growfarm.service.post.command;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.PostLikeRequestDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.post.PostInteractionService;
import jaeik.growfarm.service.post.PostPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * <h2>게시글 명령 서비스 구현체</h2>
 * <p>
 * 게시글 CUD(Create, Update, Delete) 및 좋아요 기능을 구현하는 서비스 클래스
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PostCommandServiceImpl implements PostCommandService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final PostPersistenceService postPersistenceService;
    private final PostInteractionService postInteractionService;

    /**
     * <h3>게시글 작성</h3>
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 작성된 게시글 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    @Override
    public FullPostResDTO writePost(CustomUserDetails userDetails, PostReqDTO postReqDTO) {
        Users user = (userDetails != null) ? userRepository.getReferenceById(userDetails.getUserId()) : null;
        Post post = postRepository.save(Post.createPost(user, postReqDTO));
        return FullPostResDTO.newPost(post);
    }

    /**
     * <h3>게시글 수정</h3>
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있습니다.
     * </p>
     *
     * @param userDetails 현재 로그인 한 사용자 정보
     * @param postReqDTO     수정할 게시글 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    @Override
    public void updatePost(CustomUserDetails userDetails, PostReqDTO postReqDTO) {
        Post post = validatePost(userDetails, postReqDTO);
        postPersistenceService.updatePost(postReqDTO, post);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있습니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO     게시글 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deletePost(CustomUserDetails userDetails, PostReqDTO postReqDTO) {
        Post post = validatePost(userDetails, postReqDTO);
        postPersistenceService.deletePost(post.getId());
    }

    /**
     * <h3>게시글 추천</h3>
     * <p>
     * 게시글을 추천하거나 추천 취소한다.
     * </p>
     *
     * @param likeRequestDTO     추천할 게시글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void likePost(PostLikeRequestDTO likeRequestDTO, CustomUserDetails userDetails) {
        Long postId = likeRequestDTO.getPostId();
        Long userId = userDetails.getUserId();

        Post post = postRepository.getReferenceById(postId);
        Users user = userRepository.getReferenceById(userId);

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
        postInteractionService.toggleLike(existingLike, post, user);
    }

    /**
     * <h3>게시글 유효성 검사</h3>
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO     게시글 정보 DTO
     * @return 유효한 게시글 엔티티
     * @throws CustomException 게시글이 존재하지 않거나 작성자가 일치하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private Post validatePost(CustomUserDetails userDetails, PostReqDTO postReqDTO) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        Post post = postRepository.findById(postReqDTO.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(postReqDTO.getUserId(), (userId))) {
            throw new CustomException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }

        if (userId == null) {
            if (!Objects.equals(postReqDTO.getPassword(), post.getPassword())) {
                throw new CustomException(ErrorCode.POST_PASSWORD_NOT_MATCH);
            }
        }
        return post;
    }
}

package jaeik.growfarm.domain.user.application.service;

import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.user.application.port.out.LoadCommentPort;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.UserRepository;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.global.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>사용자 조회 서비스</h2>
 * <p>UserQueryUseCase의 구현체. 사용자 정보 조회 로직을 담당합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryService implements UserQueryUseCase {

    private final UserRepository userRepository;
    private final LoadPostPort loadPostPort;
    private final LoadCommentPort loadCommentPort;

    /**
     * <h3>소셜 정보로 사용자 조회</h3>
     * <p>제공자(Provider)와 소셜 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param socialId 사용자의 소셜 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userRepository.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     *
     * @param userName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     *
     * @param userName 사용자 닉네임
     * @return Optional<User> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>해당 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> getUserPosts(Long userId, Pageable pageable) {
        return loadPostPort.findPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 좋아요한 게시글 목록 조회</h3>
     * <p>해당 사용자가 좋아요한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 좋아요한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> getUserLikedPosts(Long userId, Pageable pageable) {
        return loadPostPort.findLikedPostsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>해당 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 댓글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> getUserComments(Long userId, Pageable pageable) {
        return loadCommentPort.findCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 좋아요한 댓글 목록 조회</h3>
     * <p>해당 사용자가 좋아요한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 좋아요한 댓글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> getUserLikedComments(Long userId, Pageable pageable) {
        return loadCommentPort.findLikedCommentsByUserId(userId, pageable);
    }
}

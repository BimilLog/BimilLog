package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostToUserPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * <h2>PostCommandService</h2>
 * <p>
 * 게시글 명령 관련 UseCase 인터페이스의 구체적 구현체로서 게시글의 생성, 수정, 삭제 비즈니스 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 게시글 도메인의 명령 처리를 담당하며, 익명/회원 게시글 시스템의 
 * 권한 검증과 캐시 무효화를 포함한 복잡한 비즈니스 규칙을 관리합니다.
 * </p>
 * <p>
 * 트랜잭션 경계를 설정하고 ISP 원칙에 따라 명령 전용 포트들을 사용하여 외부 의존성을 관리합니다.
 * </p>
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
    private final PostToUserPort postToUserPort;
    private final PostCacheCommandPort postCacheCommandPort;


    /**
     * <h3>게시글 작성 비즈니스 로직 실행</h3>
     * <p>PostCommandUseCase 인터페이스의 게시글 작성 기능을 구현하며, 익명/회원 게시글 시스템의 핵심 비즈니스 규칙을 적용합니다.</p>
     * <p>익명 사용자와 로그인 사용자를 구분하여 처리하고, Post 엔티티의 팩토리 메서드를 사용하여 게시글을 생성합니다.</p>
     * <p>트랜잭션 내에서 게시글 엔티티를 저장하고, 생성된 게시글의 ID를 반환하여 후속 작업에 사용합니다.</p>
     * <p>PostCommandController에서 게시글 작성 요청 시 호출됩니다.</p>
     *
     * @param userId   게시글 작성자의 사용자 ID (null이면 익명 게시글로 처리)
     * @param title    게시글 제목
     * @param content  게시글 내용
     * @param password 게시글 비밀번호 (익명 게시글인 경우)
     * @return Long 저장된 게시글의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Long writePost(Long userId, String title, String content, Integer password) {
        User user = (userId != null) ? postToUserPort.getReferenceById(userId) : null;
        Post newPost = Post.createPost(user, title, content, password);
        Post savedPost = postCommandPort.save(newPost);
        return savedPost.getId();
    }


    /**
     * <h3>게시글 수정 권한 검증 및 업데이트</h3>
     * <p>PostCommandUseCase 인터페이스의 게시글 수정 기능을 구현하며, 작성자 권한 검증과 캐시 무효화를 포함합니다.</p>
     * <p>Post 엔티티의 isAuthor 메서드를 사용하여 권한을 검증하고, updatePost 메서드로 엔티티를 업데이트합니다.</p>
     * <p>트랜잭션 내에서 게시글을 수정하고 관련 캐시를 삭제하여 데이터 일관성을 보장합니다.</p>
     * <p>PostCommandController에서 게시글 수정 요청 시 호출됩니다.</p>
     *
     * @param userId  현재 로그인한 사용자 ID
     * @param postId  수정할 게시글 ID
     * @param title   새로운 게시글 제목
     * @param content 새로운 게시글 내용
     * @throws PostCustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void updatePost(Long userId, Long postId, String title, String content) {
        Post post = postQueryPort.findById(postId);

        if (!post.isAuthor(userId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        post.updatePost(title, content);
        postCommandPort.save(post);
        postCacheCommandPort.deleteCache(null, postId);
        
        log.info("게시글 수정 완료: postId={}, userId={}, title={}", postId, userId, title);
    }

    /**
     * <h3>게시글 삭제 권한 검증 및 삭제</h3>
     * <p>PostCommandUseCase 인터페이스의 게시글 삭제 기능을 구현하며, 작성자 권한 검증과 캐시 무효화를 포함합니다.</p>
     * <p>Post 엔티티의 isAuthor 메서드를 사용하여 권한을 검증하고, CASCADE 설정에 의해 연관된 댓글과 추천이 자동 삭제됩니다.</p>
     * <p>트랜잭션 내에서 게시글을 삭제하고 관련 캐시를 제거하여 데이터 일관성을 보장합니다.</p>
     * <p>PostCommandController에서 게시글 삭제 요청 시 호출됩니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 삭제할 게시글 ID
     * @throws PostCustomException 권한이 없거나 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = postQueryPort.findById(postId);

        if (!post.isAuthor(userId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        String postTitle = post.getTitle();

        // DB CASCADE로 댓글과 추천이 자동 삭제됨
        postCommandPort.delete(post);
        postCacheCommandPort.deleteCache(null, postId);
        
        log.info("게시글 삭제 완료: postId={}, userId={}, title={}", postId, userId, postTitle);
    }

}

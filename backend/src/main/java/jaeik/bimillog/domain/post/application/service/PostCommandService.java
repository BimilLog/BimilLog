package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalMemberQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostCommandPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.post.web.PostCommandController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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
    private final GlobalMemberQueryPort globalUserQueryPort;
    private final RedisPostCommandPort redisPostCommandPort;
    private final PostQueryPort postQueryPort;


    /**
     * <h3>게시글 작성</h3>
     * <p>새로운 게시글을 생성하고 저장합니다.</p>
     * <p>익명/회원 구분 처리, Post 팩토리 메서드로 엔티티 생성</p>
     * <p>{@link PostCommandController}에서 게시글 작성 API 처리 시 호출됩니다.</p>
     *
     * @param memberId   작성자 사용자 ID (null이면 익명 게시글)
     * @param title    게시글 제목
     * @param content  게시글 내용
     * @param password 게시글 비밀번호 (익명 게시글인 경우)
     * @return 저장된 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public Long writePost(Long memberId, String title, String content, Integer password) {
        Member member = (memberId != null) ? globalUserQueryPort.getReferenceById(memberId) : null;
        Post newPost = Post.createPost(member, title, content, password);
        Post savedPost = postCommandPort.create(newPost);
        return savedPost.getId();
    }


    /**
     * <h3>게시글 수정 (라이트 어라운드 패턴)</h3>
     * <p>기존 게시글의 제목과 내용을 수정하고 캐시를 무효화합니다.</p>
     * <p>작성자 권한 검증 → 게시글 업데이트 → 캐시 무효화 (다음 조회 시 캐시 어사이드로 재생성)</p>
     * <p>{@link PostCommandController}에서 게시글 수정 API 처리 시 호출됩니다.</p>
     *
     * @param memberId  현재 로그인 사용자 ID
     * @param postId  수정할 게시글 ID
     * @param title   새로운 제목
     * @param content 새로운 내용
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updatePost(Long memberId, Long postId, String title, String content) {
        Post post = globalPostQueryPort.findById(postId);

        if (!post.isAuthor(memberId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        post.updatePost(title, content);
        redisPostCommandPort.deleteSinglePostCache(postId);

        log.info("게시글 수정 완료: postId={}, memberId={}, title={}", postId, memberId, title);
    }

    /**
     * <h3>게시글 삭제 (라이트 어라운드 패턴)</h3>
     * <p>게시글을 데이터베이스에서 완전히 삭제하고 캐시를 무효화합니다.</p>
     * <p>작성자 권한 검증 → 게시글 삭제 → 캐시 무효화</p>
     * <p>CASCADE로 Comment와 PostLike가 자동 삭제됩니다.</p>
     * <p>{@link PostCommandController}에서 게시글 삭제 API 처리 시 호출됩니다.</p>
     *
     * @param memberId 현재 로그인 사용자 ID
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = globalPostQueryPort.findById(postId);

        if (!post.isAuthor(memberId)) {
            throw new PostCustomException(PostErrorCode.FORBIDDEN);
        }

        String postTitle = post.getTitle();

        // CASCADE로 Comment와 PostLike 자동 삭제
        postCommandPort.delete(post);
        redisPostCommandPort.deleteSinglePostCache(postId);
        log.info("게시글 삭제 완료: postId={}, memberId={}, title={}", postId, memberId, postTitle);
    }

    /**
     * <h3>회원 작성 게시글 일괄 삭제 (라이트 어라운드 패턴)</h3>
     * <p>회원 탈퇴 시 사용자가 작성한 모든 게시글을 삭제하고 캐시를 무효화합니다.</p>
     *
     * @param memberId 게시글을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteAllPostsByMemberId(Long memberId) {
        List<Long> postIds = postQueryPort.findPostIdsMemberId(memberId);
        for (Long postId : postIds) {
            redisPostCommandPort.deleteSinglePostCache(postId);
        }
        postCommandPort.deleteAllByMemberId(memberId);
    }
}

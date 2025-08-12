package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostPersistenceAdapter implements
        SavePostPort, LoadPostPort, DeletePostPort,
        SavePostLikePort, DeletePostLikePort, ExistPostLikePort, CountPostLikePort {

    private final PostJpaRepository postJpaRepository;
    private final PostLikeJpaRepository postLikeJpaRepository;
    private final PostQueryDslRepository postQueryDslRepository;

    @Override
    public Post save(Post post) {
        return postJpaRepository.save(post);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id);
    }

    @Override
    public Page<SimplePostResDTO> findByPage(Pageable pageable) {
        return postQueryDslRepository.findSimplePost(pageable);
    }

    @Override
    public Page<SimplePostResDTO> findBySearch(String type, String query, Pageable pageable) {
        return postQueryDslRepository.searchPosts(query, type, pageable);
    }

    @Override
    public void incrementView(Post post) {
        post.incrementView();
        postJpaRepository.save(post);
    }

    @Override
    public void delete(Post post) {
        postJpaRepository.delete(post);
    }

    @Override
    public void save(PostLike postLike) {
        postLikeJpaRepository.save(postLike);
    }

    @Override
    public void deleteByUserAndPost(User user, Post post) {
        postLikeJpaRepository.deleteByUserAndPost(user, post);
    }

    @Override
    public void deleteAllByPostId(Long postId) {
        postLikeJpaRepository.deleteAllByPostId(postId);
    }

    @Override
    public boolean existsByUserAndPost(User user, Post post) {
        return postLikeJpaRepository.existsByUserAndPost(user, post);
    }

    @Override
    public long countByPost(Post post) {
        return postLikeJpaRepository.countByPost(post);
    }

    @Override
    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        return postQueryDslRepository.findPostsByUserId(userId, pageable);
    }

    @Override
    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        return postQueryDslRepository.findLikedPostsByUserId(userId, pageable);
    }

    @Override
    public List<SimplePostResDTO> findNoticePosts() {
        // 임시 구현: 실제로는 PostJpaRepository에 쿼리 메서드를 추가하는 것이 좋습니다.
        return postJpaRepository.findAll().stream()
                .filter(Post::isNotice)
                .map(post -> SimplePostResDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .userName(post.getUser() != null ? post.getUser().getUserName() : "익명")
                        .createdAt(post.getCreatedAt())
                        .views(post.getViews())
                        .build())
                .collect(Collectors.toList());
    }
}

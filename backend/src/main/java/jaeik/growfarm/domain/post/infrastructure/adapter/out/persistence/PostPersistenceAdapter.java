package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.post.domain.PostLike;
import jaeik.growfarm.domain.user.domain.User;
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
        SavePostLikePort, DeletePostLikePort, ExistPostLikePort {

    private final PostJpaRepository postJpaRepository;
    private final PostLikeJpaRepository postLikeJpaRepository;
    // private final PostQueryDslRepository postQueryDslRepository; // QueryDSL을 위한 리포지토리

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
        // return postQueryDslRepository.findSimplePost(pageable);
        return Page.empty(); // 임시
    }

    @Override
    public Page<SimplePostResDTO> findBySearch(String type, String query, Pageable pageable) {
        // return postQueryDslRepository.searchPosts(query, type, pageable);
        return Page.empty(); // 임시
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
    public boolean existsByUserAndPost(User user, Post post) {
        return postLikeJpaRepository.existsByUserAndPost(user, post);
    }

    @Override
    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        return Page.empty(); // 임시 구현
    }

    @Override
    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        return Page.empty(); // 임시 구현
    }

    @Override
    public List<SimplePostResDTO> findNoticePosts() {
        // 임시 구현: 실제로는 PostJpaRepository에 쿼리 메서드를 추가하는 것이 좋습니다.
        return postJpaRepository.findAll().stream()
                .filter(Post::isNotice)
                .map(post -> SimplePostResDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .writer(post.getUser() != null ? post.getUser().getUserName() : "익명")
                        .createdAt(post.getCreatedAt())
                        .views(post.getViews())
                        .build())
                .collect(Collectors.toList());
    }
}

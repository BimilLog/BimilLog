package jaeik.growfarm.integration;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.repository.post.popular.PostPopularRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * <h2>인기글 레포지터리 통합 테스트</h2>
 * <p>
 * PostPopularRepository의 리팩토링된 메서드들이 정상적으로 작동하는지 확인
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PostPopularRepositoryTest {

    @Autowired
    private PostPopularRepository postPopularRepository;

    @Test
    public void testUpdateRealtimePopularPosts() {
        // Given & When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

        // Then
        assertNotNull(result);
        // 결과는 빈 리스트일 수 있지만 null이 아니어야 함
    }

    @Test
    public void testUpdateWeeklyPopularPosts() {
        // Given & When
        List<SimplePostDTO> result = postPopularRepository.updateWeeklyPopularPosts();

        // Then
        assertNotNull(result);
        // 결과는 빈 리스트일 수 있지만 null이 아니어야 함
    }

    @Test
    public void testUpdateLegendPosts() {
        // Given & When
        List<SimplePostDTO> result = postPopularRepository.updateLegendPosts();

        // Then
        assertNotNull(result);
        // 결과는 빈 리스트일 수 있지만 null이 아니어야 함
    }
}
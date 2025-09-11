package jaeik.bimillog.infrastructure.adapter.paper.user;

import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.paper.out.user.PaperToUserAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>PaperToUserAdapter 단위 테스트</h2>
 * <p>PaperToUserAdapter의 UserQueryUseCase 위임 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaperToUserAdapter 단위 테스트")
class PaperToUserAdapterTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private PaperToUserAdapter paperToUserAdapter;

    @Test
    @DisplayName("사용자명으로 조회 - UserQueryUseCase 위임 확인")
    void shouldDelegateToUserQueryUseCase_ForFindByUserName() {
        // Given
        String userName = "testUser";
        User mockUser = User.builder().build();
        when(userQueryUseCase.findByUserName(userName)).thenReturn(Optional.of(mockUser));

        // When
        Optional<User> result = paperToUserAdapter.findByUserName(userName);

        // Then
        verify(userQueryUseCase).findByUserName(userName);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockUser);
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인 - UserQueryUseCase 위임 확인")
    void shouldDelegateToUserQueryUseCase_ForExistsByUserName() {
        // Given
        String userName = "testUser";
        when(userQueryUseCase.existsByUserName(userName)).thenReturn(true);

        // When
        boolean result = paperToUserAdapter.existsByUserName(userName);

        // Then
        verify(userQueryUseCase).existsByUserName(userName);
        assertThat(result).isTrue();
    }
}
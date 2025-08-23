package jaeik.growfarm.infrastructure.adapter.paper.out.persistence.paper;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.paper.entity.DecoType;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.paper.entity.QMessage;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.paper.in.web.dto.MessageDTO;
import jaeik.growfarm.infrastructure.adapter.paper.in.web.dto.VisitMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>PaperQueryAdapter 테스트</h2>
 * <p>PaperQueryAdapter의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PaperQueryAdapterTest {

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @InjectMocks
    private PaperQueryAdapter paperQueryAdapter;

    @Mock
    private JPAQuery<Message> mockMessageJPAQuery;
    @Mock
    private JPAQuery<MessageDTO> mockMessageDTOJPAQuery;
    @Mock
    private JPAQuery<VisitMessageDTO> mockVisitMessageDTOJPAQuery;

    private Message testMessage;
    private MessageDTO testMessageDTO;
    private VisitMessageDTO testVisitMessageDTO;

    @BeforeEach
    void setUp() {
        User testUser = User.builder().id(1L).userName("testUser").build();
        testMessage = Message.builder()
                .id(1L)
                .user(testUser)
                .content("Hello World")
                .decoType(DecoType.POTATO)
                .anonymity("테스트익명") // Changed from boolean to String
                .width(100)
                .height(200)
                .createdAt(Instant.now())
                .build();

        testMessageDTO = new MessageDTO();
        testMessageDTO.setId(1L);
        testMessageDTO.setUserId(1L);
        testMessageDTO.setContent("Hello World");
        testMessageDTO.setDecoType(DecoType.POTATO);
        testMessageDTO.setAnonymity("테스트익명"); // Changed from null to String
        testMessageDTO.setWidth(100);
        testMessageDTO.setHeight(200);
        testMessageDTO.setCreatedAt(Instant.now());

        testVisitMessageDTO = new VisitMessageDTO();
        testVisitMessageDTO.setId(1L);
        testVisitMessageDTO.setUserId(1L);
        testVisitMessageDTO.setDecoType(DecoType.POTATO);
        testVisitMessageDTO.setWidth(100);
        testVisitMessageDTO.setHeight(200);

        when(jpaQueryFactory.selectFrom(any(QMessage.class))).thenReturn(mockMessageJPAQuery);
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(mockMessageDTOJPAQuery);
        when(mockMessageDTOJPAQuery.from(any(QMessage.class))).thenReturn(mockMessageDTOJPAQuery);
        when(mockMessageDTOJPAQuery.where(any(BooleanExpression.class))).thenReturn(mockMessageDTOJPAQuery);
        when(mockMessageDTOJPAQuery.orderBy(any(OrderSpecifier.class))).thenReturn(mockMessageDTOJPAQuery);

        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(mockVisitMessageDTOJPAQuery);
        when(mockVisitMessageDTOJPAQuery.from(any(QMessage.class))).thenReturn(mockVisitMessageDTOJPAQuery);
        when(mockVisitMessageDTOJPAQuery.join(any(QUser.class), any(QUser.class))).thenReturn(mockVisitMessageDTOJPAQuery);
        when(mockVisitMessageDTOJPAQuery.on(any(BooleanExpression.class))).thenReturn(mockVisitMessageDTOJPAQuery);
        when(mockVisitMessageDTOJPAQuery.where(any(BooleanExpression.class))).thenReturn(mockVisitMessageDTOJPAQuery);
    }

    @Test
    @DisplayName("정상 케이스 - 메시지 ID로 메시지 조회")
    void shouldFindMessageById_WhenValidIdProvided() {
        // Given
        Long messageId = 1L;
        given(mockMessageJPAQuery.where(any(BooleanExpression.class))).willReturn(mockMessageJPAQuery);
        given(mockMessageJPAQuery.fetchOne()).willReturn(testMessage);

        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(messageId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(messageId);
        assertThat(result.get().getContent()).isEqualTo("Hello World");
        verify(mockMessageJPAQuery).where(any(BooleanExpression.class));
        verify(mockMessageJPAQuery).fetchOne();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 메시지 ID로 메시지 조회")
    void shouldReturnEmpty_WhenNonExistentMessageIdProvided() {
        // Given
        Long nonExistentMessageId = 999L;
        given(mockMessageJPAQuery.where(any(BooleanExpression.class))).willReturn(mockMessageJPAQuery);
        given(mockMessageJPAQuery.fetchOne()).willReturn(null);

        // When
        Optional<Message> result = paperQueryAdapter.findMessageById(nonExistentMessageId);

        // Then
        assertThat(result).isEmpty();
        verify(mockMessageJPAQuery).where(any(BooleanExpression.class));
        verify(mockMessageJPAQuery).fetchOne();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 ID로 MessageDTO 목록 조회")
    void shouldFindMessageDTOsByUserId_WhenValidUserIdProvided() {
        // Given
        Long userId = 1L;
        given(mockMessageDTOJPAQuery.fetch()).willReturn(Collections.singletonList(testMessageDTO));

        // When
        List<MessageDTO> result = paperQueryAdapter.findMessageDTOsByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        assertThat(result.getFirst().getContent()).isEqualTo("Hello World");
        verify(mockMessageDTOJPAQuery).where(any(BooleanExpression.class));
        verify(mockMessageDTOJPAQuery).orderBy(any(OrderSpecifier.class));
        verify(mockMessageDTOJPAQuery).fetch();
    }

    @Test
    @DisplayName("경계값 - 메시지가 없는 사용자 ID로 MessageDTO 목록 조회")
    void shouldReturnEmptyList_WhenUserHasNoMessages() {
        // Given
        Long userIdWithoutMessages = 999L;
        given(mockMessageDTOJPAQuery.fetch()).willReturn(List.of());

        // When
        List<MessageDTO> result = paperQueryAdapter.findMessageDTOsByUserId(userIdWithoutMessages);

        // Then
        assertThat(result).isEmpty();
        verify(mockMessageDTOJPAQuery).where(any(BooleanExpression.class));
        verify(mockMessageDTOJPAQuery).orderBy(any(OrderSpecifier.class));
        verify(mockMessageDTOJPAQuery).fetch();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 이름으로 VisitMessageDTO 목록 조회")
    void shouldFindVisitMessageDTOsByUserName_WhenValidUserNameProvided() {
        // Given
        String userName = "testUser";
        given(mockVisitMessageDTOJPAQuery.fetch()).willReturn(Collections.singletonList(testVisitMessageDTO));

        // When
        List<VisitMessageDTO> result = paperQueryAdapter.findVisitMessageDTOsByUserName(userName);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(testVisitMessageDTO.getUserId());
        verify(mockVisitMessageDTOJPAQuery).join(any(QUser.class));
        verify(mockVisitMessageDTOJPAQuery).where(any(BooleanExpression.class));
        verify(mockVisitMessageDTOJPAQuery).fetch();
    }

    @Test
    @DisplayName("경계값 - 메시지가 없는 사용자 이름으로 VisitMessageDTO 목록 조회")
    void shouldReturnEmptyList_WhenUserHasNoVisitMessages() {
        // Given
        String userNameWithoutMessages = "nonExistentUser";
        given(mockVisitMessageDTOJPAQuery.fetch()).willReturn(List.of());

        // When
        List<VisitMessageDTO> result = paperQueryAdapter.findVisitMessageDTOsByUserName(userNameWithoutMessages);

        // Then
        assertThat(result).isEmpty();
        verify(mockVisitMessageDTOJPAQuery).join(any(QUser.class));
        verify(mockVisitMessageDTOJPAQuery).where(any(BooleanExpression.class));
        verify(mockVisitMessageDTOJPAQuery).fetch();
    }
}

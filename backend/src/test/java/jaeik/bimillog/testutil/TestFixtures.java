package jaeik.bimillog.testutil;

import jaeik.bimillog.infrastructure.adapter.in.paper.dto.MessageDTO;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.PostCreateDTO;
import jaeik.bimillog.infrastructure.adapter.in.post.dto.PostUpdateDTO;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * <h2>테스트 데이터 Fixtures</h2>
 * <p>테스트에서 자주 사용되는 데이터 생성 유틸리티</p>
 * <p>엔티티, DTO, 인증 객체 등 다양한 테스트 데이터 생성 메서드 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>엔티티 생성 (Post, Comment, Notification, RollingPaper 등)</li>
 *   <li>DTO 생성 (요청/응답 DTO)</li>
 *   <li>인증 객체 생성 (CustomUserDetails, ExistingUserDetail)</li>
 *   <li>쿠키 및 토큰 생성</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestFixtures {

    // ==================== Auth Test Constants ====================

    // ==================== Entity Creation ====================



    // ==================== DTO Creation ====================

    /**
     * 게시글 작성 요청 DTO 생성
     * @param title 제목
     * @param content 내용
     * @return PostCreateDTO
     */
    public static PostCreateDTO createPostRequest(String title, String content) {
        return PostCreateDTO.builder()
                .title(title)
                .content(content)
                .password("1234")
                .build();
    }



    /**
     * 게시글 수정 요청 DTO 생성
     * @param title 제목
     * @param content 내용
     * @return PostUpdateDTO
     */
    public static PostUpdateDTO createPostUpdateDTO(String title, String content) {
        return PostUpdateDTO.builder()
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 기본 게시글 수정 요청 DTO 생성
     * @return PostUpdateDTO
     */
    public static PostUpdateDTO createPostUpdateDTO() {
        return createPostUpdateDTO("수정된 제목", "수정된 내용입니다. 10자 이상으로 작성합니다.");
    }

    /**
     * 롤링페이퍼 메시지 요청 DTO 생성
     * @param content 메시지 내용
     * @param positionX X 위치
     * @param positionY Y 위치
     * @return MessageDTO
     */
    public static MessageDTO createPaperMessageRequest(String content, int positionX, int positionY) {
        MessageDTO dto = new MessageDTO();
        dto.setDecoType(jaeik.bimillog.domain.paper.entity.DecoType.POTATO);
        dto.setContent(content);
        dto.setAnonymity("테스트사용자");
        dto.setX(positionX);
        dto.setY(positionY);
        return dto;
    }



    // ==================== Utility Methods ====================


    /**
     * 리플렉션을 통한 private 필드 값 설정 (테스트 전용)
     * @param target 대상 객체
     * @param fieldName 필드명
     * @param value 설정할 값
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

}

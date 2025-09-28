package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.user.entity.User;

/**
 * Paper 도메인 테스트 데이터 빌더
 * <p>
 * Rolling Paper 메시지 관련 테스트 데이터 생성 유틸리티
 */
public class PaperTestDataBuilder {
    
    /**
     * Rolling Paper 메시지 생성
     */
    public static Message createRollingPaper(User receiver, String content, int positionX, int positionY) {
        return Message.createMessage(
                receiver,
                DecoType.POTATO,
                "테스트사용자",
                content,
                positionX,
                positionY
        );
    }
}
package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;

/**
 * Paper 도메인 테스트 데이터 빌더
 * <p>
 * Rolling Paper 메시지 관련 테스트 데이터 생성 유틸리티
 */
public class PaperTestDataBuilder {
    
    /**
     * Rolling Paper 메시지 생성
     */
    public static Message createRollingPaper(Member receiver, String content, int positionX, int positionY) {
        return Message.createMessage(
                receiver,
                DecoType.POTATO,
                "테스트회원",
                content,
                positionX,
                positionY
        );
    }
}
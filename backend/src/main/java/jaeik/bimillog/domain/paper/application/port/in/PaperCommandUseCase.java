package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;

/**
 * <h2>롤링페이퍼 명령 유스케이스</h2>
 * <p>
 * 인바운드 포트: 롤링페이퍼 생성/수정/삭제 관련 유스케이스를 정의
 * </p>
 * <p>
 * 롤링페이퍼를 통한 소통에서 발생하는 메시지 작성, 메시지 삭제를 다룹니다:
 * </p>
 * <p>PaperCommandController에서 이런 사용자 요구사항을 받아 추상화된 인터페이스로 전달합니다</p>
 * <p>PaperCommandService에서 구현되어 실제 비즈니스 로직을 담당합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCommandUseCase {

    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>사용자가 자신의 롤링페이퍼에서 메시지를 삭제합니다.</p>
     * <p>메시지 소유권을 검증한 후 삭제를 수행합니다.</p>
     * <p>
     * 사용자가 마이페이지에서 자신의 롤링페이퍼를 관리하던 중
     * "이 메시지는 부적절하네" 또는 "스팸 메시지인 것 같아" 또는 "개인적으로 불쾌한 메시지네"라며
     * 해당 메시지를 제거하여 깨끗하고 의미 있는 롤링페이퍼를 유지하고 싶을 때
     * PaperCommandController를 통해 호출되는 메서드입니다.
     * </p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param messageId 삭제할 메시지 ID
     * @throws PaperCustomException 삭제 권한이 없는 경우 (MESSAGE_DELETE_FORBIDDEN)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteMessageInMyPaper(Long userId, Long messageId);

    /**
     * <h3>메시지 작성</h3>
     * <p>지정된 사용자의 롤링페이퍼에 새로운 메시지를 작성합니다.</p>
     * <p>메시지 작성 완료 후 알림 이벤트를 발행합니다.</p>
     * <p>
     * 친구나 지인이 "생일축하해줘야지", "시험 잘 보라고 응원해줄게", "기념일 축하한다"는 마음으로
     * 롤링페이퍼 링크를 클릭해서 접속한 후, 따뜻한 메시지를 작성하고 예쁜 디자인을 선택해서
     * "전송" 버튼을 누르는 순간 PaperCommandController를 통해 호출되는 메서드입니다.
     * 익명성을 보장하면서도 진심 어린 메시지가 상대방에게 전달되고,
     * 롤링페이퍼 소유자에게는 즉시 "새 메시지가 도착했어요!" 알림이 발송됩니다.
     * </p>
     *
     * @param userName 롤링페이퍼 소유자의 사용자명
     * @param decoType 데코레이션 타입
     * @param anonymity 익명 이름
     * @param content 메시지 내용
     * @param width 메시지 너비
     * @param height 메시지 높이
     * @throws PaperCustomException 사용자가 존재하지 않거나 유효성 검증 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    void writeMessage(String userName, DecoType decoType, String anonymity, 
                     String content, int width, int height);
}
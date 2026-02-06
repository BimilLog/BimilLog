package jaeik.bimillog.domain.global.event;

/**
 * <h2>친구 상호작용 점수를 변동하는 이벤트들을 묶은 인터페이스</h2>
 * @version 2.7.0
 * @author Jaeik
 */
public interface FriendInteractionEvent {
    Long getMemberId(); // 회원 ID
    Long getTargetMemberId();   // 대상 회원 ID
    String getIdempotencyKey(); // 멱등 키
    void getAlreadyProcess();
    void getDlqMessage(Exception e);
}
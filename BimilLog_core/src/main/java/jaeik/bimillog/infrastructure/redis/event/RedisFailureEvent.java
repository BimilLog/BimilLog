package jaeik.bimillog.infrastructure.redis.event;

/**
 * <h2>Redis 장애 이벤트</h2>
 * <p>Redis 연결 실패 또는 쿼리 실패 시 발생하는 이벤트입니다.</p>
 * <p>이 이벤트 발생 시 자동으로 Redis 재구성이 트리거됩니다.</p>
 *
 * @param source 이벤트를 발생시킨 소스 (클래스명)
 * @param errorType 오류 타입 (QUERY, DELETE, WRITE 등)
 * @param message 오류 메시지
 * @param cause 원인 예외
 *
 * @author Jaeik
 * @version 1.0.0
 */
public record RedisFailureEvent(
        String source,
        String errorType,
        String message,
        Throwable cause
) {
    public static RedisFailureEvent of(String source, String errorType, String message, Throwable cause) {
        return new RedisFailureEvent(source, errorType, message, cause);
    }
}

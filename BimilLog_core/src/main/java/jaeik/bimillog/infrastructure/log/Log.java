package jaeik.bimillog.infrastructure.log;

import java.lang.annotation.*;

/**
 * <h2>메서드 로깅 애노테이션</h2>
 * <p>AOP를 통한 자동 로깅을 위한 커스텀 애노테이션입니다.</p>
 * <p>메서드 실행 전후 로깅, 파라미터/결과값 로깅, 실행시간 측정</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    
    /**
     * 로그 레벨 설정
     */
    LogLevel level() default LogLevel.INFO;
    
    /**
     * 메서드 파라미터 로깅 여부
     */
    boolean logParams() default true;
    
    /**
     * 반환값 로깅 여부
     */
    boolean logResult() default true;
    
    /**
     * 실행 시간 측정 여부
     */
    boolean logExecutionTime() default false;
    
    /**
     * 민감한 파라미터 제외 (파라미터 이름)
     */
    String[] excludeParams() default {};
    
    /**
     * 커스텀 로그 메시지 (없으면 자동 생성)
     */
    String message() default "";
    
    /**
     * 에러 로깅 활성화
     */
    boolean logErrors() default true;
    
    /**
     * 로그 레벨 정의
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}

package jaeik.bimillog.infrastructure.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.global.annotation.Log;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h2>로깅 AOP 구현체</h2>
 * <p>@Log 애노테이션이 붙은 메서드의 로깅을 자동으로 처리합니다.</p>
 * <p>메서드 실행 전후 로깅, 실행시간 측정, 민감정보 마스킹</p>
 * <p>MDC를 통한 사용자 컨텍스트 정보 추가</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Aspect
@Component
public class LoggingAspect {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "token", "secret", "key", "credential", 
        "authorization", "cookie", "session", "jwt", "code", "fcmToken"
    );
    
    /**
     * <h3>@Log 애노테이션 메서드 인터셉트</h3>
     * <p>@Log 애노테이션이 붙은 메서드 실행을 가로채서 로깅을 수행합니다.</p>
     * <p>시작/종료/에러 로깅, 실행시간 측정, MDC 컨텍스트 설정</p>
     * 
     * @param joinPoint 조인포인트
     * @param logAnnotation Log 애노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(logAnnotation)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        setupMDC();
        
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        if (shouldLog(logger, logAnnotation.level())) {
            logMethodEntry(logger, logAnnotation, className, methodName, joinPoint, signature);
        }
        
        StopWatch stopWatch = null;
        if (logAnnotation.logExecutionTime()) {
            stopWatch = new StopWatch();
            stopWatch.start();
        }
        
        try {
            Object result = joinPoint.proceed();
            
            if (shouldLog(logger, logAnnotation.level())) {
                logMethodExit(logger, logAnnotation, className, methodName, result, stopWatch);
            }
            
            return result;
            
        } catch (Exception e) {
            if (logAnnotation.logErrors()) {
                logMethodError(logger, className, methodName, e, stopWatch);
            }
            throw e;
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * <h3>클래스 레벨 @Log 처리</h3>
     * <p>클래스에 @Log가 붙은 경우 모든 public 메서드에 적용합니다.</p>
     */
    @Around("@within(logAnnotation) && execution(public * *(..))")
    public Object logClass(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        Log methodLog = method.getAnnotation(Log.class);
        if (methodLog != null) {
            return joinPoint.proceed();
        }
        
        return logMethod(joinPoint, logAnnotation);
    }
    
    private void setupMDC() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            MDC.put("userId", String.valueOf(userDetails.getUserId()));
            MDC.put("username", userDetails.getUsername());
        }
        MDC.put("traceId", generateTraceId());
    }
    
    private void logMethodEntry(Logger logger, Log logAnnotation, String className, 
                                String methodName, ProceedingJoinPoint joinPoint, 
                                MethodSignature signature) {
        
        StringBuilder logMessage = new StringBuilder();
        
        if (!logAnnotation.message().isEmpty()) {
            logMessage.append(logAnnotation.message()).append(" - ");
        }
        
        logMessage.append("[").append(className).append(".").append(methodName).append("] 시작");
        
        if (logAnnotation.logParams() && joinPoint.getArgs().length > 0) {
            Map<String, Object> params = getParameters(joinPoint, signature, logAnnotation.excludeParams());
            if (!params.isEmpty()) {
                logMessage.append(" | 파라미터: ").append(maskSensitiveData(params));
            }
        }
        
        logAtLevel(logger, logAnnotation.level(), logMessage.toString());
    }
    
    private void logMethodExit(Logger logger, Log logAnnotation, String className, 
                               String methodName, Object result, StopWatch stopWatch) {
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[").append(className).append(".").append(methodName).append("] 완료");
        
        if (logAnnotation.logResult() && result != null) {
            String resultStr = maskSensitiveData(result);
            if (resultStr.length() > 500) {
                resultStr = resultStr.substring(0, 500) + "...";
            }
            logMessage.append(" | 결과: ").append(resultStr);
        }
        
        if (stopWatch != null) {
            stopWatch.stop();
            logMessage.append(" | 실행시간: ").append(stopWatch.getTotalTimeMillis()).append("ms");
        }
        
        logAtLevel(logger, logAnnotation.level(), logMessage.toString());
    }
    
    private void logMethodError(Logger logger, String className, String methodName, 
                                Exception e, StopWatch stopWatch) {
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[").append(className).append(".").append(methodName).append("] 실패");
        logMessage.append(" | 에러: ").append(e.getClass().getSimpleName());
        logMessage.append(" - ").append(e.getMessage());
        
        if (stopWatch != null && stopWatch.isRunning()) {
            stopWatch.stop();
            logMessage.append(" | 실행시간: ").append(stopWatch.getTotalTimeMillis()).append("ms");
        }
        
        logger.error(logMessage.toString());
    }
    
    private Map<String, Object> getParameters(ProceedingJoinPoint joinPoint, 
                                              MethodSignature signature, 
                                              String[] excludeParams) {
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Set<String> excludeSet = Arrays.stream(excludeParams).collect(Collectors.toSet());
        
        return IntStream.range(0, paramNames.length)
            .filter(i -> !excludeSet.contains(paramNames[i]))
            .filter(i -> args[i] != null)
            .boxed()
            .collect(Collectors.toMap(
                i -> paramNames[i],
                i -> {
                    Object arg = args[i];
                    if (arg.getClass().getName().contains("org.springframework")) {
                        return arg.getClass().getSimpleName();
                    }
                    return arg;
                },
                (a, b) -> b,
                HashMap::new
            ));
    }
    
    private String maskSensitiveData(Object data) {
        if (data == null) {
            return "null";
        }
        
        try {
            String json = objectMapper.writeValueAsString(data);
            
            for (String field : SENSITIVE_FIELDS) {
                json = json.replaceAll(
                    "\"" + field + "\"\\s*:\\s*\"[^\"]*\"", 
                    "\"" + field + "\":\"[MASKED]\""
                );
                json = json.replaceAll(
                    "\"" + field + "[A-Z]\\w*\"\\s*:\\s*\"[^\"]*\"", 
                    "\"" + field + "\":\"[MASKED]\""
                );
            }
            
            return json;
        } catch (Exception e) {
            return data.getClass().getSimpleName();
        }
    }
    
    private void logAtLevel(Logger logger, Log.LogLevel level, String message) {
        switch (level) {
            case DEBUG -> logger.debug(message);
            case INFO -> logger.info(message);
            case WARN -> logger.warn(message);
            case ERROR -> logger.error(message);
        }
    }
    
    private boolean shouldLog(Logger logger, Log.LogLevel level) {
        return switch (level) {
            case DEBUG -> logger.isDebugEnabled();
            case INFO -> logger.isInfoEnabled();
            case WARN -> logger.isWarnEnabled();
            case ERROR -> logger.isErrorEnabled();
        };
    }
    
    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
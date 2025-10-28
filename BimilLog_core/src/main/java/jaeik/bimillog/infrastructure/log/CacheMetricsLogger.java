package jaeik.bimillog.infrastructure.log;

import org.slf4j.Logger;

/**
 * <h2>캐시 메트릭 로거</h2>
 * <p>Redis 기반 캐시의 히트/미스 로그 메시지를 일관된 포맷으로 출력하기 위한 유틸리티입니다.</p>
 * <p>로그 집계를 통해 캐시 히트율을 산출하는 데 필요한 구조화된 메시지를 제공합니다.</p>
 *
 * @author Jaeik
 */
public final class CacheMetricsLogger {

    private static final String PREFIX = "CACHE_METRIC";
    private static final String EMPTY_VALUE = "-";

    private CacheMetricsLogger() {
    }

    /**
     * <h3>캐시 히트 로그</h3>
     * <p>캐시 조회가 성공했을 때 히트 로그를 남깁니다.</p>
     *
     * @param logger    로그를 출력할 {@link Logger}
     * @param cacheName 캐시 식별용 이름
     * @param cacheKey  조회한 캐시 키 또는 파티션 정보
     */
    public static void hit(Logger logger, String cacheName, Object cacheKey) {
        hit(logger, cacheName, cacheKey, EMPTY_VALUE);
    }

    /**
     * <h3>캐시 히트 로그 (추가 정보 포함)</h3>
     * <p>캐시 조회가 성공했을 때 히트 로그를 남기고 추가 메타데이터를 기록합니다.</p>
     *
     * @param logger    로그를 출력할 {@link Logger}
     * @param cacheName 캐시 식별용 이름
     * @param cacheKey  조회한 캐시 키 또는 파티션 정보
     * @param metadata  추가 메타데이터 (예: 조회 결과 개수)
     */
    public static void hit(Logger logger, String cacheName, Object cacheKey, Object metadata) {
        if (logger.isInfoEnabled()) {
            logger.info("{} hit cache={} key={} meta={}", PREFIX, cacheName,
                    sanitize(cacheKey), sanitize(metadata));
        }
    }

    /**
     * <h3>캐시 미스 로그 (사유 포함)</h3>
     * <p>캐시 조회가 실패했을 때 미스 로그를 남기고 실패 사유를 기록합니다.</p>
     *
     * @param logger    로그를 출력할 {@link Logger}
     * @param cacheName 캐시 식별용 이름
     * @param cacheKey  조회한 캐시 키 또는 파티션 정보
     * @param reason    미스 발생 사유
     */
    public static void miss(Logger logger, String cacheName, Object cacheKey, Object reason) {
        if (logger.isInfoEnabled()) {
            logger.info("{} miss cache={} key={} reason={}", PREFIX, cacheName,
                    sanitize(cacheKey), sanitize(reason));
        }
    }

    private static String sanitize(Object value) {
        if (value == null) {
            return EMPTY_VALUE;
        }
        String asString = value.toString();
        if (asString.length() > 120) {
            return asString.substring(0, 117) + "...";
        }
        return asString.replaceAll("[\\r\\n]", " ");
    }
}

package jaeik.bimillog.e2e.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * 대기 조건 헬퍼
 * 복잡한 대기 조건 및 재시도 로직 처리
 */
public class WaitHelper {

    private static final int DEFAULT_TIMEOUT = 30000;
    private static final int DEFAULT_POLLING_INTERVAL = 500;

    /**
     * 커스텀 조건 대기
     */
    public static boolean waitForCondition(BooleanSupplier condition, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.getAsBoolean()) {
                return true;
            }
            sleep(DEFAULT_POLLING_INTERVAL);
        }
        return false;
    }

    /**
     * 요소가 특정 텍스트를 포함할 때까지 대기
     */
    public static boolean waitForTextContent(Locator locator, String expectedText, int timeoutMs) {
        return waitForCondition(() -> {
            try {
                String actualText = locator.textContent();
                return actualText != null && actualText.contains(expectedText);
            } catch (Exception e) {
                return false;
            }
        }, timeoutMs);
    }

    /**
     * 요소의 개수가 특정 값이 될 때까지 대기
     */
    public static boolean waitForElementCount(Page page, String selector, int expectedCount, int timeoutMs) {
        return waitForCondition(() -> {
            try {
                return page.locator(selector).count() == expectedCount;
            } catch (Exception e) {
                return false;
            }
        }, timeoutMs);
    }

    /**
     * URL이 특정 패턴과 일치할 때까지 대기
     */
    public static boolean waitForUrl(Page page, String urlPattern, int timeoutMs) {
        return waitForCondition(() -> {
            String currentUrl = page.url();
            return currentUrl.matches(urlPattern) || currentUrl.contains(urlPattern);
        }, timeoutMs);
    }

    /**
     * 네트워크 요청 완료 대기
     */
    public static void waitForNetworkIdle(Page page, int timeoutMs) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions()
                .setTimeout(timeoutMs));
        } catch (Exception e) {
            System.err.println("Network idle timeout: " + e.getMessage());
        }
    }

    /**
     * API 응답 대기
     */
    public static void waitForApiResponse(Page page, String urlPattern, int expectedStatus) {
        page.waitForResponse(response ->
            response.url().contains(urlPattern) && response.status() == expectedStatus,
            () -> {}
        );
    }

    /**
     * 요소가 활성화될 때까지 대기
     */
    public static boolean waitForEnabled(Locator locator, int timeoutMs) {
        return waitForCondition(() -> {
            try {
                return locator.isEnabled();
            } catch (Exception e) {
                return false;
            }
        }, timeoutMs);
    }

    /**
     * 요소가 비활성화될 때까지 대기
     */
    public static boolean waitForDisabled(Locator locator, int timeoutMs) {
        return waitForCondition(() -> {
            try {
                return !locator.isEnabled();
            } catch (Exception e) {
                return false;
            }
        }, timeoutMs);
    }

    /**
     * 애니메이션 완료 대기
     */
    public static void waitForAnimation(Page page, int durationMs) {
        page.waitForTimeout(durationMs);
    }

    /**
     * 재시도 로직과 함께 액션 실행
     */
    public static <T> T retryAction(java.util.function.Supplier<T> action, int maxRetries, int delayMs) {
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    sleep(delayMs);
                }
            }
        }
        throw new RuntimeException("Action failed after " + maxRetries + " retries", lastException);
    }

    /**
     * 요소가 특정 클래스를 가질 때까지 대기
     */
    public static boolean waitForClass(Locator locator, String className, int timeoutMs) {
        return waitForCondition(() -> {
            try {
                String classes = locator.getAttribute("class");
                return classes != null && classes.contains(className);
            } catch (Exception e) {
                return false;
            }
        }, timeoutMs);
    }

    /**
     * 속성 값이 변경될 때까지 대기
     */
    public static boolean waitForAttributeChange(Locator locator, String attribute, String expectedValue, int timeoutMs) {
        return waitForCondition(() -> {
            try {
                String actualValue = locator.getAttribute(attribute);
                return expectedValue.equals(actualValue);
            } catch (Exception e) {
                return false;
            }
        }, timeoutMs);
    }

    /**
     * 로딩 인디케이터가 사라질 때까지 대기
     */
    public static void waitForLoadingComplete(Page page, String loadingSelector) {
        // 먼저 로딩 인디케이터가 나타나기를 잠시 기다림
        sleep(500);

        // 로딩 인디케이터가 사라질 때까지 대기
        if (page.locator(loadingSelector).isVisible()) {
            page.waitForSelector(loadingSelector, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(DEFAULT_TIMEOUT));
        }
    }

    /**
     * 토스트 메시지가 나타났다가 사라질 때까지 대기
     */
    public static String waitForToastMessage(Page page, String toastSelector) {
        // 토스트 출현 대기
        page.waitForSelector(toastSelector, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));

        // 메시지 텍스트 가져오기
        String message = page.locator(toastSelector).textContent();

        // 토스트 사라짐 대기
        page.waitForSelector(toastSelector, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.HIDDEN)
            .setTimeout(DEFAULT_TIMEOUT));

        return message;
    }

    /**
     * 스크롤 위치가 특정 값에 도달할 때까지 대기
     */
    public static boolean waitForScrollPosition(Page page, int targetPosition, int timeoutMs) {
        return waitForCondition(() -> {
            Object scrollY = page.evaluate("window.scrollY");
            return scrollY instanceof Number && ((Number) scrollY).intValue() >= targetPosition;
        }, timeoutMs);
    }

    /**
     * 요소가 뷰포트에 보일 때까지 대기
     */
    public static boolean waitForInViewport(Page page, String selector, int timeoutMs) {
        return waitForCondition(() -> {
            return (Boolean) page.evaluate(
                "selector => {" +
                "  const element = document.querySelector(selector);" +
                "  if (!element) return false;" +
                "  const rect = element.getBoundingClientRect();" +
                "  return rect.top >= 0 && rect.bottom <= window.innerHeight;" +
                "}", selector);
        }, timeoutMs);
    }

    /**
     * 슬립 헬퍼
     */
    private static void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
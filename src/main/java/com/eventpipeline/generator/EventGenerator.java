package com.eventpipeline.generator;

import com.eventpipeline.service.UserActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventGenerator implements ApplicationRunner {

    private final UserActionService userActionService;

    private static final Random RANDOM = new Random();

    /** 세션의 현재 이벤트 시각 (세션 내에서 순차 전진) */
    private final ThreadLocal<LocalDateTime> sessionTime = new ThreadLocal<>();

    private static final String[] USERS = new String[20];
    private static final String[] TRAFFIC_SOURCES = {"google", "direct", "ad"};
    private static final String[] DEVICE_TYPES    = {"mobile", "pc"};
    private static final String[] CATEGORIES      = {"electronics", "clothing", "food", "sports", "beauty"};
    private static final String[] PAYMENT_METHODS = {"card", "kakao_pay", "naver_pay", "bank_transfer"};
    private static final String[] REFERRERS       = {"/home", "/search", "external/google"};
    private static final String[] ERROR_CODES     = {"ERR_500", "ERR_TIMEOUT", "ERR_PAYMENT_FAIL", "ERR_OUT_OF_STOCK"};

    static {
        for (int i = 0; i < 20; i++) {
            USERS[i] = String.format("user_%03d", i + 1);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 0; i < 300; i++) {
            generateSession();
        }
    }

    private void generateSession() {
        String userId        = pick(USERS);
        String sessionId     = UUID.randomUUID().toString();
        String trafficSource = pick(TRAFFIC_SOURCES);
        String deviceType    = pick(DEVICE_TYPES);

        // 최근 7일 내 랜덤 시각을 세션 기준 시각으로 설정
        LocalDateTime baseTime = LocalDateTime.now()
                .minusDays(RANDOM.nextInt(7))
                .minusHours(RANDOM.nextInt(24))
                .minusMinutes(RANDOM.nextInt(60));
        sessionTime.set(baseTime);

        MDCUtil.set(userId, sessionId, trafficSource, deviceType);
        MDCUtil.setEventTime(sessionTime.get());
        try {
            runSessionFlow();
        } finally {
            MDCUtil.clear();
            sessionTime.remove();
        }
    }

    /** 이벤트 발행 전 MDC 시각을 1~30분 전진 */
    private void advanceSessionTime() {
        LocalDateTime next = sessionTime.get().plusMinutes(1 + RANDOM.nextInt(30));
        sessionTime.set(next);
        MDCUtil.setEventTime(next);
    }

    private void runSessionFlow() {
        // PRODUCT_VIEW 1~3회
        int viewCount  = 1 + RANDOM.nextInt(3);
        String productId = randomProductId();
        String category  = pick(CATEGORIES);
        int price        = randomPrice();

        for (int i = 0; i < viewCount; i++) {
            if (i > 0) {
                productId = randomProductId();
                category  = pick(CATEGORIES);
                price     = randomPrice();
            }
            advanceSessionTime();
            userActionService.viewProduct(productViewProps(productId, category, price));

            // PRODUCT_VIEW 이후 ERROR 10%
            if (chance(0.10)) {
                advanceSessionTime();
                userActionService.recordError(errorProps("view"));
                return;
            }
        }

        // ADD_TO_CART 60%
        if (!chance(0.60)) return;

        advanceSessionTime();
        userActionService.addToCart(addToCartProps(productId, category));

        // ADD_TO_CART 이후 ERROR 10%
        if (chance(0.10)) {
            advanceSessionTime();
            userActionService.recordError(errorProps("cart"));
            return;
        }

        // PURCHASE_COMPLETED 55%
        if (!chance(0.55)) return;

        advanceSessionTime();
        userActionService.completePurchase(purchaseProps(productId, price));
    }

    // ── properties builders ────────────────────────────────────────────────

    private Map<String, Object> productViewProps(String productId, String category, int price) {
        Map<String, Object> p = new HashMap<>();
        p.put("product_id",   productId);
        p.put("category",     category);
        p.put("price",        price);
        p.put("page_url",     "/product/" + productId);
        p.put("referrer_url", pick(REFERRERS));
        return p;
    }

    private Map<String, Object> addToCartProps(String productId, String category) {
        Map<String, Object> p = new HashMap<>();
        p.put("product_id", productId);
        p.put("category",   category);
        p.put("quantity",   1 + RANDOM.nextInt(3));
        return p;
    }

    private Map<String, Object> purchaseProps(String productId, int price) {
        Map<String, Object> p = new HashMap<>();
        p.put("transaction_id",  "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        p.put("product_id",      productId);
        p.put("total_amount",    price);
        p.put("payment_method",  pick(PAYMENT_METHODS));
        return p;
    }

    private Map<String, Object> errorProps(String errorStage) {
        String errorCode = pick(ERROR_CODES);
        Map<String, Object> p = new HashMap<>();
        p.put("error_code",  errorCode);
        p.put("error_stage", errorStage);
        p.put("message",     errorCode + " occurred at " + errorStage + " stage");
        return p;
    }

    // ── utils ──────────────────────────────────────────────────────────────

    private String randomProductId() {
        return "prod_" + String.format("%04d", RANDOM.nextInt(200) + 1);
    }

    private int randomPrice() {
        return (1 + RANDOM.nextInt(100)) * 1000; // 1,000 ~ 100,000원
    }

    private boolean chance(double probability) {
        return RANDOM.nextDouble() < probability;
    }

    private <T> T pick(T[] array) {
        return array[RANDOM.nextInt(array.length)];
    }
}

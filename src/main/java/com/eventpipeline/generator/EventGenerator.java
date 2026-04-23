package com.eventpipeline.generator;

import com.eventpipeline.domain.EventStatus;
import com.eventpipeline.domain.EventType;
import com.eventpipeline.event.UserEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventGenerator implements ApplicationRunner {

    private final ApplicationEventPublisher eventPublisher;

    private static final Random RANDOM = new Random();

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

        MDCUtil.set(userId, sessionId, trafficSource, deviceType);
        try {
            runSessionFlow(userId, sessionId, trafficSource, deviceType);
        } finally {
            MDCUtil.clear();
        }
    }

    private void runSessionFlow(String userId, String sessionId, String trafficSource, String deviceType) {
        LocalDateTime eventTime = randomRecentTime();

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
            eventTime = nextEventTime(eventTime);
            publish(userId, sessionId, trafficSource, deviceType, eventTime,
                    EventType.PRODUCT_VIEW, EventStatus.SUCCESS,
                    productViewProps(productId, category, price));

            // PRODUCT_VIEW 이후 ERROR 10%
            if (chance(0.10)) {
                eventTime = nextEventTime(eventTime);
                publish(userId, sessionId, trafficSource, deviceType, eventTime,
                        EventType.ERROR_OCCURRED, EventStatus.FAILURE,
                        errorProps("view"));
                return;
            }
        }

        // ADD_TO_CART 60%
        if (!chance(0.60)) return;

        eventTime = nextEventTime(eventTime);
        publish(userId, sessionId, trafficSource, deviceType, eventTime,
                EventType.ADD_TO_CART, EventStatus.SUCCESS,
                addToCartProps(productId, category));

        // ADD_TO_CART 이후 ERROR 10%
        if (chance(0.10)) {
            eventTime = nextEventTime(eventTime);
            publish(userId, sessionId, trafficSource, deviceType, eventTime,
                    EventType.ERROR_OCCURRED, EventStatus.FAILURE,
                    errorProps("cart"));
            return;
        }

        // PURCHASE_COMPLETED 55%
        if (!chance(0.55)) return;

        eventTime = nextEventTime(eventTime);
        publish(userId, sessionId, trafficSource, deviceType, eventTime,
                EventType.PURCHASE_COMPLETED, EventStatus.SUCCESS,
                purchaseProps(productId, price));
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

    // ── publish ────────────────────────────────────────────────────────────

    private void publish(String userId, String sessionId, String trafficSource, String deviceType,
                         LocalDateTime eventTime, EventType eventType, EventStatus status,
                         Map<String, Object> properties) {
        eventPublisher.publishEvent(UserEventPayload.builder()
                .eventType(eventType)
                .userId(userId)
                .sessionId(sessionId)
                .eventTime(eventTime)
                .trafficSource(trafficSource)
                .deviceType(deviceType)
                .status(status)
                .properties(properties)
                .build());
    }

    // ── utils ──────────────────────────────────────────────────────────────

    private LocalDateTime randomRecentTime() {
        long minutesInSevenDays = 7L * 24 * 60;
        return LocalDateTime.now().minusMinutes(RANDOM.nextLong(minutesInSevenDays));
    }

    private LocalDateTime nextEventTime(LocalDateTime base) {
        long seconds = 5 + RANDOM.nextLong(596); // 5초 ~ 10분(600초)
        return base.plusSeconds(seconds);
    }

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

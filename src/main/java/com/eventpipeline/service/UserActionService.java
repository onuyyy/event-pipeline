package com.eventpipeline.service;

import com.eventpipeline.annotation.UserEvent;
import com.eventpipeline.domain.EventType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserActionService {

    @UserEvent(type = EventType.PRODUCT_VIEW)
    public void viewProduct(Map<String, Object> properties) {}

    @UserEvent(type = EventType.ADD_TO_CART)
    public void addToCart(Map<String, Object> properties) {}

    @UserEvent(type = EventType.PURCHASE_COMPLETED)
    public void completePurchase(Map<String, Object> properties) {}

    @UserEvent(type = EventType.ERROR_OCCURRED)
    public void recordError(Map<String, Object> properties) {}
}

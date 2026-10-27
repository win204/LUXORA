package com.luxora.commerce.aws.event;

public interface CommerceEventPublisher {

    void publishEvent(CommerceEvent event);
}

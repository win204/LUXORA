package com.luxora.commerce.promotion.dto;
import java.math.BigDecimal;
public record AppliedPromotionResponse(String code,String name,BigDecimal discountAmount){}
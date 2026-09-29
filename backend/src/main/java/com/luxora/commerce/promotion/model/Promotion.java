package com.luxora.commerce.promotion.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="promotions")
public class Promotion {
 @Id @GeneratedValue private UUID id;
 @Column(nullable=false,unique=true,length=64) private String code;
 @Column(nullable=false,length=160) private String name;
 @Column(length=500) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private PromotionType type;
 @Column(name="[value]",nullable=false,precision=12,scale=2) private BigDecimal value;
 @Column(name="minimum_order_amount",precision=12,scale=2) private BigDecimal minimumOrderAmount;
 @Column(name="maximum_discount_amount",precision=12,scale=2) private BigDecimal maximumDiscountAmount;
 @Column(name="starts_at",nullable=false) private Instant startsAt;
 @Column(name="ends_at",nullable=false) private Instant endsAt;
 @Column(name="usage_limit") private Integer usageLimit;
 @Column(name="usage_count",nullable=false) private int usageCount;
 @Column(nullable=false) private boolean active;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Promotion(){}
 public Promotion(String code,String name,String description,PromotionType type,BigDecimal value,BigDecimal minimum,BigDecimal maximum,Instant starts,Instant ends,Integer limit,boolean active){update(code,name,description,type,value,minimum,maximum,starts,ends,limit,active);}
 @PrePersist void created(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void updated(){updatedAt=Instant.now();}
 public void update(String code,String name,String description,PromotionType type,BigDecimal value,BigDecimal minimum,BigDecimal maximum,Instant starts,Instant ends,Integer limit,boolean active){this.code=code;this.name=name;this.description=description;this.type=type;this.value=value;this.minimumOrderAmount=minimum;this.maximumDiscountAmount=maximum;this.startsAt=starts;this.endsAt=ends;this.usageLimit=limit;this.active=active;}
 public void incrementUsage(){usageCount++;}
 public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public PromotionType getType(){return type;} public BigDecimal getValue(){return value;} public BigDecimal getMinimumOrderAmount(){return minimumOrderAmount;} public BigDecimal getMaximumDiscountAmount(){return maximumDiscountAmount;} public Instant getStartsAt(){return startsAt;} public Instant getEndsAt(){return endsAt;} public Integer getUsageLimit(){return usageLimit;} public int getUsageCount(){return usageCount;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
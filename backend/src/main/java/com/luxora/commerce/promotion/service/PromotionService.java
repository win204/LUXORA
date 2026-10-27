package com.luxora.commerce.promotion.service;
import com.luxora.commerce.common.exception.*; import com.luxora.commerce.promotion.dto.*; import com.luxora.commerce.promotion.model.*; import com.luxora.commerce.promotion.repository.PromotionRepository; import java.math.*; import java.time.Instant; import java.util.UUID; import org.springframework.data.domain.*; import com.luxora.commerce.catalog.dto.PageResponse; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class PromotionService {
 private final PromotionRepository repo; public PromotionService(PromotionRepository repo){this.repo=repo;}
 @Transactional(readOnly=true) public PageResponse<PromotionResponse> list(int page,int size,Boolean active,String search){Page<Promotion> result=repo.findAdminPage(active,trimNull(search),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt")));return new PageResponse<>(result.getContent().stream().map(this::map).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());} @Transactional(readOnly=true) public PromotionResponse get(UUID id){return map(repo.findById(id).orElseThrow(()->new NotFoundException("PROMOTION_NOT_FOUND","Promotion not found")));}
 @Transactional public PromotionResponse create(PromotionUpsertRequest r){String c=code(r.code()); if(repo.existsByCode(c))throw new ConflictException("PROMOTION_CODE_CONFLICT","Promotion code already exists"); validate(r); return map(repo.save(new Promotion(c,trim(r.name()),trimNull(r.description()),r.type(),r.value(),r.minimumOrderAmount(),r.maximumDiscountAmount(),r.startsAt(),r.endsAt(),r.usageLimit(),r.active())));}
 @Transactional public PromotionResponse update(UUID id,PromotionUpsertRequest r){String c=code(r.code()); if(repo.existsByCodeAndIdNot(c,id))throw new ConflictException("PROMOTION_CODE_CONFLICT","Promotion code already exists"); validate(r); Promotion p=repo.findById(id).orElseThrow(()->new NotFoundException("PROMOTION_NOT_FOUND","Promotion not found")); p.update(c,trim(r.name()),trimNull(r.description()),r.type(),r.value(),r.minimumOrderAmount(),r.maximumDiscountAmount(),r.startsAt(),r.endsAt(),r.usageLimit(),r.active()); return map(p);}
 public AppliedPromotion apply(String code,BigDecimal subtotal,boolean lock){if(code==null||code.isBlank())return AppliedPromotion.none(); Promotion p=(lock?repo.findByCodeForUpdate(code(code)):repo.findByCode(code(code))).orElseThrow(()->new NotFoundException("PROMOTION_NOT_FOUND","Promotion not found")); validateEligible(p,subtotal); BigDecimal amount=p.getType()==PromotionType.PERCENTAGE?subtotal.multiply(p.getValue()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP):p.getValue(); if(p.getMaximumDiscountAmount()!=null)amount=amount.min(p.getMaximumDiscountAmount()); amount=amount.min(subtotal).max(BigDecimal.ZERO); return new AppliedPromotion(p,amount);}
 public void consume(AppliedPromotion applied){if(applied.promotion()!=null)applied.promotion().incrementUsage();}

 @Transactional(readOnly = true)
 public java.util.List<AvailablePromotionResponse> getAvailablePromotions(BigDecimal subtotal) {
     BigDecimal effectiveSubtotal = (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) ? subtotal : BigDecimal.ZERO;
     java.util.List<Promotion> promotions = repo.findAvailablePromotions(Instant.now());

     java.util.List<AvailablePromotionResponse> evaluated = new java.util.ArrayList<>();
     BigDecimal maxDiscount = BigDecimal.ZERO;
     String bestDealCode = null;

     for (Promotion p : promotions) {
         boolean eligible = p.getMinimumOrderAmount() == null || effectiveSubtotal.compareTo(p.getMinimumOrderAmount()) >= 0;
         BigDecimal amountNeeded = (!eligible && p.getMinimumOrderAmount() != null)
                 ? p.getMinimumOrderAmount().subtract(effectiveSubtotal).max(BigDecimal.ZERO)
                 : BigDecimal.ZERO;

         BigDecimal discount = BigDecimal.ZERO;
         if (effectiveSubtotal.compareTo(BigDecimal.ZERO) > 0) {
             discount = (p.getType() == PromotionType.PERCENTAGE)
                     ? effectiveSubtotal.multiply(p.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                     : p.getValue();
             if (p.getMaximumDiscountAmount() != null) {
                 discount = discount.min(p.getMaximumDiscountAmount());
             }
             discount = discount.min(effectiveSubtotal).max(BigDecimal.ZERO);
         }

         if (eligible && discount.compareTo(maxDiscount) > 0) {
             maxDiscount = discount;
             bestDealCode = p.getCode();
         }

         evaluated.add(new AvailablePromotionResponse(
                 p.getCode(),
                 p.getName(),
                 p.getDescription(),
                 p.getType(),
                 p.getValue(),
                 p.getMinimumOrderAmount(),
                 p.getMaximumDiscountAmount(),
                 discount,
                 eligible,
                 amountNeeded,
                 false
         ));
     }

     final String bestCode = bestDealCode;
     return evaluated.stream()
             .map(r -> new AvailablePromotionResponse(
                     r.code(),
                     r.name(),
                     r.description(),
                     r.type(),
                     r.value(),
                     r.minimumOrderAmount(),
                     r.maximumDiscountAmount(),
                     r.potentialDiscount(),
                     r.eligible(),
                     r.amountNeeded(),
                     bestCode != null && bestCode.equals(r.code())
             ))
             .sorted((a, b) -> {
                 if (a.eligible() != b.eligible()) {
                     return a.eligible() ? -1 : 1;
                 }
                 if (a.isBestDeal() != b.isBestDeal()) {
                     return a.isBestDeal() ? -1 : 1;
                 }
                 return b.potentialDiscount().compareTo(a.potentialDiscount());
             })
             .toList();
 }

 @Transactional(readOnly = true)
 public String findBestAvailablePromotionCode(BigDecimal subtotal) {
     java.util.List<AvailablePromotionResponse> available = getAvailablePromotions(subtotal);
     return available.stream()
             .filter(AvailablePromotionResponse::isBestDeal)
             .map(AvailablePromotionResponse::code)
             .findFirst()
             .orElse(null);
 }
 private void validateEligible(Promotion p,BigDecimal subtotal){Instant now=Instant.now(); if(!p.isActive())throw new ConflictException("PROMOTION_INACTIVE","Promotion is inactive"); if(now.isBefore(p.getStartsAt()))throw new ConflictException("PROMOTION_NOT_STARTED","Promotion has not started"); if(!now.isBefore(p.getEndsAt()))throw new ConflictException("PROMOTION_EXPIRED","Promotion has expired"); if(p.getMinimumOrderAmount()!=null&&subtotal.compareTo(p.getMinimumOrderAmount())<0)throw new ConflictException("PROMOTION_MINIMUM_NOT_MET","Promotion minimum order amount was not met"); if(p.getUsageLimit()!=null&&p.getUsageCount()>=p.getUsageLimit())throw new ConflictException("PROMOTION_USAGE_LIMIT_REACHED","Promotion usage limit has been reached");}
 private void validate(PromotionUpsertRequest r){if(r.value()==null||r.value().compareTo(BigDecimal.ZERO)<=0)throw new BadRequestException("PROMOTION_VALUE_INVALID","Promotion value must be greater than zero"); if(!r.endsAt().isAfter(r.startsAt()))throw new BadRequestException("PROMOTION_DATE_RANGE_INVALID","endsAt must be after startsAt"); if(r.type()==PromotionType.PERCENTAGE&&r.value().compareTo(BigDecimal.valueOf(100))>0)throw new BadRequestException("PROMOTION_VALUE_INVALID","Percentage value must not exceed 100");}
 public PromotionResponse map(Promotion p){return new PromotionResponse(p.getId(),p.getCode(),p.getName(),p.getDescription(),p.getType(),p.getValue(),p.getMinimumOrderAmount(),p.getMaximumDiscountAmount(),p.getStartsAt(),p.getEndsAt(),p.getUsageLimit(),p.getUsageCount(),p.isActive(),p.getCreatedAt(),p.getUpdatedAt());}
 private String code(String v){return trim(v).toUpperCase(java.util.Locale.ROOT);} private String trim(String v){return v.trim();} private String trimNull(String v){return v==null||v.isBlank()?null:v.trim();}
 public record AppliedPromotion(Promotion promotion,BigDecimal discountAmount){public static AppliedPromotion none(){return new AppliedPromotion(null,BigDecimal.ZERO);} public AppliedPromotionResponse response(){return promotion==null?null:new AppliedPromotionResponse(promotion.getCode(),promotion.getName(),discountAmount);}}
}
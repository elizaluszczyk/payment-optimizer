package com.supermarket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

public record PaymentMethod(String id, int discount, BigDecimal limit) {
  public static final String POINTS_ID = "PUNKTY";

  @JsonCreator
  public PaymentMethod(
      @JsonProperty("id") String id,
      @JsonProperty("discount") String discountStr,
      @JsonProperty("limit") String limitStr) {
    this(
        Objects.requireNonNull(id, "PaymentMethod ID cannot be null"),
        Integer.parseInt(
            Objects.requireNonNull(discountStr, "PaymentMethod discount cannot be null")),
        new BigDecimal(Objects.requireNonNull(limitStr, "PaymentMethod limit cannot be null")));
    if (this.discount < 0 || this.discount > 100) {
      throw new IllegalArgumentException(
          "Discount percentage must be between 0 and 100: " + discountStr);
    }
    if (this.limit.scale() > 2) {
      throw new IllegalArgumentException(
          "PaymentMethod limit cannot have more than two decimal places: " + limitStr);
    }
    if (this.limit.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("PaymentMethod limit cannot be negative: " + limitStr);
    }
  }
}

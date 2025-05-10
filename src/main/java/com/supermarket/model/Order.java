package com.supermarket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Order(String id, BigDecimal value, List<String> promotions) {
  @JsonCreator
  public Order(
      @JsonProperty("id") String id,
      @JsonProperty("value") String valueStr,
      @JsonProperty("promotions") List<String> promotions) {
    this(
        Objects.requireNonNull(id, "Order ID cannot be null"),
        new BigDecimal(Objects.requireNonNull(valueStr, "Order value cannot be null")),
        promotions == null ? List.of() : promotions);
    if (this.value.scale() > 2) {
      throw new IllegalArgumentException(
          "Order value cannot have more than two decimal places: " + valueStr);
    }
    if (this.value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Order value cannot be negative: " + valueStr);
    }
  }
}

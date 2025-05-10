package com.supermarket.dto;

import com.supermarket.model.Order;
import com.supermarket.model.WalletPaymentMethod;
import java.math.BigDecimal;

public record PaymentOption(
    Order order,
    String type,
    BigDecimal discountApplied,
    BigDecimal pointsCost,
    BigDecimal cardCost,
    WalletPaymentMethod pointsPaymentMethod,
    WalletPaymentMethod cardPaymentMethod)
    implements Comparable<PaymentOption> {

  public BigDecimal totalCost() {
    return pointsCost.add(cardCost);
  }

  public BigDecimal originalOrderValue() {
    return order.value();
  }

  @Override
  public int compareTo(PaymentOption other) {
    int discountComparison = other.discountApplied.compareTo(this.discountApplied);
    if (discountComparison != 0) {
      return discountComparison;
    }
    return other.pointsCost.compareTo(this.pointsCost);
  }
}

package com.supermarket.model;

import java.math.BigDecimal;
import java.util.Objects;

public class WalletPaymentMethod {
  private final String id;
  private final int discountPercent;
  private BigDecimal currentLimit;

  public WalletPaymentMethod(String id, int discountPercent, BigDecimal initialLimit) {
    this.id = Objects.requireNonNull(id);
    this.discountPercent = discountPercent;
    this.currentLimit = Objects.requireNonNull(initialLimit);
    if (initialLimit.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Initial limit cannot be negative for " + id);
    }
  }

  public WalletPaymentMethod(PaymentMethod pm) {
    this(pm.id(), pm.discount(), pm.limit());
  }

  public String getId() {
    return id;
  }

  public int getDiscountPercent() {
    return discountPercent;
  }

  public BigDecimal getCurrentLimit() {
    return currentLimit;
  }

  public void spend(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Cannot spend a negative amount.");
    }
    if (this.currentLimit.compareTo(amount) < 0) {
      throw new IllegalStateException(
          "Not enough limit for payment method "
              + id
              + ". Required: "
              + amount
              + ", Available: "
              + this.currentLimit);
    }
    this.currentLimit = this.currentLimit.subtract(amount);
  }

  @Override
  public String toString() {
    return "WalletPaymentMethod{"
        + "id='"
        + id
        + '\''
        + ", discountPercent="
        + discountPercent
        + ", currentLimit="
        + currentLimit.toPlainString()
        + '}';
  }
}

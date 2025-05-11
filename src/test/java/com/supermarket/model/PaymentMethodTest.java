package com.supermarket.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentMethodTest {

  @Test
  void testPaymentMethodCreation_ValidData() {
    PaymentMethod pm = new PaymentMethod("CARD_A", "10", "200.50");
    assertEquals("CARD_A", pm.id());
    assertEquals(10, pm.discount());
    assertEquals(new BigDecimal("200.50"), pm.limit());
  }

  @Test
  void testPaymentMethodCreation_ZeroDiscountAndLimit() {
    PaymentMethod pm = new PaymentMethod("CARD_B", "0", "0.00");
    assertEquals(0, pm.discount());
    assertEquals(BigDecimal.ZERO.setScale(2), pm.limit());
  }

  @Test
  void testPaymentMethodCreation_MaxDiscount() {
    PaymentMethod pm = new PaymentMethod("CARD_C", "100", "1000.00");
    assertEquals(100, pm.discount());
  }

  @Test
  void testPaymentMethodCreation_NullId_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new PaymentMethod(null, "10", "100.00");
        },
        "PaymentMethod ID cannot be null");
  }

  @Test
  void testPaymentMethodCreation_NullDiscountString_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new PaymentMethod("CARD_D", null, "100.00");
        },
        "PaymentMethod discount cannot be null");
  }

  @Test
  void testPaymentMethodCreation_NullLimitString_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new PaymentMethod("CARD_E", "10", null);
        },
        "PaymentMethod limit cannot be null");
  }

  @Test
  void testPaymentMethodCreation_NegativeDiscount_ThrowsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new PaymentMethod("CARD_F", "-5", "100.00");
            });
    assertEquals("Discount percentage must be between 0 and 100: -5", exception.getMessage());
  }

  @Test
  void testPaymentMethodCreation_NegativeLimit_ThrowsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new PaymentMethod("CARD_I", "10", "-50.00");
            });
    assertEquals("PaymentMethod limit cannot be negative: -50.00", exception.getMessage());
  }

  @Test
  void testPaymentMethodCreation_NonNumericDiscount_ThrowsNumberFormatException() {
    assertThrows(
        NumberFormatException.class,
        () -> {
          new PaymentMethod("CARD_J", "ABC", "100.00");
        });
  }
}

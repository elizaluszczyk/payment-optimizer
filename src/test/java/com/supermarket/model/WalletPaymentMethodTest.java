package com.supermarket.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletPaymentMethodTest {

  @Test
  void testWalletCreation_DirectValues() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_A", 15, new BigDecimal("300.00"));
    assertEquals("WALLET_A", wpm.getId());
    assertEquals(15, wpm.getDiscountPercent());
    assertEquals(new BigDecimal("300.00"), wpm.getCurrentLimit());
  }

  @Test
  void testWalletCreation_FromPaymentMethod() {
    PaymentMethod pm = new PaymentMethod("PM_B", "5", "150.25");
    WalletPaymentMethod wpm = new WalletPaymentMethod(pm);

    assertEquals("PM_B", wpm.getId());
    assertEquals(5, wpm.getDiscountPercent());
    assertEquals(new BigDecimal("150.25"), wpm.getCurrentLimit());
  }

  @Test
  void testWalletCreation_NegativeInitialLimit_ThrowsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new WalletPaymentMethod("WALLET_C", 10, new BigDecimal("-100.00"));
            });
    assertEquals("Initial limit cannot be negative for WALLET_C", exception.getMessage());
  }

  @Test
  void testSpend_Successful() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_D", 0, new BigDecimal("100.00"));
    wpm.spend(new BigDecimal("40.00"));
    assertEquals(new BigDecimal("60.00"), wpm.getCurrentLimit());
  }

  @Test
  void testSpend_ExactLimit() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_E", 0, new BigDecimal("50.00"));
    wpm.spend(new BigDecimal("50.00"));
    assertEquals(BigDecimal.ZERO.setScale(2), wpm.getCurrentLimit());
  }

  @Test
  void testSpend_ZeroAmount() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_F", 0, new BigDecimal("70.00"));
    wpm.spend(BigDecimal.ZERO);
    assertEquals(new BigDecimal("70.00"), wpm.getCurrentLimit());
  }

  @Test
  void testSpend_NegativeAmount_ThrowsIllegalArgumentException() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_G", 0, new BigDecimal("100.00"));
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              wpm.spend(new BigDecimal("-10.00"));
            });
    assertEquals("Cannot spend a negative amount.", exception.getMessage());
    assertEquals(
        new BigDecimal("100.00"),
        wpm.getCurrentLimit(),
        "Limit should not change on failed spend.");
  }

  @Test
  void testSpend_InsufficientFunds_ThrowsIllegalStateException() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_H", 0, new BigDecimal("20.00"));
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              wpm.spend(new BigDecimal("25.00"));
            });
    assertEquals(
        "Not enough limit for payment method WALLET_H. Required: 25.00, Available: 20.00",
        exception.getMessage());
    assertEquals(
        new BigDecimal("20.00"), wpm.getCurrentLimit(), "Limit should not change on failed spend.");
  }

  @Test
  void testToString() {
    WalletPaymentMethod wpm = new WalletPaymentMethod("WALLET_STR", 7, new BigDecimal("123.45"));
    String wpmString = wpm.toString();
    assertTrue(wpmString.contains("id='WALLET_STR'"), "toString should contain id");
    assertTrue(wpmString.contains("discountPercent=7"), "toString should contain discountPercent");
    assertTrue(wpmString.contains("currentLimit=123.45"), "toString should contain currentLimit");
  }
}

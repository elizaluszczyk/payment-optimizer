package com.supermarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.supermarket.model.Order;
import com.supermarket.model.PaymentMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentOptimizerTest {

  private PaymentOptimizer optimizer;

  @BeforeEach
  void setUp() {
    optimizer = new PaymentOptimizer();
  }

  private BigDecimal val(String value) {
    return new BigDecimal(value);
  }

  private String bdStr(BigDecimal bd) {
    if (bd == null) {
      return "null";
    }
    return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  @Test
  void testOptimizerWithProvidedScenario() {

    List<Order> orders =
        List.of(
            new Order("ORDER1", "100.00", List.of("mZysk")),
            new Order("ORDER2", "200.00", List.of("BosBankrut")),
            new Order("ORDER3", "150.00", List.of("mZysk", "BosBankrut")),
            new Order("ORDER4", "50.00", null));

    List<PaymentMethod> paymentMethods =
        List.of(
            new PaymentMethod("PUNKTY", "15", "100.00"),
            new PaymentMethod("mZysk", "10", "180.00"),
            new PaymentMethod("BosBankrut", "5", "200.00"));

    Map<String, BigDecimal> result = optimizer.optimizePayments(orders, paymentMethods);

    assertNotNull(result, "Result map should not be null");

    long nonZeroSpendingMethods =
        result.values().stream().filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).count();
    assertEquals(3, nonZeroSpendingMethods, "Expected 3 payment methods with non-zero spending.");

    assertEquals(
        val("100.00").setScale(2, RoundingMode.HALF_UP),
        result.get("PUNKTY").setScale(2, RoundingMode.HALF_UP),
        "PUNKTY spent amount mismatch. Expected: 100.00, Actual: " + bdStr(result.get("PUNKTY")));

    assertEquals(
        val("190.00").setScale(2, RoundingMode.HALF_UP),
        result.get("BosBankrut").setScale(2, RoundingMode.HALF_UP),
        "BosBankrut spent amount mismatch. Expected: 190.00, Actual: "
            + bdStr(result.get("BosBankrut")));

    assertEquals(
        val("165.00").setScale(2, RoundingMode.HALF_UP),
        result.get("mZysk").setScale(2, RoundingMode.HALF_UP),
        "mZysk spent amount mismatch. Expected: 165.00, Actual: " + bdStr(result.get("mZysk")));
  }

  @Test
  void testEdgeCase_InsufficientFundsOverall() {
    List<Order> orders = List.of(new Order("ORDER_EXPENSIVE", "100.00", null));
    List<PaymentMethod> paymentMethods =
        List.of(
            new PaymentMethod("PUNKTY", "0", "10.00"), new PaymentMethod("CardA", "0", "10.00"));

    assertThrows(
        IllegalStateException.class,
        () -> {
          optimizer.optimizePayments(orders, paymentMethods);
        },
        "Should throw IllegalStateException when funds are insufficient to cover an order.");
  }

  @Test
  void testEdgeCase_CardPromoIsBest() {
    List<Order> orders = List.of(new Order("ORDER_CARD_PROMO", "100.00", List.of("SuperCard")));
    List<PaymentMethod> paymentMethods =
        List.of(
            new PaymentMethod("PUNKTY", "15", "100.00"),
            new PaymentMethod("SuperCard", "20", "100.00"),
            new PaymentMethod("CardB", "0", "100.00"));

    Map<String, BigDecimal> result = optimizer.optimizePayments(orders, paymentMethods);

    assertNotNull(result);
    assertEquals(
        val("80.00").setScale(2, RoundingMode.HALF_UP),
        result.get("SuperCard").setScale(2, RoundingMode.HALF_UP),
        "SuperCard spent amount mismatch. Expected: 80.00, Actual: "
            + bdStr(result.get("SuperCard")));
    assertTrue(
        result.get("PUNKTY") == null || result.get("PUNKTY").compareTo(BigDecimal.ZERO) == 0,
        "PUNKTY should not be used. Actual: " + bdStr(result.get("PUNKTY")));
    assertTrue(
        result.get("CardB") == null || result.get("CardB").compareTo(BigDecimal.ZERO) == 0,
        "CardB should not be used. Actual: " + bdStr(result.get("CardB")));
  }

  @Test
  void testEdgeCase_PartialPointsRule3WithCard() {
    List<Order> orders = List.of(new Order("ORDER_PARTIAL", "100.00", null));
    List<PaymentMethod> paymentMethods =
        List.of(
            new PaymentMethod("PUNKTY", "0", "20.00"), new PaymentMethod("CardA", "0", "100.00"));

    Map<String, BigDecimal> result = optimizer.optimizePayments(orders, paymentMethods);

    assertNotNull(result);
    assertEquals(
        val("20.00").setScale(2, RoundingMode.HALF_UP),
        result.get("PUNKTY").setScale(2, RoundingMode.HALF_UP),
        "PUNKTY spent amount for partial payment mismatch. Expected: 20.00, Actual: "
            + bdStr(result.get("PUNKTY")));
    assertEquals(
        val("70.00").setScale(2, RoundingMode.HALF_UP),
        result.get("CardA").setScale(2, RoundingMode.HALF_UP),
        "CardA spent amount for remainder mismatch. Expected: 70.00, Actual: "
            + bdStr(result.get("CardA")));
  }

  @Test
  void testEdgeCase_OrderValueZero() {
    List<Order> orders = List.of(new Order("ORDER_ZERO", "0.00", List.of("mZysk")));
    List<PaymentMethod> paymentMethods =
        List.of(
            new PaymentMethod("PUNKTY", "15", "100.00"),
            new PaymentMethod("mZysk", "10", "100.00"));

    Map<String, BigDecimal> result = optimizer.optimizePayments(orders, paymentMethods);

    assertNotNull(result);

    result.forEach(
        (methodId, spentAmount) -> {
          assertEquals(
              BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
              spentAmount.setScale(2, RoundingMode.HALF_UP),
              "Method "
                  + methodId
                  + " should have zero spending for a zero-value order. Actual: "
                  + bdStr(spentAmount));
        });

    if (result.containsKey("PUNKTY")) {
      assertEquals(
          BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
          result.get("PUNKTY").setScale(2, RoundingMode.HALF_UP));
    }
    if (result.containsKey("mZysk")) {
      assertEquals(
          BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
          result.get("mZysk").setScale(2, RoundingMode.HALF_UP));
    }
  }
}

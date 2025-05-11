package com.supermarket.service;

import com.supermarket.dto.PaymentOption;
import com.supermarket.model.Order;
import com.supermarket.model.PaymentMethod;
import com.supermarket.model.WalletPaymentMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimizes payments for a list of orders to maximize discounts by strategically applying available
 * payment methods and promotions.
 */
public class PaymentOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(PaymentOptimizer.class);

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
  private static final BigDecimal MINIMUM_POINTS_FOR_NON_ZERO_ORDER_VALUE = new BigDecimal("0.01");

  /**
   * Main method to optimize payments for a list of orders. It processes orders in two phases: 1.
   * Applies the best card-specific promotions globally. 2. Processes remaining orders by evaluating
   * various payment options (points, partial points, cards).
   *
   * @param orders The list of orders to be paid.
   * @param initialPaymentMethods The list of available payment methods with their limits and
   *     discounts.
   * @return A map where keys are payment method IDs and values are the total amounts spent using
   *     that method.
   */
  public Map<String, BigDecimal> optimizePayments(
      List<Order> orders, List<PaymentMethod> initialPaymentMethods) {
    Map<String, WalletPaymentMethod> wallet =
        initialPaymentMethods.stream()
            .collect(Collectors.toMap(PaymentMethod::id, WalletPaymentMethod::new));

    Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();
    initialPaymentMethods.forEach(pm -> totalSpentByMethod.put(pm.id(), BigDecimal.ZERO));

    Set<String> handledOrderIds = new HashSet<>();

    processPhase1CardPromotions(orders, wallet, totalSpentByMethod, handledOrderIds);
    processPhase2RemainingOrders(orders, wallet, totalSpentByMethod, handledOrderIds);

    return totalSpentByMethod;
  }

  /**
   * Phase 1: Identifies and applies the best card-specific promotions globally across all orders.
   * It sorts potential promotions by discount amount (descending) and cost (ascending for ties) and
   * applies them if the order isn't already handled and the card has sufficient limit.
   *
   * @param allOrders The complete list of orders.
   * @param wallet The customer's wallet with current payment method limits.
   * @param totalSpentByMethod Map to track spending per payment method.
   * @param handledOrderIds Set of IDs for orders already paid.
   */
  private void processPhase1CardPromotions(
      List<Order> allOrders,
      Map<String, WalletPaymentMethod> wallet,
      Map<String, BigDecimal> totalSpentByMethod,
      Set<String> handledOrderIds) {
    logger.info("Starting Phase 1: Global Card Promotion Optimization");
    List<PotentialCardPromo> potentialCardPromos = new ArrayList<>();
    for (Order order : allOrders) {
      for (String promoId : order.promotions()) {
        WalletPaymentMethod cardPm = wallet.get(promoId);
        if (cardPm != null && !PaymentMethod.POINTS_ID.equals(cardPm.getId())) {
          BigDecimal discountPercent = BigDecimal.valueOf(cardPm.getDiscountPercent());
          BigDecimal discountAmount =
              order.value().multiply(discountPercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
          BigDecimal costAfterDiscount = order.value().subtract(discountAmount);
          potentialCardPromos.add(
              new PotentialCardPromo(order, cardPm, discountAmount, costAfterDiscount));
        }
      }
    }

    Collections.sort(potentialCardPromos);

    for (PotentialCardPromo promo : potentialCardPromos) {
      if (handledOrderIds.contains(promo.order().id())) {
        continue;
      }

      WalletPaymentMethod cardPm = promo.cardPm();
      if (cardPm.getCurrentLimit().compareTo(promo.cost()) >= 0) {
        logger.info(
            "Phase 1: Applying promo '{}' to order '{}'. Discount: {}, Cost: {}",
            cardPm.getId(),
            promo.order().id(),
            promo.discountAmount(),
            promo.cost());
        applyPayment(cardPm, promo.cost(), totalSpentByMethod);
        handledOrderIds.add(promo.order().id());
      }
    }
    logger.info("Finished Phase 1. Handled {} orders.", handledOrderIds.size());
  }

  /**
   * Phase 2: Processes orders not handled in Phase 1. For each remaining order, it generates all
   * possible payment options (points, partial points, cards), selects the best option based on
   * maximizing discount then points usage, and applies it.
   *
   * @param allOrders The complete list of orders.
   * @param wallet The customer's wallet with current payment method limits.
   * @param totalSpentByMethod Map to track spending per payment method.
   * @param handledOrderIds Set of IDs for orders already paid (or being paid now).
   */
  private void processPhase2RemainingOrders(
      List<Order> allOrders,
      Map<String, WalletPaymentMethod> wallet,
      Map<String, BigDecimal> totalSpentByMethod,
      Set<String> handledOrderIds) {
    logger.info("Starting Phase 2: Processing Remaining Orders");
    List<Order> remainingOrders =
        allOrders.stream()
            .filter(o -> !handledOrderIds.contains(o.id()))
            .sorted(Comparator.comparing(Order::value).reversed())
            .toList();

    for (Order order : remainingOrders) {
      logger.debug("Processing order: {} with value {}", order.id(), order.value());

      List<PaymentOption> optionsForThisOrder = generatePaymentOptionsForOrder(order, wallet);

      if (optionsForThisOrder.isEmpty()) {
        logger.error(
            "CRITICAL: Order {} (value: {}) cannot be paid with current remaining limits. This should not happen.",
            order.id(),
            order.value());
        throw new IllegalStateException(
            "Cannot find a payment option for order "
                + order.id()
                + ". Available limits might be insufficient or algorithm logic flawed for this state.");
      }

      Collections.sort(optionsForThisOrder);
      PaymentOption bestOption = optionsForThisOrder.get(0);

      logChosenOption(bestOption);
      applyChosenPaymentOption(bestOption, totalSpentByMethod);
      handledOrderIds.add(order.id());
    }
    long totalOrderCount = allOrders.size();
    if (handledOrderIds.size() == totalOrderCount) {
      logger.info("Finished Phase 2. All {} orders processed.", totalOrderCount);
    } else {
      logger.warn(
          "Finished Phase 2. Processed {} out of {} orders. Some orders may not have been fully handled.",
          handledOrderIds.size(),
          totalOrderCount);
    }
  }

  /**
   * Generates a list of all valid payment options for a given order based on current wallet limits.
   *
   * @param order The order to generate payment options for.
   * @param wallet The customer's wallet with current payment method limits.
   * @return A list of {@link PaymentOption} objects representing possible ways to pay.
   */
  private List<PaymentOption> generatePaymentOptionsForOrder(
      Order order, Map<String, WalletPaymentMethod> wallet) {
    List<PaymentOption> options = new ArrayList<>();
    WalletPaymentMethod pointsWalletPm = wallet.get(PaymentMethod.POINTS_ID);

    addFullPointsRule4Option(order, pointsWalletPm, options);
    addPartialPointsRule3Option(order, pointsWalletPm, wallet, options);
    addFullCardNoPromoOption(order, wallet, options);
    addFullPointsNoPromoOption(order, pointsWalletPm, options);

    return options;
  }

  /**
   * Adds a payment option for paying fully with points according to Rule 4 (using POINTS method's
   * intrinsic discount), if viable.
   *
   * @param order The order to consider.
   * @param pointsWalletPm The customer's points payment method from the wallet.
   * @param options The list to add the generated payment option to.
   */
  private void addFullPointsRule4Option(
      Order order, WalletPaymentMethod pointsWalletPm, List<PaymentOption> options) {
    if (pointsWalletPm == null || order.value().compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    BigDecimal discountPercent = BigDecimal.valueOf(pointsWalletPm.getDiscountPercent());
    BigDecimal discountAmount =
        order.value().multiply(discountPercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    BigDecimal costAfterDiscount = order.value().subtract(discountAmount);

    if (pointsWalletPm.getCurrentLimit().compareTo(costAfterDiscount) >= 0) {
      options.add(
          new PaymentOption(
              order,
              "FULL_POINTS_RULE4",
              discountAmount,
              costAfterDiscount,
              BigDecimal.ZERO,
              pointsWalletPm,
              null));
      logger.debug(
          "Order {}: Added option FULL_POINTS_RULE4. Discount: {}, Cost: {}",
          order.id(),
          discountAmount,
          costAfterDiscount);
    }
  }

  /**
   * Adds payment options for paying partially with points according to Rule 3 (10% order discount),
   * if viable. This may result in points covering the whole discounted amount or a mix with a card.
   *
   * @param order The order to consider.
   * @param pointsWalletPm The customer's points payment method from the wallet.
   * @param wallet The customer's complete wallet, for finding a card for the remainder.
   * @param options The list to add the generated payment option(s) to.
   */
  private void addPartialPointsRule3Option(
      Order order,
      WalletPaymentMethod pointsWalletPm,
      Map<String, WalletPaymentMethod> wallet,
      List<PaymentOption> options) {
    if (pointsWalletPm == null || order.value().compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    BigDecimal minPointsToTriggerDiscount =
        order.value().multiply(TEN_PERCENT).setScale(2, RoundingMode.HALF_UP);
    if (minPointsToTriggerDiscount.compareTo(BigDecimal.ZERO) <= 0
        && order.value().compareTo(BigDecimal.ZERO) > 0) {
      minPointsToTriggerDiscount = MINIMUM_POINTS_FOR_NON_ZERO_ORDER_VALUE;
    }

    boolean canPayMinimumPoints =
        pointsWalletPm.getCurrentLimit().compareTo(minPointsToTriggerDiscount) >= 0;
    boolean minPointsIsZero = minPointsToTriggerDiscount.compareTo(BigDecimal.ZERO) == 0;

    if (canPayMinimumPoints || minPointsIsZero) {
      BigDecimal orderDiscount =
          order.value().multiply(TEN_PERCENT).setScale(2, RoundingMode.HALF_UP);
      BigDecimal finalOrderCost = order.value().subtract(orderDiscount);

      BigDecimal pointsToPayWith = pointsWalletPm.getCurrentLimit().min(finalOrderCost);

      if (pointsToPayWith.compareTo(minPointsToTriggerDiscount) >= 0) {
        BigDecimal cardPortion = finalOrderCost.subtract(pointsToPayWith);

        if (cardPortion.compareTo(BigDecimal.ZERO) <= 0) {
          BigDecimal actualPointsForFullCoverage = finalOrderCost;
          if (pointsWalletPm.getCurrentLimit().compareTo(actualPointsForFullCoverage) >= 0) {
            options.add(
                new PaymentOption(
                    order,
                    "PARTIAL_POINTS_RULE3_ALL_POINTS",
                    orderDiscount,
                    actualPointsForFullCoverage,
                    BigDecimal.ZERO,
                    pointsWalletPm,
                    null));
            logger.debug(
                "Order {}: Added option PARTIAL_POINTS_RULE3_ALL_POINTS. Discount: {}, PointsCost: {}",
                order.id(),
                orderDiscount,
                actualPointsForFullCoverage);
          }
        } else {
          for (WalletPaymentMethod cardCandidate : wallet.values()) {
            if (!PaymentMethod.POINTS_ID.equals(cardCandidate.getId())
                && cardCandidate.getCurrentLimit().compareTo(cardPortion) >= 0) {
              options.add(
                  new PaymentOption(
                      order,
                      "PARTIAL_POINTS_RULE3_MIXED",
                      orderDiscount,
                      pointsToPayWith,
                      cardPortion,
                      pointsWalletPm,
                      cardCandidate));
              logger.debug(
                  "Order {}: Added option PARTIAL_POINTS_RULE3_MIXED. Discount: {}, PointsCost: {}, CardCost: {} ({})",
                  order.id(),
                  orderDiscount,
                  pointsToPayWith,
                  cardPortion,
                  cardCandidate.getId());
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Adds payment options for paying fully with a card without any specific promotion, if viable. An
   * option is added for each card in the wallet that can cover the full order value.
   *
   * @param order The order to consider.
   * @param wallet The customer's wallet with current payment method limits.
   * @param options The list to add the generated payment option(s) to.
   */
  private void addFullCardNoPromoOption(
      Order order, Map<String, WalletPaymentMethod> wallet, List<PaymentOption> options) {
    if (order.value().compareTo(BigDecimal.ZERO) <= 0) return;

    for (WalletPaymentMethod cardCandidate : wallet.values()) {
      if (!PaymentMethod.POINTS_ID.equals(cardCandidate.getId())
          && cardCandidate.getCurrentLimit().compareTo(order.value()) >= 0) {
        options.add(
            new PaymentOption(
                order,
                "FULL_CARD_NO_PROMO",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                order.value(),
                null,
                cardCandidate));
        logger.debug(
            "Order {}: Added option FULL_CARD_NO_PROMO. Cost: {} using card {}",
            order.id(),
            order.value(),
            cardCandidate.getId());
      }
    }
  }

  /**
   * Adds a payment option for paying fully with points when the POINTS method itself offers no
   * intrinsic discount (0%). This serves as a non-discounted way to spend points if other options
   * are not better.
   *
   * @param order The order to consider.
   * @param pointsWalletPm The customer's points payment method from the wallet.
   * @param options The list to add the generated payment option to.
   */
  private void addFullPointsNoPromoOption(
      Order order, WalletPaymentMethod pointsWalletPm, List<PaymentOption> options) {
    if (pointsWalletPm == null || order.value().compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    if (pointsWalletPm.getDiscountPercent() == 0
        && pointsWalletPm.getCurrentLimit().compareTo(order.value()) >= 0) {
      options.add(
          new PaymentOption(
              order,
              "FULL_POINTS_NO_PROMO",
              BigDecimal.ZERO,
              order.value(),
              BigDecimal.ZERO,
              pointsWalletPm,
              null));
      logger.debug(
          "Order {}: Added option FULL_POINTS_NO_PROMO (0% points discount). Cost: {}",
          order.id(), order.value());
    }
  }

  /**
   * Helper method to apply a payment to a single payment method, updating its limit and total
   * spent.
   *
   * @param paymentMethod The wallet payment method to use.
   * @param amount The amount to spend.
   * @param totalSpentByMethod Map tracking total spending per method (will be updated).
   */
  private void applyPayment(
      WalletPaymentMethod paymentMethod,
      BigDecimal amount,
      Map<String, BigDecimal> totalSpentByMethod) {
    paymentMethod.spend(amount);
    totalSpentByMethod.merge(paymentMethod.getId(), amount, BigDecimal::add);
  }

  /**
   * Applies the chosen best payment option for an order. This involves spending from the respective
   * payment methods (points and/or card) and updating the total amounts spent.
   *
   * @param bestOption The chosen {@link PaymentOption}.
   * @param totalSpentByMethod Map to track spending per payment method (will be updated).
   */
  private void applyChosenPaymentOption(
      PaymentOption bestOption, Map<String, BigDecimal> totalSpentByMethod) {
    if (bestOption.pointsCost().compareTo(BigDecimal.ZERO) > 0
        && bestOption.pointsPaymentMethod() != null) {
      applyPayment(bestOption.pointsPaymentMethod(), bestOption.pointsCost(), totalSpentByMethod);
    }
    if (bestOption.cardCost().compareTo(BigDecimal.ZERO) > 0
        && bestOption.cardPaymentMethod() != null) {
      applyPayment(bestOption.cardPaymentMethod(), bestOption.cardCost(), totalSpentByMethod);
    }
  }

  /**
   * Logs the details of the chosen payment option for an order.
   *
   * @param bestOption The chosen {@link PaymentOption}.
   */
  private void logChosenOption(PaymentOption bestOption) {
    logger.info(
        "Order {}: Chosen option: Type='{}', Discount={}, PointsCost={}, CardCost={}, CardUsed='{}', PointsMethodUsed='{}'",
        bestOption.order().id(),
        bestOption.type(),
        bestOption.discountApplied().setScale(2, RoundingMode.HALF_UP),
        bestOption.pointsCost().setScale(2, RoundingMode.HALF_UP),
        bestOption.cardCost().setScale(2, RoundingMode.HALF_UP),
        bestOption.cardPaymentMethod() != null ? bestOption.cardPaymentMethod().getId() : "N/A",
        bestOption.pointsPaymentMethod() != null
            ? bestOption.pointsPaymentMethod().getId()
            : "N/A");
  }

  /**
   * Represents a potential application of a card-specific promotion to an order. Used in Phase 1 to
   * determine the best global card promotions. Implements {@link Comparable} to sort promotions
   * from best (highest discount, then lowest cost) to worst.
   *
   * @param order The order this promotion applies to.
   * @param cardPm The card payment method offering the promotion.
   * @param discountAmount The calculated monetary discount amount.
   * @param cost The final cost of the order after this promotion.
   */
  private record PotentialCardPromo(
      Order order, WalletPaymentMethod cardPm, BigDecimal discountAmount, BigDecimal cost)
      implements Comparable<PotentialCardPromo> {
    @Override
    public int compareTo(PotentialCardPromo other) {
      int discountCompare = other.discountAmount.compareTo(this.discountAmount);
      if (discountCompare != 0) {
        return discountCompare;
      }
      return this.cost.compareTo(other.cost);
    }
  }
}

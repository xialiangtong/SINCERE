import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShoppingCartSolution Tests")
public class ShoppingCartSolutionTest {

    private ShoppingCartSolution.ProductCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new ShoppingCartSolution.ProductCatalog();
        catalog.addProduct(new ShoppingCartSolution.Product("A", "Product A", "30.00"));
        catalog.addProduct(new ShoppingCartSolution.Product("B", "Product B", "20.00"));
        catalog.addProduct(new ShoppingCartSolution.Product("C", "Product C", "15.00"));
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }

    // ──────────────────────────────────────────────
    // 1. Cart operations
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Cart Operations")
    class CartOperationTests {

        @Test
        @DisplayName("Add items — subtotal computed correctly")
        void addItems() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);
            cart.addItem("B", 2);
            // 3*30 + 2*20 = 130
            assertEquals(0, bd("130.00").compareTo(cart.subtotal()));
        }

        @Test
        @DisplayName("Add same item twice — quantities accumulate")
        void addSameItemTwice() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 2);
            cart.addItem("A", 1);
            assertEquals(3, cart.getLineItem("A").getQuantity());
            assertEquals(0, bd("90.00").compareTo(cart.subtotal()));
        }

        @Test
        @DisplayName("Remove partial quantity")
        void removePartialQuantity() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);
            cart.removeItem("A", 1);
            assertEquals(2, cart.getLineItem("A").getQuantity());
            assertEquals(0, bd("60.00").compareTo(cart.subtotal()));
        }

        @Test
        @DisplayName("Remove all quantity — item removed from cart")
        void removeAllQuantity() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 2);
            cart.removeItem("A", 2);
            assertNull(cart.getLineItem("A"));
            assertTrue(cart.isEmpty());
        }

        @Test
        @DisplayName("Remove more than available — item removed")
        void removeMoreThanAvailable() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 1);
            cart.removeItem("A", 5);
            assertNull(cart.getLineItem("A"));
        }

        @Test
        @DisplayName("Remove non-existent item — no error")
        void removeNonExistent() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.removeItem("A", 1); // no-op
            assertTrue(cart.isEmpty());
        }

        @Test
        @DisplayName("Add unknown product — throws exception")
        void addUnknownProduct() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            assertThrows(IllegalArgumentException.class, () -> cart.addItem("Z", 1));
        }

        @Test
        @DisplayName("Empty cart subtotal is zero")
        void emptyCartSubtotal() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            assertEquals(0, BigDecimal.ZERO.compareTo(cart.subtotal()));
        }

        @Test
        @DisplayName("Add with zero quantity — no-op")
        void addZeroQuantity() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 0);
            assertTrue(cart.isEmpty());
        }
    }

    // ──────────────────────────────────────────────
    // 2. Percentage off SKU
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Percentage Off SKU")
    class PercentageOffSkuTests {

        @Test
        @DisplayName("10% off A: 3xA=$90 → discount=$9.00")
        void tenPercentOff() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.PercentageOffSku("A", "0.10"));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("9.00").compareTo(checkout.totalDiscount()));
            assertEquals(0, bd("81.00").compareTo(checkout.total()));
        }

        @Test
        @DisplayName("Percentage off non-existent SKU — no discount")
        void percentageOffNonExistentSku() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 1);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.PercentageOffSku("Z", "0.10"));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Rounding: 33.33% off $15 = $5.00 (HALF_UP)")
        void roundingHalfUp() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("C", 1); // $15.00

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.PercentageOffSku("C", "0.3333"));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            // 15 * 0.3333 = 4.9995 → rounds to 5.00
            assertEquals(0, bd("5.00").compareTo(checkout.totalDiscount()));
        }
    }

    // ──────────────────────────────────────────────
    // 3. Fixed amount off with threshold
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Fixed Amount Off Threshold")
    class FixedAmountOffThresholdTests {

        @Test
        @DisplayName("$15 off orders >= $100: subtotal=$130 → discount=$15")
        void thresholdMet() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);
            cart.addItem("B", 2); // $130

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addCartRule(new ShoppingCartSolution.FixedAmountOffThreshold("100.00", "15.00"));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("15.00").compareTo(checkout.totalDiscount()));
            assertEquals(0, bd("115.00").compareTo(checkout.total()));
        }

        @Test
        @DisplayName("$15 off orders >= $100: subtotal=$60 → no discount")
        void thresholdNotMet() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 2); // $60

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addCartRule(new ShoppingCartSolution.FixedAmountOffThreshold("100.00", "15.00"));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Threshold exactly met — discount applies")
        void thresholdExactlyMet() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("B", 5); // $100

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addCartRule(new ShoppingCartSolution.FixedAmountOffThreshold("100.00", "15.00"));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("15.00").compareTo(checkout.totalDiscount()));
        }
    }

    // ──────────────────────────────────────────────
    // 4. Buy X Get Y Free
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Buy X Get Y Free")
    class BuyXGetYFreeTests {

        @Test
        @DisplayName("Buy 2 get 1 free: B x 3 → 1 free = $20 discount")
        void buyTwoGetOneFree() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("B", 3);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.BuyXGetYFree("B", 2, 1));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("20.00").compareTo(checkout.totalDiscount()));
            assertEquals(0, bd("40.00").compareTo(checkout.total()));
        }

        @Test
        @DisplayName("Buy 2 get 1 free: B x 2 → not enough for free item")
        void notEnoughForFreeItem() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("B", 2);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.BuyXGetYFree("B", 2, 1));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Buy 2 get 1 free: B x 6 → 2 free = $40 discount")
        void multipleGroups() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("B", 6);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.BuyXGetYFree("B", 2, 1));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("40.00").compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Buy 2 get 1 free: B x 7 → 2 free (7/3=2 groups)")
        void partialGroup() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("B", 7);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.BuyXGetYFree("B", 2, 1));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            // 7 / 3 = 2 complete groups → 2 free items
            assertEquals(0, bd("40.00").compareTo(checkout.totalDiscount()));
        }
    }

    // ──────────────────────────────────────────────
    // 5. Tiered discount
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Tiered Discount")
    class TieredDiscountTests {

        private ShoppingCartSolution.DiscountEngine tieredEngine() {
            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addCartRule(new ShoppingCartSolution.TieredDiscount(Arrays.asList(
                    new ShoppingCartSolution.TieredDiscount.Tier("50.00", "0.05"),
                    new ShoppingCartSolution.TieredDiscount.Tier("100.00", "0.10")
            )));
            return engine;
        }

        @Test
        @DisplayName("Subtotal $130 → 10% tier = $13.00")
        void highestTier() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);
            cart.addItem("B", 2); // $130

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, tieredEngine());
            assertEquals(0, bd("13.00").compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Subtotal $60 → 5% tier = $3.00")
        void middleTier() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 2); // $60

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, tieredEngine());
            assertEquals(0, bd("3.00").compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Subtotal $30 → no tier qualifies")
        void noTierQualifies() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 1); // $30

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, tieredEngine());
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }
    }

    // ──────────────────────────────────────────────
    // 6. Coupon codes
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Coupon Codes")
    class CouponCodeTests {

        @Test
        @DisplayName("Coupon not applied — no discount")
        void couponNotApplied() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3); // $90

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.CouponCodeRule("WELCOME10",
                    new ShoppingCartSolution.PercentageOffSku("A", "0.10")));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Coupon applied — discount activates")
        void couponApplied() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3); // $90

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.CouponCodeRule("WELCOME10",
                    new ShoppingCartSolution.PercentageOffSku("A", "0.10")));
            engine.applyCoupon("WELCOME10");

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("9.00").compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Coupon applied then removed — no discount")
        void couponRemoved() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.CouponCodeRule("WELCOME10",
                    new ShoppingCartSolution.PercentageOffSku("A", "0.10")));
            engine.applyCoupon("WELCOME10");
            engine.removeCoupon("WELCOME10");

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }
    }

    // ──────────────────────────────────────────────
    // 7. Stacking: item-level then cart-level
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Discount Stacking")
    class StackingTests {

        @Test
        @DisplayName("Problem example: A x3 B x2, 10% off A, $15 off >= $100")
        void problemExample() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3); // $90
            cart.addItem("B", 2); // $40 → subtotal $130

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.PercentageOffSku("A", "0.10")); // $9
            engine.addCartRule(new ShoppingCartSolution.FixedAmountOffThreshold("100.00", "15.00")); // $15

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("130.00").compareTo(checkout.subtotal()));
            assertEquals(0, bd("24.00").compareTo(checkout.totalDiscount())); // 9 + 15
            assertEquals(0, bd("106.00").compareTo(checkout.total()));
        }

        @Test
        @DisplayName("Multiple item-level discounts stack")
        void multipleItemDiscounts() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 2); // $60
            cart.addItem("B", 2); // $40

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.PercentageOffSku("A", "0.10")); // $6
            engine.addItemRule(new ShoppingCartSolution.PercentageOffSku("B", "0.20")); // $8

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("14.00").compareTo(checkout.totalDiscount()));
            assertEquals(0, bd("86.00").compareTo(checkout.total()));
        }

        @Test
        @DisplayName("Total cannot go below zero")
        void totalClampedToZero() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("C", 1); // $15

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addCartRule(new ShoppingCartSolution.FixedAmountOffThreshold("0.00", "50.00")); // $50 off

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, bd("0.00").compareTo(checkout.total()));
        }
    }

    // ──────────────────────────────────────────────
    // 8. Edge cases
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty cart — total is zero")
        void emptyCart() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);

            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.subtotal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.total()));
        }

        @Test
        @DisplayName("No discount rules — total equals subtotal")
        void noDiscountRules() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 2);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);

            assertEquals(0, bd("60.00").compareTo(checkout.total()));
        }

        @Test
        @DisplayName("BOGO on SKU not in cart — no discount")
        void bogoOnMissingSku() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 3);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            engine.addItemRule(new ShoppingCartSolution.BuyXGetYFree("B", 2, 1));

            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);
            assertEquals(0, BigDecimal.ZERO.compareTo(checkout.totalDiscount()));
        }

        @Test
        @DisplayName("Single item, quantity 1 — subtotal = unit price")
        void singleItem() {
            ShoppingCartSolution.Cart cart = new ShoppingCartSolution.Cart(catalog);
            cart.addItem("A", 1);

            ShoppingCartSolution.DiscountEngine engine = new ShoppingCartSolution.DiscountEngine();
            ShoppingCartSolution checkout = new ShoppingCartSolution(cart, engine);

            assertEquals(0, bd("30.00").compareTo(checkout.total()));
        }
    }
}

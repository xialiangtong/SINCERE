import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cart Pricing Engine
 * 
 * Computes order total from cart items, discounts/promotions,
 * shipping rules, and taxes.
 */
public class CartPricing {

    // ==================== DATA MODEL ====================

    /**
     * Represents an item in the shopping cart.
     */
    public static class CartItem {
        private final String sku;
        private final BigDecimal unitPrice;
        private final int quantity;

        public CartItem(String sku, BigDecimal unitPrice, int quantity) {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must be >= 0");
            }
            this.sku = sku.trim();
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public CartItem(String sku, String unitPrice, int quantity) {
            this(sku, new BigDecimal(unitPrice), quantity);
        }

        public String getSku() { return sku; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public int getQuantity() { return quantity; }

        public BigDecimal getLineSubtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Shipping address.
     */
    public static class ShippingAddress {
        private final String country;
        private final String state;
        private final String zip;

        public ShippingAddress(String country, String state, String zip) {
            this.country = country;
            this.state = state;
            this.zip = zip;
        }

        public String getCountry() { return country; }
        public String getState() { return state; }
        public String getZip() { return zip; }
    }

    /**
     * Shopping cart containing items and shipping info.
     */
    public static class Cart {
        private final List<CartItem> items;
        private final ShippingAddress address;

        public Cart(List<CartItem> items, ShippingAddress address) {
            this.items = new ArrayList<>(items);
            this.address = address;
        }

        public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
        public ShippingAddress getAddress() { return address; }

        public BigDecimal getSubtotal() {
            return items.stream()
                    .map(CartItem::getLineSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public int getQuantityForSku(String sku) {
            return items.stream()
                    .filter(i -> i.getSku().equals(sku))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
        }
    }

    // ==================== PROMOTION TYPES ====================

    /**
     * Promotion type enum.
     */
    public enum PromotionType {
        PERCENT_OFF_CART,
        FIXED_OFF_CART,
        PERCENT_OFF_SKU,
        BUY_X_GET_Y,
        TIERED_SPEND,
        TIERED_QUANTITY
    }

    /**
     * Applied discount record.
     */
    public static class AppliedDiscount {
        private final PromotionType type;
        private final String sku;  // nullable for cart-wide
        private final String value;
        private final BigDecimal amount;

        public AppliedDiscount(PromotionType type, String sku, String value, BigDecimal amount) {
            this.type = type;
            this.sku = sku;
            this.value = value;
            this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        }

        public PromotionType getType() { return type; }
        public String getSku() { return sku; }
        public String getValue() { return value; }
        public BigDecimal getAmount() { return amount; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"").append(type).append("\"");
            if (sku != null) sb.append(",\"sku\":\"").append(sku).append("\"");
            sb.append(",\"value\":\"").append(value).append("\"");
            sb.append(",\"amount\":\"").append(amount).append("\"}");
            return sb.toString();
        }
    }

    /**
     * Mutable cart state used during discount calculation.
     */
    public static class CartState {
        private final Cart cart;
        private final Map<String, BigDecimal> lineDiscounts;  // sku -> discount amount
        private BigDecimal cartDiscount;
        private final List<AppliedDiscount> appliedDiscounts;

        public CartState(Cart cart) {
            this.cart = cart;
            this.lineDiscounts = new HashMap<>();
            this.cartDiscount = BigDecimal.ZERO;
            this.appliedDiscounts = new ArrayList<>();
        }

        public Cart getCart() { return cart; }
        
        public BigDecimal getLineDiscount(String sku) {
            return lineDiscounts.getOrDefault(sku, BigDecimal.ZERO);
        }

        public void addLineDiscount(String sku, BigDecimal amount) {
            lineDiscounts.merge(sku, amount, BigDecimal::add);
        }

        public BigDecimal getCartDiscount() { return cartDiscount; }

        public void addCartDiscount(BigDecimal amount) {
            cartDiscount = cartDiscount.add(amount);
        }

        public void addAppliedDiscount(AppliedDiscount discount) {
            appliedDiscounts.add(discount);
        }

        public List<AppliedDiscount> getAppliedDiscounts() { return appliedDiscounts; }

        public BigDecimal getTotalDiscount() {
            BigDecimal lineTotal = lineDiscounts.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return lineTotal.add(cartDiscount);
        }

        public BigDecimal getDiscountedSubtotal() {
            return cart.getSubtotal().subtract(getTotalDiscount()).max(BigDecimal.ZERO);
        }
    }

    /**
     * Promotion interface.
     */
    public interface Promotion {
        void apply(CartState state);
        PromotionType getType();
        int getPriority();  // Lower = applied first
    }

    /**
     * Percent off entire cart.
     */
    public static class PercentOffCart implements Promotion {
        private final int percent;

        public PercentOffCart(int percent) {
            this.percent = percent;
        }

        @Override
        public void apply(CartState state) {
            BigDecimal subtotal = state.getDiscountedSubtotal();
            BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            // Ensure cart doesn't go below zero
            discount = discount.min(subtotal);
            
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                state.addCartDiscount(discount);
                state.addAppliedDiscount(new AppliedDiscount(
                        PromotionType.PERCENT_OFF_CART, null, percent + "%", discount));
            }
        }

        @Override
        public PromotionType getType() { return PromotionType.PERCENT_OFF_CART; }

        @Override
        public int getPriority() { return 40; }
    }

    /**
     * Fixed amount off cart.
     */
    public static class FixedOffCart implements Promotion {
        private final BigDecimal amount;

        public FixedOffCart(BigDecimal amount) {
            this.amount = amount;
        }

        public FixedOffCart(String amount) {
            this(new BigDecimal(amount));
        }

        @Override
        public void apply(CartState state) {
            BigDecimal subtotal = state.getDiscountedSubtotal();
            BigDecimal discount = amount.min(subtotal);
            
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                state.addCartDiscount(discount);
                state.addAppliedDiscount(new AppliedDiscount(
                        PromotionType.FIXED_OFF_CART, null, "$" + amount, discount));
            }
        }

        @Override
        public PromotionType getType() { return PromotionType.FIXED_OFF_CART; }

        @Override
        public int getPriority() { return 40; }
    }

    /**
     * Percent off specific SKU.
     */
    public static class PercentOffSku implements Promotion {
        private final String sku;
        private final int percent;

        public PercentOffSku(String sku, int percent) {
            this.sku = sku;
            this.percent = percent;
        }

        @Override
        public void apply(CartState state) {
            for (CartItem item : state.getCart().getItems()) {
                if (item.getSku().equals(sku)) {
                    BigDecimal lineSubtotal = item.getLineSubtotal();
                    BigDecimal existingDiscount = state.getLineDiscount(sku);
                    BigDecimal available = lineSubtotal.subtract(existingDiscount);
                    
                    BigDecimal discount = lineSubtotal.multiply(BigDecimal.valueOf(percent))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    discount = discount.min(available);
                    
                    if (discount.compareTo(BigDecimal.ZERO) > 0) {
                        state.addLineDiscount(sku, discount);
                        state.addAppliedDiscount(new AppliedDiscount(
                                PromotionType.PERCENT_OFF_SKU, sku, percent + "%", discount));
                    }
                }
            }
        }

        @Override
        public PromotionType getType() { return PromotionType.PERCENT_OFF_SKU; }

        @Override
        public int getPriority() { return 10; }
    }

    /**
     * Buy X Get Y promotion.
     * Buy buyQty of buySku, get getQty of getSku free.
     */
    public static class BuyXGetY implements Promotion {
        private final String buySku;
        private final int buyQty;
        private final String getSku;
        private final int getQty;
        private final int maxApplications;  // -1 for unlimited

        public BuyXGetY(String buySku, int buyQty, String getSku, int getQty) {
            this(buySku, buyQty, getSku, getQty, -1);
        }

        public BuyXGetY(String buySku, int buyQty, String getSku, int getQty, int maxApplications) {
            this.buySku = buySku;
            this.buyQty = buyQty;
            this.getSku = getSku;
            this.getQty = getQty;
            this.maxApplications = maxApplications;
        }

        @Override
        public void apply(CartState state) {
            int buyAvailable = state.getCart().getQuantityForSku(buySku);
            int getAvailable = state.getCart().getQuantityForSku(getSku);
            
            // Calculate how many times the promo can apply
            int timesFromBuy = buyAvailable / buyQty;
            int timesFromGet = getAvailable / getQty;
            int times = Math.min(timesFromBuy, timesFromGet);
            
            if (maxApplications > 0) {
                times = Math.min(times, maxApplications);
            }
            
            if (times <= 0) return;

            // Find the getSku item to calculate discount
            CartItem getItem = state.getCart().getItems().stream()
                    .filter(i -> i.getSku().equals(getSku))
                    .findFirst()
                    .orElse(null);
            
            if (getItem == null) return;

            // Discount = unit price of free items
            int freeQty = times * getQty;
            BigDecimal discount = getItem.getUnitPrice().multiply(BigDecimal.valueOf(freeQty));
            
            // Don't exceed line subtotal
            BigDecimal lineSubtotal = getItem.getLineSubtotal();
            BigDecimal existingDiscount = state.getLineDiscount(getSku);
            BigDecimal available = lineSubtotal.subtract(existingDiscount);
            discount = discount.min(available);
            
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                state.addLineDiscount(getSku, discount);
                state.addAppliedDiscount(new AppliedDiscount(
                        PromotionType.BUY_X_GET_Y, getSku,
                        "Buy " + buyQty + " " + buySku + " get " + getQty + " " + getSku + " free",
                        discount));
            }
        }

        @Override
        public PromotionType getType() { return PromotionType.BUY_X_GET_Y; }

        @Override
        public int getPriority() { return 20; }
    }

    /**
     * Tiered spend discount.
     */
    public static class TieredSpendDiscount implements Promotion {
        private final List<Tier> tiers;  // Sorted by threshold descending

        public static class Tier {
            final BigDecimal threshold;
            final int percentOff;

            public Tier(BigDecimal threshold, int percentOff) {
                this.threshold = threshold;
                this.percentOff = percentOff;
            }

            public Tier(String threshold, int percentOff) {
                this(new BigDecimal(threshold), percentOff);
            }
        }

        public TieredSpendDiscount(List<Tier> tiers) {
            this.tiers = tiers.stream()
                    .sorted((a, b) -> b.threshold.compareTo(a.threshold))
                    .collect(Collectors.toList());
        }

        @Override
        public void apply(CartState state) {
            BigDecimal subtotal = state.getCart().getSubtotal();  // Pre-discount subtotal
            
            for (Tier tier : tiers) {
                if (subtotal.compareTo(tier.threshold) >= 0) {
                    BigDecimal discountedSubtotal = state.getDiscountedSubtotal();
                    BigDecimal discount = discountedSubtotal
                            .multiply(BigDecimal.valueOf(tier.percentOff))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    
                    if (discount.compareTo(BigDecimal.ZERO) > 0) {
                        state.addCartDiscount(discount);
                        state.addAppliedDiscount(new AppliedDiscount(
                                PromotionType.TIERED_SPEND, null,
                                "Spend $" + tier.threshold + " get " + tier.percentOff + "% off",
                                discount));
                    }
                    break;  // Apply only the best tier
                }
            }
        }

        @Override
        public PromotionType getType() { return PromotionType.TIERED_SPEND; }

        @Override
        public int getPriority() { return 30; }
    }

    /**
     * Tiered quantity discount for a specific SKU.
     */
    public static class TieredQuantityDiscount implements Promotion {
        private final String sku;
        private final List<Tier> tiers;

        public static class Tier {
            final int minQty;
            final int percentOff;

            public Tier(int minQty, int percentOff) {
                this.minQty = minQty;
                this.percentOff = percentOff;
            }
        }

        public TieredQuantityDiscount(String sku, List<Tier> tiers) {
            this.sku = sku;
            this.tiers = tiers.stream()
                    .sorted((a, b) -> b.minQty - a.minQty)
                    .collect(Collectors.toList());
        }

        @Override
        public void apply(CartState state) {
            int qty = state.getCart().getQuantityForSku(sku);
            
            for (Tier tier : tiers) {
                if (qty >= tier.minQty) {
                    CartItem item = state.getCart().getItems().stream()
                            .filter(i -> i.getSku().equals(sku))
                            .findFirst()
                            .orElse(null);
                    
                    if (item == null) return;

                    BigDecimal lineSubtotal = item.getLineSubtotal();
                    BigDecimal existingDiscount = state.getLineDiscount(sku);
                    BigDecimal available = lineSubtotal.subtract(existingDiscount);
                    
                    BigDecimal discount = lineSubtotal
                            .multiply(BigDecimal.valueOf(tier.percentOff))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    discount = discount.min(available);
                    
                    if (discount.compareTo(BigDecimal.ZERO) > 0) {
                        state.addLineDiscount(sku, discount);
                        state.addAppliedDiscount(new AppliedDiscount(
                                PromotionType.TIERED_QUANTITY, sku,
                                "Buy " + tier.minQty + "+ get " + tier.percentOff + "% off",
                                discount));
                    }
                    break;
                }
            }
        }

        @Override
        public PromotionType getType() { return PromotionType.TIERED_QUANTITY; }

        @Override
        public int getPriority() { return 30; }
    }

    // ==================== CALCULATORS ====================

    /**
     * Shipping calculator.
     */
    public static class ShippingCalculator {
        private static final BigDecimal BASE_SHIPPING = new BigDecimal("7.00");
        private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");

        public BigDecimal calculate(CartState state) {
            // Only US shipping
            if (state.getCart().getAddress() != null && 
                !"US".equalsIgnoreCase(state.getCart().getAddress().getCountry())) {
                throw new IllegalArgumentException("Shipping only available to US");
            }

            // Free shipping if discounted subtotal >= $50
            if (state.getDiscountedSubtotal().compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
                return BigDecimal.ZERO;
            }

            return BASE_SHIPPING;
        }
    }

    /**
     * Tax calculator (stub).
     */
    public static class TaxCalculator {
        public BigDecimal calculate(CartState state) {
            // Placeholder - return 0 for v1
            return BigDecimal.ZERO;
        }
    }

    // ==================== OUTPUT MODEL ====================

    /**
     * Line item in the receipt.
     */
    public static class LineItem {
        private final String sku;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal lineSubtotal;
        private final BigDecimal lineDiscount;
        private final BigDecimal lineTotal;

        public LineItem(String sku, int quantity, BigDecimal unitPrice,
                        BigDecimal lineSubtotal, BigDecimal lineDiscount) {
            this.sku = sku;
            this.quantity = quantity;
            this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
            this.lineSubtotal = lineSubtotal.setScale(2, RoundingMode.HALF_UP);
            this.lineDiscount = lineDiscount.setScale(2, RoundingMode.HALF_UP);
            this.lineTotal = lineSubtotal.subtract(lineDiscount)
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getLineSubtotal() { return lineSubtotal; }
        public BigDecimal getLineDiscount() { return lineDiscount; }
        public BigDecimal getLineTotal() { return lineTotal; }
    }

    /**
     * Receipt - final pricing breakdown.
     */
    public static class Receipt {
        private final BigDecimal subtotal;
        private final BigDecimal discountTotal;
        private final BigDecimal shipping;
        private final BigDecimal tax;
        private final BigDecimal total;
        private final List<LineItem> lineItems;
        private final List<AppliedDiscount> appliedDiscounts;

        public Receipt(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal shipping,
                       BigDecimal tax, List<LineItem> lineItems, List<AppliedDiscount> appliedDiscounts) {
            this.subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
            this.discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP);
            this.shipping = shipping.setScale(2, RoundingMode.HALF_UP);
            this.tax = tax.setScale(2, RoundingMode.HALF_UP);
            this.total = subtotal.subtract(discountTotal).add(shipping).add(tax)
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            this.lineItems = lineItems;
            this.appliedDiscounts = appliedDiscounts;
        }

        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscountTotal() { return discountTotal; }
        public BigDecimal getShipping() { return shipping; }
        public BigDecimal getTax() { return tax; }
        public BigDecimal getTotal() { return total; }
        public List<LineItem> getLineItems() { return lineItems; }
        public List<AppliedDiscount> getAppliedDiscounts() { return appliedDiscounts; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"subtotal\": \"").append(subtotal).append("\",\n");
            sb.append("  \"discount_total\": \"").append(discountTotal).append("\",\n");
            sb.append("  \"shipping\": \"").append(shipping).append("\",\n");
            sb.append("  \"tax\": \"").append(tax).append("\",\n");
            sb.append("  \"total\": \"").append(total).append("\",\n");
            
            sb.append("  \"line_items\": [\n");
            for (int i = 0; i < lineItems.size(); i++) {
                LineItem li = lineItems.get(i);
                sb.append("    {\"sku\": \"").append(li.getSku())
                  .append("\", \"quantity\": ").append(li.getQuantity())
                  .append(", \"unit_price\": \"").append(li.getUnitPrice())
                  .append("\", \"line_subtotal\": \"").append(li.getLineSubtotal())
                  .append("\", \"line_discount\": \"").append(li.getLineDiscount())
                  .append("\", \"line_total\": \"").append(li.getLineTotal())
                  .append("\"}");
                if (i < lineItems.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ],\n");
            
            sb.append("  \"applied_discounts\": [\n");
            for (int i = 0; i < appliedDiscounts.size(); i++) {
                sb.append("    ").append(appliedDiscounts.get(i));
                if (i < appliedDiscounts.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n");
            
            sb.append("}");
            return sb.toString();
        }
    }

    // ==================== PRICING ENGINE ====================

    /**
     * Main pricing engine.
     */
    public static class PricingEngine {
        private final ShippingCalculator shippingCalculator = new ShippingCalculator();
        private final TaxCalculator taxCalculator = new TaxCalculator();

        public Receipt calculate(Cart cart, List<Promotion> promotions) {
            // Create mutable state
            CartState state = new CartState(cart);

            // Sort promotions by priority and apply
            promotions.stream()
                    .sorted(Comparator.comparingInt(Promotion::getPriority))
                    .forEach(p -> p.apply(state));

            // Calculate shipping
            BigDecimal shipping = shippingCalculator.calculate(state);

            // Calculate tax
            BigDecimal tax = taxCalculator.calculate(state);

            // Build line items
            List<LineItem> lineItems = new ArrayList<>();
            for (CartItem item : cart.getItems()) {
                BigDecimal lineDiscount = state.getLineDiscount(item.getSku());
                lineItems.add(new LineItem(
                        item.getSku(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineSubtotal(),
                        lineDiscount
                ));
            }

            return new Receipt(
                    cart.getSubtotal(),
                    state.getTotalDiscount(),
                    shipping,
                    tax,
                    lineItems,
                    state.getAppliedDiscounts()
            );
        }
    }

    // ==================== DEMO ====================

    public static void main(String[] args) {
        PricingEngine engine = new PricingEngine();

        System.out.println("=== Example 1: Basic Cart with SKU Discount ===\n");
        {
            Cart cart = new Cart(
                    Arrays.asList(
                            new CartItem("A", "19.99", 2),
                            new CartItem("B", "5.00", 1)
                    ),
                    new ShippingAddress("US", "WA", "98056")
            );

            List<Promotion> promotions = Arrays.asList(
                    new PercentOffSku("A", 20)
            );

            Receipt receipt = engine.calculate(cart, promotions);
            System.out.println(receipt.toJson());
        }

        System.out.println("\n=== Example 2: Free Shipping Threshold ===\n");
        {
            Cart cart = new Cart(
                    Arrays.asList(
                            new CartItem("A", "30.00", 2)
                    ),
                    new ShippingAddress("US", "CA", "90210")
            );

            Receipt receipt = engine.calculate(cart, Collections.emptyList());
            System.out.println(receipt.toJson());
            System.out.println("Shipping is FREE because subtotal >= $50");
        }

        System.out.println("\n=== Example 3: Buy X Get Y ===\n");
        {
            Cart cart = new Cart(
                    Arrays.asList(
                            new CartItem("A", "20.00", 2),
                            new CartItem("B", "5.00", 2)
                    ),
                    new ShippingAddress("US", "NY", "10001")
            );

            List<Promotion> promotions = Arrays.asList(
                    new BuyXGetY("A", 2, "B", 1)  // Buy 2 A, get 1 B free
            );

            Receipt receipt = engine.calculate(cart, promotions);
            System.out.println(receipt.toJson());
        }

        System.out.println("\n=== Example 4: Tiered Spend Discount ===\n");
        {
            Cart cart = new Cart(
                    Arrays.asList(
                            new CartItem("A", "40.00", 3)  // $120 subtotal
                    ),
                    new ShippingAddress("US", "TX", "75001")
            );

            List<Promotion> promotions = Arrays.asList(
                    new TieredSpendDiscount(Arrays.asList(
                            new TieredSpendDiscount.Tier("50", 10),
                            new TieredSpendDiscount.Tier("100", 15),
                            new TieredSpendDiscount.Tier("200", 20)
                    ))
            );

            Receipt receipt = engine.calculate(cart, promotions);
            System.out.println(receipt.toJson());
        }

        System.out.println("\n=== Example 5: Multiple Promotions Stacked ===\n");
        {
            Cart cart = new Cart(
                    Arrays.asList(
                            new CartItem("A", "25.00", 2),
                            new CartItem("B", "10.00", 3)
                    ),
                    new ShippingAddress("US", "WA", "98056")
            );

            List<Promotion> promotions = Arrays.asList(
                    new PercentOffSku("A", 10),
                    new FixedOffCart("5.00")
            );

            Receipt receipt = engine.calculate(cart, promotions);
            System.out.println(receipt.toJson());
        }
    }
}

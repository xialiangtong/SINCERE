import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class ShoppingCartSolution {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ──────────────────────────────────────────────
    // 1. Product
    // ──────────────────────────────────────────────
    static class Product {
        private final String code;
        private final String name;
        private final BigDecimal unitPrice;

        Product(String code, String name, BigDecimal unitPrice) {
            this.code = code;
            this.name = name;
            this.unitPrice = unitPrice.setScale(SCALE, ROUNDING);
        }

        Product(String code, String name, String unitPrice) {
            this(code, name, new BigDecimal(unitPrice));
        }

        String getCode() { return code; }
        String getName() { return name; }
        BigDecimal getUnitPrice() { return unitPrice; }

        @Override
        public String toString() {
            return "Product{" + code + ", $" + unitPrice + "}";
        }
    }

    // ──────────────────────────────────────────────
    // 2. LineItem
    // ──────────────────────────────────────────────
    static class LineItem {
        private final Product product;
        private int quantity;

        LineItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        Product getProduct() { return product; }
        int getQuantity() { return quantity; }

        void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        BigDecimal getLineTotal() {
            return product.getUnitPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(SCALE, ROUNDING);
        }

        @Override
        public String toString() {
            return product.getCode() + " x " + quantity + " = $" + getLineTotal();
        }
    }

    // ──────────────────────────────────────────────
    // 3. ProductCatalog
    // ──────────────────────────────────────────────
    static class ProductCatalog {
        private final Map<String, Product> products = new LinkedHashMap<>();

        void addProduct(Product product) {
            products.put(product.getCode(), product);
        }

        Product getProduct(String code) {
            return products.get(code);
        }

        Collection<Product> getAllProducts() {
            return Collections.unmodifiableCollection(products.values());
        }
    }

    // ──────────────────────────────────────────────
    // 4. Cart
    // ──────────────────────────────────────────────
    static class Cart {
        private final Map<String, LineItem> items = new LinkedHashMap<>();
        private final ProductCatalog catalog;

        Cart(ProductCatalog catalog) {
            this.catalog = catalog;
        }

        void addItem(String code, int quantity) {
            if (quantity <= 0) return;
            Product product = catalog.getProduct(code);
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + code);
            }
            LineItem existing = items.get(code);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
            } else {
                items.put(code, new LineItem(product, quantity));
            }
        }

        void removeItem(String code, int quantity) {
            if (quantity <= 0) return;
            LineItem existing = items.get(code);
            if (existing == null) return;
            int newQty = existing.getQuantity() - quantity;
            if (newQty <= 0) {
                items.remove(code);
            } else {
                existing.setQuantity(newQty);
            }
        }

        LineItem getLineItem(String code) {
            return items.get(code);
        }

        Collection<LineItem> getItems() {
            return Collections.unmodifiableCollection(items.values());
        }

        BigDecimal subtotal() {
            BigDecimal total = BigDecimal.ZERO;
            for (LineItem item : items.values()) {
                total = total.add(item.getLineTotal());
            }
            return total.setScale(SCALE, ROUNDING);
        }

        boolean isEmpty() {
            return items.isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // 5. DiscountRule interface
    // ──────────────────────────────────────────────
    interface DiscountRule {
        BigDecimal apply(Cart cart);
    }

    // ──────────────────────────────────────────────
    // 6. Discount implementations
    // ──────────────────────────────────────────────

    /**
     * Percentage off a specific SKU.
     * E.g., 10% off "A" → percent = "0.10"
     */
    static class PercentageOffSku implements DiscountRule {
        private final String sku;
        private final BigDecimal percent;

        PercentageOffSku(String sku, BigDecimal percent) {
            this.sku = sku;
            this.percent = percent;
        }

        PercentageOffSku(String sku, String percent) {
            this(sku, new BigDecimal(percent));
        }

        @Override
        public BigDecimal apply(Cart cart) {
            LineItem item = cart.getLineItem(sku);
            if (item == null) return BigDecimal.ZERO;
            return item.getLineTotal()
                    .multiply(percent)
                    .setScale(SCALE, ROUNDING);
        }
    }

    /**
     * Fixed amount off when cart subtotal >= threshold.
     * E.g., $15 off orders >= $100
     */
    static class FixedAmountOffThreshold implements DiscountRule {
        private final BigDecimal threshold;
        private final BigDecimal discount;

        FixedAmountOffThreshold(BigDecimal threshold, BigDecimal discount) {
            this.threshold = threshold;
            this.discount = discount;
        }

        FixedAmountOffThreshold(String threshold, String discount) {
            this(new BigDecimal(threshold), new BigDecimal(discount));
        }

        @Override
        public BigDecimal apply(Cart cart) {
            if (cart.subtotal().compareTo(threshold) >= 0) {
                return discount.setScale(SCALE, ROUNDING);
            }
            return BigDecimal.ZERO;
        }
    }

    /**
     * Buy X get Y free on a specific SKU.
     * E.g., buy 2 get 1 free: buyQty=2, freeQty=1
     * For every (buyQty + freeQty) items, freeQty items are free.
     */
    static class BuyXGetYFree implements DiscountRule {
        private final String sku;
        private final int buyQty;
        private final int freeQty;

        BuyXGetYFree(String sku, int buyQty, int freeQty) {
            this.sku = sku;
            this.buyQty = buyQty;
            this.freeQty = freeQty;
        }

        @Override
        public BigDecimal apply(Cart cart) {
            LineItem item = cart.getLineItem(sku);
            if (item == null) return BigDecimal.ZERO;
            int qty = item.getQuantity();
            int groupSize = buyQty + freeQty;
            int freeItems = (qty / groupSize) * freeQty;
            return item.getProduct().getUnitPrice()
                    .multiply(BigDecimal.valueOf(freeItems))
                    .setScale(SCALE, ROUNDING);
        }
    }

    /**
     * Tiered discount based on cart subtotal.
     * E.g., 5% off >= $50, 10% off >= $100.
     * Highest qualifying tier applies (not stacked).
     */
    static class TieredDiscount implements DiscountRule {
        static class Tier {
            final BigDecimal threshold;
            final BigDecimal percent;

            Tier(String threshold, String percent) {
                this.threshold = new BigDecimal(threshold);
                this.percent = new BigDecimal(percent);
            }
        }

        private final List<Tier> tiers;

        TieredDiscount(List<Tier> tiers) {
            this.tiers = new ArrayList<>(tiers);
            // Sort descending by threshold so we find highest qualifying first
            this.tiers.sort((a, b) -> b.threshold.compareTo(a.threshold));
        }

        @Override
        public BigDecimal apply(Cart cart) {
            BigDecimal subtotal = cart.subtotal();
            for (Tier tier : tiers) {
                if (subtotal.compareTo(tier.threshold) >= 0) {
                    return subtotal.multiply(tier.percent).setScale(SCALE, ROUNDING);
                }
            }
            return BigDecimal.ZERO;
        }
    }

    /**
     * Coupon code wrapper — only applies when the coupon is active.
     */
    static class CouponCodeRule implements DiscountRule {
        private final String code;
        private final DiscountRule innerRule;

        CouponCodeRule(String code, DiscountRule innerRule) {
            this.code = code;
            this.innerRule = innerRule;
        }

        String getCode() { return code; }

        @Override
        public BigDecimal apply(Cart cart) {
            return innerRule.apply(cart);
        }
    }

    // ──────────────────────────────────────────────
    // 7. DiscountEngine
    // ──────────────────────────────────────────────
    static class DiscountEngine {
        private final List<DiscountRule> itemRules = new ArrayList<>();
        private final List<DiscountRule> cartRules = new ArrayList<>();
        private final Set<String> activeCoupons = new HashSet<>();

        void addItemRule(DiscountRule rule) {
            itemRules.add(rule);
        }

        void addCartRule(DiscountRule rule) {
            cartRules.add(rule);
        }

        void applyCoupon(String code) {
            activeCoupons.add(code);
        }

        void removeCoupon(String code) {
            activeCoupons.remove(code);
        }

        private boolean isActive(DiscountRule rule) {
            if (rule instanceof CouponCodeRule) {
                return activeCoupons.contains(((CouponCodeRule) rule).getCode());
            }
            return true; // non-coupon rules are always active
        }

        BigDecimal totalItemDiscount(Cart cart) {
            BigDecimal total = BigDecimal.ZERO;
            for (DiscountRule rule : itemRules) {
                if (isActive(rule)) {
                    total = total.add(rule.apply(cart));
                }
            }
            return total.setScale(SCALE, ROUNDING);
        }

        BigDecimal totalCartDiscount(Cart cart) {
            BigDecimal total = BigDecimal.ZERO;
            for (DiscountRule rule : cartRules) {
                if (isActive(rule)) {
                    total = total.add(rule.apply(cart));
                }
            }
            return total.setScale(SCALE, ROUNDING);
        }

        BigDecimal totalDiscount(Cart cart) {
            return totalItemDiscount(cart).add(totalCartDiscount(cart)).setScale(SCALE, ROUNDING);
        }
    }

    // ──────────────────────────────────────────────
    // 8. Checkout (top-level orchestrator)
    // ──────────────────────────────────────────────
    private final Cart cart;
    private final DiscountEngine discountEngine;

    public ShoppingCartSolution(Cart cart, DiscountEngine discountEngine) {
        this.cart = cart;
        this.discountEngine = discountEngine;
    }

    public BigDecimal subtotal() {
        return cart.subtotal();
    }

    public BigDecimal totalDiscount() {
        return discountEngine.totalDiscount(cart);
    }

    public BigDecimal total() {
        return subtotal().subtract(totalDiscount()).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);
    }
}

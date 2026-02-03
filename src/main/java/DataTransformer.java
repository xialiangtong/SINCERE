import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Transformation Pipeline
 * 
 * Ingests orders from CSV or JSON, normalizes to canonical model,
 * aggregates metrics, and outputs results.
 */
public class DataTransformer {

    // ==================== CANONICAL MODEL ====================

    /**
     * Represents a line item in an order.
     */
    public static class Item {
        private final String sku;
        private final int quantity;
        private final BigDecimal unitPrice;

        public Item(String sku, int quantity, BigDecimal unitPrice) {
            this.sku = sku.trim();
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }

        public BigDecimal getLineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        @Override
        public String toString() {
            return "Item{sku='" + sku + "', qty=" + quantity + ", price=" + unitPrice + "}";
        }
    }

    /**
     * Represents order charges.
     */
    public static class Charges {
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal tax;
        private final BigDecimal shipping;

        public Charges(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal shipping) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.tax = tax;
            this.shipping = shipping;
        }

        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getTax() { return tax; }
        public BigDecimal getShipping() { return shipping; }

        public BigDecimal getTotal() {
            return subtotal.subtract(discount).add(tax).add(shipping);
        }
    }

    /**
     * Canonical order representation.
     */
    public static class Order {
        private final String orderId;
        private final Instant createdAt;
        private final String customerId;
        private final String currency;
        private final List<Item> items;
        private final Charges charges;

        public Order(String orderId, Instant createdAt, String customerId, 
                     String currency, List<Item> items, Charges charges) {
            this.orderId = orderId.trim();
            this.createdAt = createdAt;
            this.customerId = customerId.trim();
            this.currency = currency.trim().toUpperCase();
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.charges = charges;
        }

        public String getOrderId() { return orderId; }
        public Instant getCreatedAt() { return createdAt; }
        public String getCustomerId() { return customerId; }
        public String getCurrency() { return currency; }
        public List<Item> getItems() { return items; }
        public Charges getCharges() { return charges; }

        public BigDecimal getTotal() {
            return charges.getTotal();
        }

        public LocalDate getOrderDate() {
            return createdAt.atZone(ZoneOffset.UTC).toLocalDate();
        }

        public BigDecimal getItemsTotal() {
            return items.stream()
                    .map(Item::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Represents a parsing/validation error.
     */
    public static class ParseError {
        private final String source;
        private final int row;
        private final String message;

        public ParseError(String source, int row, String message) {
            this.source = source;
            this.row = row;
            this.message = message;
        }

        public String getSource() { return source; }
        public int getRow() { return row; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "{\"source\":\"" + source + "\",\"row\":" + row + 
                   ",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * Result of parsing containing valid orders and errors.
     */
    public static class ParseResult {
        private final List<Order> orders;
        private final List<ParseError> errors;

        public ParseResult(List<Order> orders, List<ParseError> errors) {
            this.orders = new ArrayList<>(orders);
            this.errors = new ArrayList<>(errors);
        }

        public List<Order> getOrders() { return orders; }
        public List<ParseError> getErrors() { return errors; }

        public void addOrder(Order order) { orders.add(order); }
        public void addError(ParseError error) { errors.add(error); }

        public void merge(ParseResult other) {
            orders.addAll(other.orders);
            errors.addAll(other.errors);
        }
    }

    // ==================== PARSERS ====================

    /**
     * Parser interface for different input formats.
     */
    public interface OrderParser {
        ParseResult parse(String input);
    }

    /**
     * CSV Order Parser.
     */
    public static class CsvOrderParser implements OrderParser {
        private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

        @Override
        public ParseResult parse(String input) {
            ParseResult result = new ParseResult(new ArrayList<>(), new ArrayList<>());
            String[] lines = input.split("\n");
            
            if (lines.length < 2) {
                return result; // Empty or header only
            }

            // Skip header row
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                try {
                    Order order = parseCsvLine(line, i + 1);
                    
                    // Validate subtotal
                    String validationError = validateOrder(order);
                    if (validationError != null) {
                        result.addError(new ParseError("csv", i + 1, validationError));
                    } else {
                        result.addOrder(order);
                    }
                } catch (Exception e) {
                    result.addError(new ParseError("csv", i + 1, e.getMessage()));
                }
            }

            return result;
        }

        private Order parseCsvLine(String line, int rowNum) {
            // Handle quoted fields with commas
            List<String> fields = parseCsvFields(line);
            
            if (fields.size() < 9) {
                throw new IllegalArgumentException("Expected 9 fields, got " + fields.size());
            }

            String orderId = fields.get(0);
            Instant createdAt = Instant.parse(fields.get(1));
            String customerId = fields.get(2);
            String currency = fields.get(3);
            String itemsStr = fields.get(4);
            BigDecimal subtotal = new BigDecimal(fields.get(5));
            BigDecimal discount = new BigDecimal(fields.get(6));
            BigDecimal tax = new BigDecimal(fields.get(7));
            BigDecimal shipping = new BigDecimal(fields.get(8));

            List<Item> items = parseItemsString(itemsStr);
            Charges charges = new Charges(subtotal, discount, tax, shipping);

            return new Order(orderId, createdAt, customerId, currency, items, charges);
        }

        private List<String> parseCsvFields(String line) {
            List<String> fields = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;

            for (char c : line.toCharArray()) {
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    fields.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            fields.add(current.toString().trim());

            return fields;
        }

        private List<Item> parseItemsString(String itemsStr) {
            List<Item> items = new ArrayList<>();
            String[] itemParts = itemsStr.split("\\|");

            for (String itemPart : itemParts) {
                Map<String, String> props = new HashMap<>();
                for (String prop : itemPart.split(";")) {
                    String[] kv = prop.split("=");
                    if (kv.length == 2) {
                        props.put(kv[0].trim(), kv[1].trim());
                    }
                }

                String sku = props.get("sku");
                int qty = Integer.parseInt(props.get("qty"));
                BigDecimal price = new BigDecimal(props.get("price"));

                items.add(new Item(sku, qty, price));
            }

            return items;
        }

        private String validateOrder(Order order) {
            // Check quantity > 0
            for (Item item : order.getItems()) {
                if (item.getQuantity() <= 0) {
                    return "qty must be > 0 for sku " + item.getSku();
                }
                if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    return "unit_price must be >= 0 for sku " + item.getSku();
                }
            }

            // Check subtotal matches items total (within tolerance)
            BigDecimal itemsTotal = order.getItemsTotal();
            BigDecimal subtotal = order.getCharges().getSubtotal();
            if (itemsTotal.subtract(subtotal).abs().compareTo(TOLERANCE) > 0) {
                return "subtotal mismatch: expected " + itemsTotal + ", got " + subtotal;
            }

            // Check currency present
            if (order.getCurrency().isEmpty()) {
                return "currency is required";
            }

            return null; // Valid
        }
    }

    /**
     * JSON Order Parser.
     */
    public static class JsonOrderParser implements OrderParser {
        private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

        @Override
        public ParseResult parse(String input) {
            ParseResult result = new ParseResult(new ArrayList<>(), new ArrayList<>());
            
            try {
                // Simple JSON array parsing (no external dependencies)
                List<Map<String, Object>> jsonOrders = parseJsonArray(input);
                
                int row = 1;
                for (Map<String, Object> jsonOrder : jsonOrders) {
                    try {
                        Order order = parseJsonOrder(jsonOrder);
                        
                        String validationError = validateOrder(order);
                        if (validationError != null) {
                            result.addError(new ParseError("json", row, validationError));
                        } else {
                            result.addOrder(order);
                        }
                    } catch (Exception e) {
                        result.addError(new ParseError("json", row, e.getMessage()));
                    }
                    row++;
                }
            } catch (Exception e) {
                result.addError(new ParseError("json", 0, "Failed to parse JSON: " + e.getMessage()));
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        private Order parseJsonOrder(Map<String, Object> json) {
            String orderId = getString(json, "id");
            Instant createdAt = Instant.parse(getString(json, "createdAt"));
            
            Map<String, Object> customer = (Map<String, Object>) json.get("customer");
            String customerId = getString(customer, "id");
            
            String currency = getString(json, "currency");
            
            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) json.get("lineItems");
            List<Item> items = new ArrayList<>();
            for (Map<String, Object> li : lineItems) {
                String sku = getString(li, "sku");
                int qty = getInt(li, "quantity");
                BigDecimal price = getBigDecimal(li, "unitPrice");
                items.add(new Item(sku, qty, price));
            }
            
            Map<String, Object> chargesJson = (Map<String, Object>) json.get("charges");
            Charges charges = new Charges(
                getBigDecimal(chargesJson, "subtotal"),
                getBigDecimal(chargesJson, "discount"),
                getBigDecimal(chargesJson, "tax"),
                getBigDecimal(chargesJson, "shipping")
            );

            return new Order(orderId, createdAt, customerId, currency, items, charges);
        }

        private String getString(Map<String, Object> map, String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : "";
        }

        private int getInt(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            return Integer.parseInt(val.toString());
        }

        private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return BigDecimal.valueOf(((Number) val).doubleValue());
            }
            return new BigDecimal(val.toString());
        }

        // Simple JSON parser (for demonstration - in production use Jackson/Gson)
        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> parseJsonArray(String json) {
            json = json.trim();
            if (!json.startsWith("[")) {
                throw new IllegalArgumentException("Expected JSON array");
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            int depth = 0;
            int start = -1;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') {
                    if (depth == 1) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 1 && start >= 0) {
                        String objStr = json.substring(start, i + 1);
                        result.add(parseJsonObject(objStr));
                        start = -1;
                    }
                } else if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                }
            }
            
            return result;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parseJsonObject(String json) {
            Map<String, Object> map = new LinkedHashMap<>();
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new IllegalArgumentException("Invalid JSON object");
            }
            
            json = json.substring(1, json.length() - 1).trim();
            
            int i = 0;
            while (i < json.length()) {
                // Skip whitespace
                while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
                if (i >= json.length()) break;
                
                // Parse key
                if (json.charAt(i) != '"') {
                    i++;
                    continue;
                }
                i++;
                int keyStart = i;
                while (i < json.length() && json.charAt(i) != '"') i++;
                String key = json.substring(keyStart, i);
                i++; // skip closing quote
                
                // Skip colon
                while (i < json.length() && json.charAt(i) != ':') i++;
                i++;
                
                // Skip whitespace
                while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
                
                // Parse value
                Object value;
                if (json.charAt(i) == '"') {
                    i++;
                    int valStart = i;
                    while (i < json.length() && json.charAt(i) != '"') i++;
                    value = json.substring(valStart, i);
                    i++;
                } else if (json.charAt(i) == '{') {
                    int depth = 1;
                    int objStart = i;
                    i++;
                    while (i < json.length() && depth > 0) {
                        if (json.charAt(i) == '{') depth++;
                        else if (json.charAt(i) == '}') depth--;
                        i++;
                    }
                    value = parseJsonObject(json.substring(objStart, i));
                } else if (json.charAt(i) == '[') {
                    int depth = 1;
                    int arrStart = i;
                    i++;
                    while (i < json.length() && depth > 0) {
                        if (json.charAt(i) == '[' || json.charAt(i) == '{') depth++;
                        else if (json.charAt(i) == ']' || json.charAt(i) == '}') depth--;
                        i++;
                    }
                    String arrStr = json.substring(arrStart, i);
                    value = parseJsonArrayOfObjects(arrStr);
                } else {
                    int valStart = i;
                    while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                    String valStr = json.substring(valStart, i).trim();
                    if (valStr.contains(".")) {
                        value = Double.parseDouble(valStr);
                    } else {
                        try {
                            value = Long.parseLong(valStr);
                        } catch (NumberFormatException e) {
                            value = valStr;
                        }
                    }
                }
                
                map.put(key, value);
                
                // Skip comma
                while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
            }
            
            return map;
        }

        private List<Map<String, Object>> parseJsonArrayOfObjects(String json) {
            List<Map<String, Object>> result = new ArrayList<>();
            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                return result;
            }
            
            json = json.substring(1, json.length() - 1).trim();
            int i = 0;
            while (i < json.length()) {
                while (i < json.length() && json.charAt(i) != '{') i++;
                if (i >= json.length()) break;
                
                int depth = 1;
                int objStart = i;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '{') depth++;
                    else if (json.charAt(i) == '}') depth--;
                    i++;
                }
                result.add(parseJsonObject(json.substring(objStart, i)));
            }
            
            return result;
        }

        private String validateOrder(Order order) {
            for (Item item : order.getItems()) {
                if (item.getQuantity() <= 0) {
                    return "qty must be > 0 for sku " + item.getSku();
                }
                if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    return "unit_price must be >= 0 for sku " + item.getSku();
                }
            }

            BigDecimal itemsTotal = order.getItemsTotal();
            BigDecimal subtotal = order.getCharges().getSubtotal();
            if (itemsTotal.subtract(subtotal).abs().compareTo(TOLERANCE) > 0) {
                return "subtotal mismatch: expected " + itemsTotal + ", got " + subtotal;
            }

            if (order.getCurrency().isEmpty()) {
                return "currency is required";
            }

            return null;
        }
    }

    // ==================== AGGREGATION ====================

    /**
     * Product statistics for aggregation.
     */
    public static class ProductStats {
        private final String sku;
        private BigDecimal grossSales;
        private int unitsSold;

        public ProductStats(String sku) {
            this.sku = sku;
            this.grossSales = BigDecimal.ZERO;
            this.unitsSold = 0;
        }

        public void add(int quantity, BigDecimal lineTotal) {
            this.unitsSold += quantity;
            this.grossSales = this.grossSales.add(lineTotal);
        }

        public String getSku() { return sku; }
        public BigDecimal getGrossSales() { return grossSales; }
        public int getUnitsSold() { return unitsSold; }
    }

    /**
     * Aggregation report.
     */
    public static class Report {
        private final Map<String, BigDecimal> revenueByCountry;
        private final List<ProductStats> topProducts;
        private final int returningCustomers;
        private final Map<String, Integer> ordersByDate;
        private final List<ParseError> errors;

        public Report(Map<String, BigDecimal> revenueByCountry,
                      List<ProductStats> topProducts,
                      int returningCustomers,
                      Map<String, Integer> ordersByDate,
                      List<ParseError> errors) {
            this.revenueByCountry = revenueByCountry;
            this.topProducts = topProducts;
            this.returningCustomers = returningCustomers;
            this.ordersByDate = ordersByDate;
            this.errors = errors;
        }

        public Map<String, BigDecimal> getRevenueByCountry() { return revenueByCountry; }
        public List<ProductStats> getTopProducts() { return topProducts; }
        public int getReturningCustomers() { return returningCustomers; }
        public Map<String, Integer> getOrdersByDate() { return ordersByDate; }
        public List<ParseError> getErrors() { return errors; }
    }

    /**
     * Order Aggregator - computes metrics from orders.
     */
    public static class OrderAggregator {

        public Report aggregate(List<Order> orders, List<ParseError> errors, int topN) {
            Map<String, BigDecimal> revenueByCountry = computeRevenueByCountry(orders);
            List<ProductStats> topProducts = computeTopProducts(orders, topN);
            int returningCustomers = computeReturningCustomers(orders);
            Map<String, Integer> ordersByDate = computeOrdersByDate(orders);

            return new Report(revenueByCountry, topProducts, returningCustomers, ordersByDate, errors);
        }

        private Map<String, BigDecimal> computeRevenueByCountry(List<Order> orders) {
            Map<String, BigDecimal> result = new TreeMap<>();
            
            for (Order order : orders) {
                String currency = order.getCurrency();
                BigDecimal total = order.getTotal();
                result.merge(currency, total, BigDecimal::add);
            }
            
            return result;
        }

        private List<ProductStats> computeTopProducts(List<Order> orders, int topN) {
            Map<String, ProductStats> statsMap = new HashMap<>();
            
            for (Order order : orders) {
                for (Item item : order.getItems()) {
                    ProductStats stats = statsMap.computeIfAbsent(
                        item.getSku(), ProductStats::new);
                    stats.add(item.getQuantity(), item.getLineTotal());
                }
            }
            
            return statsMap.values().stream()
                    .sorted((a, b) -> {
                        int cmp = b.getGrossSales().compareTo(a.getGrossSales());
                        return cmp != 0 ? cmp : a.getSku().compareTo(b.getSku());
                    })
                    .limit(topN)
                    .collect(Collectors.toList());
        }

        private int computeReturningCustomers(List<Order> orders) {
            Map<String, Integer> customerOrderCount = new HashMap<>();
            
            for (Order order : orders) {
                customerOrderCount.merge(order.getCustomerId(), 1, Integer::sum);
            }
            
            return (int) customerOrderCount.values().stream()
                    .filter(count -> count >= 2)
                    .count();
        }

        private Map<String, Integer> computeOrdersByDate(List<Order> orders) {
            Map<String, Integer> result = new TreeMap<>();
            
            for (Order order : orders) {
                String date = order.getOrderDate().toString();
                result.merge(date, 1, Integer::sum);
            }
            
            return result;
        }
    }

    // ==================== OUTPUT ====================

    /**
     * JSON Report Writer.
     */
    public static class JsonReportWriter {

        public String write(Report report) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            
            // Revenue by currency
            sb.append("  \"total_revenue_by_currency\": {");
            boolean first = true;
            for (Map.Entry<String, BigDecimal> entry : report.getRevenueByCountry().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\n    \"").append(entry.getKey()).append("\": \"")
                  .append(entry.getValue().setScale(2, RoundingMode.HALF_UP)).append("\"");
                first = false;
            }
            sb.append("\n  },\n");
            
            // Top products
            sb.append("  \"top_products\": [");
            first = true;
            for (ProductStats ps : report.getTopProducts()) {
                if (!first) sb.append(",");
                sb.append("\n    {\"sku\": \"").append(ps.getSku())
                  .append("\", \"gross_sales\": \"")
                  .append(ps.getGrossSales().setScale(2, RoundingMode.HALF_UP))
                  .append("\", \"units_sold\": ").append(ps.getUnitsSold()).append("}");
                first = false;
            }
            sb.append("\n  ],\n");
            
            // Returning customers
            sb.append("  \"returning_customers\": ").append(report.getReturningCustomers()).append(",\n");
            
            // Orders by date
            sb.append("  \"orders_by_date\": {");
            first = true;
            for (Map.Entry<String, Integer> entry : report.getOrdersByDate().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\n    \"").append(entry.getKey()).append("\": ")
                  .append(entry.getValue());
                first = false;
            }
            sb.append("\n  },\n");
            
            // Errors
            sb.append("  \"errors\": [");
            first = true;
            for (ParseError error : report.getErrors()) {
                if (!first) sb.append(",");
                sb.append("\n    ").append(error.toString());
                first = false;
            }
            sb.append("\n  ]\n");
            
            sb.append("}");
            return sb.toString();
        }
    }

    // ==================== MAIN ORCHESTRATOR ====================

    private final CsvOrderParser csvParser = new CsvOrderParser();
    private final JsonOrderParser jsonParser = new JsonOrderParser();
    private final OrderAggregator aggregator = new OrderAggregator();
    private final JsonReportWriter reportWriter = new JsonReportWriter();

    /**
     * Process CSV input and generate report.
     */
    public String processCsv(String csvInput, int topN) {
        ParseResult result = csvParser.parse(csvInput);
        Report report = aggregator.aggregate(result.getOrders(), result.getErrors(), topN);
        return reportWriter.write(report);
    }

    /**
     * Process JSON input and generate report.
     */
    public String processJson(String jsonInput, int topN) {
        ParseResult result = jsonParser.parse(jsonInput);
        Report report = aggregator.aggregate(result.getOrders(), result.getErrors(), topN);
        return reportWriter.write(report);
    }

    /**
     * Process both CSV and JSON inputs and generate combined report.
     */
    public String processAll(String csvInput, String jsonInput, int topN) {
        ParseResult csvResult = csvParser.parse(csvInput);
        ParseResult jsonResult = jsonParser.parse(jsonInput);
        csvResult.merge(jsonResult);
        
        Report report = aggregator.aggregate(csvResult.getOrders(), csvResult.getErrors(), topN);
        return reportWriter.write(report);
    }

    // ==================== DEMO ====================

    public static void main(String[] args) {
        DataTransformer transformer = new DataTransformer();

        // Sample CSV input
        String csvInput = """
            order_id,created_at,customer_id,currency,items,subtotal,discount,tax,shipping
            1001,2026-01-12T10:03:22Z,C001,USD,"sku=A12;qty=2;price=19.99|sku=B55;qty=1;price=5.00",44.98,5.00,3.60,4.99
            1002,2026-01-12T11:15:00Z,C002,USD,"sku=A12;qty=1;price=19.99",19.99,0.00,1.60,0.00
            """;

        // Sample JSON input
        String jsonInput = """
            [
              {
                "id": "1003",
                "createdAt": "2026-01-12T12:05:00Z",
                "customer": { "id": "C001" },
                "currency": "USD",
                "lineItems": [
                  { "sku": "B55", "quantity": 3, "unitPrice": 5.0 }
                ],
                "charges": { "subtotal": 15.0, "discount": 0, "tax": 1.2, "shipping": 4.99 }
              }
            ]
            """;

        System.out.println("=== Processing CSV Only ===");
        System.out.println(transformer.processCsv(csvInput, 10));

        System.out.println("\n=== Processing JSON Only ===");
        System.out.println(transformer.processJson(jsonInput, 10));

        System.out.println("\n=== Processing Combined ===");
        System.out.println(transformer.processAll(csvInput, jsonInput, 10));
    }
}

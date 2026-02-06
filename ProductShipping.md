You have a catalog of products. Some products have shipping dependencies - they must be shipped together with other products. Given a list of products to ship and the dependency rules, determine:

Which products must be shipped together (find connected components)
The minimum number of shipments needed
Detect circular dependencies (invalid rules)
Validate if shipping is possible (topological sort for directed dependencies)


Edge cases:
Empty catalog → 0 shipments
Single product with no dependencies → 1 shipment
Product depends on itself → Should be ignored or rejected
Duplicate dependency rules → Set handles this
Product added after dependency rule → Currently adds missing products automatically
a product is in a dependency rule but not explicitly added


Tests:
Empty catalog returns 0 shipments
Single product returns 1 shipment
Two connected products return 1 shipment
Two disconnected products return 2 shipments
Chain of products (A-B-C) returns 1 shipment
Cycle (A-B-C-A) returns 1 shipment
Large graph performance test
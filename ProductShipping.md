You have a catalog of products. Some products have shipping dependencies - they must be shipped together with other products. Given a list of products to ship and the dependency rules, determine:

Which products must be shipped together (find connected components)
The minimum number of shipments needed
Detect circular dependencies (invalid rules)
Validate if shipping is possible (topological sort for directed dependencies)
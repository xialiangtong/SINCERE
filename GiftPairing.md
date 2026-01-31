Build a system that reads participants from a CSV file and generates valid gift exchange pairings (like Secret Santa). Each person is assigned exactly one other person to give a gift to, with certain constraints.

Requirements
Input
CSV file containing participants with their information
Constraints/rules for valid pairings
Output
Valid pairings where each person gives a gift to exactly one other person
Output as CSV or displayed matches
Constraints (Common Variants)
No self-matching - Person cannot be assigned to themselves
No immediate family - Family members cannot be matched together
No repeat from last year - Cannot have same pairing as previous year
Department restriction - Cannot match people from same team/department
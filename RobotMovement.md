A robot starts at (0,0) facing North on an infinite 2D grid.It receives a list of commands and must move according to rules. Return the robot’s final position (or maximum distance / whether it reaches a point, depending on variant). Command examples Common variants: Simple commands "L": turn left 90° "R": turn right 90° "F": move forward 1 step Mixed commands "F3": move forward 3 "B2": move back 2 "Rw": replace something (cursor variant) Obstacle rules (very common) Robot cannot move onto blocked cells (list given) Example input: commands = ["F2", "R", "F1", "L", "F2"] obstacles = {(1,1), (2,2)}


Corner cases :
Robot starts on an obstacle
Two robots at same starting position
Negative coordinates
Very large step counts (integer overflow)
Circular movement (robot returns to start)
Empty command list
Invalid robot ID

Tests:
Stress tests: 1000 robots, 10000 commands
Boundary tests: Integer.MAX_VALUE coordinates
Fuzzing: Random command sequences
Concurrency tests: Multiple threads navigating simultaneously
Property-based tests: "Robot never moves through obstacles"
A robot starts at (0,0) facing North on an infinite 2D grid.It receives a list of commands and must move according to rules. Return the robot’s final position (or maximum distance / whether it reaches a point, depending on variant). Command examples Common variants: Simple commands "L": turn left 90° "R": turn right 90° "F": move forward 1 step Mixed commands "F3": move forward 3 "B2": move back 2 "Rw": replace something (cursor variant) Obstacle rules (very common) Robot cannot move onto blocked cells (list given) Example input: commands = ["F2", "R", "F1", "L", "F2"] obstacles = {(1,1), (2,2)}


---

## Design Ideas

### 1. Top-level class: `RobotNavigationSolution`
- Entry point containing all inner classes, enums, and interfaces.

### 2. Inner Enum: `Direction`
- Values: `NORTH(0,1)`, `EAST(1,0)`, `SOUTH(0,-1)`, `WEST(-1,0)`
- Methods: `turnLeft()` → returns the Direction 90° counter-clockwise, `turnRight()` → returns the Direction 90° clockwise

### 3. Inner class: `Position` (immutable)
- Fields: `final int x`, `final int y`
- Immutable value object representing a coordinate on the 2D grid.
- Override `equals()` / `hashCode()` for value equality.
- Method: `Position translate(int dx, int dy)` — returns a **new** Position.

### 4. Inner class: `Robot`
- Fields: `String id`, `Position position`, `Direction direction`
- Methods:
  - `Boolean moveForward(int steps)` — move `steps` cells in the current direction
  - `Boolean moveBackward(int steps)` — move `steps` cells opposite to the current direction
  - `void turnLeft()` — rotate 90° left
  - `void turnRight()` — rotate 90° right
  - `Position getPosition()` — returns an **immutable** Position copy
  - `Direction getDirection()` — returns current direction
- Bare `"F"` / `"B"` (no digit) defaults to 1 step — handled by the command parser.

### 5. Interface: `Command`
- Single method: `Boolean execute(Robot robot)` — returns `true` on success, `false` if blocked.
- Implementations:
  - `TurnLeftCommand` — calls `robot.turnLeft()`
  - `TurnRightCommand` — calls `robot.turnRight()`
  - `MoveForwardCommand(int steps)` — calls `robot.moveForward(steps)`
  - `MoveBackwardCommand(int steps)` — calls `robot.moveBackward(steps)`

### 6. Inner class: `RobotNavigateController`
- Reads/parses a string command sequence (e.g. `"F2", "R", "F1", "L", "F2"`) into a `List<Command>`.
- Executes commands one-by-one against a `Robot`.

---

## Design Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    RobotNavigationSolution                      │
│                                                                 │
│  ┌──────────────┐  ┌────────────────────┐ ┌────────────────────────┐│
│  │ «enum»       │  │ «immutable»        │ │        Robot           ││
│  │ Direction    │  │ Position           │ │────────────────────────││
│  │──────────────│  │────────────────────│ │ - id : String          ││
│  │ NORTH(0,1)   │  │ - final x : int    │ │ - position : Position  ││
│  │ EAST (1,0)   │  │ - final y : int    │ │ - direction : Direction││
│  │ SOUTH(0,-1)  │  │────────────────────│ │────────────────────────││
│  │ WEST (-1,0)  │  │ + translate(dx,dy) │ │ + moveForward(int):Bool││
│  │──────────────│  │ + equals/hashCode  │ │ + moveBackward(int):Bol││
│  │ + turnLeft() │  └────────────────────┘ │ + turnLeft():void      ││
│  │ + turnRight()│          ▲              │ + turnRight():void     ││
│  └──────────────┘          │has-a         │ + getPosition():Pos    ││
│         │                  │              │ + getDirection():Dir   ││
│         └──── used-by ─────┘              └────────────────────────┘│
│                                               ▲                │
│  ┌──────────────────────┐                     │ operates on    │
│  │  «interface»         │                     │                │
│  │  Command             │─────────────────────┘                │
│  │──────────────────────│                                      │
│  │ + execute(Robot):Bool│                                      │
│  └──────────┬───────────┘                                      │
│             │ implements                                       │
│  ┌──────────┼──────────────────────────────────────────┐       │
│  │          │                                          │       │
│  │  ┌───────┴────────┐  ┌────────────────┐             │       │
│  │  │ TurnLeftCommand │  │TurnRightCommand│             │       │
│  │  └────────────────┘  └────────────────┘             │       │
│  │  ┌─────────────────────┐  ┌──────────────────────┐  │       │
│  │  │ MoveForwardCommand  │  │ MoveBackwardCommand  │  │       │
│  │  │ - steps : int       │  │ - steps : int        │  │       │
│  │  └─────────────────────┘  └──────────────────────┘  │       │
│  └─────────────────────────────────────────────────────┘       │
│                                                                 │
│  ┌──────────────────────────────────────┐                      │
│  │    RobotNavigateController           │                      │
│  │──────────────────────────────────────│                      │
│  │ + parse(String[]) : List<Command>    │                      │
│  │ + execute(Robot, List<Command>)      │                      │
│  └──────────────────────────────────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

---

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
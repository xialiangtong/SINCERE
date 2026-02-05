/**
 * Robot Navigation Solution
 * 
 * Robot starts at (0,0) facing North on an infinite 2D grid.
 * Executes commands (L/R/F/B) and avoids obstacles.
 */
public class RobotNavigationSolution {

    // ==================== Instance Fields ====================

    private final java.util.Map<String, Robot> robots = new java.util.HashMap<>();
    private java.util.Set<Position> obstacles = new java.util.HashSet<>();
    private static final String DEFAULT_ROBOT_ID = "default";

    public RobotNavigationSolution() {
        // Create a default robot
        robots.put(DEFAULT_ROBOT_ID, new Robot());
    }

    // ==================== Robot Management ====================

    /**
     * Add a new robot with the given ID.
     * @param robotId unique identifier for the robot
     * @return the newly created robot
     */
    public Robot addRobot(String robotId) {
        Robot robot = new Robot();
        robots.put(robotId, robot);
        return robot;
    }

    /**
     * Add a new robot with the given ID at specified position and direction.
     */
    public Robot addRobot(String robotId, Position position, Direction direction) {
        Robot robot = new Robot(position, direction);
        robots.put(robotId, robot);
        return robot;
    }

    /**
     * Get a robot by ID.
     * @return the robot, or null if not found
     */
    public Robot getRobot(String robotId) {
        return robots.get(robotId);
    }

    /**
     * Get the default robot.
     */
    public Robot getRobot() {
        return robots.get(DEFAULT_ROBOT_ID);
    }

    /**
     * Remove a robot by ID.
     * @return true if robot was removed
     */
    public boolean removeRobot(String robotId) {
        return robots.remove(robotId) != null;
    }

    /**
     * Reset a robot to initial state (position 0,0 facing NORTH).
     */
    public void resetRobot(String robotId) {
        Robot robot = robots.get(robotId);
        if (robot != null) {
            robots.put(robotId, new Robot());
        }
    }

    /**
     * Get all robot IDs.
     */
    public java.util.Set<String> getRobotIds() {
        return robots.keySet();
    }

    // ==================== Navigation ====================

    /**
     * Navigate robot using a list of command strings.
     * Uses the default robot.
     * 
     * @param commandStrs list of command strings (e.g., "F3", "R", "B2")
     * @return final position of the robot
     */
    public Position robotNavigate(java.util.List<String> commandStrs) {
        return robotNavigate(DEFAULT_ROBOT_ID, commandStrs);
    }

    /**
     * Navigate a specific robot using a list of command strings.
     * 
     * @param robotId the robot to navigate
     * @param commandStrs list of command strings (e.g., "F3", "R", "B2")
     * @return final position of the robot, or null if robot not found
     */
    public Position robotNavigate(String robotId, java.util.List<String> commandStrs) {
        Robot robot = robots.get(robotId);
        if (robot == null) {
            return null;
        }
        
        for (String cmdStr : commandStrs) {
            Command command = CommandParser.parse(cmdStr);
            if (command != null) {
                command.execute(robot, obstacles);
            }
        }
        return robot.getPosition();
    }

    /**
     * Set obstacles for the navigation.
     */
    public void setObstacles(java.util.Set<Position> obstacles) {
        this.obstacles = obstacles != null ? obstacles : new java.util.HashSet<>();
    }

    // ==================== Direction Enum ====================

    public enum Direction {
        EAST(1, 0),
        SOUTH(0, -1),
        WEST(-1, 0),
        NORTH(0, 1);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public int getDx() {
            return dx;
        }

        public int getDy() {
            return dy;
        }

        /**
         * Turn left 90 degrees (counter-clockwise).
         * NORTH -> WEST -> SOUTH -> EAST -> NORTH
         */
        public Direction turnLeft() {
            return values()[(ordinal() + 3) % 4];
        }

        /**
         * Turn right 90 degrees (clockwise).
         * NORTH -> EAST -> SOUTH -> WEST -> NORTH
         */
        public Direction turnRight() {
            return values()[(ordinal() + 1) % 4];
        }
    }

    // ==================== Position Class ====================

    public static class Position {
        private final int x;
        private final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        /**
         * Move position by given steps in the specified direction.
         * Returns a new Position.
         */
        public Position move(int dx, int dy, int steps) {
            return new Position(x + dx * steps, y + dy * steps);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return x == position.x && y == position.y;
        }

        @Override
        public int hashCode() {
            return 31 * 31 + 31 * x + y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    // ==================== Robot Class ====================

    public static class Robot {
        private Position position;
        private Direction direction;

        public Robot() {
            this.position = new Position(0, 0);
            this.direction = Direction.NORTH;
        }

        public Robot(Position position, Direction direction) {
            this.position = position;
            this.direction = direction;
        }

        public Position getPosition() {
            return position;
        }

        public Direction getDirection() {
            return direction;
        }

        public void turnLeft() {
            direction = direction.turnLeft();
        }

        public void turnRight() {
            direction = direction.turnRight();
        }

        /**
         * Move forward by given steps. Stops if obstacle encountered.
         * @param steps number of steps to move (must be non-negative)
         * @param obstacles set of obstacle positions (can be null)
         * @return actual steps moved (may be less if blocked), or 0 if steps is negative
         */
        public int moveForward(int steps, java.util.Set<Position> obstacles) {
            if (steps < 0) {
                return 0; // Reject negative steps
            }
            int moved = 0;
            for (int i = 0; i < steps; i++) {
                Position next = position.move(direction.getDx(), direction.getDy(), 1);
                if (obstacles != null && obstacles.contains(next)) {
                    break; // Blocked by obstacle
                }
                position = next;
                moved++;
            }
            return moved;
        }

        /**
         * Move backward by given steps. Stops if obstacle encountered.
         * @param steps number of steps to move (must be non-negative)
         * @param obstacles set of obstacle positions (can be null)
         * @return actual steps moved (may be less if blocked), or 0 if steps is negative
         */
        public int moveBackward(int steps, java.util.Set<Position> obstacles) {
            if (steps < 0) {
                return 0; // Reject negative steps
            }
            int moved = 0;
            for (int i = 0; i < steps; i++) {
                Position next = position.move(-direction.getDx(), -direction.getDy(), 1);
                if (obstacles != null && obstacles.contains(next)) {
                    break; // Blocked by obstacle
                }
                position = next;
                moved++;
            }
            return moved;
        }

        @Override
        public String toString() {
            return "Robot{pos=" + position + ", dir=" + direction + "}";
        }
    }

    // ==================== Command Interface ====================

    public interface Command {
        boolean execute(Robot robot, java.util.Set<Position> obstacles);
    }

    // ==================== Turn Commands ====================

    public static class TurnLeft implements Command {
        @Override
        public boolean execute(Robot robot, java.util.Set<Position> obstacles) {
            robot.turnLeft();
            return true;
        }
    }

    public static class TurnRight implements Command {
        @Override
        public boolean execute(Robot robot, java.util.Set<Position> obstacles) {
            robot.turnRight();
            return true;
        }
    }

    // ==================== Move Commands ====================

    public static class MoveForward implements Command {
        private final int steps;

        public MoveForward(int steps) {
            this.steps = steps;
        }

        @Override
        public boolean execute(Robot robot, java.util.Set<Position> obstacles) {
            int moved = robot.moveForward(steps, obstacles);
            return moved == steps; // true if all steps successful
        }
    }

    public static class MoveBackward implements Command {
        private final int steps;

        public MoveBackward(int steps) {
            this.steps = steps;
        }

        @Override
        public boolean execute(Robot robot, java.util.Set<Position> obstacles) {
            int moved = robot.moveBackward(steps, obstacles);
            return moved == steps; // true if all steps successful
        }
    }

    // ==================== Command Parser ====================

    public static class CommandParser {
        private static final java.util.regex.Pattern COMMAND_PATTERN = 
            java.util.regex.Pattern.compile("([LRFB])(\\d*)");

        /**
         * Parse a single command string into a Command object.
         * Valid formats: "L", "R", "F", "F3", "B", "B2"
         * 
         * @param commandStr command string (e.g., "L", "R", "F3", "B2")
         * @return Command object, or null if invalid
         */
        public static Command parse(String commandStr) {
            if (commandStr == null || commandStr.trim().isEmpty()) {
                return null;
            }

            String cmd = commandStr.trim().toUpperCase();
            java.util.regex.Matcher matcher = COMMAND_PATTERN.matcher(cmd);

            if (!matcher.matches()) {
                return null;
            }

            String action = matcher.group(1);
            String stepsStr = matcher.group(2);
            int steps = stepsStr.isEmpty() ? 1 : Integer.parseInt(stepsStr);

            return switch (action) {
                case "L" -> new TurnLeft();
                case "R" -> new TurnRight();
                case "F" -> new MoveForward(steps);
                case "B" -> new MoveBackward(steps);
                default -> null;
            };
        }
    }
}

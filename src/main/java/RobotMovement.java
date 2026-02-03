import java.util.*;
import java.util.regex.*;

/**
 * Robot Movement System using Command Pattern
 * 
 * Robot starts at (0,0) facing North. Executes commands (L/R/F/B),
 * avoids obstacles, supports undo, returns final position.
 */
public class RobotMovement {

    // ==================== Position ====================

    public static class Position {
        private final int x;
        private final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }

        public Position move(int dx, int dy) {
            return new Position(x + dx, y + dy);
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
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", x, y);
        }
    }

    // ==================== Direction ====================

    public enum Direction {
        NORTH(0, 1),
        EAST(1, 0),
        SOUTH(0, -1),
        WEST(-1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public int getDx() { return dx; }
        public int getDy() { return dy; }

        public Direction turnLeft() {
            return values()[(ordinal() + 3) % 4];
        }

        public Direction turnRight() {
            return values()[(ordinal() + 1) % 4];
        }
    }

    // ==================== Robot (Receiver) ====================

    public static class Robot {
        private Position position;
        private Direction direction;
        private final Set<Position> obstacles;

        public Robot() {
            this(new Position(0, 0), Direction.NORTH, Collections.emptySet());
        }

        public Robot(Set<Position> obstacles) {
            this(new Position(0, 0), Direction.NORTH, obstacles);
        }

        public Robot(Position position, Direction direction, Set<Position> obstacles) {
            this.position = position;
            this.direction = direction;
            this.obstacles = new HashSet<>(obstacles);
        }

        public Position getPosition() { return position; }
        public Direction getDirection() { return direction; }

        public void turnLeft() {
            direction = direction.turnLeft();
        }

        public void turnRight() {
            direction = direction.turnRight();
        }

        /**
         * Move forward one step. Returns false if blocked.
         */
        public boolean moveForward() {
            Position next = position.move(direction.getDx(), direction.getDy());
            if (obstacles.contains(next)) {
                return false;
            }
            position = next;
            return true;
        }

        /**
         * Move backward one step. Returns false if blocked.
         */
        public boolean moveBackward() {
            Position next = position.move(-direction.getDx(), -direction.getDy());
            if (obstacles.contains(next)) {
                return false;
            }
            position = next;
            return true;
        }

        /**
         * Force set position (used for undo).
         */
        public void setPosition(Position position) {
            this.position = position;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        @Override
        public String toString() {
            return String.format("Robot{pos=%s, dir=%s}", position, direction);
        }
    }

    // ==================== Command Interface ====================

    public interface Command {
        /**
         * Execute the command on the robot.
         * @return true if fully executed, false if blocked
         */
        boolean execute(Robot robot);

        /**
         * Undo the command.
         */
        void undo(Robot robot);

        /**
         * Get command description.
         */
        String getDescription();
    }

    // ==================== Turn Commands ====================

    public static class TurnLeftCommand implements Command {
        @Override
        public boolean execute(Robot robot) {
            robot.turnLeft();
            return true;
        }

        @Override
        public void undo(Robot robot) {
            robot.turnRight(); // Reverse of turn left
        }

        @Override
        public String getDescription() {
            return "L";
        }
    }

    public static class TurnRightCommand implements Command {
        @Override
        public boolean execute(Robot robot) {
            robot.turnRight();
            return true;
        }

        @Override
        public void undo(Robot robot) {
            robot.turnLeft(); // Reverse of turn right
        }

        @Override
        public String getDescription() {
            return "R";
        }
    }

    // ==================== Move Commands ====================

    public static class ForwardCommand implements Command {
        private final int steps;
        private int actualSteps; // Steps actually taken (may be less if blocked)
        private Position startPosition; // For undo

        public ForwardCommand(int steps) {
            this.steps = steps;
            this.actualSteps = 0;
        }

        @Override
        public boolean execute(Robot robot) {
            startPosition = robot.getPosition();
            actualSteps = 0;
            
            for (int i = 0; i < steps; i++) {
                if (!robot.moveForward()) {
                    return false; // Blocked
                }
                actualSteps++;
            }
            return true;
        }

        @Override
        public void undo(Robot robot) {
            // Move backward the actual steps taken
            for (int i = 0; i < actualSteps; i++) {
                robot.moveBackward();
            }
        }

        @Override
        public String getDescription() {
            return steps == 1 ? "F" : "F" + steps;
        }
    }

    public static class BackwardCommand implements Command {
        private final int steps;
        private int actualSteps;
        private Position startPosition;

        public BackwardCommand(int steps) {
            this.steps = steps;
            this.actualSteps = 0;
        }

        @Override
        public boolean execute(Robot robot) {
            startPosition = robot.getPosition();
            actualSteps = 0;
            
            for (int i = 0; i < steps; i++) {
                if (!robot.moveBackward()) {
                    return false;
                }
                actualSteps++;
            }
            return true;
        }

        @Override
        public void undo(Robot robot) {
            for (int i = 0; i < actualSteps; i++) {
                robot.moveForward();
            }
        }

        @Override
        public String getDescription() {
            return steps == 1 ? "B" : "B" + steps;
        }
    }

    // ==================== Composite Command ====================

    public static class CompositeCommand implements Command {
        private final List<Command> commands;
        private final String name;
        private int executedCount;

        public CompositeCommand(String name, List<Command> commands) {
            this.name = name;
            this.commands = new ArrayList<>(commands);
            this.executedCount = 0;
        }

        @Override
        public boolean execute(Robot robot) {
            executedCount = 0;
            for (Command cmd : commands) {
                if (!cmd.execute(robot)) {
                    return false;
                }
                executedCount++;
            }
            return true;
        }

        @Override
        public void undo(Robot robot) {
            // Undo in reverse order
            for (int i = executedCount - 1; i >= 0; i--) {
                commands.get(i).undo(robot);
            }
        }

        @Override
        public String getDescription() {
            return name;
        }
    }

    // ==================== Command Parser (Factory) ====================

    public static class CommandParser {
        private static final Pattern COMMAND_PATTERN = Pattern.compile("([LRFB])(\\d*)");

        public static Command parse(String commandStr) {
            Matcher matcher = COMMAND_PATTERN.matcher(commandStr.toUpperCase());
            
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid command: " + commandStr);
            }

            String action = matcher.group(1);
            String stepsStr = matcher.group(2);
            int steps = stepsStr.isEmpty() ? 1 : Integer.parseInt(stepsStr);

            return switch (action) {
                case "L" -> new TurnLeftCommand();
                case "R" -> new TurnRightCommand();
                case "F" -> new ForwardCommand(steps);
                case "B" -> new BackwardCommand(steps);
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            };
        }

        public static List<Command> parseAll(List<String> commandStrings) {
            List<Command> commands = new ArrayList<>();
            for (String cmdStr : commandStrings) {
                commands.add(parse(cmdStr));
            }
            return commands;
        }

        public static List<Command> parseAll(String... commandStrings) {
            return parseAll(Arrays.asList(commandStrings));
        }
    }

    // ==================== Robot Controller (Invoker) ====================

    public static class RobotController {
        private final Robot robot;
        private final Deque<Command> commandHistory;
        private boolean verbose;

        public RobotController(Robot robot) {
            this.robot = robot;
            this.commandHistory = new ArrayDeque<>();
            this.verbose = false;
        }

        public RobotController setVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Execute a list of command strings.
         */
        public Position executeCommands(List<String> commandStrings) {
            List<Command> commands = CommandParser.parseAll(commandStrings);
            for (Command cmd : commands) {
                execute(cmd);
            }
            return robot.getPosition();
        }

        /**
         * Execute a single command.
         */
        public boolean execute(Command command) {
            if (verbose) {
                System.out.printf("  Execute %-5s: %s", command.getDescription(), robot);
            }
            
            boolean success = command.execute(robot);
            commandHistory.push(command);
            
            if (verbose) {
                System.out.printf(" → %s %s%n", robot.getPosition(), success ? "" : "(BLOCKED)");
            }
            
            return success;
        }

        /**
         * Undo the last command.
         */
        public boolean undo() {
            if (commandHistory.isEmpty()) {
                return false;
            }
            
            Command lastCommand = commandHistory.pop();
            
            if (verbose) {
                System.out.printf("  Undo    %-5s: %s", lastCommand.getDescription(), robot);
            }
            
            lastCommand.undo(robot);
            
            if (verbose) {
                System.out.printf(" → %s%n", robot.getPosition());
            }
            
            return true;
        }

        /**
         * Undo multiple commands.
         */
        public void undo(int count) {
            for (int i = 0; i < count && !commandHistory.isEmpty(); i++) {
                undo();
            }
        }

        public Position getPosition() {
            return robot.getPosition();
        }

        public Direction getDirection() {
            return robot.getDirection();
        }

        public Robot getRobot() {
            return robot;
        }

        public int getHistorySize() {
            return commandHistory.size();
        }
    }

    // ==================== Convenience API ====================

    /**
     * Execute commands and return final position.
     */
    public static Position execute(String[] commands, int[][] obstacles) {
        Set<Position> obstacleSet = new HashSet<>();
        if (obstacles != null) {
            for (int[] obs : obstacles) {
                obstacleSet.add(new Position(obs[0], obs[1]));
            }
        }
        
        Robot robot = new Robot(obstacleSet);
        RobotController controller = new RobotController(robot);
        return controller.executeCommands(Arrays.asList(commands));
    }

    /**
     * Execute commands without obstacles.
     */
    public static Position execute(String... commands) {
        return execute(commands, null);
    }

    // ==================== Demo ====================

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        Robot Movement - Command Pattern Demo           ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Example 1: Basic movement
        System.out.println("Example 1: Basic Movement");
        System.out.println("─────────────────────────────────────────");
        Robot robot1 = new Robot();
        RobotController controller1 = new RobotController(robot1).setVerbose(true);
        
        System.out.println("Commands: F3, R, F2, L, F1");
        System.out.println("Start: " + robot1);
        controller1.executeCommands(Arrays.asList("F3", "R", "F2", "L", "F1"));
        System.out.println("Final: " + robot1.getPosition());

        // Example 2: With obstacles
        System.out.println("\n\nExample 2: Movement with Obstacles");
        System.out.println("─────────────────────────────────────────");
        Set<Position> obstacles = new HashSet<>();
        obstacles.add(new Position(0, 3));
        obstacles.add(new Position(2, 2));
        
        Robot robot2 = new Robot(obstacles);
        RobotController controller2 = new RobotController(robot2).setVerbose(true);
        
        System.out.println("Obstacles: (0,3), (2,2)");
        System.out.println("Commands: F5, R, F3");
        System.out.println("Start: " + robot2);
        controller2.executeCommands(Arrays.asList("F5", "R", "F3"));
        System.out.println("Final: " + robot2.getPosition() + " (stopped at obstacle)");

        // Example 3: Undo support
        System.out.println("\n\nExample 3: Undo Support");
        System.out.println("─────────────────────────────────────────");
        Robot robot3 = new Robot();
        RobotController controller3 = new RobotController(robot3).setVerbose(true);
        
        System.out.println("Commands: F2, R, F2");
        controller3.executeCommands(Arrays.asList("F2", "R", "F2"));
        System.out.println("Position: " + robot3.getPosition());
        
        System.out.println("\nUndo last 2 commands:");
        controller3.undo(2);
        System.out.println("Position after undo: " + robot3.getPosition());

        // Example 4: Composite command (macro)
        System.out.println("\n\nExample 4: Composite Command (Macro)");
        System.out.println("─────────────────────────────────────────");
        Robot robot4 = new Robot();
        RobotController controller4 = new RobotController(robot4).setVerbose(true);
        
        // Create a "square" macro: F1, R, F1, R, F1, R, F1, R
        List<Command> squareCommands = Arrays.asList(
            new ForwardCommand(1), new TurnRightCommand(),
            new ForwardCommand(1), new TurnRightCommand(),
            new ForwardCommand(1), new TurnRightCommand(),
            new ForwardCommand(1), new TurnRightCommand()
        );
        CompositeCommand squareMacro = new CompositeCommand("SQUARE", squareCommands);
        
        System.out.println("Execute 'SQUARE' macro (moves in a square, returns to start):");
        System.out.println("Start: " + robot4);
        controller4.execute(squareMacro);
        System.out.println("Final: " + robot4.getPosition() + " (back to origin!)");

        // Example 5: Simple API
        System.out.println("\n\nExample 5: Simple API");
        System.out.println("─────────────────────────────────────────");
        Position result = execute("F3", "R", "F2", "L", "B1");
        System.out.println("execute(\"F3\", \"R\", \"F2\", \"L\", \"B1\") = " + result);
        
        // With obstacles
        Position result2 = execute(
            new String[]{"F5", "R", "F3"},
            new int[][]{{0, 3}, {2, 2}}
        );
        System.out.println("execute with obstacles at (0,3), (2,2) = " + result2);

        // Example 6: Edge cases
        System.out.println("\n\nExample 6: Direction Visualization");
        System.out.println("─────────────────────────────────────────");
        Robot robot6 = new Robot();
        System.out.println("Starting direction: " + robot6.getDirection());
        robot6.turnRight();
        System.out.println("After R: " + robot6.getDirection());
        robot6.turnRight();
        System.out.println("After R: " + robot6.getDirection());
        robot6.turnRight();
        System.out.println("After R: " + robot6.getDirection());
        robot6.turnRight();
        System.out.println("After R: " + robot6.getDirection() + " (full circle)");
    }
}

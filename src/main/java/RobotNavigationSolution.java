import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RobotNavigationSolution {

    // ──────────────────────────────────────────────
    // 1. Direction Enum
    // ──────────────────────────────────────────────
    enum Direction {
        NORTH(0, 1),
        EAST(1, 0),
        SOUTH(0, -1),
        WEST(-1, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        Direction turnLeft() {
            return values()[(ordinal() + 3) % 4];
        }

        Direction turnRight() {
            return values()[(ordinal() + 1) % 4];
        }
    }

    // ──────────────────────────────────────────────
    // 2. Position (immutable)
    // ──────────────────────────────────────────────
    static class Position {
        private final int x;
        private final int y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int getX() { return x; }
        int getY() { return y; }

        Position translate(int dx, int dy) {
            return new Position(x + dx, y + dy);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position)) return false;
            Position p = (Position) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    // ──────────────────────────────────────────────
    // 3. Robot
    // ──────────────────────────────────────────────
    static class Robot {
        private final int id;
        private Position position;
        private Direction direction;
        private final List<Position> history;

        Robot(int id, Position position, Direction direction) {
            this.id = id;
            this.position = position;
            this.direction = direction;
            this.history = new ArrayList<>();
            this.history.add(position);
        }

        Robot(int id) {
            this(id, new Position(0, 0), Direction.NORTH);
        }

        int getId() { return id; }

        Position getPosition() {
            return this.position;
        }

        Direction getDirection() { return direction; }

        List<Position> getHistory() {
            return Collections.unmodifiableList(history);
        }

        Boolean moveForward(int steps, Set<Position> obstacles) {
            for (int i = 0; i < steps; i++) {
                Position next = position.translate(direction.dx, direction.dy);
                if (obstacles.contains(next)) {
                    return false;
                }
                position = next;
                history.add(position);
            }
            return true;
        }

        Boolean moveBackward(int steps, Set<Position> obstacles) {
            for (int i = 0; i < steps; i++) {
                Position next = position.translate(-direction.dx, -direction.dy);
                if (obstacles.contains(next)) {
                    return false;
                }
                position = next;
                history.add(position);
            }
            return true;
        }

        void turnLeft() {
            direction = direction.turnLeft();
        }

        void turnRight() {
            direction = direction.turnRight();
        }

        @Override
        public String toString() {
            return "Robot{id='" + id + "', pos=" + position + ", dir=" + direction + "}";
        }
    }

    // ──────────────────────────────────────────────
    // 4. Command Interface
    // ──────────────────────────────────────────────
    interface Command {
        Boolean execute(Robot robot, Set<Position> obstacles);
    }

    // ──────────────────────────────────────────────
    // 5. Command Implementations
    // ──────────────────────────────────────────────
    static class TurnLeftCommand implements Command {
        @Override
        public Boolean execute(Robot robot, Set<Position> obstacles) {
            robot.turnLeft();
            return true;
        }
    }

    static class TurnRightCommand implements Command {
        @Override
        public Boolean execute(Robot robot, Set<Position> obstacles) {
            robot.turnRight();
            return true;
        }
    }

    static class MoveForwardCommand implements Command {
        private final int steps;

        MoveForwardCommand(int steps) {
            this.steps = steps;
        }

        @Override
        public Boolean execute(Robot robot, Set<Position> obstacles) {
            return robot.moveForward(steps, obstacles);
        }
    }

    static class MoveBackwardCommand implements Command {
        private final int steps;

        MoveBackwardCommand(int steps) {
            this.steps = steps;
        }

        @Override
        public Boolean execute(Robot robot, Set<Position> obstacles) {
            return robot.moveBackward(steps, obstacles);
        }
    }

    // ──────────────────────────────────────────────
    // 6. CommandParser
    // ──────────────────────────────────────────────
    static class CommandParser {

        Command parseCommandStr(String command) {
            if (command == null || command.isEmpty()) {
                return null;
            }
            char type = Character.toUpperCase(command.charAt(0));
            switch (type) {
                case 'L':
                    return new TurnLeftCommand();
                case 'R':
                    return new TurnRightCommand();
                case 'F': {
                    try {
                        int steps = command.length() > 1 ? Integer.parseInt(command.substring(1)) : 1;
                        return steps > 0 ? new MoveForwardCommand(steps) : null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                case 'B': {
                    try {
                        int steps = command.length() > 1 ? Integer.parseInt(command.substring(1)) : 1;
                        return steps > 0 ? new MoveBackwardCommand(steps) : null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                default:
                    return null;
            }
        }
    }

    // ──────────────────────────────────────────────
    // 7. Top-level orchestration
    // ──────────────────────────────────────────────
    private final Map<Integer, Robot> robotMap;
    private final Set<Position> obstacles;
    private final CommandParser parser;

    public RobotNavigationSolution(Set<Position> obstacles) {
        this.robotMap = new HashMap<>();
        this.obstacles = obstacles != null ? new HashSet<>(obstacles) : new HashSet<>();
        this.parser = new CommandParser();
    }

    public RobotNavigationSolution() {
        this(new HashSet<>());
    }

    public void addRobot(Robot robot) {
        if (robot == null) {
            throw new IllegalArgumentException("Robot cannot be null");
        }
        robotMap.put(robot.getId(), robot);
    }

    public Robot removeRobot(int id) {
        return robotMap.remove(id);
    }

    public Robot getRobot(int id) {
        return robotMap.get(id);
    }

    public Position robotNavigate(int robotId, List<String> commandStrs) {
        Robot robot = robotMap.get(robotId);
        if (robot == null) {
            throw new IllegalArgumentException("Robot not found: " + robotId);
        }
        if (commandStrs == null || commandStrs.isEmpty()) {
            return robot.getPosition();
        }
        // Build effective obstacles: static obstacles + other robots' current positions
        Set<Position> effectiveObstacles = new HashSet<>(obstacles);
        for (Map.Entry<Integer, Robot> entry : robotMap.entrySet()) {
            if (entry.getKey() != robotId) {
                effectiveObstacles.add(entry.getValue().getPosition());
            }
        }
        if (effectiveObstacles.contains(robot.getPosition())) {
            return robot.getPosition();
        }
        for (String cmdStr : commandStrs) {
            Command command = parser.parseCommandStr(cmdStr);
            if (command != null) {
                command.execute(robot, effectiveObstacles);
            }
        }
        return robot.getPosition();
    }
}

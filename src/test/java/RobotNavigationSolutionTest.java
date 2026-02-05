import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test cases for RobotNavigationSolution
 */
public class RobotNavigationSolutionTest {

    private RobotNavigationSolution solution;

    @BeforeEach
    void setUp() {
        solution = new RobotNavigationSolution();
    }

    // ==================== Direction Enum Tests ====================

    @Nested
    @DisplayName("Direction Enum Tests")
    class DirectionTests {

        @Test
        @DisplayName("Direction has correct dx/dy values")
        void testDirectionValues() {
            assertEquals(1, RobotNavigationSolution.Direction.EAST.getDx());
            assertEquals(0, RobotNavigationSolution.Direction.EAST.getDy());

            assertEquals(0, RobotNavigationSolution.Direction.SOUTH.getDx());
            assertEquals(-1, RobotNavigationSolution.Direction.SOUTH.getDy());

            assertEquals(-1, RobotNavigationSolution.Direction.WEST.getDx());
            assertEquals(0, RobotNavigationSolution.Direction.WEST.getDy());

            assertEquals(0, RobotNavigationSolution.Direction.NORTH.getDx());
            assertEquals(1, RobotNavigationSolution.Direction.NORTH.getDy());
        }

        @Test
        @DisplayName("turnLeft rotates counter-clockwise")
        void testTurnLeft() {
            assertEquals(RobotNavigationSolution.Direction.WEST, 
                RobotNavigationSolution.Direction.NORTH.turnLeft());
            assertEquals(RobotNavigationSolution.Direction.SOUTH, 
                RobotNavigationSolution.Direction.WEST.turnLeft());
            assertEquals(RobotNavigationSolution.Direction.EAST, 
                RobotNavigationSolution.Direction.SOUTH.turnLeft());
            assertEquals(RobotNavigationSolution.Direction.NORTH, 
                RobotNavigationSolution.Direction.EAST.turnLeft());
        }

        @Test
        @DisplayName("turnRight rotates clockwise")
        void testTurnRight() {
            assertEquals(RobotNavigationSolution.Direction.EAST, 
                RobotNavigationSolution.Direction.NORTH.turnRight());
            assertEquals(RobotNavigationSolution.Direction.SOUTH, 
                RobotNavigationSolution.Direction.EAST.turnRight());
            assertEquals(RobotNavigationSolution.Direction.WEST, 
                RobotNavigationSolution.Direction.SOUTH.turnRight());
            assertEquals(RobotNavigationSolution.Direction.NORTH, 
                RobotNavigationSolution.Direction.WEST.turnRight());
        }

        @Test
        @DisplayName("Four left turns return to original direction")
        void testFourLeftTurns() {
            RobotNavigationSolution.Direction dir = RobotNavigationSolution.Direction.NORTH;
            dir = dir.turnLeft().turnLeft().turnLeft().turnLeft();
            assertEquals(RobotNavigationSolution.Direction.NORTH, dir);
        }

        @Test
        @DisplayName("Four right turns return to original direction")
        void testFourRightTurns() {
            RobotNavigationSolution.Direction dir = RobotNavigationSolution.Direction.NORTH;
            dir = dir.turnRight().turnRight().turnRight().turnRight();
            assertEquals(RobotNavigationSolution.Direction.NORTH, dir);
        }
    }

    // ==================== Position Class Tests ====================

    @Nested
    @DisplayName("Position Class Tests")
    class PositionTests {

        @Test
        @DisplayName("Position stores x and y correctly")
        void testPositionValues() {
            RobotNavigationSolution.Position pos = new RobotNavigationSolution.Position(3, 5);
            assertEquals(3, pos.getX());
            assertEquals(5, pos.getY());
        }

        @Test
        @DisplayName("Position move returns new position")
        void testPositionMove() {
            RobotNavigationSolution.Position pos = new RobotNavigationSolution.Position(0, 0);
            RobotNavigationSolution.Position newPos = pos.move(1, 0, 3); // Move east 3 steps
            
            assertEquals(3, newPos.getX());
            assertEquals(0, newPos.getY());
            // Original unchanged
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Position equals works correctly")
        void testPositionEquals() {
            RobotNavigationSolution.Position pos1 = new RobotNavigationSolution.Position(2, 3);
            RobotNavigationSolution.Position pos2 = new RobotNavigationSolution.Position(2, 3);
            RobotNavigationSolution.Position pos3 = new RobotNavigationSolution.Position(3, 2);

            assertEquals(pos1, pos2);
            assertNotEquals(pos1, pos3);
            assertNotEquals(pos1, null);
        }

        @Test
        @DisplayName("Position hashCode is consistent with equals")
        void testPositionHashCode() {
            RobotNavigationSolution.Position pos1 = new RobotNavigationSolution.Position(2, 3);
            RobotNavigationSolution.Position pos2 = new RobotNavigationSolution.Position(2, 3);

            assertEquals(pos1.hashCode(), pos2.hashCode());
        }

        @Test
        @DisplayName("Position toString formats correctly")
        void testPositionToString() {
            RobotNavigationSolution.Position pos = new RobotNavigationSolution.Position(5, -3);
            assertEquals("(5, -3)", pos.toString());
        }
    }

    // ==================== Robot Class Tests ====================

    @Nested
    @DisplayName("Robot Class Tests")
    class RobotTests {

        @Test
        @DisplayName("Robot starts at (0,0) facing NORTH")
        void testRobotInitialState() {
            RobotNavigationSolution.Robot robot = new RobotNavigationSolution.Robot();
            assertEquals(0, robot.getPosition().getX());
            assertEquals(0, robot.getPosition().getY());
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }

        @Test
        @DisplayName("Robot moveForward rejects negative steps")
        void testMoveForwardNegativeSteps() {
            RobotNavigationSolution.Robot robot = new RobotNavigationSolution.Robot();
            int moved = robot.moveForward(-5, null);
            assertEquals(0, moved);
            assertEquals(0, robot.getPosition().getX());
            assertEquals(0, robot.getPosition().getY());
        }

        @Test
        @DisplayName("Robot moveBackward rejects negative steps")
        void testMoveBackwardNegativeSteps() {
            RobotNavigationSolution.Robot robot = new RobotNavigationSolution.Robot();
            int moved = robot.moveBackward(-5, null);
            assertEquals(0, moved);
            assertEquals(0, robot.getPosition().getX());
            assertEquals(0, robot.getPosition().getY());
        }
    }

    // ==================== Command Parser Tests ====================

    @Nested
    @DisplayName("CommandParser Tests")
    class CommandParserTests {

        @Test
        @DisplayName("Parse valid commands")
        void testParseValidCommands() {
            assertNotNull(RobotNavigationSolution.CommandParser.parse("L"));
            assertNotNull(RobotNavigationSolution.CommandParser.parse("R"));
            assertNotNull(RobotNavigationSolution.CommandParser.parse("F"));
            assertNotNull(RobotNavigationSolution.CommandParser.parse("B"));
            assertNotNull(RobotNavigationSolution.CommandParser.parse("F3"));
            assertNotNull(RobotNavigationSolution.CommandParser.parse("B10"));
        }

        @Test
        @DisplayName("Parse lowercase commands")
        void testParseLowercaseCommands() {
            assertNotNull(RobotNavigationSolution.CommandParser.parse("l"));
            assertNotNull(RobotNavigationSolution.CommandParser.parse("f5"));
        }

        @Test
        @DisplayName("Parse invalid commands returns null")
        void testParseInvalidCommands() {
            assertNull(RobotNavigationSolution.CommandParser.parse(null));
            assertNull(RobotNavigationSolution.CommandParser.parse(""));
            assertNull(RobotNavigationSolution.CommandParser.parse("   "));
            assertNull(RobotNavigationSolution.CommandParser.parse("X"));
            assertNull(RobotNavigationSolution.CommandParser.parse("FF"));
            assertNull(RobotNavigationSolution.CommandParser.parse("123"));
        }
    }

    // ==================== Robot Navigation Tests ====================

    @Nested
    @DisplayName("Robot Navigation - Turn Tests")
    class NavigationTurnTests {

        @Test
        @DisplayName("Single left turn")
        void testSingleLeftTurn() {
            solution.robotNavigate(Arrays.asList("L"));
            assertEquals(RobotNavigationSolution.Direction.WEST, solution.getRobot().getDirection());
        }

        @Test
        @DisplayName("Single right turn")
        void testSingleRightTurn() {
            solution.robotNavigate(Arrays.asList("R"));
            assertEquals(RobotNavigationSolution.Direction.EAST, solution.getRobot().getDirection());
        }

        @Test
        @DisplayName("Multiple left turns")
        void testMultipleLeftTurns() {
            solution.robotNavigate(Arrays.asList("L", "L"));
            assertEquals(RobotNavigationSolution.Direction.SOUTH, solution.getRobot().getDirection());

            solution.resetRobot("default");
            solution.robotNavigate(Arrays.asList("L", "L", "L"));
            assertEquals(RobotNavigationSolution.Direction.EAST, solution.getRobot().getDirection());
        }

        @Test
        @DisplayName("Multiple right turns")
        void testMultipleRightTurns() {
            solution.robotNavigate(Arrays.asList("R", "R"));
            assertEquals(RobotNavigationSolution.Direction.SOUTH, solution.getRobot().getDirection());

            solution.resetRobot("default");
            solution.robotNavigate(Arrays.asList("R", "R", "R"));
            assertEquals(RobotNavigationSolution.Direction.WEST, solution.getRobot().getDirection());
        }

        @Test
        @DisplayName("Four turns return to original direction")
        void testFourTurns() {
            solution.robotNavigate(Arrays.asList("L", "L", "L", "L"));
            assertEquals(RobotNavigationSolution.Direction.NORTH, solution.getRobot().getDirection());

            solution.resetRobot("default");
            solution.robotNavigate(Arrays.asList("R", "R", "R", "R"));
            assertEquals(RobotNavigationSolution.Direction.NORTH, solution.getRobot().getDirection());
        }

        @Test
        @DisplayName("Mixed left and right turns")
        void testMixedTurns() {
            // L then R should return to NORTH
            solution.robotNavigate(Arrays.asList("L", "R"));
            assertEquals(RobotNavigationSolution.Direction.NORTH, solution.getRobot().getDirection());

            solution.resetRobot("default");
            // R, R, L = EAST
            solution.robotNavigate(Arrays.asList("R", "R", "L"));
            assertEquals(RobotNavigationSolution.Direction.EAST, solution.getRobot().getDirection());
        }
    }

    @Nested
    @DisplayName("Robot Navigation - Move Without Obstacles")
    class NavigationMoveWithoutObstaclesTests {

        @Test
        @DisplayName("Move forward single step")
        void testMoveForwardSingleStep() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F"));
            assertEquals(0, pos.getX());
            assertEquals(1, pos.getY());
        }

        @Test
        @DisplayName("Move forward multiple steps")
        void testMoveForwardMultipleSteps() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F5"));
            assertEquals(0, pos.getX());
            assertEquals(5, pos.getY());
        }

        @Test
        @DisplayName("Move backward single step")
        void testMoveBackwardSingleStep() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("B"));
            assertEquals(0, pos.getX());
            assertEquals(-1, pos.getY());
        }

        @Test
        @DisplayName("Move backward multiple steps")
        void testMoveBackwardMultipleSteps() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("B3"));
            assertEquals(0, pos.getX());
            assertEquals(-3, pos.getY());
        }

        @Test
        @DisplayName("Move in different directions")
        void testMoveInDifferentDirections() {
            // Move north 2, turn right, move east 3
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F2", "R", "F3"));
            assertEquals(3, pos.getX());
            assertEquals(2, pos.getY());
        }
    }

    @Nested
    @DisplayName("Robot Navigation - Move With Obstacles")
    class NavigationMoveWithObstaclesTests {

        @BeforeEach
        void setUpObstacles() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 3));  // Obstacle at (0, 3)
            obstacles.add(new RobotNavigationSolution.Position(2, 0));  // Obstacle at (2, 0)
            obstacles.add(new RobotNavigationSolution.Position(-1, 0)); // Obstacle at (-1, 0)
            solution.setObstacles(obstacles);
        }

        @Test
        @DisplayName("Move blocked at first step")
        void testMoveBlockedAtFirstStep() {
            // Obstacle at (0, 3), try to move from (0, 2) to (0, 3)
            solution.robotNavigate(Arrays.asList("F2")); // Move to (0, 2)
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F")); // Blocked at (0, 3)
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY()); // Stays at (0, 2)
        }

        @Test
        @DisplayName("Move blocked in the middle")
        void testMoveBlockedInMiddle() {
            // Obstacle at (0, 3), try F5 from origin - should stop at (0, 2)
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F5"));
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY()); // Stopped before obstacle
        }

        @Test
        @DisplayName("Move blocked at last step")
        void testMoveBlockedAtLastStep() {
            // Obstacle at (0, 3), try F3 from origin - should stop at (0, 2)
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F3"));
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY()); // Last step blocked
        }

        @Test
        @DisplayName("Backward move blocked")
        void testBackwardMoveBlocked() {
            // Start at origin facing NORTH, turn left to face WEST
            solution.robotNavigate(Arrays.asList("L")); // Now facing WEST
            // From (0,0) facing WEST, F1 would go to (-1, 0) which is blocked
            // So robot stays at (0, 0)
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F2"));
            assertEquals(0, pos.getX()); // Blocked at first step, stays at origin
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Move blocked then turn and continue")
        void testMoveBlockedThenContinue() {
            // Move north until blocked, turn right, move east
            solution.robotNavigate(Arrays.asList("F5")); // Blocked at (0, 2)
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("R", "F2"));
            assertEquals(2, pos.getX()); // Obstacle at (2,0) doesn't affect (2, 2)
            assertEquals(2, pos.getY());
        }

        @Test
        @DisplayName("No obstacle in path - full movement")
        void testNoObstacleInPath() {
            // Turn right, move east (no obstacles in this path up to x=1)
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("R", "F1"));
            assertEquals(1, pos.getX());
            assertEquals(0, pos.getY());
        }
    }

    @Nested
    @DisplayName("Robot Navigation - Combined Commands")
    class NavigationCombinedTests {

        @Test
        @DisplayName("Square path returns to origin")
        void testSquarePath() {
            // Move in a square: F1, R, F1, R, F1, R, F1, R
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F1", "R", "F1", "R", "F1", "R", "F1", "R"));
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
            assertEquals(RobotNavigationSolution.Direction.NORTH, solution.getRobot().getDirection());
        }

        @Test
        @DisplayName("Complex path with forward, backward, and turns")
        void testComplexPath() {
            // F3, R, F2, B1, L, F1
            // Start (0,0) N
            // F3 -> (0,3) N
            // R  -> (0,3) E
            // F2 -> (2,3) E
            // B1 -> (1,3) E (backward is west)
            // L  -> (1,3) N
            // F1 -> (1,4) N
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F3", "R", "F2", "B1", "L", "F1"));
            assertEquals(1, pos.getX());
            assertEquals(4, pos.getY());
        }

        @Test
        @DisplayName("Forward and backward cancel out")
        void testForwardBackwardCancel() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F3", "B3"));
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Multiple forward commands accumulate")
        void testMultipleForwardAccumulate() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F2", "F3", "F1"));
            assertEquals(0, pos.getX());
            assertEquals(6, pos.getY());
        }

        @Test
        @DisplayName("Turn around and move")
        void testTurnAroundAndMove() {
            // F2, turn 180, F2 should return to origin
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F2", "R", "R", "F2"));
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Navigate all four directions")
        void testAllFourDirections() {
            // North 1, East 1, South 1, West 1 - should return to origin
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F1", "R", "F1", "R", "F1", "R", "F1"));
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Complex path with obstacles")
        void testComplexPathWithObstacles() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 5));
            obstacles.add(new RobotNavigationSolution.Position(3, 4));
            solution.setObstacles(obstacles);

            // Try to go north 10 (blocked at 5), turn right, go east 5 (blocked at 3)
            // Start (0,0) N
            // F10 -> blocked, stops at (0, 4)
            // R   -> (0, 4) E
            // F5  -> blocked at (3, 4), stops at (2, 4)
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F10", "R", "F5"));
            assertEquals(2, pos.getX());
            assertEquals(4, pos.getY());
        }
    }

    @Nested
    @DisplayName("Robot Navigation - Edge Cases")
    class NavigationEdgeCasesTests {

        @Test
        @DisplayName("Empty command list")
        void testEmptyCommandList() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList());
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Invalid commands are skipped")
        void testInvalidCommandsSkipped() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F2", "INVALID", "F1"));
            assertEquals(0, pos.getX());
            assertEquals(3, pos.getY()); // Invalid command skipped
        }

        @Test
        @DisplayName("Move zero steps (F0)")
        void testMoveZeroSteps() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F0"));
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Large number of steps")
        void testLargeSteps() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(Arrays.asList("F100"));
            assertEquals(0, pos.getX());
            assertEquals(100, pos.getY());
        }

        @Test
        @DisplayName("Negative coordinates reachable")
        void testNegativeCoordinates() {
            // Turn around (south) and move
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("R", "R", "F5")); // Face south, move 5
            assertEquals(0, pos.getX());
            assertEquals(-5, pos.getY());
        }
    }

    // ==================== Invalid Command Tests ====================

    @Nested
    @DisplayName("Invalid Command String Tests")
    class InvalidCommandTests {

        @Test
        @DisplayName("Lowercase commands are converted to uppercase (valid)")
        void testLowercaseCommandValid() {
            // Parser calls toUpperCase(), so lowercase IS valid
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("f5", "F3"));
            assertEquals(0, pos.getX());
            assertEquals(8, pos.getY()); // f5 (=F5) + F3 = 8
        }

        @Test
        @DisplayName("Command with unknown prefix is invalid")
        void testUnknownPrefixInvalid() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("X5", "F2"));
            // X5 is invalid, F2 executes
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY());
        }

        @Test
        @DisplayName("Move command without number defaults to 1 step")
        void testMoveCommandWithoutNumberDefaults() {
            // Parser regex is \d* (0 or more digits), so F alone = 1 step
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F", "F3"));
            assertEquals(0, pos.getX());
            assertEquals(4, pos.getY()); // F (=1) + F3 = 4
        }

        @Test
        @DisplayName("Multiple invalid commands in sequence")
        void testMultipleInvalidCommands() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("GARBAGE", "123", "!!!", "F2"));
            // All invalid except F2
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY());
        }

        @Test
        @DisplayName("All commands invalid returns origin")
        void testAllCommandsInvalid() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("XYZ", "ABC", "123"));
            assertEquals(0, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Empty string command is invalid")
        void testEmptyStringCommand() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("", "F2"));
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY());
        }

        @Test
        @DisplayName("Leading/trailing spaces are trimmed (valid)")
        void testCommandWithLeadingTrailingSpaces() {
            // Parser calls trim(), so leading/trailing spaces are OK
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList(" F2", "F2 ", "F1"));
            assertEquals(0, pos.getX());
            assertEquals(5, pos.getY()); // " F2"(=2) + "F2 "(=2) + F1 = 5
        }

        @Test
        @DisplayName("Command with internal space is invalid")
        void testCommandWithInternalSpace() {
            // Internal spaces make it invalid
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F 2", "F1"));
            assertEquals(0, pos.getX());
            assertEquals(1, pos.getY()); // "F 2" invalid, F1 = 1
        }

        @Test
        @DisplayName("Move command with negative number is invalid")
        void testMoveWithNegativeNumber() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("F-5", "F2"));
            // F-5 is invalid (parser returns null), F2 executes
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY());
        }

        @Test
        @DisplayName("Turn commands with numbers are valid (numbers ignored by regex)")
        void testTurnWithNumber() {
            // The regex ([LRFB])(\d*) allows digits after turn commands
            // but turnLeft/turnRight ignore the number
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("L2", "R3", "L", "F2"));
            // L2 (turns left), R3 (turns right, now facing NORTH), L (turns left), F2 (moves west)
            assertEquals(-2, pos.getX());
            assertEquals(0, pos.getY());
        }

        @Test
        @DisplayName("Null in command list is skipped")
        void testNullCommand() {
            List<String> commands = new ArrayList<>();
            commands.add("F2");
            commands.add(null);
            commands.add("F3");
            RobotNavigationSolution.Position pos = solution.robotNavigate(commands);
            assertEquals(0, pos.getX());
            assertEquals(5, pos.getY());
        }

        @Test
        @DisplayName("Multi-character invalid prefix")
        void testMultiCharInvalidPrefix() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                Arrays.asList("FORWARD5", "BACK2", "F1"));
            // Both invalid, only F1 executes
            assertEquals(0, pos.getX());
            assertEquals(1, pos.getY());
        }
    }

    // ==================== Multi-Robot Management Tests ====================

    @Nested
    @DisplayName("Multi-Robot Management Tests")
    class MultiRobotManagementTests {

        @Test
        @DisplayName("Add robot with position and direction")
        void testAddAndGetRobot() {
            solution.addRobot("robot1", 
                new RobotNavigationSolution.Position(5, 10), 
                RobotNavigationSolution.Direction.EAST);
            RobotNavigationSolution.Robot robot = solution.getRobot("robot1");
            
            assertNotNull(robot);
            assertEquals(5, robot.getPosition().getX());
            assertEquals(10, robot.getPosition().getY());
            assertEquals(RobotNavigationSolution.Direction.EAST, robot.getDirection());
        }

        @Test
        @DisplayName("Add robot with defaults (origin, facing NORTH)")
        void testAddRobotDefaults() {
            solution.addRobot("robot1");
            RobotNavigationSolution.Robot robot = solution.getRobot("robot1");
            
            assertNotNull(robot);
            assertEquals(0, robot.getPosition().getX());
            assertEquals(0, robot.getPosition().getY());
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }

        @Test
        @DisplayName("Get non-existent robot returns null")
        void testGetNonExistentRobot() {
            RobotNavigationSolution.Robot robot = solution.getRobot("nonexistent");
            assertNull(robot);
        }

        @Test
        @DisplayName("Add multiple robots")
        void testAddMultipleRobots() {
            solution.addRobot("robot1", 
                new RobotNavigationSolution.Position(0, 0), 
                RobotNavigationSolution.Direction.NORTH);
            solution.addRobot("robot2", 
                new RobotNavigationSolution.Position(10, 10), 
                RobotNavigationSolution.Direction.SOUTH);
            solution.addRobot("robot3", 
                new RobotNavigationSolution.Position(-5, 5), 
                RobotNavigationSolution.Direction.WEST);
            
            assertNotNull(solution.getRobot("robot1"));
            assertNotNull(solution.getRobot("robot2"));
            assertNotNull(solution.getRobot("robot3"));
            
            assertEquals(RobotNavigationSolution.Direction.NORTH, 
                solution.getRobot("robot1").getDirection());
            assertEquals(RobotNavigationSolution.Direction.SOUTH, 
                solution.getRobot("robot2").getDirection());
            assertEquals(RobotNavigationSolution.Direction.WEST, 
                solution.getRobot("robot3").getDirection());
        }

        @Test
        @DisplayName("Remove robot")
        void testRemoveRobot() {
            solution.addRobot("robot1");
            assertNotNull(solution.getRobot("robot1"));
            
            boolean removed = solution.removeRobot("robot1");
            assertTrue(removed);
            assertNull(solution.getRobot("robot1"));
        }

        @Test
        @DisplayName("Remove non-existent robot returns false")
        void testRemoveNonExistentRobot() {
            boolean removed = solution.removeRobot("nonexistent");
            assertFalse(removed);
        }

        @Test
        @DisplayName("Reset robot to initial position")
        void testResetRobot() {
            solution.addRobot("robot1");
            
            // Navigate the robot
            solution.robotNavigate("robot1", Arrays.asList("F5", "R", "F3"));
            RobotNavigationSolution.Robot robot = solution.getRobot("robot1");
            assertEquals(3, robot.getPosition().getX());
            assertEquals(5, robot.getPosition().getY());
            assertEquals(RobotNavigationSolution.Direction.EAST, robot.getDirection());
            
            // Reset the robot (resets to origin facing NORTH)
            solution.resetRobot("robot1");
            robot = solution.getRobot("robot1");
            assertEquals(0, robot.getPosition().getX());
            assertEquals(0, robot.getPosition().getY());
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }

        @Test
        @DisplayName("Reset non-existent robot does not throw")
        void testResetNonExistentRobot() {
            assertDoesNotThrow(() -> solution.resetRobot("nonexistent"));
        }

        @Test
        @DisplayName("Navigate specific robot by ID")
        void testNavigateRobotById() {
            solution.addRobot("robot1");
            solution.addRobot("robot2", 
                new RobotNavigationSolution.Position(5, 5), 
                RobotNavigationSolution.Direction.EAST);
            
            // Navigate robot1
            RobotNavigationSolution.Position pos1 = solution.robotNavigate(
                "robot1", Arrays.asList("F3"));
            assertEquals(0, pos1.getX());
            assertEquals(3, pos1.getY());
            
            // Navigate robot2
            RobotNavigationSolution.Position pos2 = solution.robotNavigate(
                "robot2", Arrays.asList("F2"));
            assertEquals(7, pos2.getX());
            assertEquals(5, pos2.getY());
            
            // Verify both robots updated correctly
            assertEquals(3, solution.getRobot("robot1").getPosition().getY());
            assertEquals(7, solution.getRobot("robot2").getPosition().getX());
        }

        @Test
        @DisplayName("Navigate non-existent robot returns null")
        void testNavigateNonExistentRobot() {
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                "nonexistent", Arrays.asList("F5"));
            assertNull(pos);
        }

        @Test
        @DisplayName("Navigate robot with obstacles using setObstacles")
        void testNavigateRobotByIdWithObstacles() {
            solution.addRobot("robot1");
            
            solution.setObstacles(Set.of(
                new RobotNavigationSolution.Position(0, 3)));
            
            RobotNavigationSolution.Position pos = solution.robotNavigate(
                "robot1", Arrays.asList("F5"));
            
            // Robot stopped at (0, 2) due to obstacle at (0, 3)
            assertEquals(0, pos.getX());
            assertEquals(2, pos.getY());
        }

        @Test
        @DisplayName("Overwrite existing robot with same ID")
        void testOverwriteRobot() {
            solution.addRobot("robot1");
            assertEquals(0, solution.getRobot("robot1").getPosition().getX());
            
            // Add robot with same ID overwrites
            solution.addRobot("robot1", 
                new RobotNavigationSolution.Position(100, 200), 
                RobotNavigationSolution.Direction.SOUTH);
            assertEquals(100, solution.getRobot("robot1").getPosition().getX());
            assertEquals(200, solution.getRobot("robot1").getPosition().getY());
            assertEquals(RobotNavigationSolution.Direction.SOUTH, 
                solution.getRobot("robot1").getDirection());
        }

        @Test
        @DisplayName("Multiple robots navigate independently")
        void testMultipleRobotsNavigateIndependently() {
            solution.addRobot("A");
            solution.addRobot("B", 
                new RobotNavigationSolution.Position(0, 0), 
                RobotNavigationSolution.Direction.EAST);
            
            solution.robotNavigate("A", Arrays.asList("F5"));
            solution.robotNavigate("B", Arrays.asList("F5"));
            
            // A moved north, B moved east
            assertEquals(0, solution.getRobot("A").getPosition().getX());
            assertEquals(5, solution.getRobot("A").getPosition().getY());
            assertEquals(5, solution.getRobot("B").getPosition().getX());
            assertEquals(0, solution.getRobot("B").getPosition().getY());
        }

        @Test
        @DisplayName("Get all robot IDs")
        void testGetRobotIds() {
            solution.addRobot("robot1");
            solution.addRobot("robot2");
            solution.addRobot("robot3");
            
            java.util.Set<String> ids = solution.getRobotIds();
            // Note: default robot is already added in setUp
            assertTrue(ids.contains("robot1"));
            assertTrue(ids.contains("robot2"));
            assertTrue(ids.contains("robot3"));
        }

        @Test
        @DisplayName("Get default robot")
        void testGetDefaultRobot() {
            // Default robot is created in constructor
            RobotNavigationSolution.Robot robot = solution.getRobot();
            assertNotNull(robot);
            assertEquals(0, robot.getPosition().getX());
            assertEquals(0, robot.getPosition().getY());
        }
    }
}

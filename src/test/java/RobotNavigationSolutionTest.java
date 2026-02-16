import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RobotNavigationSolution Tests")
public class RobotNavigationSolutionTest {

    private RobotNavigationSolution.Robot robot;
    private RobotNavigationSolution solution;

    @BeforeEach
    void setUp() {
        robot = new RobotNavigationSolution.Robot(1);
        solution = new RobotNavigationSolution();
        solution.addRobot(robot);
    }

    // ──────────────────────────────────────────────────────
    // 1. Invalid command strings
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Invalid Command Strings")
    class InvalidCommandTests {

        @Test
        @DisplayName("Unknown letter command returns null from parser")
        void unknownLetterCommand() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr("X"));
            assertNull(parser.parseCommandStr("Z3"));
            assertNull(parser.parseCommandStr("G"));
        }

        @Test
        @DisplayName("Null command returns null from parser")
        void nullCommand() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr(null));
        }

        @Test
        @DisplayName("Empty string returns null from parser")
        void emptyCommand() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr(""));
        }

        @Test
        @DisplayName("F with non-numeric suffix returns null")
        void forwardWithNonNumericSuffix() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr("Fabc"));
        }

        @Test
        @DisplayName("F-1 negative steps returns null — robot does not move")
        void forwardWithNegativeSteps() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr("F-1"));
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("F-1"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("B with non-numeric suffix returns null")
        void backwardWithNonNumericSuffix() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr("B!"));
        }

        @Test
        @DisplayName("B-2 negative steps returns null — robot does not move")
        void backwardWithNegativeSteps() {
            RobotNavigationSolution.CommandParser parser = new RobotNavigationSolution.CommandParser();
            assertNull(parser.parseCommandStr("B-2"));
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("B-2"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("F0 — zero steps, robot stays in place")
        void forwardZeroSteps() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("F0"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. Turn left / right / forward / backward combinations
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Movement and Turn Combinations")
    class CombinationTests {

        @Test
        @DisplayName("Move forward 1 step (bare F) — facing North")
        void moveForwardOneStep() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 1), result);
        }

        @Test
        @DisplayName("Move forward 3 steps — facing North")
        void moveForwardMultipleSteps() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("F3"));
            assertEquals(new RobotNavigationSolution.Position(0, 3), result);
        }

        @Test
        @DisplayName("Move backward 2 steps — facing North goes to (0,-2)")
        void moveBackward() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("B2"));
            assertEquals(new RobotNavigationSolution.Position(0, -2), result);
        }

        @Test
        @DisplayName("Turn left then move forward — faces West, moves to (-1,0)")
        void turnLeftThenForward() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("L", "F"));
            assertEquals(new RobotNavigationSolution.Position(-1, 0), result);
        }

        @Test
        @DisplayName("Turn right then move forward — faces East, moves to (1,0)")
        void turnRightThenForward() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Arrays.asList("R", "F"));
            assertEquals(new RobotNavigationSolution.Position(1, 0), result);
        }

        @Test
        @DisplayName("Turn right, forward 2, turn right, forward 3 — ends at (2,-3)")
        void rightForwardRightForward() {
            // Start: (0,0) North
            // R → East, F2 → (2,0), R → South, F3 → (2,-3)
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("R", "F2", "R", "F3"));
            assertEquals(new RobotNavigationSolution.Position(2, -3), result);
        }

        @Test
        @DisplayName("Square walk: F1 R F1 R F1 R F1 — returns to origin")
        void squareWalkReturnsToOrigin() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F", "R", "F", "R", "F", "R", "F"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Forward then backward same steps — returns to origin")
        void forwardThenBackwardCancels() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F5", "B5"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Problem example: F2 R F1 L F2 — ends at (1,4)")
        void problemExample() {
            // Start: (0,0) North
            // F2 → (0,2), R → East, F1 → (1,2), L → North, F2 → (1,4)
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F2", "R", "F1", "L", "F2"));
            assertEquals(new RobotNavigationSolution.Position(1, 4), result);
        }

        @Test
        @DisplayName("Turn left then backward — facing West, backward goes East to (1,0)")
        void turnLeftThenBackward() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("L", "B"));
            assertEquals(new RobotNavigationSolution.Position(1, 0), result);
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. Continuous turns / continuous same-direction moves
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Continuous Turns and Moves")
    class ContinuousTests {

        @Test
        @DisplayName("Four left turns — returns to original direction North")
        void fourLeftTurnsFullRotation() {
            solution.robotNavigate(1, Arrays.asList("L", "L", "L", "L"));
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }

        @Test
        @DisplayName("Four right turns — returns to original direction North")
        void fourRightTurnsFullRotation() {
            solution.robotNavigate(1, Arrays.asList("R", "R", "R", "R"));
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }

        @Test
        @DisplayName("Two left turns — faces South")
        void twoLeftTurnsFacesSouth() {
            solution.robotNavigate(1, Arrays.asList("L", "L"));
            assertEquals(RobotNavigationSolution.Direction.SOUTH, robot.getDirection());
        }

        @Test
        @DisplayName("Three right turns — faces West")
        void threeRightTurnsFacesWest() {
            solution.robotNavigate(1, Arrays.asList("R", "R", "R"));
            assertEquals(RobotNavigationSolution.Direction.WEST, robot.getDirection());
        }

        @Test
        @DisplayName("Continuous forward moves accumulate — F2 + F3 = (0,5)")
        void continuousForwardAccumulates() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F2", "F3"));
            assertEquals(new RobotNavigationSolution.Position(0, 5), result);
        }

        @Test
        @DisplayName("Continuous backward moves accumulate — B1 + B2 + B3 = (0,-6)")
        void continuousBackwardAccumulates() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("B1", "B2", "B3"));
            assertEquals(new RobotNavigationSolution.Position(0, -6), result);
        }

        @Test
        @DisplayName("Continuous same direction: R F1 F2 F3 — all East = (6,0)")
        void continuousForwardAfterTurn() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("R", "F1", "F2", "F3"));
            assertEquals(new RobotNavigationSolution.Position(6, 0), result);
        }
    }

    // ──────────────────────────────────────────────────────
    // 4. All command strings are invalid
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("All Commands Invalid")
    class AllInvalidTests {

        @Test
        @DisplayName("All unknown letters — robot stays at origin")
        void allUnknownCommands() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("X", "Y", "Z"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("All empty strings — robot stays at origin")
        void allEmptyStrings() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("", "", ""));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Direction unchanged after all invalid commands")
        void directionUnchangedAfterInvalid() {
            solution.robotNavigate(1, Arrays.asList("X", "Z", "Q"));
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }
    }

    // ──────────────────────────────────────────────────────
    // 5. Mix of valid and invalid commands
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Mixed Valid and Invalid Commands")
    class MixedValidInvalidTests {

        @Test
        @DisplayName("Invalid commands skipped, valid ones execute — F2 X R F1 = (1,2)")
        void invalidSkippedValidExecuted() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F2", "X", "R", "F1"));
            // F2 → (0,2), X skipped, R → East, F1 → (1,2)
            assertEquals(new RobotNavigationSolution.Position(1, 2), result);
        }

        @Test
        @DisplayName("Invalid at start and end — only middle commands run")
        void invalidAtStartAndEnd() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("Z", "F3", "R", "F2", "Q"));
            // Z skip, F3 → (0,3), R → East, F2 → (2,3), Q skip
            assertEquals(new RobotNavigationSolution.Position(2, 3), result);
        }

        @Test
        @DisplayName("Empty strings interspersed — skipped gracefully")
        void emptyStringsInterspersed() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("", "F1", "", "L", "F1", ""));
            // F1 → (0,1), L → West, F1 → (-1,1)
            assertEquals(new RobotNavigationSolution.Position(-1, 1), result);
        }
    }

    // ──────────────────────────────────────────────────────
    // 6. All command strings are valid
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("All Commands Valid")
    class AllValidTests {

        @Test
        @DisplayName("Zigzag path: F1 R F1 L F1 R F1 = (2,2)")
        void zigzagPath() {
            // N→(0,1), R→E, E→(1,1), L→N, N→(1,2), R→E, E→(2,2)
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F1", "R", "F1", "L", "F1", "R", "F1"));
            assertEquals(new RobotNavigationSolution.Position(2, 2), result);
        }

        @Test
        @DisplayName("All four directions traversal")
        void allFourDirections() {
            // North F2 → (0,2), R → East F3 → (3,2), R → South F1 → (3,1), R → West F4 → (-1,1)
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F2", "R", "F3", "R", "F1", "R", "F4"));
            assertEquals(new RobotNavigationSolution.Position(-1, 1), result);
        }

        @Test
        @DisplayName("Only turns — position stays at origin")
        void onlyTurns() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("L", "R", "L", "L", "R"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Large steps — F100 then B50 = (0,50)")
        void largeSteps() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, 
                    Arrays.asList("F100", "B50"));
            assertEquals(new RobotNavigationSolution.Position(0, 50), result);
        }
    }

    // ──────────────────────────────────────────────────────
    // 7. Empty command list
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Empty Command List")
    class EmptyCommandListTests {

        @Test
        @DisplayName("Empty list — robot stays at origin")
        void emptyList() {
            RobotNavigationSolution.Position result = solution.robotNavigate(1, Collections.emptyList());
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Empty list — direction stays North")
        void emptyListDirectionUnchanged() {
            solution.robotNavigate(1, Collections.emptyList());
            assertEquals(RobotNavigationSolution.Direction.NORTH, robot.getDirection());
        }
    }

    // ──────────────────────────────────────────────────────
    // 8. Obstacle tests
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Obstacle Tests")
    class ObstacleTests {

        @Test
        @DisplayName("Robot blocked by obstacle directly ahead — stays at origin")
        void blockedByObstacleAhead() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 1));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Robot blocked mid-movement — stops at last valid position")
        void blockedMidMovement() {
            // Obstacle at (0,3), robot tries F5 from origin facing North
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 3));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F5"));
            // Moves to (0,1), (0,2), then blocked at (0,3) — stops at (0,2)
            assertEquals(new RobotNavigationSolution.Position(0, 2), result);
        }

        @Test
        @DisplayName("Backward blocked by obstacle — stays in place")
        void backwardBlocked() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, -1));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("B"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Turn avoids obstacle — can move in different direction")
        void turnAvoidsObstacle() {
            // Obstacle at (0,1) blocks North, but East is clear
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 1));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F", "R", "F2"));
            // F blocked → stay (0,0), R → East, F2 → (2,0)
            assertEquals(new RobotNavigationSolution.Position(2, 0), result);
        }

        @Test
        @DisplayName("Multiple obstacles — blocked in multiple directions")
        void multipleObstacles() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 1));  // blocks North
            obstacles.add(new RobotNavigationSolution.Position(1, 0));  // blocks East
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F", "R", "F"));
            // F blocked North, R → East, F blocked East → stays (0,0)
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Robot starts on obstacle — no commands execute, returns start position")
        void robotStartsOnObstacle() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 0));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F3", "R", "F2"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("No obstacles — full movement")
        void noObstacles() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F2", "R", "F1"));
            assertEquals(new RobotNavigationSolution.Position(1, 2), result);
        }

        @Test
        @DisplayName("Obstacle behind only blocks backward, not forward")
        void obstacleBehindDoesNotBlockForward() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, -1)); // behind
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F3"));
            assertEquals(new RobotNavigationSolution.Position(0, 3), result);
        }
    }

    // ──────────────────────────────────────────────────────
    // 9. History tests
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("History Tests")
    class HistoryTests {

        @Test
        @DisplayName("Initial history contains starting position")
        void initialHistory() {
            List<RobotNavigationSolution.Position> history = robot.getHistory();
            assertEquals(1, history.size());
            assertEquals(new RobotNavigationSolution.Position(0, 0), history.get(0));
        }

        @Test
        @DisplayName("History records each step of movement")
        void historyRecordsEachStep() {
            solution.robotNavigate(1, Arrays.asList("F3"));
            List<RobotNavigationSolution.Position> history = robot.getHistory();
            assertEquals(4, history.size()); // origin + 3 steps
            assertEquals(new RobotNavigationSolution.Position(0, 0), history.get(0));
            assertEquals(new RobotNavigationSolution.Position(0, 1), history.get(1));
            assertEquals(new RobotNavigationSolution.Position(0, 2), history.get(2));
            assertEquals(new RobotNavigationSolution.Position(0, 3), history.get(3));
        }

        @Test
        @DisplayName("History records movement with turns")
        void historyWithTurns() {
            solution.robotNavigate(1, Arrays.asList("F1", "R", "F1"));
            List<RobotNavigationSolution.Position> history = robot.getHistory();
            assertEquals(3, history.size()); // origin + F1 + F1 (turns don't add)
            assertEquals(new RobotNavigationSolution.Position(0, 0), history.get(0));
            assertEquals(new RobotNavigationSolution.Position(0, 1), history.get(1));
            assertEquals(new RobotNavigationSolution.Position(1, 1), history.get(2));
        }

        @Test
        @DisplayName("History does not record blocked movement")
        void historyNotRecordedWhenBlocked() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 1));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            sol.robotNavigate(1, Arrays.asList("F"));
            List<RobotNavigationSolution.Position> history = r.getHistory();
            assertEquals(1, history.size()); // only origin
        }

        @Test
        @DisplayName("History records partial movement before blocked")
        void historyRecordsPartialBeforeBlocked() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 3));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            sol.robotNavigate(1, Arrays.asList("F5"));
            List<RobotNavigationSolution.Position> history = r.getHistory();
            assertEquals(3, history.size()); // origin + (0,1) + (0,2), blocked at (0,3)
            assertEquals(new RobotNavigationSolution.Position(0, 0), history.get(0));
            assertEquals(new RobotNavigationSolution.Position(0, 1), history.get(1));
            assertEquals(new RobotNavigationSolution.Position(0, 2), history.get(2));
        }

        @Test
        @DisplayName("History is unmodifiable")
        void historyIsUnmodifiable() {
            assertThrows(UnsupportedOperationException.class, () -> robot.getHistory().add(
                    new RobotNavigationSolution.Position(99, 99)));
        }
    }

    // ──────────────────────────────────────────────────────
    // 10. Movement tests with non-interfering obstacles
    //     (copied from no-obstacle tests, obstacles placed
    //      away from the path so results are identical)
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Movement With Non-Interfering Obstacles")
    class MovementWithObstaclesTests {

        private Set<RobotNavigationSolution.Position> farObstacles() {
            Set<RobotNavigationSolution.Position> obs = new HashSet<>();
            obs.add(new RobotNavigationSolution.Position(100, 100));
            obs.add(new RobotNavigationSolution.Position(-50, -50));
            obs.add(new RobotNavigationSolution.Position(99, -99));
            return obs;
        }

        @Test
        @DisplayName("Forward 1 step with distant obstacles — (0,1)")
        void moveForwardOneStep() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 1), result);
        }

        @Test
        @DisplayName("Forward 3 steps with distant obstacles — (0,3)")
        void moveForwardMultipleSteps() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F3"));
            assertEquals(new RobotNavigationSolution.Position(0, 3), result);
        }

        @Test
        @DisplayName("Backward 2 steps with distant obstacles — (0,-2)")
        void moveBackward() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("B2"));
            assertEquals(new RobotNavigationSolution.Position(0, -2), result);
        }

        @Test
        @DisplayName("Turn left then forward with obstacles — (-1,0)")
        void turnLeftThenForward() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("L", "F"));
            assertEquals(new RobotNavigationSolution.Position(-1, 0), result);
        }

        @Test
        @DisplayName("Turn right then forward with obstacles — (1,0)")
        void turnRightThenForward() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("R", "F"));
            assertEquals(new RobotNavigationSolution.Position(1, 0), result);
        }

        @Test
        @DisplayName("R F2 R F3 with obstacles — (2,-3)")
        void rightForwardRightForward() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("R", "F2", "R", "F3"));
            assertEquals(new RobotNavigationSolution.Position(2, -3), result);
        }

        @Test
        @DisplayName("Square walk with obstacles — returns to origin")
        void squareWalkReturnsToOrigin() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F", "R", "F", "R", "F", "R", "F"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Forward then backward cancels with obstacles — origin")
        void forwardThenBackwardCancels() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F5", "B5"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Problem example with obstacles — (1,4)")
        void problemExample() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F2", "R", "F1", "L", "F2"));
            assertEquals(new RobotNavigationSolution.Position(1, 4), result);
        }

        @Test
        @DisplayName("Zigzag path with obstacles — (2,2)")
        void zigzagPath() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F1", "R", "F1", "L", "F1", "R", "F1"));
            assertEquals(new RobotNavigationSolution.Position(2, 2), result);
        }

        @Test
        @DisplayName("Continuous forward with obstacles — F2 + F3 = (0,5)")
        void continuousForwardAccumulates() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F2", "F3"));
            assertEquals(new RobotNavigationSolution.Position(0, 5), result);
        }

        @Test
        @DisplayName("Continuous backward with obstacles — B1+B2+B3 = (0,-6)")
        void continuousBackwardAccumulates() {
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(farObstacles());
            sol.addRobot(r);
            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("B1", "B2", "B3"));
            assertEquals(new RobotNavigationSolution.Position(0, -6), result);
        }

        @Test
        @DisplayName("Robot starts on obstacle — no commands execute")
        void robotStartsOnObstacleWithCommands() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(0, 0));
            RobotNavigationSolution.Robot r = new RobotNavigationSolution.Robot(1);
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            sol.addRobot(r);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, 
                    Arrays.asList("F2", "R", "F1", "L", "F2"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
            // Direction should also remain unchanged since no commands ran
            assertEquals(RobotNavigationSolution.Direction.NORTH, r.getDirection());
        }
    }

    // ──────────────────────────────────────────────────────
    // 11. Multi-robot blocking tests
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Multi-Robot Blocking Tests")
    class MultiRobotBlockingTests {

        @Test
        @DisplayName("Robot blocked by another robot directly ahead")
        void blockedByAnotherRobot() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0) North
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(0, 1), RobotNavigationSolution.Direction.NORTH);
            sol.addRobot(r1);
            sol.addRobot(r2);

            // r1 tries to move forward but r2 is at (0,1)
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), result);
        }

        @Test
        @DisplayName("Robot blocked mid-movement by another robot")
        void blockedMidMovementByAnotherRobot() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0) North
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(0, 3), RobotNavigationSolution.Direction.EAST);
            sol.addRobot(r1);
            sol.addRobot(r2);

            // r1 tries F5, blocked at (0,3) by r2, stops at (0,2)
            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F5"));
            assertEquals(new RobotNavigationSolution.Position(0, 2), result);
        }

        @Test
        @DisplayName("Robot can move when other robot is not in the way")
        void notBlockedByDistantRobot() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0) North
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(5, 5), RobotNavigationSolution.Direction.NORTH);
            sol.addRobot(r1);
            sol.addRobot(r2);

            RobotNavigationSolution.Position result = sol.robotNavigate(1, Arrays.asList("F3"));
            assertEquals(new RobotNavigationSolution.Position(0, 3), result);
        }

        @Test
        @DisplayName("Two robots move sequentially without blocking each other")
        void twoRobotsMoveSeparately() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0) North
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(3, 0), RobotNavigationSolution.Direction.NORTH);
            sol.addRobot(r1);
            sol.addRobot(r2);

            // r1 goes East, r2 goes North — no collision
            sol.robotNavigate(1, Arrays.asList("R", "F2")); // r1 → (2,0)
            sol.robotNavigate(2, Arrays.asList("F2"));       // r2 → (3,2)

            assertEquals(new RobotNavigationSolution.Position(2, 0), r1.getPosition());
            assertEquals(new RobotNavigationSolution.Position(3, 2), r2.getPosition());
        }

        @Test
        @DisplayName("Robot blocked by another robot AND static obstacle")
        void blockedByRobotAndObstacle() {
            Set<RobotNavigationSolution.Position> obstacles = new HashSet<>();
            obstacles.add(new RobotNavigationSolution.Position(1, 0)); // static obstacle East
            RobotNavigationSolution sol = new RobotNavigationSolution(obstacles);
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0)
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(0, 1), RobotNavigationSolution.Direction.NORTH);
            sol.addRobot(r1);
            sol.addRobot(r2);

            // r1 blocked North by r2, blocked East by obstacle
            sol.robotNavigate(1, Arrays.asList("F")); // blocked by r2
            assertEquals(new RobotNavigationSolution.Position(0, 0), r1.getPosition());
            sol.robotNavigate(1, Arrays.asList("R", "F")); // blocked by obstacle
            assertEquals(new RobotNavigationSolution.Position(0, 0), r1.getPosition());
        }

        @Test
        @DisplayName("Robot can turn even when surrounded by other robots")
        void canTurnWhenSurrounded() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0)
            RobotNavigationSolution.Robot rN = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(0, 1), RobotNavigationSolution.Direction.NORTH);
            RobotNavigationSolution.Robot rE = new RobotNavigationSolution.Robot(3,
                    new RobotNavigationSolution.Position(1, 0), RobotNavigationSolution.Direction.NORTH);
            RobotNavigationSolution.Robot rS = new RobotNavigationSolution.Robot(4,
                    new RobotNavigationSolution.Position(0, -1), RobotNavigationSolution.Direction.NORTH);
            RobotNavigationSolution.Robot rW = new RobotNavigationSolution.Robot(5,
                    new RobotNavigationSolution.Position(-1, 0), RobotNavigationSolution.Direction.NORTH);
            sol.addRobot(r1);
            sol.addRobot(rN);
            sol.addRobot(rE);
            sol.addRobot(rS);
            sol.addRobot(rW);

            // r1 can turn but cannot move in any direction
            sol.robotNavigate(1, Arrays.asList("R", "R")); // turn to South
            assertEquals(RobotNavigationSolution.Direction.SOUTH, r1.getDirection());
            assertEquals(new RobotNavigationSolution.Position(0, 0), r1.getPosition());
        }

        @Test
        @DisplayName("After one robot moves away, another can move into vacated spot")
        void moveIntoVacatedSpot() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1); // (0,0)
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(0, 1), RobotNavigationSolution.Direction.NORTH);
            sol.addRobot(r1);
            sol.addRobot(r2);

            // r1 blocked by r2 at (0,1)
            sol.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), r1.getPosition());

            // r2 moves away to (0,2)
            sol.robotNavigate(2, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 2), r2.getPosition());

            // Now r1 can move forward — (0,1) is free
            sol.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 1), r1.getPosition());
        }

        @Test
        @DisplayName("Three robots in a line — middle blocks both ends")
        void threeRobotsInLine() {
            RobotNavigationSolution sol = new RobotNavigationSolution();
            RobotNavigationSolution.Robot r1 = new RobotNavigationSolution.Robot(1,
                    new RobotNavigationSolution.Position(0, 0), RobotNavigationSolution.Direction.NORTH);
            RobotNavigationSolution.Robot r2 = new RobotNavigationSolution.Robot(2,
                    new RobotNavigationSolution.Position(0, 1), RobotNavigationSolution.Direction.SOUTH);
            RobotNavigationSolution.Robot r3 = new RobotNavigationSolution.Robot(3,
                    new RobotNavigationSolution.Position(0, 2), RobotNavigationSolution.Direction.SOUTH);
            sol.addRobot(r1);
            sol.addRobot(r2);
            sol.addRobot(r3);

            // r1 tries North — blocked by r2 at (0,1)
            sol.robotNavigate(1, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 0), r1.getPosition());

            // r3 tries South — blocked by r2 at (0,1)
            sol.robotNavigate(3, Arrays.asList("F"));
            assertEquals(new RobotNavigationSolution.Position(0, 2), r3.getPosition());
        }
    }
}

/**
 * Connect Four Game Engine
 * 
 * Implements the core rules for Connect Four with configurable board size.
 */
public class ConnectFour {

    /**
     * Represents a player or empty cell.
     */
    public enum Player {
        NONE, PLAYER_1, PLAYER_2;

        public Player opponent() {
            return this == PLAYER_1 ? PLAYER_2 : (this == PLAYER_2 ? PLAYER_1 : NONE);
        }

        @Override
        public String toString() {
            return this == PLAYER_1 ? "X" : (this == PLAYER_2 ? "O" : ".");
        }
    }

    /**
     * Represents the game status.
     */
    public enum GameStatus {
        IN_PROGRESS,
        PLAYER_1_WINS,
        PLAYER_2_WINS,
        DRAW
    }

    /**
     * Result of a move attempt.
     */
    public enum MoveResult {
        SUCCESS,
        INVALID_COLUMN,
        COLUMN_FULL,
        GAME_OVER
    }

    /**
     * Result containing move outcome and updated game state.
     */
    public static class DropResult {
        private final MoveResult result;
        private final int row;  // Row where disc landed (-1 if failed)
        private final GameStatus status;

        private DropResult(MoveResult result, int row, GameStatus status) {
            this.result = result;
            this.row = row;
            this.status = status;
        }

        public static DropResult success(int row, GameStatus status) {
            return new DropResult(MoveResult.SUCCESS, row, status);
        }

        public static DropResult failure(MoveResult result, GameStatus status) {
            return new DropResult(result, -1, status);
        }

        public MoveResult getResult() { return result; }
        public int getRow() { return row; }
        public GameStatus getStatus() { return status; }
        public boolean isSuccess() { return result == MoveResult.SUCCESS; }

        @Override
        public String toString() {
            return "DropResult{result=" + result + ", row=" + row + ", status=" + status + "}";
        }
    }

    // Board configuration
    private final int width;
    private final int height;
    private final int winLength;

    // Game state
    private final Player[][] board;
    private Player currentPlayer;
    private GameStatus status;
    private int movesCount;

    /**
     * Creates a new Connect Four game with default 7x6 board.
     */
    public ConnectFour() {
        this(7, 6, 4);
    }

    /**
     * Creates a new Connect Four game with custom dimensions.
     */
    public ConnectFour(int width, int height, int winLength) {
        this.width = width;
        this.height = height;
        this.winLength = winLength;
        this.board = new Player[height][width];
        reset();
    }

    /**
     * Resets the game to initial state.
     */
    public void reset() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                board[row][col] = Player.NONE;
            }
        }
        currentPlayer = Player.PLAYER_1;
        status = GameStatus.IN_PROGRESS;
        movesCount = 0;
    }

    /**
     * Drops a disc into the specified column.
     * @param column Column index (0-based)
     * @return DropResult with outcome
     */
    public DropResult dropDisc(int column) {
        // Check if game is already over
        if (status != GameStatus.IN_PROGRESS) {
            return DropResult.failure(MoveResult.GAME_OVER, status);
        }

        // Validate column index
        if (column < 0 || column >= width) {
            return DropResult.failure(MoveResult.INVALID_COLUMN, status);
        }

        // Check if column is full (top row is filled)
        if (board[0][column] != Player.NONE) {
            return DropResult.failure(MoveResult.COLUMN_FULL, status);
        }

        // Find the lowest empty row in the column
        int row = findLowestEmptyRow(column);
        board[row][column] = currentPlayer;
        movesCount++;

        // Check for win
        if (checkWin(row, column)) {
            status = (currentPlayer == Player.PLAYER_1) ? 
                     GameStatus.PLAYER_1_WINS : GameStatus.PLAYER_2_WINS;
            return DropResult.success(row, status);
        }

        // Check for draw
        if (movesCount == width * height) {
            status = GameStatus.DRAW;
            return DropResult.success(row, status);
        }

        // Switch player
        currentPlayer = currentPlayer.opponent();
        return DropResult.success(row, status);
    }

    /**
     * Finds the lowest empty row in a column.
     */
    private int findLowestEmptyRow(int column) {
        for (int row = height - 1; row >= 0; row--) {
            if (board[row][column] == Player.NONE) {
                return row;
            }
        }
        return -1; // Should never reach here if column is not full
    }

    /**
     * Checks if the last move at (row, col) resulted in a win.
     * Efficiently checks only the four directions from the last move.
     */
    private boolean checkWin(int row, int col) {
        Player player = board[row][col];
        
        // Check all four directions: horizontal, vertical, two diagonals
        return countDirection(row, col, 0, 1, player) >= winLength ||  // Horizontal
               countDirection(row, col, 1, 0, player) >= winLength ||  // Vertical
               countDirection(row, col, 1, 1, player) >= winLength ||  // Diagonal \
               countDirection(row, col, 1, -1, player) >= winLength;   // Diagonal /
    }

    /**
     * Counts consecutive pieces in a direction (both ways from the position).
     */
    private int countDirection(int row, int col, int dRow, int dCol, Player player) {
        int count = 1; // Count the piece at (row, col)
        
        // Count in positive direction
        count += countInDirection(row, col, dRow, dCol, player);
        
        // Count in negative direction
        count += countInDirection(row, col, -dRow, -dCol, player);
        
        return count;
    }

    /**
     * Counts consecutive pieces in one direction.
     */
    private int countInDirection(int row, int col, int dRow, int dCol, Player player) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;
        
        while (r >= 0 && r < height && c >= 0 && c < width && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }
        
        return count;
    }

    /**
     * Gets the cell value at the specified position.
     * Row 0 is the top row.
     */
    public Player getCell(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return null;
        }
        return board[row][col];
    }

    /**
     * Gets the current game status.
     */
    public GameStatus getStatus() {
        return status;
    }

    /**
     * Gets the current player.
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Gets the board width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the board height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Checks if the game is over.
     */
    public boolean isGameOver() {
        return status != GameStatus.IN_PROGRESS;
    }

    /**
     * Gets a string representation of the board.
     */
    public String getBoardString() {
        StringBuilder sb = new StringBuilder();
        
        // Column numbers
        for (int col = 0; col < width; col++) {
            sb.append(" ").append(col);
        }
        sb.append("\n");
        
        // Board
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                sb.append("|").append(board[row][col]);
            }
            sb.append("|\n");
        }
        
        // Bottom border
        sb.append("-".repeat(width * 2 + 1));
        
        return sb.toString();
    }

    /**
     * Factory method to create a new game.
     */
    public static ConnectFour newGame() {
        return new ConnectFour();
    }

    /**
     * Factory method to create a new game with custom dimensions.
     */
    public static ConnectFour newGame(int width, int height) {
        return new ConnectFour(width, height, 4);
    }

    public static void main(String[] args) {
        ConnectFour game = ConnectFour.newGame();
        
        System.out.println("=== Connect Four Demo ===\n");
        System.out.println(game.getBoardString());
        
        // Simulate a game
        int[] moves = {3, 3, 4, 4, 5, 5, 6}; // Player 1 wins horizontally
        
        for (int col : moves) {
            System.out.println("\n" + game.getCurrentPlayer() + " drops in column " + col);
            DropResult result = game.dropDisc(col);
            System.out.println("Result: " + result);
            System.out.println(game.getBoardString());
            
            if (game.isGameOver()) {
                System.out.println("\nGame Over! Status: " + game.getStatus());
                break;
            }
        }
        
        // Test error cases
        System.out.println("\n=== Error Cases ===");
        
        // Try to play after game is over
        DropResult afterGame = game.dropDisc(0);
        System.out.println("Move after game over: " + afterGame);
        
        // Test with new game
        game.reset();
        System.out.println("\n=== New Game - Testing Errors ===");
        
        // Invalid column
        DropResult invalid = game.dropDisc(-1);
        System.out.println("Invalid column (-1): " + invalid);
        
        invalid = game.dropDisc(7);
        System.out.println("Invalid column (7): " + invalid);
        
        // Fill a column and try to add more
        System.out.println("\nFilling column 0...");
        for (int i = 0; i < 6; i++) {
            game.dropDisc(0);
            game.dropDisc(1); // Alternate to avoid winning
        }
        DropResult full = game.dropDisc(0);
        System.out.println("Column full: " + full);
    }
}

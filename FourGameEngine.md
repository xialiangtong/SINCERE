Problem: Build a Connect Four Game Engine

Implement the core rules and engine for Connect Four (no fancy UI required). The engine should support a standard 7 columns × 6 rows board, two players, and dropping discs into columns.

Core requirements
1) Data model

Design a representation for:

Board: 7×6 grid (or configurable width, height)

Cell state: empty / Player 1 / Player 2

Current player: whose turn it is

Game state: in-progress / win (which player) / draw

Minimum helpful API (example—language-agnostic):

newGame(width=7, height=6) -> Game

dropDisc(columnIndex) -> Result (or throws/returns error on invalid move)

getCell(row, col) -> Player?

status() -> GameStatus

2) Move validation

When a player drops a disc into a column:

Column index must be in range [0..6]

You can’t drop into a full column

Disc falls to the lowest empty row in that column

If the game is already over (win/draw), reject further moves

Error handling:

Return a clear error/enum/result (e.g., INVALID_COLUMN, COLUMN_FULL, GAME_OVER)

3) Win detection

After each valid move, check if that move created four in a row for the current player in any direction:

Horizontal

Vertical

Diagonal down-right / up-left

Diagonal up-right / down-left

Winning condition:

Any contiguous sequence of 4 of the same player

Performance expectation:

Efficient check based on the last move (preferred), rather than scanning entire board every time.

┌─────────────────────────────────────────────────────────────────────┐
│                          ConnectFour                                 │
├─────────────────────────────────────────────────────────────────────┤
│ - width: int                                                         │
│ - height: int                                                        │
│ - winLength: int                                                     │
│ - board: Player[][]                                                  │
│ - currentPlayer: Player                                              │
│ - status: GameStatus                                                 │
│ - movesCount: int                                                    │
├─────────────────────────────────────────────────────────────────────┤
│ + ConnectFour()                          // Default 7x6              │
│ + ConnectFour(width, height, winLength)  // Custom                   │
│                                                                      │
│ + dropDisc(column: int): DropResult      // Main action              │
│ + getCell(row, col): Player              // Query cell               │
│ + getStatus(): GameStatus                // Game state               │
│ + getCurrentPlayer(): Player             // Whose turn               │
│ + isGameOver(): boolean                                              │
│ + reset(): void                                                      │
│ + getBoardString(): String               // Debug display            │
│                                                                      │
│ - findLowestEmptyRow(column): int                                    │
│ - checkWin(row, col): boolean                                        │
│ - countDirection(row, col, dRow, dCol, player): int                  │
│ - countInDirection(row, col, dRow, dCol, player): int                │
│                                                                      │
│ + static newGame(): ConnectFour                                      │
│ + static newGame(width, height): ConnectFour                         │
└─────────────────────────────────────────────────────────────────────┘
                │
                │ uses
                ▼
┌───────────────────────────────────────────────────────────────────┐
│                                                                    │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐           │
│  │   Player    │    │  GameStatus  │    │ MoveResult  │           │
│  │   (enum)    │    │   (enum)     │    │  (enum)     │           │
│  ├─────────────┤    ├──────────────┤    ├─────────────┤           │
│  │ NONE        │    │ IN_PROGRESS  │    │ SUCCESS     │           │
│  │ PLAYER_1    │    │ PLAYER_1_WINS│    │INVALID_COLUMN│          │
│  │ PLAYER_2    │    │ PLAYER_2_WINS│    │ COLUMN_FULL │           │
│  ├─────────────┤    │ DRAW         │    │ GAME_OVER   │           │
│  │ +opponent() │    └──────────────┘    └─────────────┘           │
│  └─────────────┘                                                   │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                          DropResult                                  │
├─────────────────────────────────────────────────────────────────────┤
│ - result: MoveResult                                                 │
│ - row: int                    // Where disc landed (-1 if failed)    │
│ - status: GameStatus          // Updated game status                 │
├─────────────────────────────────────────────────────────────────────┤
│ + static success(row, status): DropResult                            │
│ + static failure(result, status): DropResult                         │
│ + getResult(): MoveResult                                            │
│ + getRow(): int                                                      │
│ + getStatus(): GameStatus                                            │
│ + isSuccess(): boolean                                               │
└─────────────────────────────────────────────────────────────────────┘
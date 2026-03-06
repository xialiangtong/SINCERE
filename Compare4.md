### Here is guess game : 

this is basically a 4-letter Wordle-style feedback with outputs 1 / 0 / -1.

The only tricky part is duplicate letters (so you don’t “credit” a guessed letter more times than it appears in the given word). The standard, interview-safe rule is:

Mark exact matches (1) first.

For the remaining unmatched letters in the given word, keep counts.

For each remaining unmatched position in the guess:

if its letter still has available count → 0 (misplaced) and decrement count

else → -1

That guarantees correct behavior with duplicates.

Example

given = "AABC", guess = "ADAA"

Exact matches:

pos0 A==A → result[0]=1

pos1 A!=D

pos2 B!=A

pos3 C!=A

Remaining letters from given (excluding exact matches): one A, one B, one C

Now evaluate remaining guess letters:

pos1 D not in remaining → -1

pos2 A exists (count A=1) → 0, decrement A to 0

pos3 A no longer available → -1

Result: [1, -1, 0, -1]

We are given a dictionary (which is a set of words, each word consists 4 letters). The given word is from the dictionary. The user's purpose is to find out the given word by guess, and the less time the better. The user can call compare method to get the result, then make another guess basing on the result.


The user has 7 attempts. Every attempted guess must be a 4-digit number from the dictionlary. If the guess is not from the dictionary, the guess is excluded from the attemps.
They either:wins (guessed the number correctly) loses (7th attempt is wrong) or get "tip" (the Result).

# My ideas about the solution (in Java)
1. need a top level class Compare4Game, fields : final int MAX_TRIES = 7, final Set<String> dictionary, int attemptCount, String target, GameState gameState
   in the construction, init the dictionary, and target from the inputs. 
2. define an Enum CompareStatus : MATCH(1), MISPLACED(0), MISMATCH(-1)
3. define an Enum GameState : IN_PROGRESS, WIN, LOSE
4. an inner class CompareResult, fields : String guess, String target, CompareStatus[] statuses (array of 4, one per position).
   methods : isExactMatch() — returns true when all 4 statuses are MATCH.
   (Keep `target` here so that history records are self-contained across different games.)
5. in the top class Compare4Game, 
   add a field : List<CompareResult> guessHistory -> to record each guess history (no need to record the invalid input which does not count)
   methods : boolean validateWord(String word) -> used for validate during the dictionary init
   boolean validateGuess(String guess) -> include the same function as validateWord and also check whether the guess is among the dictionary
   CompareResult judgeGuess(String guess) -> internally increments attemptCount, appends to guessHistory, and updates gameState (WIN if exact match, LOSE if attemptCount reaches MAX_TRIES, otherwise IN_PROGRESS)
   GameState getGameState()
   int getRemainingAttempts()
   List<CompareResult> getGuessHistory()
   private Map<Character,Integer> buildLetterCounts(String word, boolean[] matched) -> helper for the two-pass duplicate-letter algorithm
6. we want to make it CLI, so in the main method, provide user input scan, validateGuess the guess first, and print the error message to the user, to let them retry. Also provide key words for user to quit the game. If user's guess does not succeed, provide the CompareResult to them. Also shows how many times guess left to user.

note : change all the input word into UPPER CASE to ignore case sensitive.


# Tests
1. tests for all inner classes / Enums
2. tests for invalid input, including invalid words in dictionary init
3. tests for user input invalid word, input word not in the dictionary
4. tests for user input valid words, and hit the target from the 1st guess to the max time guess
5. tests for user's guess over the max without hit target
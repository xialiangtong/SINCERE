Implement a NUMBER guessing game. The user must guess a 4-digit number (without repeats). The user has 7 attempts. Every attempted guess must be a 4-digit number.
They either:wins (guessed the number correctly) loses (7th attempt is wrong) or get "tip": X bulls, Y cows, where: X is the number of correct digits in correct positions Y is the number of correct digits, but wrongly positioned.
For example, the number is "1234".Attempt 1290 will result in a tip: 2 bulls, 0 cows. Attempt 1234 will be WIN. Attempt 1562 will result in a tip: 1 bull, 1 cow.Comments It should be a CLI program.
0123 - an invalid number (can't start with 0) 1023 - is valid 1123 - is invalid (digits repeat). Every time a user starts a game, they should get a random number to guess.

Bonus:Print "Gutter ball!!!" if the user hits 0 bulls and 0 cows. Every time the user starts a game, they should get a DIFFERENT number to guess (never repeat the same number)!

Tip:More than implementation focus on setting up test cases first and be ready to write unit test cases class and test it.

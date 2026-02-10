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
Update the code
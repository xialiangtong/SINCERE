Problem: “Find” command over a file tree
You’re given an in-memory representation of a file system as a tree of nodes:

A node is either a Directory (contains children) or a File (leaf).

Each File has metadata such as:

name (e.g., "cat.png")

sizeBytes

lastModifiedEpochMillis (or similar)

optionally path / owner / createdAt (not required unless asked)

Task 1 — Basic search
Write a function that, given:

a root directory node

a matching condition (criteria)

returns a list of files that match.

Example criteria:

size > 10 MB

extension is .png

name contains "invoice"

(later) last modified within N days

Task 2 — The twist (core of the interview)
Make the “matching logic” abstract and extensible so that:

Other developers can add new filters (like “last modified date”) without changing your core search function.

That means: your find(...) traversal code should not have if (size...) else if (ext...) hard-coded.

Input / Output details (what they typically expect you to clarify)
Inputs
Directory root

Predicate<File> / Filter / Matcher strategy

(optionally) search options like:

include hidden files?

stop after first K matches?

follow symlinks? (usually ignored unless they bring it up)

max recursion depth?

Output
List<File> (or list of file paths, depending on representation)

Constraints / expectations
Tree can be large → traversal should be O(N) where N is total nodes.

Memory should be:

O(H) recursion stack (H = tree height)

plus the output list.

Avoid modifying the tree; it’s a read-only search.
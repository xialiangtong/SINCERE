Build a system that reads participants from a CSV file and generates valid gift exchange pairings (like Secret Santa). Each person is assigned exactly one other person to give a gift to, with certain constraints.

Requirements
Input
CSV file containing participants with their information
Constraints/rules for valid pairings
Output
Valid pairings where each person gives a gift to exactly one other person
Output as CSV or displayed matches
Constraints (Common Variants)
No self-matching - Person cannot be assigned to themselves
No immediate family - Family members cannot be matched together
No repeat from last year - Cannot have same pairing as previous year
Department restriction - Cannot match people from same team/department


┌─────────────────────────────────────────────────────────────────────┐
│                      GiftPairingSolution (top-level)                │
│                                                                     │
│  Fields:                                                            │
│    - pairingEngine: PairingEngine                                   │
│    - csvReader: CSVReader                                           │
│    - csvWriter: WriteResultToCSV                                    │
│    - lastYearPairings: List<Pairing>                                │
│    - generatedPairings: List<Pairing>                               │
│                                                                     │
│  Methods:                                                           │
│    + generatePairs(filename): List<Pairing>                         │
│    + generatePairsFromParticipants(List<Participant>): List<Pairing>│
│    + exportGeneratedPairs(filename): void                           │
│    + addPairingRule(PairingRule): void                               │
│    + removePairingRule(PairingRule): void                            │
├─────────────────────────────────────────────────────────────────────┤
│                         Inner Classes & Interfaces                  │
│                                                                     │
│  ┌──────────────────────┐    ┌──────────────────────────┐           │
│  │   Participant        │    │   Pairing                │           │
│  │                      │    │                          │           │
│  │  - id: String        │◄───│  - sender: Participant   │           │
│  │  - name: String      │    │  - receiver: Participant │           │
│  │  - email: String     │    └──────────────────────────┘           │
│  │  - family: String    │                 ▲                         │
│  │  - department: String│                 │                         │
│  └──────────────────────┘                 │                         │
│          ▲       ▲                        │                         │
│          │       │                        │                         │
│  ┌───────┴──┐ ┌──┴──────────────┐  ┌─────┴────────────────┐        │
│  │ CSVReader│ │ WriteResultToCSV│  │   PairingEngine      │        │
│  │          │ │                 │  │                      │        │
│  │ +readFrom│ │ +writeToFile()  │  │  - rules:            │        │
│  │  File()  │ │ +writeToString()│  │    List<PairingRule>  │        │
│  │ +readFrom│ │                 │  │                      │        │
│  │  String()│ └─────────────────┘  │  +addRule()          │        │
│  └──────────┘                      │  +removeRule()       │        │
│                                    │  +generatePairing    │        │
│                                    │   Result()           │        │
│                                    │  -findPairings()     │        │
│                                    │   (backtracking)     │        │
│                                    └──────────┬───────────┘        │
│                                               │ uses               │
│                                               ▼                    │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │        <<interface>> PairingRule                          │      │
│  │        + isPairingValid(Pairing): boolean                │      │
│  └──────────────┬──────────┬──────────┬─────────────────────┘      │
│                 │          │          │          │                  │
│        ┌────────┴───┐ ┌───┴────┐ ┌───┴────┐ ┌──┴──────────────┐   │
│        │SelfMatching│ │Family  │ │Depart- │ │RepeatingFrom   │   │
│        │Rule        │ │Match   │ │ment    │ │LastYearRule     │   │
│        │            │ │Rule    │ │Rule    │ │                 │   │
│        │ no self-   │ │ no same│ │ no same│ │ no repeat from  │   │
│        │ pairing    │ │ family │ │ dept   │ │ last year       │   │
│        └────────────┘ └────────┘ └────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

                          Data Flow
                          ─────────
   CSV Input File ──► CSVReader ──► List<Participant>
                                         │
                                         ▼
                                   PairingEngine
                                   (backtracking +
                                    PairingRule validation)
                                         │
                                         ▼
                                   List<Pairing>
                                         │
                                         ▼
                              WriteResultToCSV ──► CSV Output File



Here are follow-up questions (with brief answers) for the solution:

1. Why use backtracking instead of random shuffling?
Backtracking guarantees finding a valid solution if one exists. Random shuffling may loop indefinitely or miss valid solutions when constraints are tight. Backtracking explores possibilities systematically and prunes invalid branches early.

2. What is the time complexity of the backtracking approach?
Worst case is O(n!) — for each sender, we try all remaining receivers. However, constraint pruning (via isValidPairing) cuts branches early, making average performance much better. For typical gift exchange sizes (tens to low hundreds), this is perfectly acceptable.

3. Why use the Strategy pattern for pairing rules?
Each rule is encapsulated in its own class implementing PairingRule. This gives us:

Open/Closed Principle — add new rules without modifying existing code
Single Responsibility — each rule handles one constraint
Runtime flexibility — rules can be added/removed dynamically via addRule()/removeRule()
4. What happens when no valid pairing exists?
The engine returns an empty list. For example, if all participants share the same family and the FamilyMatchRule is active, backtracking exhausts all possibilities and returns []. The caller can check pairings.isEmpty() to handle this gracefully.

5. Why does isPairingValid take a Pairing object instead of two Participant parameters?
Encapsulating sender/receiver into a Pairing object:

Keeps the interface cohesive — one parameter instead of two
Some rules (like RepeatingFromLastYearRule) need the directional relationship (sender→receiver), which Pairing makes explicit
Easier to extend if Pairing gains metadata later
6. How does RepeatingFromLastYearRule differ from the other rules?
It's the only stateful rule — it holds a List<Pairing> of last year's pairings in its constructor. The other rules are stateless and only inspect the current pairing's participant attributes. This shows the flexibility of the Strategy pattern.

7. Why do FamilyMatchRule and DepartmentRule allow pairing when the field is null/empty?
A null or empty family/department means no restriction applies. If a participant hasn't provided family info, we shouldn't block them from being paired. This avoids false negatives and makes the system tolerant of incomplete data.

8. How would you handle a very large number of participants (e.g., 10,000)?
Replace backtracking with a graph-based matching algorithm (e.g., build a bipartite graph of valid sender→receiver edges, then find a perfect matching using Hopcroft-Karp in O(E√V))
Alternatively, use randomized shuffling with constraint retries, which works well in practice when constraints aren't too restrictive
9. How would you add a new constraint, e.g., "gender diversity" rule?
Simply create a new class:

Then register it: engine.addRule(new GenderDiversityRule()). Zero changes to existing code.

10. Why are all inner classes static?
Static inner classes don't hold a reference to the enclosing GiftPairingSolution instance. This means:

Lower memory overhead — no implicit outer reference
Independent instantiation — can be created without a GiftPairingSolution instance (useful in tests)
They're essentially just namespaced classes, which is appropriate here
11. What are the limitations of the CSV parsing approach?
The current parseLine uses simple split(","), which fails for:

Fields containing commas (e.g., "Doe, John")
Fields containing quotes or newlines
A production solution should use a proper CSV library (e.g., Apache Commons CSV or OpenCSV). Note that WriteResultToCSV already handles escaping on the output side but the reader doesn't handle quoted fields on input.

12. Is the solution thread-safe?
No. PairingEngine modifies shared state (rules list), and GiftPairingSolution mutates generatedPairings. For concurrent use, you'd need synchronization or make the engine immutable (pass rules at construction time and use an unmodifiable list).

13. Why separate CSVReader and WriteResultToCSV instead of one I/O class?
Follows the Single Responsibility Principle — reading and writing are distinct operations with different formats (input: participant rows; output: pairing rows). Separating them allows independent testing and evolution.

14. How would you unit test the backtracking logic in isolation?
Create a PairingEngine with controlled rules, pass a known participant list, and assert:

Correct number of pairings
No duplicate senders/receivers
All rules satisfied
The tests already do exactly this — see testPairingEngineGeneratesPairings and testPairingEngineWithMultipleRules in the test file.
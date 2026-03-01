import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Gift Pairing Solution (Secret Santa)
 *
 * Reads participants from CSV, generates valid gift exchange pairings
 * using backtracking with pluggable constraint rules.
 *
 * Design:
 * - Participant: pure data class (name, family, department)
 * - Pairing: sender → receiver relationship
 * - Rule (interface): Strategy pattern for constraints
 * - PairingEngine: backtracking solver with rule validation
 * - CSVReader / CSVWriter: I/O adapters
 */
public class GiftPairingSolution {

    private final PairingEngine engine;
    private final CSVReader csvReader;
    private final CSVWriter csvWriter;

    public GiftPairingSolution(PairingEngine engine) {
        this.engine = engine;
        this.csvReader = new CSVReader();
        this.csvWriter = new CSVWriter();
    }

    /** Read participants from CSV file, generate pairings. */
    public List<Pairing> generatePairings(String inputFile) throws IOException {
        List<Participant> participants = csvReader.readFromFile(inputFile);
        return engine.findPairings(participants);
    }

    /** Generate pairings from an in-memory list (useful for testing). */
    public List<Pairing> generatePairings(List<Participant> participants) {
        return engine.findPairings(participants);
    }

    /** Export pairings to a CSV file. */
    public void exportPairings(String outputFile, List<Pairing> pairings) throws IOException {
        csvWriter.writeToFile(outputFile, pairings);
    }

    /** Export pairings to a string (for display). */
    public String exportPairingsToString(List<Pairing> pairings) {
        return csvWriter.writeToString(pairings);
    }

    public PairingEngine getEngine() { return engine; }
    public CSVReader getCsvReader() { return csvReader; }
    public CSVWriter getCsvWriter() { return csvWriter; }

    // ==================== Participant (pure data class) ====================

    /**
     * Represents a participant in the gift exchange.
     * Pure data — no business logic. Rules inspect fields directly.
     */
    public static class Participant {
        private final String name;
        private final String family;
        private final String department;

        public Participant(String name, String family, String department) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Name must not be null or empty");
            }
            this.name = name.trim();
            this.family = family != null ? family.trim() : "";
            this.department = department != null ? department.trim() : "";
        }

        public String getName() { return name; }
        public String getFamily() { return family; }
        public String getDepartment() { return department; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Participant)) return false;
            Participant that = (Participant) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() { return name.hashCode(); }

        @Override
        public String toString() {
            return "Participant{name='" + name + "', family='" + family + "', dept='" + department + "'}";
        }
    }

    // ==================== Pairing ====================

    /** A directional gift pairing: sender gives a gift to receiver. */
    public static class Pairing {
        private final Participant sender;
        private final Participant receiver;

        public Pairing(Participant sender, Participant receiver) {
            if (sender == null || receiver == null) {
                throw new IllegalArgumentException("Sender and receiver must not be null");
            }
            this.sender = sender;
            this.receiver = receiver;
        }

        public Participant getSender() { return sender; }
        public Participant getReceiver() { return receiver; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pairing)) return false;
            Pairing that = (Pairing) o;
            return sender.equals(that.sender) && receiver.equals(that.receiver);
        }

        @Override
        public int hashCode() { return Objects.hash(sender, receiver); }

        @Override
        public String toString() { return sender.getName() + " → " + receiver.getName(); }
    }

    // ==================== Rule (interface) ====================

    /**
     * Strategy interface for pairing constraints.
     * Each rule encapsulates one validation concern.
     */
    public interface Rule {
        boolean isValid(Pairing pairing);
    }

    // ==================== NoSelfMatchRule ====================

    /** Prevents a participant from being paired with themselves. */
    public static class NoSelfMatchRule implements Rule {
        @Override
        public boolean isValid(Pairing pairing) {
            return !pairing.getSender().equals(pairing.getReceiver());
        }
    }

    // ==================== NoSameFamilyRule ====================

    /**
     * Prevents family members from being paired together.
     * If family is null/empty, the rule does not restrict.
     */
    public static class NoSameFamilyRule implements Rule {
        @Override
        public boolean isValid(Pairing pairing) {
            String sf = pairing.getSender().getFamily();
            String rf = pairing.getReceiver().getFamily();
            if (sf.isEmpty() || rf.isEmpty()) return true;
            return !sf.equalsIgnoreCase(rf);
        }
    }

    // ==================== NoSameDeptRule ====================

    /**
     * Prevents participants from the same department from being paired.
     * If department is null/empty, the rule does not restrict.
     */
    public static class NoSameDeptRule implements Rule {
        @Override
        public boolean isValid(Pairing pairing) {
            String sd = pairing.getSender().getDepartment();
            String rd = pairing.getReceiver().getDepartment();
            if (sd.isEmpty() || rd.isEmpty()) return true;
            return !sd.equalsIgnoreCase(rd);
        }
    }

    // ==================== NoRepeatFromLastYearRule ====================

    /**
     * Prevents the same pairing as last year.
     * Stateful: holds last year's pairings (passed via constructor).
     */
    public static class NoRepeatFromLastYearRule implements Rule {
        private final Set<String> lastYearKeys;

        public NoRepeatFromLastYearRule(List<Pairing> lastYearPairings) {
            this.lastYearKeys = new HashSet<>();
            if (lastYearPairings != null) {
                for (Pairing p : lastYearPairings) {
                    lastYearKeys.add(p.getSender().getName() + "→" + p.getReceiver().getName());
                }
            }
        }

        @Override
        public boolean isValid(Pairing pairing) {
            String key = pairing.getSender().getName() + "→" + pairing.getReceiver().getName();
            return !lastYearKeys.contains(key);
        }
    }

    // ==================== PairingEngine ====================

    /**
     * Generates valid pairings using backtracking.
     * Each participant appears exactly once as sender and once as receiver.
     * All registered rules must pass for each pairing.
     */
    public static class PairingEngine {
        private final List<Rule> rules = new ArrayList<>();

        public void addRule(Rule rule) {
            if (rule == null) throw new IllegalArgumentException("Rule must not be null");
            rules.add(rule);
        }

        public void removeRule(Rule rule) { rules.remove(rule); }

        public List<Rule> getRules() { return Collections.unmodifiableList(rules); }

        /**
         * Find valid pairings for the given participants.
         * Shuffles participants before backtracking so different runs
         * produce different valid pairings for the same input.
         * Returns empty list if no valid solution exists.
         */
        public List<Pairing> findPairings(List<Participant> participants) {
            if (participants == null || participants.size() < 2) {
                return Collections.emptyList();
            }

            // Shuffle to randomize pairing output — backtracking is deterministic
            // on ordering, so without this, same input always yields same result.
            List<Participant> shuffled = new ArrayList<>(participants);
            Collections.shuffle(shuffled);

            List<Pairing> result = new ArrayList<>();
            boolean[] receiverUsed = new boolean[shuffled.size()];

            if (backtrack(shuffled, 0, receiverUsed, result)) {
                return result;
            }
            return Collections.emptyList();
        }

        private boolean backtrack(List<Participant> participants, int senderIdx,
                                  boolean[] receiverUsed, List<Pairing> result) {
            if (senderIdx == participants.size()) return true;

            Participant sender = participants.get(senderIdx);
            for (int i = 0; i < participants.size(); i++) {
                if (receiverUsed[i]) continue;

                Pairing candidate = new Pairing(sender, participants.get(i));
                if (allRulesPass(candidate)) {
                    receiverUsed[i] = true;
                    result.add(candidate);

                    if (backtrack(participants, senderIdx + 1, receiverUsed, result)) {
                        return true;
                    }
                    result.remove(result.size() - 1);
                    receiverUsed[i] = false;
                }
            }
            return false;
        }

        private boolean allRulesPass(Pairing candidate) {
            for (Rule rule : rules) {
                if (!rule.isValid(candidate)) return false;
            }
            return true;
        }
    }

    // ==================== CSVReader ====================

    /**
     * Reads participants from CSV.
     * Expected format: name,family,department (with optional header row).
     */
    public static class CSVReader {

        public List<Participant> readFromFile(String filePath) throws IOException {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            return parseLines(lines);
        }

        public List<Participant> readFromString(String csvContent) {
            return parseLines(Arrays.asList(csvContent.split("\n")));
        }

        private List<Participant> parseLines(List<String> lines) {
            List<Participant> participants = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                if (i == 0 && line.toLowerCase().contains("name")) continue; // skip header

                String[] parts = line.split(",", -1);
                String name = parts.length > 0 ? parts[0].trim() : "";
                String family = parts.length > 1 ? parts[1].trim() : "";
                String department = parts.length > 2 ? parts[2].trim() : "";

                if (!name.isEmpty()) {
                    participants.add(new Participant(name, family, department));
                }
            }
            return participants;
        }
    }

    // ==================== CSVWriter ====================

    /** Writes pairings to CSV format: sender_name,receiver_name */
    public static class CSVWriter {

        public void writeToFile(String filePath, List<Pairing> pairings) throws IOException {
            Files.writeString(Paths.get(filePath), writeToString(pairings));
        }

        public String writeToString(List<Pairing> pairings) {
            StringBuilder sb = new StringBuilder();
            sb.append("sender,receiver\n");
            for (Pairing p : pairings) {
                sb.append(escapeCsv(p.getSender().getName()))
                  .append(",")
                  .append(escapeCsv(p.getReceiver().getName()))
                  .append("\n");
            }
            return sb.toString();
        }

        private String escapeCsv(String value) {
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }

    // ==================== Main: Demo / CLI ====================

    /**
     * Run: java GiftPairingSolution [input.csv] [output.csv]
     * No args = demo with sample data.
     */
    public static void main(String[] args) throws IOException {
        PairingEngine engine = new PairingEngine();
        engine.addRule(new NoSelfMatchRule());
        engine.addRule(new NoSameFamilyRule());

        GiftPairingSolution solution = new GiftPairingSolution(engine);

        if (args.length >= 1) {
            List<Pairing> pairings = solution.generatePairings(args[0]);
            if (pairings.isEmpty()) {
                System.out.println("No valid pairings found.");
                return;
            }
            System.out.println("=== Gift Pairings ===");
            for (Pairing p : pairings) System.out.println("  " + p);
            if (args.length >= 2) {
                solution.exportPairings(args[1], pairings);
                System.out.println("\nExported to: " + args[1]);
            }
        } else {
            System.out.println("=== Gift Pairing Demo ===\n");
            List<Participant> participants = Arrays.asList(
                new Participant("Alice", "Smith", "Engineering"),
                new Participant("Bob", "Smith", "Marketing"),
                new Participant("Charlie", "Jones", "Engineering"),
                new Participant("Diana", "Jones", "Marketing"),
                new Participant("Eve", "Brown", "Engineering"),
                new Participant("Frank", "Brown", "Marketing")
            );

            System.out.println("Participants:");
            for (Participant p : participants) System.out.println("  " + p);
            System.out.println("\nRules: NoSelfMatch, NoSameFamily");

            List<Pairing> pairings = solution.generatePairings(participants);
            if (pairings.isEmpty()) {
                System.out.println("\nNo valid pairings found!");
            } else {
                System.out.println("\nPairings:");
                for (Pairing p : pairings) System.out.println("  " + p);
                System.out.println("\nCSV Output:");
                System.out.print(solution.exportPairingsToString(pairings));
            }
        }
    }
}

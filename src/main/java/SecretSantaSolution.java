import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SecretSantaSolution {
    private final List<Participant> participants;
    private final List<Rule> rules;
    private final PairingEngine engine;

    // ======================== Constructor ========================

    SecretSantaSolution(List<Participant> participants, List<Rule> rules) {
        this.participants = new ArrayList<>(participants);
        this.rules = new ArrayList<>(rules);
        List<Participant> shuffledSenders = new ArrayList<>(participants);
        Collections.shuffle(shuffledSenders);
        this.engine = new PairingEngine(shuffledSenders, this.participants, this.rules);
    }

    // ======================== Public Methods ========================

    List<Participant> getParticipantList() {
        return Collections.unmodifiableList(participants);
    }

    List<Rule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    List<Pairing> generatePairings() {
        return engine.generatePairings();
    }

    // ======================== Inner Class: Participant ========================

    static class Participant {
        String id;
        String name;
        String email;
        String family;
        String department;

        Participant(String id, String name, String email, String family, String department) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.family = family;
            this.department = department;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Participant)) return false;
            return id.equals(((Participant) o).id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    // ======================== Inner Class: Pairing ========================

    static class Pairing {
        private final Participant sender;
        private final Participant receiver;

        Pairing(Participant sender, Participant receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }

        Participant getSender() { return sender; }
        Participant getReceiver() { return receiver; }

        @Override
        public String toString() {
            return sender.id + " → " + receiver.id;
        }
    }

    // ======================== Interface: Rule ========================

    interface Rule {
        boolean isValidPair(Participant sender, Participant receiver);
    }

    // ======================== Rule: NoSelfMatchingRule ========================

    static class NoSelfMatchingRule implements Rule {
        @Override
        public boolean isValidPair(Participant sender, Participant receiver) {
            return !sender.id.equals(receiver.id);
        }
    }

    // ======================== Rule: NoFamilyMatchingRule ========================

    static class NoFamilyMatchingRule implements Rule {
        @Override
        public boolean isValidPair(Participant sender, Participant receiver) {
            if (sender.family == null || sender.family.isEmpty()) return true;
            if (receiver.family == null || receiver.family.isEmpty()) return true;

            return !sender.family.equals(receiver.family);
        }
    }

    // ======================== Rule: NoRepeatedMatchingRule ========================

    static class NoRepeatedMatchingRule implements Rule {
        private final Set<String> lastYearPairs;

        NoRepeatedMatchingRule(List<Pairing> lastYearPairings) {
            this.lastYearPairs = new HashSet<>();
            for (Pairing p : lastYearPairings) {
                lastYearPairs.add(p.getSender().id + "->" + p.getReceiver().id);
            }
        }

        @Override
        public boolean isValidPair(Participant sender, Participant receiver) {
            return !lastYearPairs.contains(sender.id + "->" + receiver.id);
        }
    }

    // ======================== Rule: DepartmentRestrictRule ========================

    static class DepartmentRestrictRule implements Rule {
        @Override
        public boolean isValidPair(Participant sender, Participant receiver) {
            if (sender.department == null || sender.department.isEmpty()) return true;
            if (receiver.department == null || receiver.department.isEmpty()) return true;
            return !sender.department.equals(receiver.department);
        }
    }

    // ======================== Inner Class: PairingEngine ========================

    static class PairingEngine {
        private final List<Participant> senders;
        private final List<Participant> receivers;
        private final List<Rule> rules;

        PairingEngine(List<Participant> senders, List<Participant> receivers, List<Rule> rules) {
            this.senders = new ArrayList<>(senders);
            this.receivers = new ArrayList<>(receivers);
            this.rules = new ArrayList<>(rules);
        }

        void addRule(Rule rule) {
            rules.add(rule);
        }

        void removeRule(Rule rule) {
            rules.remove(rule);
        }

        List<Pairing> generatePairings() {
            List<Pairing> result = new ArrayList<>();
            Set<Participant> used = new HashSet<>();
            if (backtrack(0, result, used)) {
                return result;
            }
            return new ArrayList<>();
        }

        private boolean isValid(Participant sender, Participant receiver) {
            for (Rule rule : rules) {
                if (!rule.isValidPair(sender, receiver)) return false;
            }
            return true;
        }

        private boolean backtrack(int index, List<Pairing> result, Set<Participant> used) {
            if (index == senders.size()) {
                return true; // all senders paired successfully
            }

            Participant sender = senders.get(index);

            for (Participant receiver : receivers) {
                if (used.contains(receiver)) continue;
                if (!isValid(sender, receiver)) continue;

                // Choose: pair sender with receiver
                result.add(new Pairing(sender, receiver));
                used.add(receiver);

                // Recurse: try to pair the next sender
                if (backtrack(index + 1, result, used)) {
                    return true;
                }

                // Undo: remove the pair and try next receiver
                result.remove(result.size() - 1);
                used.remove(receiver);
            }

            return false; // no valid receiver found for this sender
        }
    }

    // ======================== Inner Class: CsvReader ========================

    static class CsvReader {

        List<Participant> readParticipants(String filePath) throws IOException {
            List<Participant> participants = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String header = br.readLine(); // skip header
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 5) {
                        participants.add(new Participant(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts[2].trim(),
                                parts[3].trim(),
                                parts[4].trim()
                        ));
                    }
                }
            }
            return participants;
        }

        List<Pairing> readPairings(String filePath, List<Participant> participants) throws IOException {
            Map<String, Participant> idMap = new HashMap<>();
            for (Participant p : participants) {
                idMap.put(p.id, p);
            }

            List<Pairing> pairings = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String header = br.readLine(); // skip header
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 2) {
                        Participant sender = idMap.get(parts[0].trim());
                        Participant receiver = idMap.get(parts[1].trim());
                        if (sender != null && receiver != null) {
                            pairings.add(new Pairing(sender, receiver));
                        }
                    }
                }
            }
            return pairings;
        }
    }

    // ======================== Inner Class: PairingCsvWriter ========================

    static class PairingCsvWriter {

        void write(String filePath, List<Pairing> pairings) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                bw.write("sender_id,sender_name,receiver_id,receiver_name");
                bw.newLine();
                for (Pairing p : pairings) {
                    bw.write(String.join(",",
                            p.getSender().id,
                            p.getSender().name,
                            p.getReceiver().id,
                            p.getReceiver().name
                    ));
                    bw.newLine();
                }
            }
        }
    }

    // ======================== Main ========================

    public static void main(String[] args) {
        try {
            CsvReader reader = new CsvReader();
            List<Participant> participants = reader.readParticipants("participants.csv");

            List<Rule> rules = new ArrayList<>();
            rules.add(new NoSelfMatchingRule());
            rules.add(new NoFamilyMatchingRule());
            rules.add(new DepartmentRestrictRule());

            // Optional: load last year pairings
            // List<Pairing> lastYear = reader.readPairings("last_year.csv", participants);
            // rules.add(new NoRepeatedMatchingRule(lastYear));

            SecretSantaSolution solution = new SecretSantaSolution(participants, rules);
            List<Pairing> pairings = solution.generatePairings();

            if (pairings.isEmpty()) {
                System.out.println("No valid pairing found!");
            } else {
                System.out.println("Generated pairings:");
                for (Pairing p : pairings) {
                    System.out.println("  " + p);
                }

                PairingCsvWriter writer = new PairingCsvWriter();
                writer.write("pairings_output.csv", pairings);
                System.out.println("Pairings written to pairings_output.csv");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

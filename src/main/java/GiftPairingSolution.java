import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class GiftPairingSolution {

    // Instance fields
    private PairingEngine pairingEngine;
    private CSVReader csvReader;
    private WriteResultToCSV csvWriter;
    private List<Pairing> lastYearPairings;
    private List<Pairing> generatedPairings;

    // Default constructor
    public GiftPairingSolution() {
        this(new ArrayList<>());
    }

    // Constructor with last year's pairings
    public GiftPairingSolution(List<Pairing> lastYearPairings) {
        this.pairingEngine = new PairingEngine();
        this.csvReader = new CSVReader();
        this.csvWriter = new WriteResultToCSV();
        this.lastYearPairings = lastYearPairings;
        this.generatedPairings = new ArrayList<>();
        
        // Add all default rules
        addDefaultRules();
    }

    private void addDefaultRules() {
        pairingEngine.addRule(new SelfMatchingRule());
        pairingEngine.addRule(new FamilyMatchRule());
        pairingEngine.addRule(new RepeatingFromLastYearRule(lastYearPairings));
        pairingEngine.addRule(new DepartmentRule());
    }

    public void addPairingRule(PairingRule rule) {
        pairingEngine.addRule(rule);
    }

    public void removePairingRule(PairingRule rule) {
        pairingEngine.removeRule(rule);
    }

    public List<Pairing> generatePairs(String filename) throws IOException {
        List<Participant> participants = csvReader.readFromFile(filename);
        generatedPairings = pairingEngine.generatePairingResult(participants);
        return generatedPairings;
    }

    public List<Pairing> generatePairsFromParticipants(List<Participant> participants) {
        generatedPairings = pairingEngine.generatePairingResult(participants);
        return generatedPairings;
    }

    public void exportGeneratedPairs(String filename) throws IOException {
        csvWriter.writeToFile(generatedPairings, filename);
    }

    public String exportGeneratedPairsToString() {
        return csvWriter.writeToString(generatedPairings);
    }

    public PairingEngine getPairingEngine() {
        return pairingEngine;
    }

    public CSVReader getCsvReader() {
        return csvReader;
    }

    public WriteResultToCSV getCsvWriter() {
        return csvWriter;
    }

    public List<Pairing> getGeneratedPairings() {
        return generatedPairings;
    }

    // Inner class to represent a participant
    public static class Participant {
        private String id;
        private String name;
        private String email;
        private String family;
        private String department;

        public Participant(String id, String name, String email, String family, String department) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.family = family;
            this.department = department;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getFamily() {
            return family;
        }

        public String getDepartment() {
            return department;
        }
    }

    // Inner class to read and parse CSV data
    public static class CSVReader {
        
        // Read participants from a CSV file
        public List<Participant> readFromFile(String filePath) throws IOException {
            List<Participant> participants = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                boolean isHeader = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue; // Skip header line
                    }
                    
                    Participant participant = parseLine(line);
                    if (participant != null) {
                        participants.add(participant);
                    }
                }
            }
            
            return participants;
        }
        
        // Read participants from a CSV string
        public List<Participant> readFromString(String csvContent) throws IOException {
            List<Participant> participants = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
                String line;
                boolean isHeader = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue; // Skip header line
                    }
                    
                    Participant participant = parseLine(line);
                    if (participant != null) {
                        participants.add(participant);
                    }
                }
            }
            
            return participants;
        }
        
        // Parse a single CSV line into a Participant
        private Participant parseLine(String line) {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            
            String[] fields = line.split(",", -1);
            
            if (fields.length < 5) {
                return null; // Invalid line
            }
            
            String id = fields[0].trim();
            String name = fields[1].trim();
            String email = fields[2].trim();
            String family = fields[3].trim();
            String department = fields[4].trim();
            
            return new Participant(id, name, email, family, department);
        }
    }

    // Inner class to write pairing results to CSV
    public static class WriteResultToCSV {
        
        private static final String HEADER = "sender_id,sender_name,sender_email,receiver_id,receiver_name,receiver_email";
        
        // Write pairings to a CSV file
        public void writeToFile(List<Pairing> pairings, String filePath) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(HEADER);
                writer.newLine();
                
                if (pairings != null) {
                    for (Pairing pairing : pairings) {
                        writer.write(formatPairing(pairing));
                        writer.newLine();
                    }
                }
            }
        }
        
        // Write pairings to a CSV string
        public String writeToString(List<Pairing> pairings) {
            StringBuilder sb = new StringBuilder();
            sb.append(HEADER).append("\n");
            
            if (pairings != null) {
                for (Pairing pairing : pairings) {
                    sb.append(formatPairing(pairing)).append("\n");
                }
            }
            
            return sb.toString();
        }
        
        // Format a single pairing as a CSV line
        private String formatPairing(Pairing pairing) {
            Participant sender = pairing.getSender();
            Participant receiver = pairing.getReceiver();
            
            return String.format("%s,%s,%s,%s,%s,%s",
                escapeCSV(sender.getId()),
                escapeCSV(sender.getName()),
                escapeCSV(sender.getEmail()),
                escapeCSV(receiver.getId()),
                escapeCSV(receiver.getName()),
                escapeCSV(receiver.getEmail()));
        }
        
        // Escape special characters for CSV
        private String escapeCSV(String value) {
            if (value == null) {
                return "";
            }
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }

    // Inner class to represent a pairing between sender and receiver
    public static class Pairing {
        private Participant sender;
        private Participant receiver;

        public Pairing(Participant sender, Participant receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }

        public Participant getSender() {
            return sender;
        }

        public Participant getReceiver() {
            return receiver;
        }
    }

    // Interface for pairing rules
    public interface PairingRule {
        boolean isPairingValid(Pairing pairing);
    }

    // Rule: No self-matching - Person cannot be assigned to themselves
    public static class SelfMatchingRule implements PairingRule {
        @Override
        public boolean isPairingValid(Pairing pairing) {
            return !pairing.getSender().getId().equals(pairing.getReceiver().getId());
        }
    }

    // Rule: No immediate family - Family members cannot be matched together
    public static class FamilyMatchRule implements PairingRule {
        @Override
        public boolean isPairingValid(Pairing pairing) {
            String senderFamily = pairing.getSender().getFamily();
            String receiverFamily = pairing.getReceiver().getFamily();
            
            // If either family is null or empty, allow the pairing
            if (senderFamily == null || senderFamily.isEmpty() ||
                receiverFamily == null || receiverFamily.isEmpty()) {
                return true;
            }
            
            return !senderFamily.equals(receiverFamily);
        }
    }

    // Rule: No repeat from last year - Cannot have same pairing as previous year
    public static class RepeatingFromLastYearRule implements PairingRule {
        private List<Pairing> lastYearPairings;

        public RepeatingFromLastYearRule(List<Pairing> lastYearPairings) {
            this.lastYearPairings = lastYearPairings;
        }

        @Override
        public boolean isPairingValid(Pairing pairing) {
            if (lastYearPairings == null || lastYearPairings.isEmpty()) {
                return true;
            }
            
            for (Pairing lastYearPairing : lastYearPairings) {
                if (lastYearPairing.getSender().getId().equals(pairing.getSender().getId()) &&
                    lastYearPairing.getReceiver().getId().equals(pairing.getReceiver().getId())) {
                    return false;
                }
            }
            return true;
        }
    }

    // Rule: Department restriction - Cannot match people from same team/department
    public static class DepartmentRule implements PairingRule {
        @Override
        public boolean isPairingValid(Pairing pairing) {
            String senderDepartment = pairing.getSender().getDepartment();
            String receiverDepartment = pairing.getReceiver().getDepartment();
            
            // If either department is null or empty, allow the pairing
            if (senderDepartment == null || senderDepartment.isEmpty() ||
                receiverDepartment == null || receiverDepartment.isEmpty()) {
                return true;
            }
            
            return !senderDepartment.equals(receiverDepartment);
        }
    }

    // Inner class to manage pairing rules and generate pairings
    public static class PairingEngine {
        private List<PairingRule> rules;

        public PairingEngine() {
            this.rules = new ArrayList<>();
        }

        public void addRule(PairingRule rule) {
            if (rule != null) {
                rules.add(rule);
            }
        }

        public void removeRule(PairingRule rule) {
            rules.remove(rule);
        }

        // Check if a pairing is valid against all rules
        private boolean isValidPairing(Pairing pairing) {
            for (PairingRule rule : rules) {
                if (!rule.isPairingValid(pairing)) {
                    return false;
                }
            }
            return true;
        }

        // Generate valid pairings for all participants
        public List<Pairing> generatePairingResult(List<Participant> participants) {
            List<Pairing> result = new ArrayList<>();
            
            if (participants == null || participants.size() < 2) {
                return result;
            }

            List<Participant> availableReceivers = new ArrayList<>(participants);
            List<Participant> senders = new ArrayList<>(participants);

            // Try to find valid pairings using backtracking
            if (findPairings(senders, availableReceivers, result, 0)) {
                return result;
            }

            // If no valid pairing found, return empty list
            return new ArrayList<>();
        }

        // Backtracking algorithm to find valid pairings
        private boolean findPairings(List<Participant> senders, List<Participant> availableReceivers,
                                      List<Pairing> result, int senderIndex) {
            if (senderIndex == senders.size()) {
                return true; // All senders have been paired
            }

            Participant sender = senders.get(senderIndex);

            for (int i = 0; i < availableReceivers.size(); i++) {
                Participant receiver = availableReceivers.get(i);
                Pairing pairing = new Pairing(sender, receiver);

                if (isValidPairing(pairing)) {
                    result.add(pairing);
                    availableReceivers.remove(i);

                    if (findPairings(senders, availableReceivers, result, senderIndex + 1)) {
                        return true;
                    }

                    // Backtrack
                    result.remove(result.size() - 1);
                    availableReceivers.add(i, receiver);
                }
            }

            return false; // No valid pairing found for this sender
        }
    }
}

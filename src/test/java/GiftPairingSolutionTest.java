import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GiftPairingSolutionTest {

    // ==================== Participant Tests ====================

    @Test
    public void testParticipantCreation() {
        GiftPairingSolution.Participant participant = new GiftPairingSolution.Participant("1", "John Doe", "john@example.com", "Doe", "Engineering");
        
        assertEquals("1", participant.getId());
        assertEquals("John Doe", participant.getName());
        assertEquals("john@example.com", participant.getEmail());
        assertEquals("Doe", participant.getFamily());
        assertEquals("Engineering", participant.getDepartment());
    }

    @Test
    public void testParticipantWithNullValues() {
        GiftPairingSolution.Participant participant = new GiftPairingSolution.Participant("2", "Jane", null, null, null);
        
        assertEquals("2", participant.getId());
        assertEquals("Jane", participant.getName());
        assertNull(participant.getEmail());
        assertNull(participant.getFamily());
        assertNull(participant.getDepartment());
    }

    // ==================== Pairing Tests ====================

    @Test
    public void testPairingCreation() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        assertEquals(sender, pairing.getSender());
        assertEquals(receiver, pairing.getReceiver());
    }

    // ==================== CSVReader Tests ====================

    @Test
    public void testCSVReaderFromString() throws IOException {
        String csvContent = "id,name,email,family,department\n" +
                           "1,John Doe,john@example.com,Doe,Engineering\n" +
                           "2,Jane Smith,jane@example.com,Smith,Marketing";
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromString(csvContent);
        
        assertEquals(2, participants.size());
        assertEquals("1", participants.get(0).getId());
        assertEquals("John Doe", participants.get(0).getName());
        assertEquals("2", participants.get(1).getId());
        assertEquals("Jane Smith", participants.get(1).getName());
    }

    @Test
    public void testCSVReaderWithEmptyLines() throws IOException {
        String csvContent = "id,name,email,family,department\n" +
                           "1,John Doe,john@example.com,Doe,Engineering\n" +
                           "\n" +
                           "2,Jane Smith,jane@example.com,Smith,Marketing";
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromString(csvContent);
        
        assertEquals(2, participants.size());
    }

    @Test
    public void testCSVReaderWithInvalidLine() throws IOException {
        String csvContent = "id,name,email,family,department\n" +
                           "1,John Doe,john@example.com,Doe,Engineering\n" +
                           "invalid,line\n" +
                           "2,Jane Smith,jane@example.com,Smith,Marketing";
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromString(csvContent);
        
        assertEquals(2, participants.size());
    }

    // ==================== WriteResultToCSV Tests ====================

    @Test
    public void testWriteResultToCSVToString() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John Doe", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane Smith", "jane@example.com", "Smith", "Marketing");
        
        List<GiftPairingSolution.Pairing> pairings = new ArrayList<>();
        pairings.add(new GiftPairingSolution.Pairing(sender, receiver));
        
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(pairings);
        
        assertTrue(result.contains("sender_id,sender_name,sender_email,receiver_id,receiver_name,receiver_email"));
        assertTrue(result.contains("1,John Doe,john@example.com,2,Jane Smith,jane@example.com"));
    }

    @Test
    public void testWriteResultToCSVToStringMultiplePairings() {
        List<GiftPairingSolution.Pairing> pairings = new ArrayList<>();
        pairings.add(new GiftPairingSolution.Pairing(
            new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Eng"),
            new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Mkt")));
        pairings.add(new GiftPairingSolution.Pairing(
            new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Mkt"),
            new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Eng")));
        
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(pairings);
        
        String[] lines = result.split("\n");
        assertEquals(3, lines.length); // header + 2 pairings
    }

    @Test
    public void testWriteResultToCSVWithNullPairings() {
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(null);
        
        assertTrue(result.contains("sender_id,sender_name,sender_email,receiver_id,receiver_name,receiver_email"));
        String[] lines = result.split("\n");
        assertEquals(1, lines.length); // only header
    }

    @Test
    public void testWriteResultToCSVWithEmptyPairings() {
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(new ArrayList<>());
        
        String[] lines = result.split("\n");
        assertEquals(1, lines.length); // only header
    }

    @Test
    public void testWriteResultToCSVEscapesCommas() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "Doe, John", "john@example.com", "Doe", "Eng");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Mkt");
        
        List<GiftPairingSolution.Pairing> pairings = new ArrayList<>();
        pairings.add(new GiftPairingSolution.Pairing(sender, receiver));
        
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(pairings);
        
        assertTrue(result.contains("\"Doe, John\""));
    }

    @Test
    public void testWriteResultToCSVEscapesQuotes() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John \"JD\" Doe", "john@example.com", "Doe", "Eng");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Mkt");
        
        List<GiftPairingSolution.Pairing> pairings = new ArrayList<>();
        pairings.add(new GiftPairingSolution.Pairing(sender, receiver));
        
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(pairings);
        
        assertTrue(result.contains("\"John \"\"JD\"\" Doe\""));
    }

    @Test
    public void testWriteResultToCSVHandlesNullValues() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", null, "Doe", "Eng");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Mkt");
        
        List<GiftPairingSolution.Pairing> pairings = new ArrayList<>();
        pairings.add(new GiftPairingSolution.Pairing(sender, receiver));
        
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        String result = writer.writeToString(pairings);
        
        // Should not throw exception, null should be converted to empty string
        assertTrue(result.contains("1,John,,2,Jane,jane@example.com"));
    }

    @TempDir
    Path tempDir;

    @Test
    public void testWriteResultToCSVToFile() throws IOException {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Eng");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Mkt");
        
        List<GiftPairingSolution.Pairing> pairings = new ArrayList<>();
        pairings.add(new GiftPairingSolution.Pairing(sender, receiver));
        
        Path outputFile = tempDir.resolve("output.csv");
        
        GiftPairingSolution.WriteResultToCSV writer = new GiftPairingSolution.WriteResultToCSV();
        writer.writeToFile(pairings, outputFile.toString());
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("sender_id,sender_name,sender_email,receiver_id,receiver_name,receiver_email"));
        assertTrue(content.contains("1,John,john@example.com,2,Jane,jane@example.com"));
    }

    // ==================== SelfMatchingRule Tests ====================

    @Test
    public void testSelfMatchingRuleRejectsSamePerson() {
        GiftPairingSolution.Participant person = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(person, person);
        
        GiftPairingSolution.SelfMatchingRule rule = new GiftPairingSolution.SelfMatchingRule();
        
        assertFalse(rule.isPairingValid(pairing));
    }

    @Test
    public void testSelfMatchingRuleAcceptsDifferentPeople() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.SelfMatchingRule rule = new GiftPairingSolution.SelfMatchingRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    // ==================== FamilyMatchRule Tests ====================

    @Test
    public void testFamilyMatchRuleRejectsSameFamily() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John Doe", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane Doe", "jane@example.com", "Doe", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.FamilyMatchRule rule = new GiftPairingSolution.FamilyMatchRule();
        
        assertFalse(rule.isPairingValid(pairing));
    }

    @Test
    public void testFamilyMatchRuleAcceptsDifferentFamily() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John Doe", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane Smith", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.FamilyMatchRule rule = new GiftPairingSolution.FamilyMatchRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    @Test
    public void testFamilyMatchRuleAcceptsNullFamily() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", null, "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.FamilyMatchRule rule = new GiftPairingSolution.FamilyMatchRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    @Test
    public void testFamilyMatchRuleAcceptsEmptyFamily() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.FamilyMatchRule rule = new GiftPairingSolution.FamilyMatchRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    // ==================== RepeatingFromLastYearRule Tests ====================

    @Test
    public void testRepeatingFromLastYearRuleRejectsSamePairing() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        
        List<GiftPairingSolution.Pairing> lastYearPairings = new ArrayList<>();
        lastYearPairings.add(new GiftPairingSolution.Pairing(sender, receiver));
        
        GiftPairingSolution.Pairing currentPairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.RepeatingFromLastYearRule rule = new GiftPairingSolution.RepeatingFromLastYearRule(lastYearPairings);
        
        assertFalse(rule.isPairingValid(currentPairing));
    }

    @Test
    public void testRepeatingFromLastYearRuleAcceptsDifferentPairing() {
        GiftPairingSolution.Participant john = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant jane = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Participant bob = new GiftPairingSolution.Participant("3", "Bob", "bob@example.com", "Brown", "Sales");
        
        List<GiftPairingSolution.Pairing> lastYearPairings = new ArrayList<>();
        lastYearPairings.add(new GiftPairingSolution.Pairing(john, jane));
        
        GiftPairingSolution.Pairing currentPairing = new GiftPairingSolution.Pairing(john, bob);
        
        GiftPairingSolution.RepeatingFromLastYearRule rule = new GiftPairingSolution.RepeatingFromLastYearRule(lastYearPairings);
        
        assertTrue(rule.isPairingValid(currentPairing));
    }

    @Test
    public void testRepeatingFromLastYearRuleAcceptsNullLastYear() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        
        GiftPairingSolution.Pairing currentPairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.RepeatingFromLastYearRule rule = new GiftPairingSolution.RepeatingFromLastYearRule(null);
        
        assertTrue(rule.isPairingValid(currentPairing));
    }

    @Test
    public void testRepeatingFromLastYearRuleAcceptsEmptyLastYear() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        
        GiftPairingSolution.Pairing currentPairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.RepeatingFromLastYearRule rule = new GiftPairingSolution.RepeatingFromLastYearRule(new ArrayList<>());
        
        assertTrue(rule.isPairingValid(currentPairing));
    }

    // ==================== DepartmentRule Tests ====================

    @Test
    public void testDepartmentRuleRejectsSameDepartment() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Engineering");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.DepartmentRule rule = new GiftPairingSolution.DepartmentRule();
        
        assertFalse(rule.isPairingValid(pairing));
    }

    @Test
    public void testDepartmentRuleAcceptsDifferentDepartment() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.DepartmentRule rule = new GiftPairingSolution.DepartmentRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    @Test
    public void testDepartmentRuleAcceptsNullDepartment() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", null);
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.DepartmentRule rule = new GiftPairingSolution.DepartmentRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    @Test
    public void testDepartmentRuleAcceptsEmptyDepartment() {
        GiftPairingSolution.Participant sender = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "");
        GiftPairingSolution.Participant receiver = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        GiftPairingSolution.Pairing pairing = new GiftPairingSolution.Pairing(sender, receiver);
        
        GiftPairingSolution.DepartmentRule rule = new GiftPairingSolution.DepartmentRule();
        
        assertTrue(rule.isPairingValid(pairing));
    }

    // ==================== PairingEngine Tests ====================

    @Test
    public void testPairingEngineGeneratesPairings() {
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        participants.add(new GiftPairingSolution.Participant("3", "Bob", "bob@example.com", "Brown", "Sales"));
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        assertEquals(3, pairings.size());
        
        // Verify no self-matching
        for (GiftPairingSolution.Pairing pairing : pairings) {
            assertNotEquals(pairing.getSender().getId(), pairing.getReceiver().getId());
        }
    }

    @Test
    public void testPairingEngineWithMultipleRules() {
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        participants.add(new GiftPairingSolution.Participant("3", "Bob", "bob@example.com", "Brown", "Sales"));
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        assertEquals(3, pairings.size());
    }

    @Test
    public void testPairingEngineReturnsEmptyListForInsufficientParticipants() {
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testPairingEngineReturnsEmptyListForNullInput() {
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(null);
        
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testPairingEngineRemoveRule() {
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        GiftPairingSolution.SelfMatchingRule rule = new GiftPairingSolution.SelfMatchingRule();
        
        engine.addRule(rule);
        engine.removeRule(rule);
        
        // After removing the self-matching rule, self-pairing should be allowed
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        GiftPairingSolution.Participant john = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        participants.add(john);
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        assertEquals(2, pairings.size());
    }

    // ==================== GiftPairingSolution Instance Tests ====================

    @Test
    public void testGiftPairingSolutionCreatesWithDefaultRules() {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        // Solution should be created successfully with default rules
        assertNotNull(solution);
        assertNotNull(solution.getPairingEngine());
        assertNotNull(solution.getCsvReader());
    }

    @Test
    public void testGiftPairingSolutionCreatesWithLastYearPairings() {
        List<GiftPairingSolution.Pairing> lastYearPairings = new ArrayList<>();
        GiftPairingSolution.Participant john = new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering");
        GiftPairingSolution.Participant jane = new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing");
        lastYearPairings.add(new GiftPairingSolution.Pairing(john, jane));
        
        GiftPairingSolution solution = new GiftPairingSolution(lastYearPairings);
        
        assertNotNull(solution);
    }

    @Test
    public void testGiftPairingSolutionAddAndRemoveRule() {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        GiftPairingSolution.PairingRule customRule = new GiftPairingSolution.PairingRule() {
            @Override
            public boolean isPairingValid(GiftPairingSolution.Pairing pairing) {
                return true;
            }
        };
        
        solution.addPairingRule(customRule);
        solution.removePairingRule(customRule);
        
        // Should not throw any exception
        assertNotNull(solution);
    }

    @Test
    public void testGiftPairingSolutionGeneratePairsFromParticipants() {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        participants.add(new GiftPairingSolution.Participant("3", "Bob", "bob@example.com", "Brown", "Sales"));
        participants.add(new GiftPairingSolution.Participant("4", "Alice", "alice@example.com", "White", "HR"));
        
        List<GiftPairingSolution.Pairing> pairings = solution.generatePairsFromParticipants(participants);
        
        assertEquals(4, pairings.size());
        verifyNoDuplicateSenders(pairings);
        verifyNoDuplicateReceivers(pairings);
        verifyNoSelfMatching(pairings);
    }

    @Test
    public void testGiftPairingSolutionGeneratePairsFromFile() throws IOException {
        GiftPairingSolution solution = new GiftPairingSolution();
        String filePath = getResourcePath("valid_participants.csv");
        
        List<GiftPairingSolution.Pairing> pairings = solution.generatePairs(filePath);
        
        assertEquals(5, pairings.size());
        verifyNoDuplicateSenders(pairings);
        verifyNoDuplicateReceivers(pairings);
        verifyNoSelfMatching(pairings);
    }

    @Test
    public void testGiftPairingSolutionExportGeneratedPairs() throws IOException {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        participants.add(new GiftPairingSolution.Participant("3", "Bob", "bob@example.com", "Brown", "Sales"));
        participants.add(new GiftPairingSolution.Participant("4", "Alice", "alice@example.com", "White", "HR"));
        
        solution.generatePairsFromParticipants(participants);
        
        Path outputFile = tempDir.resolve("exported_pairs.csv");
        solution.exportGeneratedPairs(outputFile.toString());
        
        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("sender_id,sender_name,sender_email,receiver_id,receiver_name,receiver_email"));
        // Should have 4 pairing lines plus header
        String[] lines = content.split("\n");
        assertEquals(5, lines.length);
    }

    @Test
    public void testGiftPairingSolutionExportGeneratedPairsToString() throws IOException {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        
        solution.generatePairsFromParticipants(participants);
        
        String result = solution.exportGeneratedPairsToString();
        
        assertTrue(result.contains("sender_id,sender_name,sender_email,receiver_id,receiver_name,receiver_email"));
        String[] lines = result.split("\n");
        assertEquals(3, lines.length); // header + 2 pairings
    }

    @Test
    public void testGiftPairingSolutionGetGeneratedPairings() {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        // Initially empty
        assertTrue(solution.getGeneratedPairings().isEmpty());
        
        List<GiftPairingSolution.Participant> participants = new ArrayList<>();
        participants.add(new GiftPairingSolution.Participant("1", "John", "john@example.com", "Doe", "Engineering"));
        participants.add(new GiftPairingSolution.Participant("2", "Jane", "jane@example.com", "Smith", "Marketing"));
        
        solution.generatePairsFromParticipants(participants);
        
        assertEquals(2, solution.getGeneratedPairings().size());
    }

    @Test
    public void testGiftPairingSolutionGetCsvWriter() {
        GiftPairingSolution solution = new GiftPairingSolution();
        
        assertNotNull(solution.getCsvWriter());
    }

    // ==================== CSV File Integration Tests ====================

    private String getResourcePath(String filename) {
        URL resource = getClass().getClassLoader().getResource(filename);
        if (resource != null) {
            return resource.getPath();
        }
        // Fallback for direct path
        return "src/test/resources/" + filename;
    }

    // Helper method to verify pairing constraints
    private void verifyNoDuplicateSenders(List<GiftPairingSolution.Pairing> pairings) {
        Set<String> senderIds = new HashSet<>();
        for (GiftPairingSolution.Pairing pairing : pairings) {
            String senderId = pairing.getSender().getId();
            assertFalse(senderIds.contains(senderId), "Duplicate sender found: " + senderId);
            senderIds.add(senderId);
        }
    }

    private void verifyNoDuplicateReceivers(List<GiftPairingSolution.Pairing> pairings) {
        Set<String> receiverIds = new HashSet<>();
        for (GiftPairingSolution.Pairing pairing : pairings) {
            String receiverId = pairing.getReceiver().getId();
            assertFalse(receiverIds.contains(receiverId), "Duplicate receiver found: " + receiverId);
            receiverIds.add(receiverId);
        }
    }

    private void verifyNoSelfMatching(List<GiftPairingSolution.Pairing> pairings) {
        for (GiftPairingSolution.Pairing pairing : pairings) {
            assertNotEquals(pairing.getSender().getId(), pairing.getReceiver().getId(),
                "Self-matching found for: " + pairing.getSender().getName());
        }
    }

    private void verifyNoFamilyMatching(List<GiftPairingSolution.Pairing> pairings) {
        for (GiftPairingSolution.Pairing pairing : pairings) {
            String senderFamily = pairing.getSender().getFamily();
            String receiverFamily = pairing.getReceiver().getFamily();
            if (senderFamily != null && !senderFamily.isEmpty() && 
                receiverFamily != null && !receiverFamily.isEmpty()) {
                assertNotEquals(senderFamily, receiverFamily,
                    "Family matching found: " + pairing.getSender().getName() + " -> " + pairing.getReceiver().getName());
            }
        }
    }

    private void verifyNoDepartmentMatching(List<GiftPairingSolution.Pairing> pairings) {
        for (GiftPairingSolution.Pairing pairing : pairings) {
            String senderDept = pairing.getSender().getDepartment();
            String receiverDept = pairing.getReceiver().getDepartment();
            if (senderDept != null && !senderDept.isEmpty() && 
                receiverDept != null && !receiverDept.isEmpty()) {
                assertNotEquals(senderDept, receiverDept,
                    "Department matching found: " + pairing.getSender().getName() + " -> " + pairing.getReceiver().getName());
            }
        }
    }

    // ==================== Valid Result Test Cases ====================

    @Test
    public void testGeneratePairsFromValidParticipantsFile() throws IOException {
        String filePath = getResourcePath("valid_participants.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should have 5 pairings (one for each participant)
        assertEquals(5, pairings.size());
        
        // Verify all constraints
        verifyNoDuplicateSenders(pairings);
        verifyNoDuplicateReceivers(pairings);
        verifyNoSelfMatching(pairings);
        verifyNoFamilyMatching(pairings);
        verifyNoDepartmentMatching(pairings);
    }

    @Test
    public void testGeneratePairsFromMixedFamiliesDepartmentsFile() throws IOException {
        String filePath = getResourcePath("mixed_families_departments.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should have 4 pairings
        assertEquals(4, pairings.size());
        
        // Verify all constraints
        verifyNoDuplicateSenders(pairings);
        verifyNoDuplicateReceivers(pairings);
        verifyNoSelfMatching(pairings);
        verifyNoFamilyMatching(pairings);
        verifyNoDepartmentMatching(pairings);
    }

    @Test
    public void testGeneratePairsFromEmptyFamilyDepartmentFile() throws IOException {
        String filePath = getResourcePath("empty_family_department.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should have 3 pairings - empty family/department should be treated as no restriction
        assertEquals(3, pairings.size());
        
        // Verify basic constraints
        verifyNoDuplicateSenders(pairings);
        verifyNoDuplicateReceivers(pairings);
        verifyNoSelfMatching(pairings);
    }

    @Test
    public void testGeneratePairsWithOnlySelfMatchingRule() throws IOException {
        String filePath = getResourcePath("all_same_family.csv");
        
        // With only self-matching rule, same family should be allowed
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should succeed since only self-matching is restricted
        assertEquals(3, pairings.size());
        verifyNoSelfMatching(pairings);
    }

    @Test
    public void testGeneratePairsWithOnlyFamilyRule() throws IOException {
        String filePath = getResourcePath("all_same_department.csv");
        
        // With only family rule, same department should be allowed
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should succeed since department is not restricted
        assertEquals(3, pairings.size());
        verifyNoSelfMatching(pairings);
        verifyNoFamilyMatching(pairings);
    }

    @Test
    public void testGeneratePairsWithOnlyDepartmentRule() throws IOException {
        String filePath = getResourcePath("all_same_family.csv");
        
        // With only department rule, same family should be allowed
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should succeed since family is not restricted
        assertEquals(3, pairings.size());
        verifyNoSelfMatching(pairings);
        verifyNoDepartmentMatching(pairings);
    }

    // ==================== Null/Empty Result Test Cases ====================

    @Test
    public void testGeneratePairsFromSingleParticipantFile() throws IOException {
        String filePath = getResourcePath("single_participant.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should return empty list - cannot pair a single person
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testGeneratePairsFailsForTwoParticipantsSameFamily() throws IOException {
        String filePath = getResourcePath("same_family_two_participants.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should return empty list - only 2 people from same family cannot be paired
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testGeneratePairsFailsForTwoParticipantsSameFamilyAndDepartment() throws IOException {
        String filePath = getResourcePath("same_family_same_department.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should return empty list - both family and department rules violated
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testGeneratePairsFailsForAllSameFamilyAndDepartment() throws IOException {
        String filePath = getResourcePath("all_same_family_department.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should return empty list - all same family and department
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testGeneratePairsFailsForAllSameDepartmentWithDepartmentRule() throws IOException {
        String filePath = getResourcePath("all_same_department.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should return empty list - all same department
        assertTrue(pairings.isEmpty());
    }

    @Test
    public void testGeneratePairsFailsForAllSameFamilyWithFamilyRule() throws IOException {
        String filePath = getResourcePath("all_same_family.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Should return empty list - all same family
        assertTrue(pairings.isEmpty());
    }

    // ==================== Edge Case Test Cases ====================

    @Test
    public void testCSVReaderFromFile() throws IOException {
        String filePath = getResourcePath("valid_participants.csv");
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        
        assertEquals(5, participants.size());
        assertEquals("John Doe", participants.get(0).getName());
        assertEquals("john@example.com", participants.get(0).getEmail());
        assertEquals("Doe", participants.get(0).getFamily());
        assertEquals("Engineering", participants.get(0).getDepartment());
    }

    @Test
    public void testAllRulesAppliedTogether() throws IOException {
        String filePath = getResourcePath("valid_participants.csv");
        
        GiftPairingSolution.PairingEngine engine = new GiftPairingSolution.PairingEngine();
        engine.addRule(new GiftPairingSolution.SelfMatchingRule());
        engine.addRule(new GiftPairingSolution.FamilyMatchRule());
        engine.addRule(new GiftPairingSolution.DepartmentRule());
        
        GiftPairingSolution.CSVReader reader = new GiftPairingSolution.CSVReader();
        List<GiftPairingSolution.Participant> participants = reader.readFromFile(filePath);
        List<GiftPairingSolution.Pairing> pairings = engine.generatePairingResult(participants);
        
        // Verify all rules are applied
        assertEquals(5, pairings.size());
        
        for (GiftPairingSolution.Pairing pairing : pairings) {
            // Self-matching rule
            assertNotEquals(pairing.getSender().getId(), pairing.getReceiver().getId());
            
            // Family rule
            String senderFamily = pairing.getSender().getFamily();
            String receiverFamily = pairing.getReceiver().getFamily();
            if (senderFamily != null && !senderFamily.isEmpty() && 
                receiverFamily != null && !receiverFamily.isEmpty()) {
                assertNotEquals(senderFamily, receiverFamily);
            }
            
            // Department rule
            String senderDept = pairing.getSender().getDepartment();
            String receiverDept = pairing.getReceiver().getDepartment();
            if (senderDept != null && !senderDept.isEmpty() && 
                receiverDept != null && !receiverDept.isEmpty()) {
                assertNotEquals(senderDept, receiverDept);
            }
        }
    }
}

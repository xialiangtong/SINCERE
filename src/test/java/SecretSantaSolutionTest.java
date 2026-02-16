import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecretSantaSolution Tests")
public class SecretSantaSolutionTest {

    // ======================== Helpers ========================

    private SecretSantaSolution.Participant p(String id, String name, String family, String dept) {
        return new SecretSantaSolution.Participant(id, name, id + "@test.com", family, dept);
    }

    private List<SecretSantaSolution.Participant> twoParticipantsDiffFamilyDept() {
        return Arrays.asList(
                p("1", "Alice", "FamA", "Eng"),
                p("2", "Bob", "FamB", "Sales")
        );
    }

    private List<SecretSantaSolution.Participant> threeParticipantsDiffFamilyDept() {
        return Arrays.asList(
                p("1", "Alice", "FamA", "Eng"),
                p("2", "Bob", "FamB", "Sales"),
                p("3", "Carol", "FamC", "HR")
        );
    }

    private List<SecretSantaSolution.Participant> fourParticipants() {
        return Arrays.asList(
                p("1", "Alice", "FamA", "Eng"),
                p("2", "Bob", "FamB", "Sales"),
                p("3", "Carol", "FamC", "HR"),
                p("4", "Dave", "FamD", "Eng")
        );
    }

    // ==================== Participant Tests ====================

    @Nested
    @DisplayName("Participant Tests")
    class ParticipantTests {

        @Test
        @DisplayName("participant stores all fields")
        void testParticipantFields() {
            SecretSantaSolution.Participant p = new SecretSantaSolution.Participant(
                    "1", "Alice", "alice@test.com", "FamA", "Eng");
            assertEquals("1", p.id);
            assertEquals("Alice", p.name);
            assertEquals("alice@test.com", p.email);
            assertEquals("FamA", p.family);
            assertEquals("Eng", p.department);
        }

        @Test
        @DisplayName("equals based on id")
        void testEquals() {
            SecretSantaSolution.Participant p1 = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant p2 = p("1", "Different", "FamB", "Sales");
            assertEquals(p1, p2);
        }

        @Test
        @DisplayName("not equal with different id")
        void testNotEqual() {
            SecretSantaSolution.Participant p1 = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant p2 = p("2", "Alice", "FamA", "Eng");
            assertNotEquals(p1, p2);
        }

        @Test
        @DisplayName("hashCode based on id")
        void testHashCode() {
            SecretSantaSolution.Participant p1 = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant p2 = p("1", "Bob", "FamB", "Sales");
            assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        @DisplayName("toString includes name and id")
        void testToString() {
            SecretSantaSolution.Participant p = p("1", "Alice", "FamA", "Eng");
            assertTrue(p.toString().contains("Alice"));
            assertTrue(p.toString().contains("1"));
        }
    }

    // ==================== Pairing Tests ====================

    @Nested
    @DisplayName("Pairing Tests")
    class PairingTests {

        @Test
        @DisplayName("pairing stores sender and receiver")
        void testPairingFields() {
            SecretSantaSolution.Participant sender = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant receiver = p("2", "Bob", "FamB", "Sales");
            SecretSantaSolution.Pairing pairing = new SecretSantaSolution.Pairing(sender, receiver);
            assertEquals(sender, pairing.getSender());
            assertEquals(receiver, pairing.getReceiver());
        }

        @Test
        @DisplayName("toString shows sender and receiver")
        void testToString() {
            SecretSantaSolution.Participant sender = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant receiver = p("2", "Bob", "FamB", "Sales");
            SecretSantaSolution.Pairing pairing = new SecretSantaSolution.Pairing(sender, receiver);
            String str = pairing.toString();
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
        }
    }

    // ==================== NoSelfMatchingRule Tests ====================

    @Nested
    @DisplayName("NoSelfMatchingRule Tests")
    class NoSelfMatchingRuleTests {

        @Test
        @DisplayName("rejects self-matching")
        void testRejectsSelf() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoSelfMatchingRule();
            SecretSantaSolution.Participant p = p("1", "Alice", "FamA", "Eng");
            assertFalse(rule.isValidPair(p, p));
        }

        @Test
        @DisplayName("allows different participants")
        void testAllowsDifferent() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoSelfMatchingRule();
            assertFalse(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("1", "Alice", "FamA", "Eng")));
            assertTrue(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("2", "Bob", "FamB", "Sales")));
        }
    }

    // ==================== NoFamilyMatchingRule Tests ====================

    @Nested
    @DisplayName("NoFamilyMatchingRule Tests")
    class NoFamilyMatchingRuleTests {

        @Test
        @DisplayName("rejects same family")
        void testRejectsSameFamily() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoFamilyMatchingRule();
            assertFalse(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("2", "Bob", "FamA", "Sales")));
        }

        @Test
        @DisplayName("allows different family")
        void testAllowsDifferentFamily() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoFamilyMatchingRule();
            assertTrue(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("2", "Bob", "FamB", "Sales")));
        }

        @Test
        @DisplayName("allows when sender family is null")
        void testAllowsNullFamily() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoFamilyMatchingRule();
            assertTrue(rule.isValidPair(p("1", "Alice", null, "Eng"), p("2", "Bob", "FamB", "Sales")));
        }

        @Test
        @DisplayName("allows when sender family is empty")
        void testAllowsEmptyFamily() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoFamilyMatchingRule();
            assertTrue(rule.isValidPair(p("1", "Alice", "", "Eng"), p("2", "Bob", "FamB", "Sales")));
        }
    }

    // ==================== NoRepeatedMatchingRule Tests ====================

    @Nested
    @DisplayName("NoRepeatedMatchingRule Tests")
    class NoRepeatedMatchingRuleTests {

        @Test
        @DisplayName("rejects repeated pairing from last year")
        void testRejectsRepeated() {
            SecretSantaSolution.Participant alice = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant bob = p("2", "Bob", "FamB", "Sales");
            List<SecretSantaSolution.Pairing> lastYear = List.of(new SecretSantaSolution.Pairing(alice, bob));
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoRepeatedMatchingRule(lastYear);
            assertFalse(rule.isValidPair(alice, bob));
        }

        @Test
        @DisplayName("allows non-repeated pairing")
        void testAllowsNonRepeated() {
            SecretSantaSolution.Participant alice = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant bob = p("2", "Bob", "FamB", "Sales");
            SecretSantaSolution.Participant carol = p("3", "Carol", "FamC", "HR");
            List<SecretSantaSolution.Pairing> lastYear = List.of(new SecretSantaSolution.Pairing(alice, bob));
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoRepeatedMatchingRule(lastYear);
            assertTrue(rule.isValidPair(alice, carol));
        }

        @Test
        @DisplayName("allows reverse of last year pairing")
        void testAllowsReverse() {
            SecretSantaSolution.Participant alice = p("1", "Alice", "FamA", "Eng");
            SecretSantaSolution.Participant bob = p("2", "Bob", "FamB", "Sales");
            List<SecretSantaSolution.Pairing> lastYear = List.of(new SecretSantaSolution.Pairing(alice, bob));
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoRepeatedMatchingRule(lastYear);
            assertTrue(rule.isValidPair(bob, alice)); // reverse is ok
        }

        @Test
        @DisplayName("empty last year allows all pairings")
        void testEmptyLastYear() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.NoRepeatedMatchingRule(Collections.emptyList());
            assertTrue(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("2", "Bob", "FamB", "Sales")));
        }
    }

    // ==================== DepartmentRestrictRule Tests ====================

    @Nested
    @DisplayName("DepartmentRestrictRule Tests")
    class DepartmentRestrictRuleTests {

        @Test
        @DisplayName("rejects same department")
        void testRejectsSameDept() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.DepartmentRestrictRule();
            assertFalse(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("2", "Bob", "FamB", "Eng")));
        }

        @Test
        @DisplayName("allows different department")
        void testAllowsDifferentDept() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.DepartmentRestrictRule();
            assertTrue(rule.isValidPair(p("1", "Alice", "FamA", "Eng"), p("2", "Bob", "FamB", "Sales")));
        }

        @Test
        @DisplayName("allows when sender department is null")
        void testAllowsNullDept() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.DepartmentRestrictRule();
            assertTrue(rule.isValidPair(p("1", "Alice", "FamA", null), p("2", "Bob", "FamB", "Eng")));
        }

        @Test
        @DisplayName("allows when sender department is empty")
        void testAllowsEmptyDept() {
            SecretSantaSolution.Rule rule = new SecretSantaSolution.DepartmentRestrictRule();
            assertTrue(rule.isValidPair(p("1", "Alice", "FamA", ""), p("2", "Bob", "FamB", "Eng")));
        }
    }

    // ==================== PairingEngine Tests ====================

    @Nested
    @DisplayName("PairingEngine Tests")
    class PairingEngineTests {

        @Test
        @DisplayName("generate pairing with 2 participants — no self match")
        void testTwoParticipants() {
            List<SecretSantaSolution.Participant> participants = twoParticipantsDiffFamilyDept();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(2, pairings.size());
        }

        @Test
        @DisplayName("generate pairing with 3 participants")
        void testThreeParticipants() {
            List<SecretSantaSolution.Participant> participants = threeParticipantsDiffFamilyDept();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(3, pairings.size());
        }

        @Test
        @DisplayName("generate pairing with 4 participants")
        void testFourParticipants() {
            List<SecretSantaSolution.Participant> participants = fourParticipants();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(4, pairings.size());
        }

        @Test
        @DisplayName("each sender appears exactly once")
        void testEachSenderOnce() {
            List<SecretSantaSolution.Participant> participants = fourParticipants();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            Set<String> senderIds = new HashSet<>();
            for (SecretSantaSolution.Pairing p : pairings) {
                senderIds.add(p.getSender().id);
            }
            assertEquals(4, senderIds.size());
        }

        @Test
        @DisplayName("each receiver appears exactly once")
        void testEachReceiverOnce() {
            List<SecretSantaSolution.Participant> participants = fourParticipants();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            Set<String> receiverIds = new HashSet<>();
            for (SecretSantaSolution.Pairing p : pairings) {
                receiverIds.add(p.getReceiver().id);
            }
            assertEquals(4, receiverIds.size());
        }

        @Test
        @DisplayName("no self-matching in result")
        void testNoSelfMatching() {
            List<SecretSantaSolution.Participant> participants = fourParticipants();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().id, p.getReceiver().id);
            }
        }

        @Test
        @DisplayName("no family matching in result")
        void testNoFamilyMatching() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamA", "Sales"),
                    p("3", "Carol", "FamB", "HR"),
                    p("4", "Dave", "FamB", "Eng")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule()
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(4, pairings.size());
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().family, p.getReceiver().family);
            }
        }

        @Test
        @DisplayName("no department matching in result")
        void testNoDepartmentMatching() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamB", "Sales"),
                    p("3", "Carol", "FamC", "Eng"),
                    p("4", "Dave", "FamD", "Sales")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.DepartmentRestrictRule()
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(4, pairings.size());
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().department, p.getReceiver().department);
            }
        }

        @Test
        @DisplayName("no repeated matching from last year")
        void testNoRepeatedMatching() {
            List<SecretSantaSolution.Participant> participants = threeParticipantsDiffFamilyDept();
            SecretSantaSolution.Participant alice = participants.get(0);
            SecretSantaSolution.Participant bob = participants.get(1);
            List<SecretSantaSolution.Pairing> lastYear = List.of(new SecretSantaSolution.Pairing(alice, bob));
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoRepeatedMatchingRule(lastYear)
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(3, pairings.size());
            for (SecretSantaSolution.Pairing p : pairings) {
                assertFalse(p.getSender().id.equals("1") && p.getReceiver().id.equals("2"),
                        "Alice→Bob should not repeat from last year");
            }
        }

        @Test
        @DisplayName("combination of all rules")
        void testAllRulesCombined() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamB", "Sales"),
                    p("3", "Carol", "FamC", "HR"),
                    p("4", "Dave", "FamD", "Ops")
            );
            SecretSantaSolution.Participant alice = participants.get(0);
            SecretSantaSolution.Participant bob = participants.get(1);
            List<SecretSantaSolution.Pairing> lastYear = List.of(new SecretSantaSolution.Pairing(alice, bob));
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule(),
                    new SecretSantaSolution.DepartmentRestrictRule(),
                    new SecretSantaSolution.NoRepeatedMatchingRule(lastYear)
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(4, pairings.size());
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().id, p.getReceiver().id, "no self match");
                assertNotEquals(p.getSender().family, p.getReceiver().family, "no family match");
                assertNotEquals(p.getSender().department, p.getReceiver().department, "no dept match");
                assertFalse(p.getSender().id.equals("1") && p.getReceiver().id.equals("2"), "no repeat");
            }
        }

        @Test
        @DisplayName("returns empty when no valid pairing exists — all same family")
        void testNoValidPairingAllSameFamily() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamA", "Sales")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule()
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertTrue(pairings.isEmpty());
        }

        @Test
        @DisplayName("returns empty when single participant with no-self rule")
        void testSingleParticipant() {
            List<SecretSantaSolution.Participant> participants = List.of(p("1", "Alice", "FamA", "Eng"));
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertTrue(pairings.isEmpty());
        }

        @Test
        @DisplayName("addRule dynamically adds a rule")
        void testAddRule() {
            List<SecretSantaSolution.Participant> participants = twoParticipantsDiffFamilyDept();
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(
                    participants, participants, new ArrayList<>());
            engine.addRule(new SecretSantaSolution.NoSelfMatchingRule());
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(2, pairings.size());
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().id, p.getReceiver().id);
            }
        }

        @Test
        @DisplayName("removeRule dynamically removes a rule")
        void testRemoveRule() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamA", "Sales")
            );
            SecretSantaSolution.NoFamilyMatchingRule familyRule = new SecretSantaSolution.NoFamilyMatchingRule();
            List<SecretSantaSolution.Rule> rules = new ArrayList<>(Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    familyRule
            ));
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            // With family rule, no valid pairing
            assertTrue(engine.generatePairings().isEmpty());
            // Remove family rule, pairing should work
            engine.removeRule(familyRule);
            assertEquals(2, engine.generatePairings().size());
        }
    }

    // ==================== CsvReader Tests ====================

    @Nested
    @DisplayName("CsvReader Tests")
    class CsvReaderTests {

        @Test
        @DisplayName("reads participants from valid CSV")
        void testReadParticipants(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("participants.csv");
            Files.writeString(csv, "id,name,email,family,department\n1,Alice,alice@test.com,FamA,Eng\n2,Bob,bob@test.com,FamB,Sales\n");
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Participant> participants = reader.readParticipants(csv.toString());
            assertEquals(2, participants.size());
            assertEquals("Alice", participants.get(0).name);
            assertEquals("Bob", participants.get(1).name);
        }

        @Test
        @DisplayName("reads pairings from valid CSV")
        void testReadPairings(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("pairings.csv");
            Files.writeString(csv, "sender_id,receiver_id\n1,2\n2,1\n");
            List<SecretSantaSolution.Participant> participants = twoParticipantsDiffFamilyDept();
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Pairing> pairings = reader.readPairings(csv.toString(), participants);
            assertEquals(2, pairings.size());
        }

        @Test
        @DisplayName("empty CSV returns empty list")
        void testEmptyCsv(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("empty.csv");
            Files.writeString(csv, "id,name,email,family,department\n");
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Participant> participants = reader.readParticipants(csv.toString());
            assertTrue(participants.isEmpty());
        }

        @Test
        @DisplayName("CSV with fewer than 5 columns skips invalid rows")
        void testInvalidColumns(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("bad.csv");
            Files.writeString(csv, "id,name,email,family,department\n1,Alice,alice@test.com\n2,Bob,bob@test.com,FamB,Sales\n");
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Participant> participants = reader.readParticipants(csv.toString());
            assertEquals(1, participants.size());
            assertEquals("Bob", participants.get(0).name);
        }

        @Test
        @DisplayName("non-existent file throws IOException")
        void testNonExistentFile() {
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            assertThrows(IOException.class, () -> reader.readParticipants("nonexistent.csv"));
        }

        @Test
        @DisplayName("header-only CSV returns empty list")
        void testHeaderOnlyCsv(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("header_only.csv");
            Files.writeString(csv, "id,name,email,family,department");
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Participant> participants = reader.readParticipants(csv.toString());
            assertTrue(participants.isEmpty());
        }

        @Test
        @DisplayName("pairings CSV with unknown ids are skipped")
        void testPairingsUnknownIds(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("pairings.csv");
            Files.writeString(csv, "sender_id,receiver_id\n99,100\n1,2\n");
            List<SecretSantaSolution.Participant> participants = twoParticipantsDiffFamilyDept();
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Pairing> pairings = reader.readPairings(csv.toString(), participants);
            assertEquals(1, pairings.size()); // only 1→2 is valid
        }
    }

    // ==================== PairingCsvWriter Tests ====================

    @Nested
    @DisplayName("PairingCsvWriter Tests")
    class PairingCsvWriterTests {

        @Test
        @DisplayName("writes pairings to CSV")
        void testWritePairings(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("output.csv");
            List<SecretSantaSolution.Participant> participants = twoParticipantsDiffFamilyDept();
            List<SecretSantaSolution.Pairing> pairings = Arrays.asList(
                    new SecretSantaSolution.Pairing(participants.get(0), participants.get(1)),
                    new SecretSantaSolution.Pairing(participants.get(1), participants.get(0))
            );
            SecretSantaSolution.PairingCsvWriter writer = new SecretSantaSolution.PairingCsvWriter();
            writer.write(csv.toString(), pairings);

            List<String> lines = Files.readAllLines(csv);
            assertEquals(3, lines.size()); // header + 2 data rows
            assertTrue(lines.get(0).contains("sender_id"));
            assertTrue(lines.get(1).contains("Alice"));
            assertTrue(lines.get(2).contains("Bob"));
        }

        @Test
        @DisplayName("writes empty pairings — header only")
        void testWriteEmptyPairings(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("output.csv");
            SecretSantaSolution.PairingCsvWriter writer = new SecretSantaSolution.PairingCsvWriter();
            writer.write(csv.toString(), Collections.emptyList());

            List<String> lines = Files.readAllLines(csv);
            assertEquals(1, lines.size()); // header only
        }
    }

    // ==================== Corner Case Tests ====================

    @Nested
    @DisplayName("Corner Case Tests")
    class CornerCaseTests {

        @Test
        @DisplayName("empty participant list returns empty pairings")
        void testEmptyParticipants() {
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(
                    Collections.emptyList(), Collections.emptyList(),
                    List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertTrue(pairings.isEmpty());
        }

        @Test
        @DisplayName("all participants in same department — dept rule fails")
        void testAllSameDepartment() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamB", "Eng"),
                    p("3", "Carol", "FamC", "Eng")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.DepartmentRestrictRule()
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertTrue(pairings.isEmpty());
        }

        @Test
        @DisplayName("no rules — everyone can match anyone including self")
        void testNoRules() {
            List<SecretSantaSolution.Participant> participants = twoParticipantsDiffFamilyDept();
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(
                    participants, participants, new ArrayList<>());
            List<SecretSantaSolution.Pairing> pairings = engine.generatePairings();
            assertEquals(2, pairings.size());
        }

        @Test
        @DisplayName("CSV with only one participant — no valid pairing with no-self rule")
        void testOneParticipantCsv(@TempDir Path tempDir) throws IOException {
            Path csv = tempDir.resolve("one.csv");
            Files.writeString(csv, "id,name,email,family,department\n1,Alice,alice@test.com,FamA,Eng\n");
            SecretSantaSolution.CsvReader reader = new SecretSantaSolution.CsvReader();
            List<SecretSantaSolution.Participant> participants = reader.readParticipants(csv.toString());
            assertEquals(1, participants.size());

            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(
                    participants, participants, List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            assertTrue(engine.generatePairings().isEmpty());
        }

        @Test
        @DisplayName("conflicting rules make pairing impossible")
        void testConflictingRules() {
            // 2 people, same family AND same dept — both rules block
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamA", "Eng")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule(),
                    new SecretSantaSolution.DepartmentRestrictRule()
            );
            SecretSantaSolution.PairingEngine engine = new SecretSantaSolution.PairingEngine(participants, participants, rules);
            assertTrue(engine.generatePairings().isEmpty());
        }
    }

    // ==================== SecretSantaSolution Top-Level Tests ====================

    @Nested
    @DisplayName("SecretSantaSolution Top-Level Tests")
    class TopLevelTests {

        @Test
        @DisplayName("constructor stores participants")
        void testGetParticipantList() {
            List<SecretSantaSolution.Participant> participants = threeParticipantsDiffFamilyDept();
            List<SecretSantaSolution.Rule> rules = List.of(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution solution = new SecretSantaSolution(participants, rules);
            assertEquals(3, solution.getParticipantList().size());
        }

        @Test
        @DisplayName("getParticipantList returns unmodifiable list")
        void testGetParticipantListUnmodifiable() {
            SecretSantaSolution solution = new SecretSantaSolution(
                    twoParticipantsDiffFamilyDept(), List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            assertThrows(UnsupportedOperationException.class,
                    () -> solution.getParticipantList().add(p("99", "X", "F", "D")));
        }

        @Test
        @DisplayName("constructor stores rules")
        void testGetRules() {
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule()
            );
            SecretSantaSolution solution = new SecretSantaSolution(twoParticipantsDiffFamilyDept(), rules);
            assertEquals(2, solution.getRules().size());
        }

        @Test
        @DisplayName("getRules returns unmodifiable list")
        void testGetRulesUnmodifiable() {
            SecretSantaSolution solution = new SecretSantaSolution(
                    twoParticipantsDiffFamilyDept(), List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            assertThrows(UnsupportedOperationException.class,
                    () -> solution.getRules().add(new SecretSantaSolution.NoFamilyMatchingRule()));
        }

        @Test
        @DisplayName("constructor defensively copies participants")
        void testDefensiveCopyParticipants() {
            List<SecretSantaSolution.Participant> participants = new ArrayList<>(twoParticipantsDiffFamilyDept());
            SecretSantaSolution solution = new SecretSantaSolution(participants, List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            participants.clear(); // mutate original
            assertEquals(2, solution.getParticipantList().size()); // solution unaffected
        }

        @Test
        @DisplayName("constructor defensively copies rules")
        void testDefensiveCopyRules() {
            List<SecretSantaSolution.Rule> rules = new ArrayList<>();
            rules.add(new SecretSantaSolution.NoSelfMatchingRule());
            SecretSantaSolution solution = new SecretSantaSolution(twoParticipantsDiffFamilyDept(), rules);
            rules.clear(); // mutate original
            assertEquals(1, solution.getRules().size()); // solution unaffected
        }

        @Test
        @DisplayName("generatePairings with 2 participants returns valid pairings")
        void testGeneratePairingsTwoParticipants() {
            SecretSantaSolution solution = new SecretSantaSolution(
                    twoParticipantsDiffFamilyDept(), List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            List<SecretSantaSolution.Pairing> pairings = solution.generatePairings();
            assertEquals(2, pairings.size());
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().id, p.getReceiver().id);
            }
        }

        @Test
        @DisplayName("generatePairings with 4 participants and all rules")
        void testGeneratePairingsFourParticipantsAllRules() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamB", "Sales"),
                    p("3", "Carol", "FamC", "HR"),
                    p("4", "Dave", "FamD", "Ops")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule(),
                    new SecretSantaSolution.DepartmentRestrictRule()
            );
            SecretSantaSolution solution = new SecretSantaSolution(participants, rules);
            List<SecretSantaSolution.Pairing> pairings = solution.generatePairings();
            assertEquals(4, pairings.size());
            Set<String> senderIds = new HashSet<>();
            Set<String> receiverIds = new HashSet<>();
            for (SecretSantaSolution.Pairing p : pairings) {
                assertNotEquals(p.getSender().id, p.getReceiver().id);
                assertNotEquals(p.getSender().family, p.getReceiver().family);
                assertNotEquals(p.getSender().department, p.getReceiver().department);
                senderIds.add(p.getSender().id);
                receiverIds.add(p.getReceiver().id);
            }
            assertEquals(4, senderIds.size());
            assertEquals(4, receiverIds.size());
        }

        @Test
        @DisplayName("generatePairings returns empty when impossible")
        void testGeneratePairingsImpossible() {
            List<SecretSantaSolution.Participant> participants = Arrays.asList(
                    p("1", "Alice", "FamA", "Eng"),
                    p("2", "Bob", "FamA", "Eng")
            );
            List<SecretSantaSolution.Rule> rules = Arrays.asList(
                    new SecretSantaSolution.NoSelfMatchingRule(),
                    new SecretSantaSolution.NoFamilyMatchingRule()
            );
            SecretSantaSolution solution = new SecretSantaSolution(participants, rules);
            assertTrue(solution.generatePairings().isEmpty());
        }

        @Test
        @DisplayName("generatePairings with empty participants returns empty")
        void testGeneratePairingsEmpty() {
            SecretSantaSolution solution = new SecretSantaSolution(
                    Collections.emptyList(), List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            assertTrue(solution.generatePairings().isEmpty());
        }

        @Test
        @DisplayName("generatePairings with single participant returns empty")
        void testGeneratePairingsSingle() {
            SecretSantaSolution solution = new SecretSantaSolution(
                    List.of(p("1", "Alice", "FamA", "Eng")),
                    List.of(new SecretSantaSolution.NoSelfMatchingRule()));
            assertTrue(solution.generatePairings().isEmpty());
        }
    }
}

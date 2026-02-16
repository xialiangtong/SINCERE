import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class TestRunner {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(args.length > 0 ? args[0] : "VimOperationsTest"))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        System.out.println("Tests started:   " + summary.getTestsStartedCount());
        System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests failed:    " + summary.getTestsFailedCount());
        System.out.println("Tests skipped:   " + summary.getTestsSkippedCount());

        if (!summary.getFailures().isEmpty()) {
            System.out.println("\n--- FAILURES ---");
            for (TestExecutionSummary.Failure f : summary.getFailures()) {
                System.out.println("\n" + f.getTestIdentifier().getDisplayName());
                StringWriter sw = new StringWriter();
                f.getException().printStackTrace(new PrintWriter(sw));
                System.out.println(sw);
            }
        }

        System.exit(summary.getTestsFailedCount() > 0 ? 1 : 0);
    }
}

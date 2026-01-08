package dtu.project.triage;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.util.List;

import static dtu.project.triage.Tuples.*;

public class TimeoutViewer {
    public static void main(String[] args) throws Exception {
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");
        RemoteSpace board = new RemoteSpace(spaceUri);

        List<Object[]> items = board.queryAll(
                new ActualField(REVIEW_TIMEOUT),
                new FormalField(String.class),   // caseId
                new FormalField(String.class),   // taskId
                new FormalField(Integer.class),  // attempt
                new FormalField(String.class),   // previousWorker
                new FormalField(String.class),   // leaseUntil
                new FormalField(String.class)    // queuedAt
        );

        System.out.println("=== TIMEOUT ESCALATIONS (" + items.size() + ") ===");
        for (Object[] t : items) {
            System.out.println("case=" + t[1] + " task=" + t[2] + " attempt=" + t[3]
                    + " prevWorker=" + t[4] + " leaseUntil=" + t[5] + " queuedAt=" + t[6]);
        }
    }
}

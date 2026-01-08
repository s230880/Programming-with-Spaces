package dtu.project.triage;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.time.Instant;
import java.util.List;
import java.util.Scanner;

import static dtu.project.triage.Tuples.*;

public class Reviewer {

    public static void main(String[] args) throws Exception {
        String reviewerId = (args.length > 0) ? args[0] : "reviewer";
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");

        RemoteSpace board = new RemoteSpace(spaceUri);
        Scanner sc = new Scanner(System.in);

        System.out.println("Reviewer started: " + reviewerId);
        System.out.println("Commands: list | take | quit");

        while (true) {
            System.out.print("> ");
            String cmd = sc.nextLine().trim().toLowerCase();

            if (cmd.equals("quit") || cmd.equals("exit")) {
                break;
            }

            if (cmd.equals("list")) {
                // ("REVIEW_REQUEST", caseId, taskId, score, uncertainty, workerId, createdAt)
                List<Object[]> reqs = board.queryAll(
                        new ActualField(REVIEW_REQUEST),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Double.class),
                        new FormalField(Double.class),
                        new FormalField(String.class),
                        new FormalField(String.class)
                );

                System.out.println("Review requests: " + reqs.size());
                for (Object[] r : reqs) {
                    System.out.println("case=" + r[1] + " task=" + r[2] +
                            " score=" + r[3] + " unc=" + r[4] +
                            " from=" + r[5] + " at=" + r[6]);
                }
                continue;
            }

            if (cmd.equals("take")) {
                // NON-BLOCKING take: returns null if none exists (prevents freezing)
                Object[] r = board.getp(
                        new ActualField(REVIEW_REQUEST),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Double.class),
                        new FormalField(Double.class),
                        new FormalField(String.class),
                        new FormalField(String.class)
                );

                if (r == null) {
                    System.out.println("No review requests available right now.");
                    continue;
                }

                String caseId = (String) r[1];
                String taskId = (String) r[2];
                double score = (Double) r[3];
                double unc = (Double) r[4];
                String fromWorker = (String) r[5];
                String createdAt = (String) r[6];

                System.out.println("Taking request:");
                System.out.println("case=" + caseId + " task=" + taskId +
                        " score=" + score + " unc=" + unc +
                        " from=" + fromWorker + " at=" + createdAt);

                // Keep asking until valid answer (smooth UX)
                String decision;
                while (true) {
                    System.out.print("Decision (approve/reject/back): ");
                    decision = sc.nextLine().trim().toLowerCase();

                    if (decision.equals("approve") || decision.equals("reject") || decision.equals("back")) {
                        break;
                    }
                    System.out.println("Please type: approve | reject | back");
                }

                if (decision.equals("back")) {
                    // Put it back exactly as it was
                    board.put(REVIEW_REQUEST, caseId, taskId, score, unc, fromWorker, createdAt);
                    System.out.println("Returned request to queue.");
                    continue;
                }

                board.put(REVIEW_DECISION, caseId, taskId, decision, reviewerId, Instant.now().toString());

                Audit.log(board, "REVIEWER", reviewerId, "REVIEW_DECISION",
                        "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"decision\":\"" + decision + "\"}");

                System.out.println("Saved decision: " + decision);
                continue;
            }

            System.out.println("Commands: list | take | quit");
        }

        System.out.println("Reviewer exiting.");
    }
}

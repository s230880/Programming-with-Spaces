package dtu.project.triage;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.time.Instant;
import java.util.List;

import static dtu.project.triage.Tuples.*;

public class Reaper {

    public static void main(String[] args) throws Exception {
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");
        long intervalMs = Long.parseLong(System.getenv().getOrDefault("REAPER_INTERVAL_MS", "2000"));

        RemoteSpace board = new RemoteSpace(spaceUri);

        System.out.println("🧹 Reaper started. intervalMs=" + intervalMs + " space=" + spaceUri);

        while (true) {
            Instant now = Instant.now();

            // ("IN_PROGRESS", taskId, caseId, workerId, startedAt, leaseUntil)
            List<Object[]> inprog = board.queryAll(
                    new ActualField(IN_PROGRESS),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class)
            );

            for (Object[] t : inprog) {
                String taskId = (String) t[1];
                String caseId = (String) t[2];
                String workerId = (String) t[3];
                String startedAt = (String) t[4];
                String leaseUntil = (String) t[5];

                Instant until;
                try {
                    until = Instant.parse(leaseUntil);
                } catch (Exception e) {
                    continue; // ignore malformed
                }

                if (until.isBefore(now)) {
                    // Try to remove and requeue. Race-safe: worker might finish at same time.
                    try {
                        board.get(
                                new ActualField(IN_PROGRESS),
                                new ActualField(taskId),
                                new ActualField(caseId),
                                new ActualField(workerId),
                                new ActualField(startedAt),
                                new ActualField(leaseUntil)
                        );

                        // Put back into queue
                        board.put(AVAILABLE, taskId, caseId, Instant.now().toString());

                        Audit.log(board, "REAPER", "reaper", "TASK_REQUEUED_TIMEOUT",
                                "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"previousWorker\":\"" + workerId + "\",\"leaseUntil\":\"" + leaseUntil + "\"}");

                        System.out.println("♻️ Requeued taskId=" + taskId + " caseId=" + caseId + " (expired lease)");
                    } catch (Exception ignored) {
                        // Someone else removed it (finished or another reaper), that's fine.
                    }
                }
            }

            Thread.sleep(intervalMs);
        }
    }
}

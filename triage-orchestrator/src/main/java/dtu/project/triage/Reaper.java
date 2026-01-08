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
        int maxAttempts = Integer.parseInt(System.getenv().getOrDefault("MAX_ATTEMPTS", "3"));

        RemoteSpace board = new RemoteSpace(spaceUri);

        System.out.println("... Reaper started. intervalMs=" + intervalMs +
                           " maxAttempts=" + maxAttempts +
                           " space=" + spaceUri);

        while (true) {
            // ("IN_PROGRESS", taskId, caseId, workerId, startedAt, leaseUntil, attempt)
            List<Object[]> inprog = board.queryAll(
                    new ActualField(IN_PROGRESS),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(String.class),
                    new FormalField(Integer.class)
            );

            for (Object[] t : inprog) {
                String taskId = (String) t[1];
                String caseId = (String) t[2];
                String workerId = (String) t[3];
                String startedAt = (String) t[4];
                String leaseUntil = (String) t[5];
                int attempt = (Integer) t[6];

                if (Instant.parse(leaseUntil).isBefore(Instant.now())) {
                    try {
                        board.get(
                                new ActualField(IN_PROGRESS),
                                new ActualField(taskId),
                                new ActualField(caseId),
                                new ActualField(workerId),
                                new ActualField(startedAt),
                                new ActualField(leaseUntil),
                                new ActualField(attempt)
                        );

                        int nextAttempt = attempt + 1;

                        if (nextAttempt >= maxAttempts) {
                            board.put(
                                    REVIEW_TIMEOUT,
                                    caseId,
                                    taskId,
                                    nextAttempt,
                                    workerId,
                                    leaseUntil,
                                    Instant.now().toString()
                            );

                            Audit.log(board, "REAPER", "reaper", "TASK_ESCALATED_TIMEOUT",
                                    "{\"taskId\":\"" + taskId +
                                    "\",\"caseId\":\"" + caseId +
                                    "\",\"attempt\":" + nextAttempt +
                                    ",\"previousWorker\":\"" + workerId +
                                    "\",\"leaseUntil\":\"" + leaseUntil + "\"}");

                        } else {
                            board.put(
                                    AVAILABLE,
                                    taskId,
                                    caseId,
                                    Instant.now().toString(),
                                    nextAttempt
                            );

                            Audit.log(board, "REAPER", "reaper", "TASK_REQUEUED_TIMEOUT",
                                    "{\"taskId\":\"" + taskId +
                                    "\",\"caseId\":\"" + caseId +
                                    "\",\"attempt\":" + nextAttempt +
                                    ",\"previousWorker\":\"" + workerId +
                                    "\",\"leaseUntil\":\"" + leaseUntil + "\"}");
                        }

                        System.out.println("... Handled expired lease for taskId=" + taskId +
                                           " caseId=" + caseId +
                                           " attempt=" + nextAttempt);

                    } catch (Exception ignored) {
                        // Worker finished or another reaper won the race
                    }
                }
            }

            Thread.sleep(intervalMs);
        }
    }
}

package dtu.project.triage;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.time.Instant;
import java.util.Random;
import org.jspace.ActualField;


import static dtu.project.triage.Tuples.*;

public class Worker {
    public static void main(String[] args) throws Exception {
        String workerId = (args.length > 0) ? args[0] : "worker-" + ProcessHandle.current().pid();
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");

        RemoteSpace board = new RemoteSpace(spaceUri);
        Random rnd = new Random();

        System.out.println("✅ Worker started: " + workerId + " space=" + spaceUri);

        while (true) {
            // Take next NEW inference task:
            // ("TASK", taskId, caseId, "INFER", "NEW", createdAt)
            Object[] task = board.get(
        	new ActualField(TASK),
        	new FormalField(String.class),   // taskId
        	new FormalField(String.class),   // caseId
        	new ActualField(INFER),
        	new ActualField(NEW),
        	new FormalField(String.class)    // createdAt
	    );



            String taskId = (String) task[1];
            String caseId = (String) task[2];
            String createdAt = (String) task[5];

            // Claim tuple (simple first version):
            // ("CLAIM", taskId, workerId, claimedAt)
            board.put(CLAIM, taskId, workerId, Instant.now().toString());

            // Fake ML inference (for now): score + uncertainty
            double score = rnd.nextDouble();          // 0..1
            double uncertainty = rnd.nextDouble();    // 0..1

            // RESULT tuple: ("RESULT", caseId, taskId, score, uncertainty, workerId, finishedAt)
            board.put(RESULT, caseId, taskId, score, uncertainty, workerId, Instant.now().toString());

            // Mark task done (we re-put TASK as DONE)
            board.put(TASK, taskId, caseId, INFER, DONE, createdAt);

            System.out.println("✅ Done task=" + taskId + " case=" + caseId + " score=" + score + " unc=" + uncertainty);
        }
    }
}

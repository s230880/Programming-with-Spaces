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

        System.out.println("Ã¢Å“â€¦ Worker started: " + workerId + " space=" + spaceUri);

        while (true) {
            // Take next available task (this is the atomic claim):
	    // ("AVAILABLE", taskId, caseId, createdAt)
	    Object[] avail = board.get(
        	    new ActualField(AVAILABLE),
        	    new FormalField(String.class),
        	    new FormalField(String.class),
        	    new FormalField(String.class)
	    );

	    String taskId = (String) avail[1];
	    String caseId = (String) avail[2];
	    String createdAt = (String) avail[3];

	    // Mark in progress
	    board.put(IN_PROGRESS, taskId, caseId, workerId, Instant.now().toString());
	    Audit.log(board, "WORKER", workerId, "TASK_STARTED",
        	    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\"}");




            String taskId = (String) task[1];
            String caseId = (String) task[2];
            String createdAt = (String) task[5];

            // Claim tuple (simple first version):
            // ("CLAIM", taskId, workerId, claimedAt)
            board.put(CLAIM, taskId, workerId, Instant.now().toString());
	    Audit.log(board, "WORKER", workerId, "TASK_CLAIMED",
        	    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\"}");

            // Fake ML inference (for now): score + uncertainty
            double score = rnd.nextDouble();          // 0..1
            double uncertainty = rnd.nextDouble();    // 0..1

            // RESULT tuple: ("RESULT", caseId, taskId, score, uncertainty, workerId, finishedAt)
            board.put(RESULT, caseId, taskId, score, uncertainty, workerId, Instant.now().toString());
	    Audit.log(board, "WORKER", workerId, "RESULT_WRITTEN",
        	    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"score\":" + score + ",\"uncertainty\":" + uncertainty + "}");
	    board.put(DONE_T, taskId, caseId, workerId, Instant.now().toString());
            Audit.log(board, "WORKER", workerId, "TASK_DONE",
       		    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\"}");


            // Mark task done (we re-put TASK as DONE)
            board.put(TASK, taskId, caseId, INFER, DONE, createdAt);

            System.out.println("Ã¢Å“â€¦ Done task=" + taskId + " case=" + caseId + " score=" + score + " unc=" + uncertainty);
        }
    }
}

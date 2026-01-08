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
	int sleepMs = 0;
	if (args.length > 1) {
    	    sleepMs = Integer.parseInt(args[1]);
	}

        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");

        RemoteSpace board = new RemoteSpace(spaceUri);
        Random rnd = new Random();

        System.out.println(" Worker started: " + workerId + " space=" + spaceUri);

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
	    // Lease settings (ms)
	    long leaseMs = Long.parseLong(System.getenv().getOrDefault("LEASE_MS", "15000"));

	    String startedAt = Instant.now().toString();
	    String leaseUntil = Instant.now().plusMillis(leaseMs).toString();

	    // Mark in progress with a lease deadline
	    board.put(IN_PROGRESS, taskId, caseId, workerId, startedAt, leaseUntil);

	    Audit.log(board, "WORKER", workerId, "TASK_STARTED",
        	    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"leaseUntil\":\"" + leaseUntil + "\"}");






            
            // Fake ML inference (for now): score + uncertainty
            double score = rnd.nextDouble();          // 0..1
            double uncertainty = rnd.nextDouble();    // 0..1
	    if (sleepMs > 0) {
    		Thread.sleep(sleepMs);
	    }

	    boolean leaseValid = true;
	    try {
    		// Try to "finalize" by removing our own in-progress tuple.
    		board.get(
            		new ActualField(IN_PROGRESS),
            		new ActualField(taskId),
            		new ActualField(caseId),
            		new ActualField(workerId),
            		new ActualField(startedAt),
            		new ActualField(leaseUntil)
    		);
	    } catch (Exception e) {
    		leaseValid = false;
	    }

if (!leaseValid) {
    Audit.log(board, "WORKER", workerId, "TASK_STALE_RESULT_DROPPED",
            "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"leaseUntil\":\"" + leaseUntil + "\"}");
    System.out.println("âš ï¸ Lease expired, dropping result taskId=" + taskId);
    continue; // go back to get another AVAILABLE task
}



            double threshold = Double.parseDouble(System.getenv().getOrDefault("REVIEW_THRESHOLD", "0.75"));
	    String finishedAt = Instant.now().toString();

	    if (uncertainty >= threshold) {
    		// Send to human review queue (non-blocking)
    		board.put(REVIEW_REQUEST, caseId, taskId, score, uncertainty, workerId, finishedAt);

    		Audit.log(board, "WORKER", workerId, "REVIEW_REQUESTED",
            		"{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"score\":" + score + ",\"uncertainty\":" + uncertainty + "}");

    		// Optionally still store model suggestion:
    		board.put(RESULT, caseId, taskId, score, uncertainty, workerId, finishedAt);
    		Audit.log(board, "WORKER", workerId, "RESULT_WRITTEN",
            		"{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"score\":" + score + ",\"uncertainty\":" + uncertainty + "}");

	    } else {
    		// Normal result path
    		board.put(RESULT, caseId, taskId, score, uncertainty, workerId, finishedAt);
    		Audit.log(board, "WORKER", workerId, "RESULT_WRITTEN",
            		"{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"score\":" + score + ",\"uncertainty\":" + uncertainty + "}");
	    }

	    board.put(DONE_T, taskId, caseId, workerId, finishedAt);
	    Audit.log(board, "WORKER", workerId, "TASK_DONE",
        	    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\"}");





            System.out.println(" Done task=" + taskId + " case=" + caseId + " score=" + score + " unc=" + uncertainty);
        }
    }
}

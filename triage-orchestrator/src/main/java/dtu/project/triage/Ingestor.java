package dtu.project.triage;

import org.jspace.RemoteSpace;

import java.io.File;
import java.time.Instant;
import java.util.UUID;

import static dtu.project.triage.Tuples.*;
import static dtu.project.triage.Audit.log;


public class Ingestor {
    public static void main(String[] args) throws Exception {
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");
        String casesDir = (args.length > 0) ? args[0] : "cases";

        RemoteSpace board = new RemoteSpace(spaceUri);

        File dir = new File(casesDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Cases dir not found: " + dir.getAbsolutePath());
        }

        File[] files = dir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            System.out.println("No case files found in: " + dir.getAbsolutePath());
            return;
        }

        for (File f : files) {
            String caseId = UUID.randomUUID().toString();
            String taskId = UUID.randomUUID().toString();
            String createdAt = Instant.now().toString();

            // CASE tuple: ("CASE", caseId, filePath, createdAt)
            board.put(CASE, caseId, f.getAbsolutePath(), createdAt);
            Audit.log(board, "INGESTOR", "ingestor", "CASE_CREATED",
        	    "{\"caseId\":\"" + caseId + "\",\"file\":\"" + f.getName() + "\",\"path\":\"" + f.getAbsolutePath().replace("\\","\\\\") + "\"}");


            // TASK tuple: ("TASK", taskId, caseId, "INFER", "NEW", createdAt)
            board.put(TASK, taskId, caseId, INFER, NEW, createdAt);
            Audit.log(board, "INGESTOR", "ingestor", "TASK_CREATED",
        	    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\",\"type\":\"" + INFER + "\",\"state\":\"" + NEW + "\"}");
            board.put(AVAILABLE, taskId, caseId, createdAt);
            Audit.log(board, "INGESTOR", "ingestor", "TASK_ENQUEUED",
                    "{\"taskId\":\"" + taskId + "\",\"caseId\":\"" + caseId + "\"}");



            System.out.println("Ã¢Å“â€¦ Ingested case: " + caseId + " file=" + f.getName() + " task=" + taskId);
        }
    }
}

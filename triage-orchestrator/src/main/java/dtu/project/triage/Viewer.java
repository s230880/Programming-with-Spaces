package dtu.project.triage;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dtu.project.triage.Tuples.*;

public class Viewer {

    static class ResultRow {
        final String caseId;
        final String taskId;
        final double score;
        final double uncertainty;
        final String workerId;
        final String finishedAt;

        ResultRow(String caseId, String taskId, double score, double uncertainty, String workerId, String finishedAt) {
            this.caseId = caseId;
            this.taskId = taskId;
            this.score = score;
            this.uncertainty = uncertainty;
            this.workerId = workerId;
            this.finishedAt = finishedAt;
        }
    }

    public static void main(String[] args) throws Exception {
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");
        int topN = (args.length > 0) ? Integer.parseInt(args[0]) : 10;

        RemoteSpace board = new RemoteSpace(spaceUri);

        // Query all RESULT tuples without removing them
        // ("RESULT", caseId, taskId, score, uncertainty, workerId, finishedAt)
        List<Object[]> tuples = board.queryAll(
                new ActualField(RESULT),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(Double.class),
                new FormalField(Double.class),
                new FormalField(String.class),
                new FormalField(String.class)
        );

        List<ResultRow> rows = new ArrayList<>();
        for (Object[] t : tuples) {
            rows.add(new ResultRow(
                    (String) t[1],
                    (String) t[2],
                    (Double) t[3],
                    (Double) t[4],
                    (String) t[5],
                    (String) t[6]
            ));
        }

        rows.sort(Comparator.comparingDouble((ResultRow r) -> r.uncertainty).reversed());

        System.out.println("=== RESULTS (top " + Math.min(topN, rows.size()) + " by uncertainty) ===");
        for (int i = 0; i < rows.size() && i < topN; i++) {
            ResultRow r = rows.get(i);
            System.out.printf(
                    "%d) case=%s  score=%.4f  unc=%.4f  worker=%s  time=%s%n",
                    (i + 1), r.caseId, r.score, r.uncertainty, r.workerId, r.finishedAt
            );
        }

        if (rows.isEmpty()) {
            System.out.println("(No results yet)");
        }
    }
}

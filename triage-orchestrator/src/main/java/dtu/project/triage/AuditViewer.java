package dtu.project.triage;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static dtu.project.triage.Tuples.AUDIT;

public class AuditViewer {
    public static void main(String[] args) throws Exception {
        String spaceUri = System.getenv().getOrDefault("SPACE_URI", "tcp://localhost:9001/board?conn");
        RemoteSpace board = new RemoteSpace(spaceUri);

        // ("AUDIT", ts, eventId, actorType, actorId, action, payloadJson)
        List<Object[]> logs = board.queryAll(
                new ActualField(AUDIT),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(String.class)
        );

        // Sort by timestamp (string ISO-8601 sorts lexicographically)
        List<Object[]> sorted = logs.stream()
                .sorted(Comparator.comparing(o -> (String) o[1]))
                .collect(Collectors.toList());

        System.out.println("=== AUDIT LOG (" + sorted.size() + " events) ===");
        for (Object[] e : sorted) {
            System.out.println(
                    e[1] + " | event=" + e[2] +
                    " | " + e[3] + ":" + e[4] +
                    " | " + e[5] +
                    " | " + e[6]
            );
        }
    }
}

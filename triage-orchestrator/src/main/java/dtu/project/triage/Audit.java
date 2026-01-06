package dtu.project.triage;

import org.jspace.RemoteSpace;

import java.time.Instant;
import java.util.UUID;

import static dtu.project.triage.Tuples.AUDIT;

/**
 * Append-only audit log.
 * Each audit tuple is an immutable record of an important system event.
 *
 * Tuple format:
 * ("AUDIT", ts, eventId, actorType, actorId, action, payloadJson)
 */
public final class Audit {
    private Audit() {}

    public static void log(RemoteSpace space,
                           String actorType,
                           String actorId,
                           String action,
                           String payloadJson) throws Exception {
        String ts = Instant.now().toString();
        String eventId = UUID.randomUUID().toString();

        space.put(AUDIT,
                ts,
                eventId,
                actorType,
                actorId,
                action,
                payloadJson
        );
    }
}

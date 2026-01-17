# Project 017 - ML Triage Orchestrator

## Abstract

This project implements a distributed triage pipeline for running (mock) ML inference on incoming “cases” and ensuring that uncertain cases can be routed to human review, while keeping the system fault-tolerant and auditable.

The system is coordinated using a shared tuple space (jSpace). Components do not call each other directly; instead, they coordinate via generative communication: they write tuples and atomically take tuples using pattern matching. This makes the system naturally distributed: processes can run on one or multiple computers.

### Workflow

1. **SpaceServer** hosts a tuple space and exposes a TCP gate.
2. **Ingestor** creates a CASE and a TASK, then enqueues work as `AVAILABLE`.
3. **Workers** compete for tasks using an atomic claim (`get(AVAILABLE, …)`), mark tasks as `IN_PROGRESS` with a lease deadline, perform inference (here: simple randomized scoring), and write `RESULT`.
4. **Reaper** provides crash/timeout recovery by re-queuing tasks whose lease expired (handles slow or crashed workers).
5. **Viewer** ranks results by uncertainty (triage view).
6. **Reviewer** is a CLI for human decisions on flagged cases (approve/reject).
7. **Audit log** records important events as structured entries (without storing sensitive raw data in tuples).

The ML model is pluggable: the coordination logic (queueing, leasing, recovery, audit) is reusable across domains by swapping the model adapter and data ingestion.


## Contributors

Project contributors:

- **Soheil** (s230880@student.dtu.dk)
- **Adal** (234545@student.dtu.dk)

### Contributions

- Design of main coordination aspects: Soheil, Adal  
- Coding of main coordination aspects: Soheil, Adal  
- Documentation (this README file): Soheil  
- Videos: Soheil, Adal  
- Other aspects (e.g. CLI tooling, test scenarios): Soheil, Adal  

**IMPORTANT:** The history of the repository shows that all members have been active contributors.


## Demo video

- Demo video: https://youtu.be/paWE-GvDO1c?si=SR6srFgJOtMZ1ECE  
- Running on multiple computers video: https://www.youtube.com/shorts/76W1ZtZfgFk  

(Back-up videos are included in the submission.)


## Main coordination challenge

### Challenge: Distributed worker queue with failures

We needed a distributed “worker queue” where multiple workers compete for tasks, but the system must remain correct if a worker crashes, hangs, or becomes slow. A naïve approach (workers simply take tasks and process them) risks lost tasks or duplicate results.

The system must ensure:

1. Mutual exclusion when claiming work  
2. No permanent task loss if a worker crashes  
3. Progress despite failures  
4. Controlled handling of duplicate results  


### Solution: Lease-based claiming + timeout recovery (Reaper)

We represent work using tuples and implement a lease so task ownership expires automatically.

#### Key tuples (simplified)

- `("AVAILABLE", taskId, caseId, createdAt, attempt)`
- `("IN_PROGRESS", taskId, caseId, workerId, startedAt, leaseUntil, attempt)`
- `("RESULT", caseId, taskId, score, uncertainty, workerId, finishedAt)`
- Audit events:  
  `("AUDIT", ts, eventId, actorType, actorId, action, payloadJson)`

#### State diagram (coordination logic)

```text
           Ingestor
              |
              v
      ("AVAILABLE", ...)
              |
              | atomic get() by a Worker
              v
("IN_PROGRESS", ..., leaseUntil)
              |
      +-------+---------------------------+
      |                                   |
      | Worker finalizes in time          | Worker crashes / stalls / too slow
      v                                   v
remove IN_PROGRESS                  Reaper detects leaseUntil < now
write RESULT                        remove IN_PROGRESS
mark DONE                           re-put AVAILABLE (attempt+1)
                                    (or escalate after max attempts)

```





#### Why this is a coordination achievement

- **Mutual exclusion / atomic claim:** `get(AVAILABLE, …)` ensures only one worker claims a task.
- **Fault tolerance:** If a worker dies or exceeds the lease, the task is re-queued.
- **Progress despite failures:** The system is self-healing as long as at least one worker is alive.
- **Controlled duplicates:** A slow worker finishing after timeout must remove its exact `IN_PROGRESS` tuple to finalize. If this fails, the stale result is dropped and audited.

This directly aligns with core course concepts: tuple spaces, atomic operations, and robust distributed coordination.


## Programming language and coordination mechanism

- **Language:** Java  
- **Coordination mechanism:** jSpace (pSpaces tuple space library)

We use:

- `put(tuple)` to publish work, results, and audit events  
- `get(template)` to atomically take a matching tuple (destructive read) — used for claims and exclusive ownership  
- `queryAll(template)` for non-destructive reads (monitoring views like Viewer and AuditViewer)

Coordination is done using Linda-style pattern matching on tuples (e.g., `ActualField("AVAILABLE")`, `FormalField(String.class)`), decoupling processes in time and space.


## Installation

### Requirements

- Windows / macOS / Linux  
- Java (JDK 17+ recommended)  
- Maven 3.9+  

### Step 1 - Install jSpace locally (required)

jSpace must be installed into your local Maven repository.

1. Download: https://github.com/pSpaces/jSpace/archive/master.zip  
2. Unzip somewhere writable  
3. Open a terminal in `jSpace-master/` (where `pom.xml` is located)  
4. Run:


mvn clean verify
mvn install

### Step 2 - Build this project
from the repo root:

cd triage-orchestrator
mvn package

### Running the project

##Option A - Single computer (quick demo) 
Open multiple terminals.

Terminal 1 - spaceServer
cd triage-orchestrator
mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.SpaceServer


Terminal 2 - Reaper
cd triage-orchestrator
mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.Reaper


Terminal 3 - worker 1

cd triage-orchestrator

mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.Worker -Dexec.args="worker-1 0"


Terminal 4 - worker 2

cd triage-orchestrator

mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.Worker -Dexec.args="worker-2 0"


Terminal 6 - viewer(uncertainty ranking)

cd triage-orchestrator

mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.Viewer



Terminal 7 - AuditViewer

cd triage-orchestrator

mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.AuditViewer



Terminal 8 - Reviwer(Human decision)

cd triage-orchestrator

mvn -DskipTests exec:java -Dexec.mainClass=dtu.project.triage.Reviewer -Dexec.args="reviewer-1"

##Option B (Distributed demo) 
1) Run SpaceServer on one computer (the host) and ensure the TCP port is reachable.
2) On other computers, run Workers/Reviewer/etc. using:

export SPACE_URI="tcp://<SERVER_IP>:9001/board?conn"

(or on Windows PowerShell: $env:SPACE_URI="tcp://<SERVER_IP>:9001/board?conn")

This demonstrates real distributed coordination: multiple machines competing for tasks in the same tuple space.


## References:
- pSpaces / jSpace (tuple spaces in Java): https://github.com/pSpaces/jSpace
- Course modules (DTU 02148): tuple spaces, Linda-style coordination, distributed coordination patterns
- Linda / generative communication background (tuple spaces): David Gelernter, “Generative Communication in Linda” (conceptual reference)
- Martin Kleppmann, Designing Data-Intensive Applications (fault tolerance and distributed systems patterns; conceptual reference)









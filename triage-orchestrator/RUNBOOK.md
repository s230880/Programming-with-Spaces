# Runbook (PowerShell)



## 0) Open terminals in the correct folder (IMPORTANT)

In every terminal, first run:



cd $HOME\Documents\code\Programming-with-Spaces\triage-orchestrator

$env:Path += ";C:\tools\apache-maven-3.9.12\bin"



(Optional sanity checks)

mvn -v

java -version



---



## Reset (recommended between demos)

Stop **SpaceServer** and start it again to clear all tuples (fresh run).



---



# Option A: Single computer demo (recommended for grading)



## Terminal 1: SpaceServer

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.SpaceServer"

Expected: prints that a TCP gate/space is running (listening on port 9001).



## Terminal 2: Reaper (timeout recovery)

$env:LEASE_MS="15000"

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Reaper"

Expected: periodically scans IN_PROGRESS and re-queues expired leases.



## Terminal 3: Worker 1 (slow worker to trigger timeout)

$env:LEASE_MS="15000"

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Worker" "-Dexec.args=worker-1 30000"

Expected: claims tasks and may finish late (after the lease) due to 30s delay.



## Terminal 4: Worker 2 (fast worker)

$env:LEASE_MS="15000"

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Worker" "-Dexec.args=worker-2 0"

Expected: claims tasks and completes within the lease window.



## Terminal 5: Ingest cases

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Ingestor"

Expected: puts CASE/TASK tuples and enqueues AVAILABLE work.



## Terminal 6: Viewer (rank results by uncertainty)

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Viewer"

Expected: shows results ordered by uncertainty (highest uncertainty first).



## Terminal 7: Human review demo (lower threshold so review triggers)

$env:REVIEW_THRESHOLD="0.30"

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Reviewer" "-Dexec.args=reviewer-1"

Expected: prompts for approve/reject decisions on flagged cases.



## Terminal 8: Audit log viewer

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.AuditViewer"

Expected: shows structured audit events (claims, re-queues, stale drops, reviews, etc.).



---



# Demo checklist (coordination evidence)



## A) Mutual exclusion (competing workers)

Steps:

1) Start SpaceServer, Reaper, Worker 1, Worker 2

2) Run Ingestor

Expected:

- Each AVAILABLE task is atomically claimed by exactly one worker (no double-claim).

- Audit shows TASK_STARTED per attempt (and TASK_REQUEUED_TIMEOUT / TASK_STALE_RESULT_DROPPED when relevant).



## B) Crash/timeout recovery (lease + reaper)

Setup:

- Worker 1 has 30s delay, LEASE_MS=15000 (15s)

Steps:

1) Run Ingestor

Expected:

- Reaper detects lease expiry and re-queues tasks (attempt increments).

- Worker 2 (fast) completes re-queued tasks.

- Late results from slow worker are treated as stale (dropped) and audited.



## C) Reaper offline test (shows dependency)

Steps:

1) Start SpaceServer + Workers (do NOT start Reaper yet)

2) Run Ingestor (some tasks will be claimed and may time out)

3) Start Reaper

Expected:

- Without Reaper, expired IN_PROGRESS can stall.

- When Reaper starts, it recovers expired leases and progress resumes.



## D) Worker crash test (optional, strong evidence)

Steps:

1) Start SpaceServer + Reaper + at least one Worker

2) While a worker has claimed work (IN_PROGRESS), close that worker terminal

Expected:

- After lease expiry, Reaper re-queues the task and another worker completes it.



---



# Option B: Distributed mode (multiple computers)



## 1) Run SpaceServer on a host machine

- Ensure TCP port 9001 is reachable from other machines (firewall may need allowing inbound 9001).



On the host machine:

cd $HOME\Documents\code\Programming-with-Spaces\triage-orchestrator

$env:Path += ";C:\tools\apache-maven-3.9.12\bin"

mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.SpaceServer"



## 2) On each client machine, set SPACE_URI and run components

PowerShell (replace <SERVER_IP>):

cd $HOME\Documents\code\Programming-with-Spaces\triage-orchestrator

$env:Path += ";C:\tools\apache-maven-3.9.12\bin"

$env:SPACE_URI="tcp://<SERVER_IP>:9001/board?conn"



Then run Workers / Reaper / Viewer / Reviewer exactly as in Option A.



Expected:

- Multiple machines compete for tasks via the same tuple space (true distributed coordination).



---



# Troubleshooting



## Maven not found

Run:

$env:Path += ";C:\tools\apache-maven-3.9.12\bin"

mvn -v



## Wrong folder / "pom.xml not found"

Make sure you are in:

cd $HOME\Documents\code\Programming-with-Spaces\triage-orchestrator



## Distributed clients can’t connect

- Verify the server IP is correct.

- Check firewall rules on the host for inbound TCP 9001.

- Ensure host and client are on the same network / routing is allowed.




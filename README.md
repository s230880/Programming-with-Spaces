# Project 17 - ML Triage Orchestrator (jSpace)



## Abstract

This project builds a distributed “factory line” for running ML inference on incoming cases (e.g., hospital images or engineering inspection items) and sending the most important cases to a human reviewer first. The system is coordinated using a shared tuple space (jSpace/pSpaces): components do not call each other directly, they communicate by placing and taking tuples (small structured messages) in the tuple space.

How it works:
1) An Ingestor creates a CASE tuple for each incoming case and a TASK tuple that says “run inference on this case”.
2) A pool of Worker processes (running on one or multiple computers) takes tasks from the tuple space, claims them using a lease, runs ML inference (or a plug-in model), and writes RESULT tuples.
3) A Prioritizer reads results and writes PRIORITY tuples based on risk/uncertainty so the human sees urgent/uncertain cases first.
4) A Reviewer (CLI/UI) shows the top prioritized cases and writes REVIEW tuples containing the human decision.
5) An Audit service records all important events in structured logs without storing raw sensitive data in tuples or logs.

The ML model is pluggable: the same coordination system can be reused across different domains (hospital vs engineering) by swapping the model adapter and the data connector. The main focus is coordination, fault tolerance, and auditability in a distributed application using tuple spaces.

(≈200 words)


## Contributors

Project contributors:

- [Your Name] ([your email])
- [Name] ([email])
- [Name] ([email])
- [Name] ([email])


Contributions:

- Design of main coordination aspects: [Name], [Name].
- Coding of main coordination aspects: [Name], [Name].
- Documentation (this README file): [Name], [Name].
- Videos: [Name], [Name].
- Other aspects (e.g. coding of UI, etc.): [Name], [Name].

IMPORTANT: The history of the repo shows that all members have been active contributors.


## Demo video

Demo video: [PASTE LINK]

If your demo video uses one single computer (as it would be easier to screencast), please add a link to an additional video showing that it can also run on multiple computers.

Running on multiple computers video: [PASTE LINK]

As a back-up, please upload the videos as part of your submission.


## Main coordination challenge

### Challenge: “Exactly-once” task completion with failures (avoiding duplicates and stuck tasks)

We coordinate work using tuples like:
- CASE(caseId, dataRef, createdTs, domain)
- TASK(taskId, caseId, type, priority, status)
- CLAIM(taskId, workerId, leaseExpiryTs)
- RESULT(taskId, caseId, score, uncertainty, modelVersion, finishedTs)
- PRIORITY(caseId, rankScore, reason, ts)
- REVIEW(caseId, reviewerId, decision, comment, ts)
- AUDIT(eventType, entityId, ts, payloadJson)

The biggest coordination challenge is: multiple workers run in parallel, and workers can crash mid-task. We need to ensure:
1) Tasks do not get “lost” (system must continue).
2) We avoid duplicate processing as much as possible.
3) If duplicates happen (due to failure/retry), the final state remains correct.

### Solution: Lease-based claiming + idempotent result handling

When a worker takes a TASK, it immediately writes a CLAIM(taskId, workerId, leaseExpiryTs). The lease is a time limit: if the worker dies, the lease expires.

A reaper/monitor process periodically checks for expired claims. If a CLAIM has expired and the TASK is still not DONE, it re-queues the task (or resets TASK status) so another worker can process it.

To handle the possibility of duplicates (e.g., two workers complete around the same time), results are treated as idempotent: the system accepts the first valid RESULT for a task/case and ignores later duplicates or stores them for debugging. This prevents inconsistent final states while still allowing recovery.

This pattern addresses fault tolerance and coordination in a distributed worker-pool using tuple spaces: shared space for tasks, atomic operations for task pickup/claiming, and time-based leases for recovery.

(Insert 1–2 small diagrams here: “task lifecycle” and “lease expiry recovery”.)


## Installation

### Requirements
- Java (JDK 8+ recommended)
- Maven
- jSpace/pSpaces library available (either installed locally via `mvn install` from the jSpace repo, or fetched via Maven dependency)

### Build
From the project root:

```bash
mvn clean package

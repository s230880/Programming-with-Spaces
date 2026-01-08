# Runbook (PowerShell)

## Terminal 1: Space server
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.SpaceServer"

## Terminal 2: Reaper (timeout recovery)
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
$env:LEASE_MS="15000"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Reaper"

## Terminal 3: Worker 1 (slow)
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
$env:LEASE_MS="15000"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Worker" "-Dexec.args=worker-1 30000"

## Terminal 4: Worker 2 (fast)
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
$env:LEASE_MS="15000"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Worker" "-Dexec.args=worker-2 0"

## Terminal 5: Ingest cases
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Ingestor"

## Terminal 6: Viewer (rank results)
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Viewer"

## Terminal 7: Human review demo
# Lower threshold so review triggers
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
$env:REVIEW_THRESHOLD="0.30"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.Reviewer" "-Dexec.args=reviewer-1"

## Audit log
$env:Path += ";C:\tools\apache-maven-3.9.12\bin"
mvn -q -DskipTests exec:java "-Dexec.mainClass=dtu.project.triage.AuditViewer"

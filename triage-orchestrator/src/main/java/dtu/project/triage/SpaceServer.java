package dtu.project.triage;

import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

public class SpaceServer {
    public static void main(String[] args) throws Exception {
        int port = 9001;
        String gateUri = "tcp://localhost:" + port + "/?conn";

        SpaceRepository repo = new SpaceRepository();
        repo.add("board", new SequentialSpace());
        repo.addGate(gateUri);

        System.out.println("✅ SpaceServer running");
        System.out.println("   Gate:  " + gateUri);
        System.out.println("   Space: board");
        System.out.println("   Leave this window open.");

        Thread.currentThread().join();
    }
}

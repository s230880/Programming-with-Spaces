package dtu.project.triage;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;

public class HelloClient {
    public static void main(String[] args) throws Exception {
        String uri = "tcp://localhost:9001/board?conn";
        RemoteSpace board = new RemoteSpace(uri);

        board.put("hello", "world");

        Object[] t = board.get(
                new FormalField(String.class),
                new FormalField(String.class)
        );

        System.out.println("✅ Got tuple back: (" + t[0] + ", " + t[1] + ")");
    }
}

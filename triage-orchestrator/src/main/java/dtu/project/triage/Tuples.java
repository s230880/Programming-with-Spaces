package dtu.project.triage;

public final class Tuples {
    private Tuples() {}

    // Tuple tags (first element of each tuple)
    public static final String CASE = "CASE";
    public static final String TASK = "TASK";
    public static final String CLAIM = "CLAIM";
    public static final String RESULT = "RESULT";

    // Task states
    public static final String NEW = "NEW";
    public static final String DONE = "DONE";

    // Task type (for later extension)
    public static final String INFER = "INFER";
}

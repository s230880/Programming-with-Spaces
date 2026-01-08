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
    public static final String AVAILABLE = "AVAILABLE";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String DONE_T = "DONE"; // renamed to avoid confusion with task state string
    public static final String REVIEW_REQUEST = "REVIEW_REQUEST";
    public static final String REVIEW_DECISION = "REVIEW_DECISION";


    // Task type (for later extension)
    public static final String INFER = "INFER";
    public static final String AUDIT = "AUDIT";
}

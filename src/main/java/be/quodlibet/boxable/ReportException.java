package be.quodlibet.boxable;

/**
 * Custom exception for report generation errors, specifically used when
 * errors occur during page footer operations.
 */
public class ReportException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ReportException with the specified detail message.
     *
     * @param message the detail message
     */
    public ReportException(String message) {
        super(message);
    }

    /**
     * Constructs a new ReportException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
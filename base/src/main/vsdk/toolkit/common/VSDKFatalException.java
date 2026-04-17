package vsdk.toolkit.common;

/**
Unchecked exception raised when VSDK reports a fatal error in non-exit mode.
*/
public class VSDKFatalException extends RuntimeException {
    public static final long serialVersionUID = 20260417L;

    public VSDKFatalException(String message) {
        super(message);
    }

    public VSDKFatalException(String message, Throwable cause) {
        super(message, cause);
    }
}

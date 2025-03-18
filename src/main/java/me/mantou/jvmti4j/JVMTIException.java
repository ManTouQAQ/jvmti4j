package me.mantou.jvmti4j;

public class JVMTIException extends RuntimeException {
    private final JVMTIError error;

    public JVMTIException(JVMTIError error) {
        super("JVMTI ERROR: " + error);
        if (error == JVMTIError.JVMTI_ERROR_NONE){
            throw new IllegalArgumentException("JVMTIError can not be JVMTI_ERROR_NONE");
        }
        this.error = error;
    }

    public JVMTIError getError() {
        return error;
    }
}

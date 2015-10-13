package org.apache.hadoop.metrics2.sink.elasticsearch;

public class UnableToConnectException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String connectUrl;

    public UnableToConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnableToConnectException(String message) {
        super(message);
    }

    public UnableToConnectException(Throwable cause) {
        super(cause);
    }

    public UnableToConnectException setConnectUrl(String connectUrl) {
        this.connectUrl = connectUrl;
        return this;
    }

    public String getConnectUrl() {
        return connectUrl;
    }
}

package splat.executor;

import splat.SplatException;

public class ExecutionException extends SplatException {

    public ExecutionException(String msg, int line, int column) {
        super(msg, line, column);
    }
}


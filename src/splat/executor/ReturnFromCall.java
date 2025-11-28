package splat.executor;

public class ReturnFromCall extends Exception {
    private static final long serialVersionUID = 1L;

    private final Value result;

    public ReturnFromCall(Value value) {
        this.result = value;
    }

    public Value getValue() {
        return result;
    }

    public Value value() {
        return result;
    }
}


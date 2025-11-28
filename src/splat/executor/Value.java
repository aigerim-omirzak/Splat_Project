package splat.executor;

import splat.semanticanalyzer.Type;

public class Value {
    private final Type type;
    private final Object rawValue;

    public Value(Type type, Object value) {
        this.type = type;
        this.rawValue = value;
    }

    public static Value ofInteger(int v) {
        return new Value(Type.INTEGER, v);
    }

    public static Value ofBoolean(boolean v) {
        return new Value(Type.BOOLEAN, v);
    }

    public static Value ofString(String v) {
        return new Value(Type.STRING, v);
    }

    public static Value voidValue() {
        return new Value(Type.VOID, null);
    }

    public static Value defaultFor(Type type) {
        switch (type) {
            case INTEGER:
                return ofInteger(0);
            case BOOLEAN:
                return ofBoolean(false);
            case STRING:
                return ofString("");
            case VOID:
                return voidValue();
            default:
                throw new IllegalArgumentException("Unknown type for default value: " + type);
        }
    }

    public Type getType() {
        return type;
    }

    public Object getRaw() {
        return rawValue;
    }

    public Object getValue() {
        return rawValue;
    }

    public int asInt() {
        return (Integer) rawValue;
    }

    public boolean asBoolean() {
        return (Boolean) rawValue;
    }

    public String asString() {
        return (String) rawValue;
    }
}


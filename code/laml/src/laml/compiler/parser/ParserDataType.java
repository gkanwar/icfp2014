package laml.compiler.parser;

/**
 * Data type definition. Integer, cons, or closure. If closure, how many args to
 * take.
 */
public class ParserDataType {
    public enum DataType {
        INTEGER, CLOSURE;
    }

    public final DataType type;
    // Only for CLOSURE type.
    public final int numArgs;

    public ParserDataType(DataType type) {
        if (type == DataType.CLOSURE) {
            throw new RuntimeException("Closure type must specify num args.");
        }
        this.type = type;
        this.numArgs = -1;
    }

    public ParserDataType(DataType type, int numArgs) {
        if (type != DataType.CLOSURE) {
            throw new RuntimeException(
                    "Non-closure type should not use numArgs constructor");
        }
        this.type = type;
        this.numArgs = numArgs;
    }

    public static ParserDataType integerType() {
        return new ParserDataType(DataType.INTEGER);
    }

    public static ParserDataType closureType(int numArgs) {
        return new ParserDataType(DataType.CLOSURE, numArgs);
    }

    public static boolean matchesIntegerType(ParserDataType type) {
        return type == null || type.type == DataType.INTEGER;
    }

    public static boolean matchesClosureType(ParserDataType type, int numArgs) {
        return type == null
                || (type.type == DataType.CLOSURE && type.numArgs == numArgs);
    }

    public static boolean matches(ParserDataType type1, ParserDataType type2) {
        if (type1 == null || type2 == null) {
            return true;
        }
        return (type1.type == DataType.INTEGER && type2.type == DataType.INTEGER)
                ||
                (type1.type == DataType.CLOSURE
                        && type2.type == DataType.CLOSURE && type1.numArgs == type2.numArgs);
    }
}

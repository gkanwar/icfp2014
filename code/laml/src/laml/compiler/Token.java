package laml.compiler;

/**
 * A single GCC assembly token.
 */
public class Token {
    public enum TokenType {
        LABEL, CONST, OP, LABEL_DEF
    };

    private String value;

    public final TokenType type;

    public Token(TokenType type, String value) {
        if (type != TokenType.LABEL &&
                type != TokenType.OP &&
                type != TokenType.LABEL_DEF) {
            throw new RuntimeException(
                    "String token must be of type LABEL, REG, or OP");
        }
        if (type == TokenType.LABEL_DEF) {
            this.value = value + ":";
        } else {
            this.value = value;
        }
        this.type = type;
    }

    public Token(TokenType type, int value) {
        if (type != TokenType.CONST) {
            throw new RuntimeException("Int token must be of type CONST");
        }
        this.value = Integer.toString(value);
        this.type = type;
    }

    @Override
    public String toString() {
        return value;
    }
}

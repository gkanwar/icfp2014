package laml.compiler;

/**
 * A single GCC assembly token.
 */
public class Token {
    enum TokenType {
        LABEL, CONST, OP
    };

    private String value;

    public final TokenType type;

    public Token(TokenType type, String value) {
        if (type != TokenType.LABEL && type != TokenType.OP) {
            throw new RuntimeException(
                    "String token must be of type LABEL, REG, or OP");
        }
        this.value = value;
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

package laml.compiler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import laml.compiler.Token.TokenType;

/**
 * Representation of a single line of GCC assembly.
 */
public class Line {
    private List<Token> code;
    private String comment;

    public Line(List<Token> code, String comment) {
        this.code = code;
        this.comment = comment;
    }

    /**
     * Setter for the comment section of the line. Compiler will want to
     * annotate code.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Mutator to replace labels with const addresses.
     */
    public void replaceLabels(Map<String, Integer> labelMap) {
        for (int i = 0; i < code.size(); ++i) {
            Token t = code.get(i);
            if (t.type == TokenType.LABEL) {
                if (labelMap.containsKey(t.toString())) {
                    int address = labelMap.get(t.toString());
                    Token newToken = new Token(TokenType.CONST, address);
                    code.set(i, newToken);
                } else {
                    throw new RuntimeException(
                            "Label map does not contain label: " + t.toString());
                }
            }
        }
    }

    /**
     * Whether this line contains actual code or just a comment
     */
    public boolean isEmpty() {
        return code.size() == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (code.size() == 0) {
            // Cannot have a hanging comment (must be attached to code). Replace
            // with empty line.
            return "";
        }
        for (Token t : code) {
            sb.append(t.toString());
            sb.append(" ");
        }
        sb.append("; ");
        if (comment.contains("\n")) {
            throw new RuntimeException("Comment may not contain newlines: "
                    + comment);
        }
        sb.append(comment);
        return sb.toString();
    }

    public static Line makeLabelDef(String label, String comment) {
        return new Line(Arrays.asList(
                new Token(TokenType.LABEL_DEF, label)),
                comment);
    }

    public static Line makeRtn(String comment) {
        return new Line(Arrays.asList(
                new Token(TokenType.OP, "RTN")),
                comment);
    }
}

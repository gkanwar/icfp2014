package laml.compiler.lexer;

import java.util.ArrayList;
import java.util.List;

public class LexerNode {
    public enum NodeType {
        FUNCTION, VARIABLE
    };

    public List<LexerNode> children;
    // VARIABLE type
    public String token;
    // FUNCTION type
    public LexerNode op;
    public NodeType type;
    // Line number meta-data
    private int lineNum;

    public LexerNode(NodeType type, String token, int lineNum) {
        if (type != NodeType.VARIABLE) {
            throw new RuntimeException(
                    "String token is only valid for variable type LexerNode");
        }
        children = new ArrayList<LexerNode>();
        this.type = type;
        this.token = token;
        this.lineNum = lineNum;
    }

    public LexerNode(NodeType type, LexerNode op, int lineNum) {
        if (type != NodeType.FUNCTION) {
            throw new RuntimeException(
                    "Node op is only valid for function type LexerNode");
        }
        children = new ArrayList<LexerNode>();
        this.type = type;
        this.op = op;
        this.lineNum = lineNum;
    }

    public int getLineNum() {
        return lineNum;
    }

    @Override
    public String toString() {
        if (type == NodeType.VARIABLE) {
            return token;
        }
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(op.toString());
            sb.append(" ");
            for (LexerNode child : children) {
                sb.append(child.toString());
                sb.append(" ");
            }
            sb.append(")");
            return sb.toString();
        }
    }
}

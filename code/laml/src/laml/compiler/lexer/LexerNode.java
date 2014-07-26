package laml.compiler.lexer;

import java.util.ArrayList;
import java.util.List;

public class LexerNode {
    enum NodeType {
        FUNCTION, VARIABLE
    };

    public List<LexerNode> children;
    public String token;
    public NodeType type;

    public LexerNode(NodeType type, String token) {
        children = new ArrayList<LexerNode>();
        this.type = type;
        this.token = token;
    }
}

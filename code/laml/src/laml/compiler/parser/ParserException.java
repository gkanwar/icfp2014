package laml.compiler.parser;

import laml.compiler.lexer.LexerNode;

public class ParserException extends RuntimeException {

    public ParserException(String cause, LexerNode failedNode) {
        super(cause +
                "\nFailed at node: " + failedNode.toString() +
                "\nOn line: " + failedNode.getLineNum());
    }

    private static final long serialVersionUID = -5832939526053052948L;
}

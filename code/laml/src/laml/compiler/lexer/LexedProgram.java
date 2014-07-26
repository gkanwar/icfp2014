package laml.compiler.lexer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;

import laml.compiler.lexer.LexerNode.NodeType;

/**
 * Internal tree representation of a lexed LaML program. Implicitly wraps the
 * top-level sequence of commands in a single begin, so that the output of the
 * last statement is the output of the program.
 */
public class LexedProgram {
    public LexerNode rootNode;

    public LexedProgram(LexerNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Read characters from the input until a non-whitespace. The non-whitespace
     * character is pushed back on the stream, so only whitespace characters are
     * removed.
     */
    public static void eatWhitespace(PushbackInputStream is) {
        try {
            char next = (char) is.read();
            if (!Character.isWhitespace(next)) {
                is.unread(next);
            }
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Check whether the stream is done by reading in a character and checking
     * for the special -1 value.
     */
    public static boolean isStreamDone(PushbackInputStream is) {
        try {
            int next;
            next = is.read();
            is.unread(next);
            // Depending on the underlying input stream, we'll get -1 or 255.
            // ByteArrayStream gives 255, since we're dealing in bytes, but
            // char is unicode (two bytes).
            return next == 255 || next == -1;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Try to read a ')' off the input stream. Returns whether or not ')' was
     * found, and pushes the character back if it is not ')'.
     */
    public static boolean tryEatNodeEnd(PushbackInputStream is) {
        try {
            char next = (char) is.read();
            if (next != ')') {
                is.unread(next);
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Ensure that a token is valid. Cannot be empty or contain parentheses.
     */
    public static void checkValidToken(String token) {
        if (token.length() == 0) {
            throw new RuntimeException("Invalid token of length 0");
        }
        if (token.contains("(")) {
            throw new RuntimeException("Token may not contain '(': " + token);
        }
        if (token.contains(")")) {
            throw new RuntimeException("Token may not contain ')': " + token);
        }
        if (token.matches(".*\\s+.*")) {
            throw new RuntimeException("Token may not contain whitespace: "
                    + token);
        }
    }

    /**
     * Read in a token from input stream. Token ends on whitespace or ')'
     * character.
     */
    public static String getToken(PushbackInputStream is) {
        StringBuilder sb = new StringBuilder();
        try {
            while (!isStreamDone(is)) {
                char next = (char) is.read();
                if (Character.isWhitespace(next) || next == ')') {
                    is.unread(next);
                    return sb.toString();
                } else {
                    sb.append(next);
                }
            }
        } catch (IOException e) {
            // Input stream done, return what we got
            return sb.toString();
        }
        return sb.toString();
    }

    /**
     * Lex a node from the input. A node is a function call of the form:
     * (op[arg]*). Input must contain a node.
     */
    public static LexerNode lexNode(PushbackInputStream is) {
        try {
            char next = (char) is.read();
            if (next != '(') {
                // VARIABLE
                is.unread(next);
                String var = getToken(is);
                checkValidToken(var);
                return new LexerNode(NodeType.VARIABLE, var);
            } else {
                // FUNCTION
                eatWhitespace(is);
                String op = getToken(is);
                checkValidToken(op);
                LexerNode node = new LexerNode(NodeType.FUNCTION, op);
                while (!isStreamDone(is)) {
                    eatWhitespace(is);
                    if (tryEatNodeEnd(is)) {
                        break;
                    }
                    if (isStreamDone(is)) {
                        throw new RuntimeException(
                                "Unexpected node end without closing paren.");
                    }
                    LexerNode child = lexNode(is);
                    node.children.add(child);
                }
                return node;
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Expected a node, but got an IOException");
        }
    }

    /**
     * Lex the program fed in through the given input stream.
     */
    public static LexedProgram lexProgram(InputStream progIn) {
        // All programs wrapped in a begin
        LexerNode root = new LexerNode(NodeType.FUNCTION, "begin");
        LexedProgram prog = new LexedProgram(root);

        PushbackInputStream is = new PushbackInputStream(progIn);
        while (!isStreamDone(is)) {
            eatWhitespace(is);
            if (isStreamDone(is)) {
                break;
            }
            LexerNode node = lexNode(is);
            root.children.add(node);
        }
        return prog;
    }

    /**
     * Helper function to lex a program from string. It's not recommended to use
     * this for the main compiling flow.
     */
    public static LexedProgram lexProgramString(String progString) {
        InputStream in = new ByteArrayInputStream(progString.getBytes(Charset
                .defaultCharset()));
        return lexProgram(in);
    }
}

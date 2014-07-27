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
     * Subclass of pushback input stream to track line numbers.
     */
    public static class LexerInputStream extends PushbackInputStream {
        int lineNum;

        public LexerInputStream(InputStream is) {
            super(is);
            lineNum = 1;
        }

        @Override
        public int read() throws IOException {
            int out = super.read();
            if ((char) out == '\n') {
                lineNum += 1;
            }
            return out;
        }

        @Override
        public void unread(int b) throws IOException {
            super.unread(b);
            if ((char) b == '\n') {
                lineNum -= 1;
            }
        }

        public int getLineNum() {
            return lineNum;
        }
    }

    /**
     * Custom exception class to augment reason with line number.
     */
    public static class LexerException extends RuntimeException {
        public LexerException(String cause, LexerInputStream is) {
            super("\nLine " +
                    (is == null ? "Unknown" : is.getLineNum()) +
                    ": " + cause);
        }

        // NOTE(gkanwar): Auto-generated
        private static final long serialVersionUID = 4191797007213323463L;
    }

    /**
     * Read characters from the input until non-whitespace and non-comment. The
     * non-whitespace character is pushed back on the stream, so only whitespace
     * and comment characters are removed.
     * 
     * Comments are started by a ';' character, and ended by a '\n' character.
     */
    public static void eatWhitespaceAndComments(LexerInputStream is) {
        try {
            boolean commentMode = false;
            while (true) {
                char next = (char) is.read();
                if (commentMode) {
                    if (next == '\n') {
                        commentMode = false;
                    }
                    continue;
                }
                else if (next == ';') {
                    commentMode = true;
                    continue;
                }
                // Not a comment char
                else if (!Character.isWhitespace(next)) {
                    is.unread(next);
                    return;
                }
            }
        } catch (IOException e) {
            return;
        }
    }

    /**
     * Check whether the stream is done by reading in a character and checking
     * for the special -1 value.
     */
    public static boolean isStreamDone(LexerInputStream is) {
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
    public static boolean tryEatNodeEnd(LexerInputStream is) {
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
    public static void checkValidToken(String token, LexerInputStream is) {
        if (token.length() == 0) {
            throw new LexerException("Invalid token of length 0", is);
        }
        if (token.contains("(")) {
            throw new LexerException("Token may not contain '(': " + token, is);
        }
        if (token.contains(")")) {
            throw new LexerException("Token may not contain ')': " + token, is);
        }
        if (token.matches(".*\\s+.*")) {
            throw new LexerException("Token may not contain whitespace: "
                    + token, is);
        }
    }

    /**
     * Read in a token from input stream. Token ends on whitespace or ')'
     * character.
     */
    public static String getToken(LexerInputStream is) {
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
    public static LexerNode lexNode(LexerInputStream is) {
        try {
            int lineNum = is.getLineNum();
            char next = (char) is.read();
            if (next != '(') {
                // VARIABLE
                is.unread(next);
                String var = getToken(is);
                checkValidToken(var, is);
                return new LexerNode(NodeType.VARIABLE, var, lineNum);
            } else {
                // FUNCTION
                eatWhitespaceAndComments(is);
                LexerNode op = lexNode(is);
                LexerNode node = new LexerNode(NodeType.FUNCTION, op, lineNum);
                while (!isStreamDone(is)) {
                    eatWhitespaceAndComments(is);
                    if (tryEatNodeEnd(is)) {
                        break;
                    }
                    if (isStreamDone(is)) {
                        throw new LexerException(
                                "Unexpected node end without closing paren.",
                                is);
                    }
                    LexerNode child = lexNode(is);
                    node.children.add(child);
                }
                return node;
            }
        } catch (IOException e) {
            throw new LexerException(
                    "Expected a node, but got an IOException", is);
        }
    }

    /**
     * Lex the program fed in through the given input stream.
     */
    public static LexedProgram lexProgram(InputStream progIn) {
        // All programs wrapped in a begin
        LexerNode root = new LexerNode(NodeType.FUNCTION, new LexerNode(
                NodeType.VARIABLE, "begin", 0), 0);
        LexedProgram prog = new LexedProgram(root);

        LexerInputStream is = new LexerInputStream(progIn);
        while (!isStreamDone(is)) {
            eatWhitespaceAndComments(is);
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

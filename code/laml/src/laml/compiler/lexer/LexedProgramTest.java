package laml.compiler.lexer;

import static org.junit.Assert.assertEquals;
import laml.compiler.lexer.LexerNode.NodeType;

import org.junit.Test;

public class LexedProgramTest {

    @Test
    public void testCheckValidToken() {
        String valid1 = "asdf";
        String valid2 = "asdf_asdf_adsf";
        String valid3 = "test12345678!@#$%^&*";
        LexedProgram.checkValidToken(valid1);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckValidTokenInvalidToken1() {
        String invalid1 = "";
        LexedProgram.checkValidToken(invalid1);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckValidTokenInvalidToken2() {
        String invalid2 = "(asdf";
        LexedProgram.checkValidToken(invalid2);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckValidTokenInvalidToken3() {
        String invalid3 = "asdf)";
        LexedProgram.checkValidToken(invalid3);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckValidTokenInvalidToken4() {
        String invalid4 = "asdf asdf";
        LexedProgram.checkValidToken(invalid4);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckValidTokenInvalidToken5() {
        String invalid5 = "asdf\nasdf";
        LexedProgram.checkValidToken(invalid5);
    }

    @Test
    public void testLexProgramStringBasicProgram() {
        String prog1 = "(+ 1 2)";
        LexedProgram p = LexedProgram.lexProgramString(prog1);
        LexerNode root = p.rootNode;
        // Root should be "begin"
        assertEquals(NodeType.FUNCTION, root.type);
        assertEquals("begin", root.token);
        assertEquals(1, root.children.size());
        LexerNode child = root.children.get(0);
        assertEquals(NodeType.FUNCTION, child.type);
        assertEquals("+", child.token);
        assertEquals(2, child.children.size());
        LexerNode arg1 = child.children.get(0);
        LexerNode arg2 = child.children.get(1);
        assertEquals(NodeType.VARIABLE, arg1.type);
        assertEquals("1", arg1.token);
        assertEquals(NodeType.VARIABLE, arg2.type);
        assertEquals("2", arg2.token);
    }

    @Test
    public void testLexProgramStringNoText() {
        String prog = "";
        LexedProgram p = LexedProgram.lexProgramString(prog);
        LexerNode root = p.rootNode;
        // Root should be "begin"
        assertEquals(NodeType.FUNCTION, root.type);
        assertEquals("begin", root.token);
        assertEquals(0, root.children.size());
    }

    @Test
    public void testLexProgramStringComplexWorks() {
        String prog = "1 2 (+ 3 4) (cons 3 (cons 5 6))";
        LexedProgram p = LexedProgram.lexProgramString(prog);
        LexerNode root = p.rootNode;
        // Root should be "begin"
        assertEquals(NodeType.FUNCTION, root.type);
        assertEquals("begin", root.token);
        assertEquals(4, root.children.size());
    }

    @Test(expected = RuntimeException.class)
    public void testLexProgramStringFailedUnmatchedParens1() {
        String prog = "(";
        LexedProgram p = LexedProgram.lexProgramString(prog);
    }

    @Test(expected = RuntimeException.class)
    public void testLexProgramStringFailedUnbalancedParens2() {
        String prog = "())";
        LexedProgram p = LexedProgram.lexProgramString(prog);
    }

    @Test(expected = RuntimeException.class)
    public void testLexProgramStringFailedUnbalancedParens3() {
        String prog = "(()";
        LexedProgram p = LexedProgram.lexProgramString(prog);
    }

    @Test(expected = RuntimeException.class)
    public void testLexProgramStringFailedNoOp() {
        String prog = "()";
        LexedProgram p = LexedProgram.lexProgramString(prog);
    }
}

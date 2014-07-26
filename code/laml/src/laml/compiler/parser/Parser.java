package laml.compiler.parser;

import java.util.Arrays;

import laml.compiler.LabelledFunction;
import laml.compiler.Line;
import laml.compiler.RelativeProgram;
import laml.compiler.Token;
import laml.compiler.Token.TokenType;
import laml.compiler.lexer.LexedProgram;
import laml.compiler.lexer.LexerNode;
import laml.compiler.lexer.LexerNode.NodeType;

public class Parser {
    /**
     * Parse a node to produce compiled code.
     */
    public static CodeSequence parseNode(LexerNode functionNode, EnvFrame env) {
        CodeSequence c = new CodeSequence();
        if (functionNode.type == NodeType.VARIABLE) {
            try {
                // Try treating this as a numeric literal
                int literal = Integer.parseInt(functionNode.token);
                c.code.add(new Line(Arrays.asList(
                        new Token(TokenType.OP, "LDC"), new Token(
                                TokenType.CONST, literal)), ""));
            } catch (NumberFormatException e) {
                // Not a numeric literal. Resolve this as a variable.
                // TODO(gkanwar)
            }
        } else { // FUNCTION
            if (functionNode.token.equals("+")) {
                if (functionNode.children.size() != 2) {
                    throw new RuntimeException("+ takes two arguments");
                }
                CodeSequence buildArg1 = parseNode(
                        functionNode.children.get(0), env);
                CodeSequence buildArg2 = parseNode(
                        functionNode.children.get(1), env);
                c.code.addAll(buildArg1.code);
                c.code.addAll(buildArg2.code);
                c.code.add(new Line(Arrays
                        .asList(new Token(TokenType.OP, "ADD")), ""));
            } else if (functionNode.token.equals("begin")) {
                for (LexerNode child : functionNode.children) {
                    CodeSequence childCode = parseNode(child, env);
                    c.code.addAll(childCode.code);
                }
                c.code.add(new Line(Arrays
                        .asList(new Token(TokenType.OP, "RTN")), ""));
            } else {
                throw new RuntimeException("Unknown token: "
                        + functionNode.token);
            }
        }
        return c;
    }

    /**
     * Parse a lexed program.
     */
    public static RelativeProgram parseProgram(LexedProgram lexProg) {
        RelativeProgram prog = new RelativeProgram();
        LabelledFunction mainFunc = new LabelledFunction("main");
        CodeSequence mainCode = parseNode(lexProg.rootNode, new EnvFrame());
        for (Line l : mainCode.code) {
            mainFunc.addLine(l);
        }
        prog.addLabelledFunction(mainFunc);
        return prog;
    }
}

package laml.compiler.parser;

import java.util.Arrays;

import laml.compiler.Line;
import laml.compiler.RelativeProgram;
import laml.compiler.Token;
import laml.compiler.Token.TokenType;
import laml.compiler.lexer.LexedProgram;
import laml.compiler.lexer.LexerNode;
import laml.compiler.lexer.LexerNode.NodeType;
import laml.compiler.parser.EnvFrame.Binding;

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
                        new Token(TokenType.OP, "LDC"),
                        new Token(TokenType.CONST, literal)),
                        ""));
            } catch (NumberFormatException e) {
                // Not a numeric literal. Resolve this as a variable.
                String symbol = functionNode.token;
                c.code.add(new Line(Arrays.asList(
                        new Token(TokenType.OP, "LD"),
                        new Token(TokenType.CONST, env
                                .findBindingDepth(symbol)),
                        new Token(TokenType.CONST, env
                                .findBindingIndex(symbol))),
                        "Var " + symbol));
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
            } else if (functionNode.token.equals("define")) {
                System.err.println("Parsing define");
                if (functionNode.children.size() != 2) {
                    throw new RuntimeException("define takes two arguments");
                }
                LexerNode binding = functionNode.children.get(0);
                if (binding.type != NodeType.VARIABLE) {
                    // We can do something like (define (x) (+ x 1)) yet.
                    // Need to do:
                    // (define x (lambda (x) (+ x 1))).
                    throw new RuntimeException(
                            "define doesn't support argument-style binding shortcuts yet");
                }
                CodeSequence definition = parseNode(
                        functionNode.children.get(1), env);
                // TODO(gkanwar): Add typing, this just assumes everything is
                // an int for now, and never bothers to check.
                Binding envBinding = new Binding(binding.token,
                        Binding.ParserDataType.INTEGER, definition);
                System.out.println("Adding binding for " + binding.token);
                env.addBinding(binding.token, envBinding);
                // NOTE(gkanwar): No code added directly here. All definitions
                // will be lifted to the beginning of the function call, where
                // a new environment scope is created and defined.

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
        EnvFrame rootEnv = new EnvFrame();
        CodeSequence mainCode = parseNode(lexProg.rootNode, rootEnv);
        ParserFunction mainFunc = new ParserFunction("main", rootEnv, mainCode);
        prog.addLabeledFunctions(mainFunc.toLabeledFunctions());
        return prog;
    }
}

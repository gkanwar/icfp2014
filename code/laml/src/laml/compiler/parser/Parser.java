package laml.compiler.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import laml.compiler.Line;
import laml.compiler.RelativeProgram;
import laml.compiler.Token;
import laml.compiler.Token.TokenType;
import laml.compiler.lexer.LexedProgram;
import laml.compiler.lexer.LexerNode;
import laml.compiler.lexer.LexerNode.NodeType;
import laml.compiler.parser.EnvFrame.Binding;
import laml.compiler.parser.EnvFrame.Binding.ParserDataType;

public class Parser {
    /**
     * Parse a node to produce compiled code.
     * 
     * @param functionNode lexer-generated node containing the token and
     *            children
     * @param env environment frame with parent pointer, used to look up and add
     *            symbols
     * @param globalFuncMap map of global function names to parser functions.
     *            These names are only used for label generation, so they should
     *            be a unique generated name.
     */
    public static CodeSequence parseNode(LexerNode functionNode, EnvFrame env,
            Map<String, ParserFunction> globalFuncMap) {
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
                        functionNode.children.get(0), env, globalFuncMap);
                CodeSequence buildArg2 = parseNode(
                        functionNode.children.get(1), env, globalFuncMap);
                c.code.addAll(buildArg1.code);
                c.code.addAll(buildArg2.code);
                c.code.add(new Line(Arrays
                        .asList(new Token(TokenType.OP, "ADD")), ""));
            } else if (functionNode.token.equals("begin")) {
                for (LexerNode child : functionNode.children) {
                    CodeSequence childCode = parseNode(child, env,
                            globalFuncMap);
                    c.code.addAll(childCode.code);
                }
            } else if (functionNode.token.equals("define")) {
                if (functionNode.children.size() != 2) {
                    throw new RuntimeException(
                            "define takes two arguments: the binding and definition");
                }
                LexerNode binding = functionNode.children.get(0);
                if (binding.type != NodeType.VARIABLE) {
                    // We can't do implicit lambda definition like
                    // (define (x) (+ x 1)) yet.
                    // Need to do:
                    // (define x (lambda (x) (+ x 1))).
                    throw new RuntimeException(
                            "define doesn't support argument-style binding shortcuts yet");
                }
                CodeSequence definition = parseNode(
                        functionNode.children.get(1), env, globalFuncMap);
                // TODO(gkanwar): Add typing, this just assumes everything is
                // an int for now, and never bothers to check.
                Binding envBinding = new Binding(binding.token,
                        Binding.ParserDataType.INTEGER, definition);
                env.addBinding(binding.token, envBinding);
                // NOTE(gkanwar): No code added directly here. All definitions
                // will be lifted to the beginning of the function call, where
                // a new environment scope is created and defined.

            }
            else if (functionNode.token.equals("lambda")) {
                if (functionNode.children.size() != 2) {
                    throw new RuntimeException(
                            "lambda takes two arguments: the binding and definition");
                }
                // Environment to hold argument bindings
                EnvFrame argEnv = new EnvFrame(env);
                // Hack to check for list of bindings; they are interpreted by
                // the lexer as a function call, because really they would be,
                // but we're just faking some post-processing here instead.
                LexerNode bindingNode = functionNode.children.get(0);
                if (bindingNode.type != NodeType.FUNCTION) {
                    throw new RuntimeException(
                            "lambda first arg must be a list of bindings");
                }
                // First binding pulled from token, the rest from children
                // No bindings have code definitions -- as arguments it's the
                // caller's responsibility to define these.
                argEnv.addBinding(bindingNode.token, new Binding(
                        bindingNode.token, ParserDataType.INTEGER,
                        new CodeSequence()));
                for (LexerNode bindingChild : bindingNode.children) {
                    if (bindingChild.type != NodeType.VARIABLE) {
                        throw new RuntimeException(
                                "lambda bindings must all be var type");
                    }
                    argEnv.addBinding(bindingChild.token, new Binding(
                            bindingChild.token, ParserDataType.INTEGER,
                            new CodeSequence()));
                }
                // Wrap argEnv in another env which holds local variable
                // definitions.
                EnvFrame funcEnv = new EnvFrame(argEnv);
                CodeSequence definition = parseNode(
                        functionNode.children.get(1), funcEnv, globalFuncMap);
                String uniqueName = getUniqueName(globalFuncMap);
                globalFuncMap.put(uniqueName, new ParserFunction(uniqueName,
                        funcEnv, definition));
                c.code.add(new Line(Arrays.asList(
                        new Token(TokenType.OP, "LDF"),
                        new Token(TokenType.LABEL, uniqueName)),
                        ""));
            }
            else if (env.bindingMap.containsKey(functionNode.token)) {
                // User-defined function
                // TODO(gkanwar): Check that this is actually a function, check
                // arg numbers
                for (LexerNode child : functionNode.children) {
                    CodeSequence definition = parseNode(child, env,
                            globalFuncMap);
                    c.code.addAll(definition.code);
                }
                String symbol = functionNode.token;
                c.code.add(new Line(Arrays.asList(
                        new Token(TokenType.OP, "LD"),
                        new Token(TokenType.CONST, env
                                .findBindingDepth(symbol)),
                        new Token(TokenType.CONST, env
                                .findBindingIndex(symbol))),
                        "Load func " + symbol));
                c.code.add(new Line(Arrays.asList(
                        new Token(TokenType.OP, "AP"),
                        new Token(TokenType.CONST,
                                functionNode.children.size())),
                        "Func call " + symbol));
            }
            else {
                throw new RuntimeException("Unknown token: "
                        + functionNode.token);
            }
        }
        return c;
    }

    /**
     * Generate a function name which doesn't collide with any functions in
     * globalFuncMap.
     * 
     * @param globalFuncMap Current global function map
     */
    private static String getUniqueName(
            Map<String, ParserFunction> globalFuncMap) {
        int i = 0;
        while (true) {
            String tryName = "func" + i;
            if (!globalFuncMap.containsKey(tryName)) {
                return tryName;
            }
            ++i;
        }
    }

    /**
     * Parse a lexed program.
     */
    public static RelativeProgram parseProgram(LexedProgram lexProg) {
        RelativeProgram prog = new RelativeProgram();
        EnvFrame rootEnv = new EnvFrame();
        Map<String, ParserFunction> globalFuncMap = new HashMap<String, ParserFunction>();
        CodeSequence mainCode = parseNode(lexProg.rootNode, rootEnv,
                globalFuncMap);
        ParserFunction mainFunc = new ParserFunction("main", rootEnv, mainCode);
        prog.addLabeledFunctions(mainFunc.toLabeledFunctions());
        for (ParserFunction func : globalFuncMap.values()) {
            prog.addLabeledFunctions(func.toLabeledFunctions());
        }
        return prog;
    }
}

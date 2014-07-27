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
    private static CodeSequence parseNArgBuiltIn(String assemblyOp,
            int numArgs, LexerNode functionNode, EnvFrame env,
            Map<String, ParserLabeledBlock> globalFuncMap) {
        if (functionNode.children.size() != numArgs) {
            throw new RuntimeException("+ takes " + numArgs + " arguments");
        }
        CodeSequence out = new CodeSequence();
        for (LexerNode child : functionNode.children) {
            out.code.addAll(parseNode(child, env, globalFuncMap).code);
        }
        out.code.add(new Line(Arrays
                .asList(new Token(TokenType.OP, assemblyOp)), ""));
        return out;
    }

    public static CodeSequence parseNode(LexerNode functionNode, EnvFrame env,
            Map<String, ParserLabeledBlock> globalFuncMap) {
        return parseNode(functionNode, env, globalFuncMap, false);
    }

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
     * @param headerCode whether or not this code will go in the header. If the
     *            code is going in the header, then it must not reference
     *            anything in the innermost env. Instead, it should copy those
     *            definitions in directly.
     */
    public static CodeSequence parseNode(LexerNode functionNode, EnvFrame env,
            Map<String, ParserLabeledBlock> globalFuncMap, boolean headerCode) {
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
                // findBindingDepth / findBindingIndex will complain if cannot
                // be resolved.
                String symbol = functionNode.token;
                int depth = env.findBindingDepth(symbol);
                // Special case: Cannot reference innermost environment from
                // header code, since the header code is defining that
                // environment using a dummy env and RAP. Instead, copy
                // definition.
                if (headerCode && depth == 0) {
                    c.code.addAll(env.bindingMap.get(symbol).definition.code);
                } else {
                    c.code.add(Line.makeLd(depth, env.findBindingIndex(symbol),
                            "Var " + symbol));
                }
            }
        } else { // FUNCTION
            LexerNode op = functionNode.op;
            if (op.type == NodeType.VARIABLE) {
                // TODO(gkanwar): Built-ins are a hack right now.
                if (op.token.equals("+")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "ADD", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("-")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "SUB", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("*")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "MUL", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("/")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "DIV", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("=")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "CEQ", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals(">")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "CGT", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals(">=")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "CGTE", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("cons")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "CONS", 2,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("atom")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "ATOM", 1,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("car")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "CAR", 1,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("cdr")) {
                    c.code.addAll(parseNArgBuiltIn(
                            "CDR", 1,
                            functionNode, env, globalFuncMap).code);
                }
                else if (op.token.equals("begin")) {
                    for (LexerNode child : functionNode.children) {
                        CodeSequence childCode = parseNode(child, env,
                                globalFuncMap);
                        c.code.addAll(childCode.code);
                    }
                }
                else if (op.token.equals("if")) {
                    if (functionNode.children.size() != 3) {
                        throw new ParserException(
                                "if takes three arguments: "
                                        + "the prediction, true branch, and false branch. "
                                        +
                                        "Got " + functionNode.children.size()
                                        + " arguments.", functionNode);
                    }
                    CodeSequence predicate = parseNode(
                            functionNode.children.get(0), env, globalFuncMap);
                    c.code.addAll(predicate.code);
                    String trueBranchLabel = getUniqueName(globalFuncMap);
                    // HACK: Claim this label so an inner branch can't conflict
                    globalFuncMap.put(trueBranchLabel, null);
                    ParserBranch trueBranch = new ParserBranch(trueBranchLabel,
                            parseNode(functionNode.children.get(1), env,
                                    globalFuncMap));
                    // Now replace the labeled block ref with the actual block
                    globalFuncMap.put(trueBranchLabel, trueBranch);
                    String falseBranchLabel = getUniqueName(globalFuncMap);
                    // HACK: Claim this label so an inner branch can't conflict
                    globalFuncMap.put(falseBranchLabel, null);
                    ParserBranch falseBranch = new ParserBranch(
                            falseBranchLabel,
                            parseNode(functionNode.children.get(2), env,
                                    globalFuncMap));
                    // Now replace the labeled block ref with the actual block
                    globalFuncMap.put(falseBranchLabel, falseBranch);
                    c.code.add(Line.makeSel(
                            trueBranchLabel, falseBranchLabel,
                            "Sel on predicate "
                                    + functionNode.children.get(0).toString()));
                }
                else if (op.token.equals("define")) {
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
                    // TODO(gkanwar): Add typing, this just assumes everything
                    // is an int for now, and never bothers to check
                    Binding envBinding = new Binding(binding.token,
                            Binding.ParserDataType.INTEGER);
                    env.addBinding(binding.token, envBinding);
                    // Use header parse mode, so variable resolutions in
                    // innermost env are replaced by data copy.
                    CodeSequence definition = parseNode(
                            functionNode.children.get(1), env,
                            globalFuncMap, true);
                    envBinding.setDefinition(definition);
                    // NOTE(gkanwar): No code added directly here. All
                    // definitions will be lifted to the beginning of the
                    // function call, where a new environment scope is created
                    // and defined.

                }
                else if (op.token.equals("lambda")) {
                    if (functionNode.children.size() != 2) {
                        throw new RuntimeException(
                                "lambda takes two arguments: the binding and definition");
                    }
                    // Environment to hold argument bindings
                    EnvFrame argEnv = new EnvFrame(env);
                    // Hack to check for list of bindings; they are interpreted
                    // by
                    // the lexer as a function call, because really they would
                    // be,
                    // but we're just faking some post-processing here instead.
                    LexerNode bindingNode = functionNode.children.get(0);
                    if (bindingNode.type != NodeType.FUNCTION) {
                        throw new RuntimeException(
                                "lambda first arg must be a list of bindings");
                    }
                    // First binding pulled from op, the rest from children
                    // No bindings have code definitions -- as arguments it's
                    // the
                    // caller's responsibility to define these.
                    if (bindingNode.op.type != NodeType.VARIABLE) {
                        throw new RuntimeException(
                                "lambda bindings must all be var type");
                    }
                    // HACK: Allow declaring a thunk using (lambda (THUNK) def)
                    if (bindingNode.op.token != "THUNK") {
                        argEnv.addBinding(bindingNode.op.token, new Binding(
                                bindingNode.op.token, ParserDataType.INTEGER));
                    }
                    for (LexerNode bindingChild : bindingNode.children) {
                        if (bindingChild.type != NodeType.VARIABLE) {
                            throw new RuntimeException(
                                    "lambda bindings must all be var type");
                        }
                        argEnv.addBinding(bindingChild.token, new Binding(
                                bindingChild.token, ParserDataType.INTEGER));
                    }
                    // Wrap argEnv in another env which holds local variable
                    // definitions.
                    EnvFrame funcEnv = new EnvFrame(argEnv);
                    CodeSequence definition = parseNode(
                            functionNode.children.get(1), funcEnv,
                            globalFuncMap);
                    String uniqueName = getUniqueName(globalFuncMap);
                    globalFuncMap.put(uniqueName, new ParserFunction(
                            uniqueName, funcEnv, definition));
                    c.code.add(new Line(Arrays.asList(
                            new Token(TokenType.OP, "LDF"),
                            new Token(TokenType.LABEL, uniqueName)),
                            ""));
                }
                else if (op.token.equals("list")) {
                    // First push all children
                    for (LexerNode child : functionNode.children) {
                        c.code.addAll(parseNode(child, env, globalFuncMap).code);
                    }
                    // Then push NIL
                    c.code.add(Line.makeNil(""));
                    // Then execute CONS once for each child
                    for (int i = 0; i < functionNode.children.size(); ++i) {
                        c.code.add(Line.makeCons(""));
                    }
                }
                else if (op.token.equals("tuple")) {
                    // First push all children
                    for (LexerNode child : functionNode.children) {
                        c.code.addAll(parseNode(child, env, globalFuncMap).code);
                    }
                    // Then execute CONS once for each child - 1
                    for (int i = 0; i < functionNode.children.size() - 1; ++i) {
                        c.code.add(Line.makeCons(""));
                    }
                }
                else {
                    // TODO(gkanwar): This is identical to the function op case,
                    // perhaps abstract this code...
                    // User-defined variable op
                    for (LexerNode child : functionNode.children) {
                        CodeSequence definition = parseNode(child, env,
                                globalFuncMap);
                        c.code.addAll(definition.code);
                    }
                    // Load the function and call it
                    c.code.addAll(parseNode(op, env, globalFuncMap).code);
                    c.code.add(new Line(Arrays.asList(
                            new Token(TokenType.OP, "AP"),
                            new Token(TokenType.CONST,
                                    functionNode.children.size())),
                            "Func call " + op.toString()));
                }
            }
            else {
                // TODO(gkanwar): Check that this is actually a function, check
                // arg numbers. Type checking is annoying...
                for (LexerNode child : functionNode.children) {
                    CodeSequence definition = parseNode(child, env,
                            globalFuncMap);
                    c.code.addAll(definition.code);
                }
                // Load function and call it
                c.code.addAll(parseNode(op, env, globalFuncMap).code);
                c.code.add(new Line(Arrays.asList(
                        new Token(TokenType.OP, "AP"),
                        new Token(TokenType.CONST,
                                functionNode.children.size())),
                        "Func call " + op.toString()));
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
            Map<String, ParserLabeledBlock> globalFuncMap) {
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
        Map<String, ParserLabeledBlock> globalFuncMap = new HashMap<String, ParserLabeledBlock>();
        CodeSequence mainCode = parseNode(lexProg.rootNode, rootEnv,
                globalFuncMap);
        ParserFunction mainFunc = new ParserFunction("main", rootEnv, mainCode);
        prog.addLabeledFunctions(mainFunc.toLabeledFunctions());
        for (ParserLabeledBlock func : globalFuncMap.values()) {
            prog.addLabeledFunctions(func.toLabeledFunctions());
        }
        return prog;
    }
}

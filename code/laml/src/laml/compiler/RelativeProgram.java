package laml.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import laml.compiler.Token.TokenType;

/**
 * A full GCC program, with relative addresses instead of absolute. Use
 * AddressResovlePostProcessor to create the final code.
 */
public class RelativeProgram {
    private List<Line> lines;
    private Map<String, Integer> labels;
    // Where to place the next labeled function
    private int nextAddr;

    public RelativeProgram() {
        lines = new ArrayList<Line>();
        labels = new HashMap<String, Integer>();
        nextAddr = 0;
    }

    public void addLabeledFunction(LabeledFunction function) {
        if (labels.containsKey(function.label)) {
            throw new RuntimeException("Duplicated label definition "
                    + function.label);
        }

        labels.put(function.label, nextAddr);
        lines.addAll(function.getCode().getLines());
        nextAddr += function.getNumInstructions();
    }

    public void addLabeledFunctions(List<LabeledFunction> functions) {
        for (LabeledFunction f : functions) {
            addLabeledFunction(f);
        }
    }

    /**
     * Translate all label addresses into actual const addresses
     */
    public AbsoluteProgram translate() {
        AbsoluteProgram p = new AbsoluteProgram();
        for (Line l : lines) {
            l.replaceLabels(labels);
            p.addLine(l);
        }
        return p;
    }

    public static RelativeProgram parseFromLabelledString(
            String relativeProgramString) {
        RelativeProgram p = new RelativeProgram();
        String[] lines = relativeProgramString.split("\n");
        LabeledFunction currentFunc = null;
        for (String l : lines) {
            String code;
            String comment;
            if (l.contains(";")) {
                // Has a comment
                int index = l.indexOf(';');
                code = l.substring(0, index);
                comment = l.substring(index + 1);
            } else {
                code = l;
                comment = "";
            }

            String[] tokenStrings = code.trim().split("\\s+");
            // Special case for empty line
            if (tokenStrings.length == 0) {
                continue;
            }
            // Hack to identify labels (we aren't using this parsing in the
            // compiler itself
            // so this is probably fine).
            if (tokenStrings.length == 1 && tokenStrings[0].endsWith(":")) {
                String label = tokenStrings[0].substring(0,
                        tokenStrings[0].length() - 1);
                // New label means start new LabelledFunction
                int nextAddr = 0;
                if (currentFunc != null) {
                    p.addLabeledFunction(currentFunc);
                }
                currentFunc = new LabeledFunction(label);
                continue;
            }
            List<Token> codeTokens = new ArrayList<Token>();
            boolean seenOp = false;
            for (String token : tokenStrings) {
                // Empty token -- either leading or trailing whitespace
                if (token.length() == 0) {
                    continue;
                }
                Token t;
                if (!seenOp) {
                    // This is the first token, so it's the operation
                    seenOp = true;
                    t = new Token(TokenType.OP, token);
                } else {
                    try {
                        int val = Integer.parseInt(token);
                        t = new Token(TokenType.CONST, val);
                    } catch (NumberFormatException e) {
                        t = new Token(TokenType.LABEL, token);
                    }
                }
                codeTokens.add(t);
            }
            Line codeLine = new Line(codeTokens, comment);
            if (currentFunc == null) {
                throw new RuntimeException(
                        "Program must start with a label (probably main)");
            }
            currentFunc.addLine(codeLine);
        }
        // Add the last labelled function
        if (currentFunc != null) {
            p.addLabeledFunction(currentFunc);
        }
        return p;
    }

    /**
     * Test by passing in a relative described function on stdin, and will
     * output the translated program to stdout.
     */
    public static void main(String[] args) {
        // Dumb way to convert stream to string:
        // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
        Scanner s = new Scanner(System.in).useDelimiter("\\A");
        String prog = s.hasNext() ? s.next() : "";
        String translated = RelativeProgram.parseFromLabelledString(prog)
                .translate().toString();
        System.out.println("\n");
        System.out.println(translated);
    }
}

package laml.compiler;

import java.util.ArrayList;

import laml.compiler.parser.CodeSequence;

public class LabeledFunction {
    public String label;
    // Body of the code.
    private CodeSequence body;
    private int numInstructions;

    public LabeledFunction(String label) {
        this.label = label;
        body = new CodeSequence();
        body.code.add(new Line(new ArrayList<Token>(), label));
        numInstructions = 0;
    }

    public void addLine(Line line) {
        body.code.add(line);
        if (!line.isEmpty()) {
            numInstructions++;
        }
    }

    public void addCodeSequence(CodeSequence seq) {
        body.code.addAll(seq.code);
        for (Line l : seq.code) {
            if (!l.isEmpty()) {
                numInstructions++;
            }
        }
    }

    public CodeSequence getCode() {
        return body;
    }

    public int getNumInstructions() {
        return numInstructions;
    }
}

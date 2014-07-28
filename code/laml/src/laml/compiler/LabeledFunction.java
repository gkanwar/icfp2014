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
        body.add(new Line(new ArrayList<Token>(), label));
        numInstructions = 0;
    }

    public void addLine(Line line) {
        body.add(line);
        if (!line.isEmpty()) {
            numInstructions++;
        }
    }

    public void addCodeSequence(CodeSequence seq) {
        body.addAll(seq.getLines());
        for (Line l : seq.getLines()) {
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

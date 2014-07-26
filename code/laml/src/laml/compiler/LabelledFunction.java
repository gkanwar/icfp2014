package laml.compiler;

import java.util.ArrayList;
import java.util.List;

public class LabelledFunction {
    public String label;
    public List<Line> lines;
    private int startAddr = -1;
    // How many actual instructions does this contain?
    private int numInstructions;

    public LabelledFunction(String label) {
        this.label = label;
        lines = new ArrayList<Line>();
        numInstructions = 0;
    }

    public void addLine(Line line) {
        lines.add(line);
        if (!line.isEmpty()) {
            numInstructions += 1;
        }
    }

    public void setStartAddr(int startAddr) {
        this.startAddr = startAddr;
    }

    public int getStartAddr() {
        return this.startAddr;
    }

    public int getNumInstructions() {
        return numInstructions;
    }
}

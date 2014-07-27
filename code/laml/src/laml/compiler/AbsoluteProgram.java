package laml.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * A final GCC program, using absolute addresses rather than labels. Calling
 * toString on this should produce a final runnable GCC String.
 */
public class AbsoluteProgram {
    // None of the lines should have LABEL tokens
    private List<Line> code;

    public AbsoluteProgram() {
        code = new ArrayList<Line>();
    }

    public void addLine(Line l) {
        code.add(l);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Line l : code) {
            sb.append(l.toString());
            sb.append("\n");
        }
        // Trim and ensure the program ends with a single newline
        String prog = sb.toString().trim().concat("\n");
        return prog;
    }
}

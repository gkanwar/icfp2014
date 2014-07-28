package laml.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import laml.compiler.Line;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class CodeSequence {
    private List<Line> code;

    public CodeSequence() {
        code = new ArrayList<Line>();
    }

    public void addAll(CodeSequence otherCode) {
        code.addAll(otherCode.getLines());
    }

    public void addAll(List<Line> lines) {
        code.addAll(lines);
    }

    public void add(Line line) {
        code.add(line);
    }

    public List<Line> getLines() {
        return code;
    }

    /**
     * Code sequences shouldn't be converted to strings directly, the just
     * shuffle around bits of code. This can be used to debug, though.
     */
    public String debugString() {
        // TODO(gkanwar)
        throw new NotImplementedException();
    }
}

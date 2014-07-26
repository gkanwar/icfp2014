package laml.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import laml.compiler.Line;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class CodeSequence {
    public List<Line> code;

    public CodeSequence() {
        code = new ArrayList<Line>();
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

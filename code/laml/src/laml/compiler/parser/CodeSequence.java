package laml.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import laml.compiler.Line;

public class CodeSequence {
    public List<Line> code;

    public CodeSequence() {
        code = new ArrayList<Line>();
    }
}

package laml.compiler.parser;

import java.util.Arrays;
import java.util.List;

import laml.compiler.LabeledFunction;
import laml.compiler.Line;

/**
 * Internal representation of a labeled block which will be branched to using
 * SEL. Appends JOIN after body to return to calling code.
 */
public class ParserBranch implements ParserLabeledBlock {

    private String name;
    private ParserDefinition body;

    public ParserBranch(String name, ParserDefinition body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public List<LabeledFunction> toLabeledFunctions() {
        LabeledFunction f = new LabeledFunction(name);
        f.addCodeSequence(body.code);
        f.addLine(Line.makeJoin(name + " join"));
        return Arrays.asList(f);
    }

}

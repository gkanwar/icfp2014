package laml.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import laml.compiler.LabeledFunction;
import laml.compiler.Line;

/**
 * Parsed function definition. This will eventually turn into a LabelledFunction
 * which will be post-processed to resolve the labels to absolute addresses.
 */
public class ParserFunction implements ParserLabeledBlock {
    private String name;
    private EnvFrame env;
    private ParserDefinition body;

    public ParserFunction(String name, EnvFrame env, ParserDefinition body) {
        this.name = name;
        this.env = env;
        this.body = body;
    }

    @Override
    public List<LabeledFunction> toLabeledFunctions() {
        List<LabeledFunction> out = new ArrayList<LabeledFunction>();

        String bodyLabel = name + "_$body$";
        LabeledFunction entryFunc = new LabeledFunction(name);
        LabeledFunction bodyFunc = new LabeledFunction(bodyLabel);

        // Header
        entryFunc.addCodeSequence(env.buildEnvHeader(name, bodyLabel));
        entryFunc.addLine(Line.makeRtn(name + " return"));
        out.add(entryFunc);

        // Body
        bodyFunc.addCodeSequence(env.buildEnvDefinitions(name, bodyLabel));
        bodyFunc.addCodeSequence(body.code);
        bodyFunc.addLine(Line.makeRtn(bodyLabel + " return"));
        out.add(bodyFunc);

        return out;
    }
}

package laml.compiler.parser;

import java.util.List;

import laml.compiler.LabeledFunction;

/**
 * Labeled block of code. Known implementations: labeled function (i.e. has its
 * own environment frames) or labeled code block (no environment frame, just a
 * branch).
 */
public interface ParserLabeledBlock {
    public List<LabeledFunction> toLabeledFunctions();
}

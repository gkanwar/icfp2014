package laml.compiler.parser;

public class ParserDefinition {
    /**
     * The actual code.
     */
    public CodeSequence code;
    public ParserDataType returnType;

    public ParserDefinition() {
        code = new CodeSequence();
    }

    public void setReturnType(ParserDataType returnType) {
        this.returnType = returnType;
    }
}

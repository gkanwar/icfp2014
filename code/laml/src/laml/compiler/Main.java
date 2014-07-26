package laml.compiler;

import laml.compiler.lexer.LexedProgram;
import laml.compiler.parser.Parser;

/**
 * Main compiler entry point. Expects the program on stdin and will output the
 * compiled assembly on stdout. It's probably a good idea to pipe to/from files
 * for these.
 */
public class Main {
    public static void main(String[] args) {
        LexedProgram lexedProgram = LexedProgram.lexProgram(System.in);
        RelativeProgram parsedProgram = Parser.parseProgram(lexedProgram);
        AbsoluteProgram finalProgram = parsedProgram.translate();
        System.out.println("\n");
        System.out.println(finalProgram.toString());
    }
}

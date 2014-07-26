package laml.compiler.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal representation of an environment frame in the CPU.
 */
public class EnvFrame {
    public Map<String, ParserData> bindings;
    public EnvFrame parent;

    public EnvFrame() {
        this.parent = null;
        bindings = new HashMap<String, ParserData>();
    }

    public EnvFrame(EnvFrame parent) {
        this.parent = parent;
        bindings = new HashMap<String, ParserData>();
    }

    public void addBinding(String name, ParserData value) {
        bindings.put(name, value);
    }
}

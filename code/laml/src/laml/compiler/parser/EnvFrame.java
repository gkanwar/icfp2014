package laml.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import laml.compiler.Line;

/**
 * Internal representation of an environment frame in the CPU. Contains a
 * binding from variable names to Binding objects, which define their types
 * (type checking coming soon) and code definitions. Environment frames are
 * "owned" by the node which generates them. Once they have been filled in, that
 * node should lift all definition code out to the environment generation
 * section, which precedes the body code of the node.
 */
public class EnvFrame {
    /**
     * Defines the info needed to store a binding. We assume that bindings are
     * never removed or overwritten, so the index and type can be final safely.
     */
    public static class Binding {
        public enum ParserDataType {
            INTEGER, PAIR, CLOSURE
        }

        public int index;
        public final String name;
        public final ParserDataType type;
        // GCC code which should put the desired value on top of the data stack
        public CodeSequence definition;

        public Binding(String name, ParserDataType type) {
            this.name = name;
            this.type = type;
            // Start with an empty definition -- fill this in once the binding
            // is complete. This allows recursive definitions and also argument
            // binding for lamdbas. It's a bit of a hack, but not too bad.
            this.definition = new CodeSequence();
        }

        public void setDefinition(CodeSequence definition) {
            this.definition = definition;
            if (definition.code.size() > 0) {
                // Annotate the variable definition
                definition.code.get(0).setComment("Define " + name);
            }
        }

        /**
         * Set the index of the binding within the frame. Should only be called
         * once when the binding is being added to the environment and not
         * changed after that.
         */
        public void setIndex(int index) {
            this.index = index;
        }
    }

    /**
     * Exception class specifying that a lookup wasn't possible, with a reason.
     */
    public class EnvException extends Exception {
        public EnvException(String reason) {
            super(reason);
        }

        // NOTE(gkanwar): Auto-generated
        private static final long serialVersionUID = -8343829440805755485L;
    }

    // The map and list must both contain all bindings.
    // Binding index must match index in the bindings list.
    public Map<String, Binding> bindingMap;
    public List<Binding> bindings;

    public EnvFrame parent;

    public EnvFrame() {
        this.parent = null;
        bindingMap = new HashMap<String, Binding>();
        bindings = new ArrayList<Binding>();
    }

    public EnvFrame(EnvFrame parent) {
        this.parent = parent;
        bindingMap = new HashMap<String, Binding>();
        bindings = new ArrayList<Binding>();
    }

    public void addBinding(String name, Binding binding) throws EnvException {
        if (bindingMap.containsKey(name)) {
            throw new EnvException("Cannot redefine symbol " + name);
        }
        binding.setIndex(bindings.size());
        bindings.add(binding);
        bindingMap.put(name, binding);
    }

    /**
     * Finds the depth of the closest environment frame a symbol is bound in. 0
     * is this environment, 1 is the parent, etc. This is used to compute the
     * first number on a LD. TODO(gkanwar): Determine the final resolved
     * variable order.
     */
    public int findBindingDepth(String name) throws EnvException {
        if (bindingMap.containsKey(name)) {
            return 0;
        } else if (parent != null) {
            return parent.findBindingDepth(name) + 1;
        } else {
            throw new EnvException(
                    "Cannot find binding depth for symbol " + name);
        }
    }

    /**
     * Finds the variable index of the symbol in the nearest enclosing
     * environment.
     */
    public int findBindingIndex(String name) throws EnvException {
        if (bindingMap.containsKey(name)) {
            return bindingMap.get(name).index;
        } else if (parent != null) {
            return parent.findBindingIndex(name);
        } else {
            throw new EnvException(
                    "Cannot find binding index for symbol " + name);
        }
    }

    /**
     * Compile the environment frame header for a labeled function. Initializes
     * an environment frame with all 0s for each symbol for now, then calls into
     * the body. The body should be started by a definition for each symbol.
     * 
     * @param headerLabel Label of the header for which we're definition
     *            symbols.
     * @param bodyLabel label of the body function to call into once done.
     */
    public CodeSequence buildEnvHeader(String headerLabel, String bodyLabel) {
        CodeSequence c = new CodeSequence();
        // Load an initial 0 for each symbol
        for (Binding binding : bindings) {
            c.code.add(Line.makeLdc(0, "Symbol " + binding.name +
                    " init for " + headerLabel));
        }
        c.code.add(Line.makeLdf(bodyLabel, "Load " + bodyLabel));
        c.code.add(Line.makeAp(bindings.size(), "Call " + bodyLabel));
        return c;
    }

    /**
     * Compile the environment frame definitions for a labeled function. This
     * should be the first code in the body of a labeled function.
     * 
     * @param name Label of the overall function.
     */
    public CodeSequence buildEnvDefinitions(String headerLabel, String bodyLabel) {
        CodeSequence c = new CodeSequence();
        for (Binding binding : bindings) {
            c.code.addAll(binding.definition.code);
            // Store the definition into the binding index
            c.code.add(Line.makeSt(0, binding.index,
                    "End define " + binding.name));
        }
        return c;
    }
}

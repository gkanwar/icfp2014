package laml.compiler.parser;

/**
 * Adds the program inputs to the global namespace. All other environment frames
 * should derive from this.
 */
public class RootEnvFrame extends EnvFrame {
    public RootEnvFrame() {
        super();
        Binding worldMapBinding = new Binding("WORLD-STATE", ParserDataType
                .integerType());
        Binding ghostCodeBinding = new Binding("GHOST-CODE", ParserDataType
                .integerType());
        worldMapBinding.setIndex(0);
        ghostCodeBinding.setIndex(1);
        bindings.add(worldMapBinding);
        bindings.add(ghostCodeBinding);
        bindingMap.put("WORLD-STATE", worldMapBinding);
        bindingMap.put("GHOST-CODE", ghostCodeBinding);
    }

    @Override
    public void addBinding(String name, Binding binding) throws EnvException {
        throw new EnvException("Cannot add binding " + name
                + " to root frame!");
    }
}

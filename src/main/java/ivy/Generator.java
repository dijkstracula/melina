package ivy;

import com.microsoft.z3.*;

import java.util.function.Supplier;

/**
 * A port of the generator parts of ivy_gen and gen classes, which are autogenerated each time
 * by ivy_to_cpp.  Generates a particular isolate's particular (public) action.
 * @param <P> The isolate that we are generating events for
 * @param <T> The type(s) of the arguments to the action that this is generating.
 */
public abstract class Generator<T> implements Supplier<T> {

    protected Protocol protocol;


    // TODO: Put "Action" in the class name somewhere.
    public Generator(Protocol proto) {
        protocol = proto;
    }

    public T get() {
        protocol.push();
        randomConstrain();
        Model m = protocol.solve();
        protocol.pop();
        return eval(m);
    }


    abstract protected void randomConstrain();
    abstract protected T eval(Model m);

    public static final Supplier<Void> Unit = () -> null;

}

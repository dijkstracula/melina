package ivy;

import com.microsoft.z3.*;
import java.util.function.Supplier;

/**
 * A port of the generator parts of ivy_gen and gen classes, which are autogenerated each time
 * by ivy_to_cpp.  Generates a particular isolate's particular (public) action.
 * @param <I> The isolate that we are generating events for
 * @param <T> The type(s) of the arguments to the action that this is generating.
 */
public abstract class Generator<I, S extends Specification<I>, T> implements Supplier<T> {

    protected S specification; // TODO: wildcard bounds on P?


    // TODO: Put "Action" in the class name somewhere.
    public Generator(S spec) {
        specification = spec;
    }

    protected void add(Expr<BoolSort> pred) {
        specification.addPredicate(pred);
    }


    public T get() {
        specification.push();
        randomize();
        Model m = specification.solve();
        specification.pop();
        return eval(m);
    }


    abstract protected void randomize();
    abstract protected T eval(Model m);


    public static class UnitGenerator<I, S extends Specification<I>> extends Generator<I, S, Void> {
        UnitGenerator(S spec){
            super(spec);
        }

        @Override
        protected void randomize() {}

        @Override
        protected Void eval(Model m) {
            return null;
        }
    }
}

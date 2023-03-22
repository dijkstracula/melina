package ivy;

import com.microsoft.z3.*;
import ivy.decls.Decls;
import ivy.sorts.IvySort;
import ivy.sorts.Sorts;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * I guess solver stuff at the im.module layer??  S
 * TODO: Is this actually the top-level class?  If so, is it better named "module" (per Ivy) or "Protocol"?
 * @param <I> The implementation class. (TODO: what if there isn't a single impl class, does this make sense)
 */
public abstract class Specification<I> {
    protected I impl;

    private int tmp_ctr;

    private final Context ctx;
    private final Solver slvr;

    private final Sorts sorts;
    private final Decls decls;

    private List<Conjecture<I>> conjectures;

    private List<Runnable> actions;

    public Specification(Random r, I impl) {
        ctx = new Context();
        slvr = ctx.mkSolver();
        sorts = new Sorts(ctx, r);
        decls = new Decls(ctx, sorts, r);
        this.impl = impl;
    }

    public void push() { slvr.push(); }
    public void pop() { slvr.pop(); }

    protected void addConjecture(String desc, Predicate<I> pred) {
        Objects.requireNonNull(desc);
        Objects.requireNonNull(pred);
        conjectures.add(new Conjecture<>(desc, pred));
    }
    protected void addConjecture(Conjecture<I> conj) {
        Objects.requireNonNull(conj);
        conjectures.add(conj);
    }

    public void addAction(Runnable r) {
        Objects.requireNonNull(r);
        actions.add(r);
    }

    public <T> void addAction(Supplier<T> source, Consumer<T> sink) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(sink);
        actions.add(pipe(source, sink));
    }

    protected void addPredicate(Expr<BoolSort> pred) {
        slvr.add(pred);
    }

    protected Sorts.IvyBool mkBool(String name) {
        return sorts.mkBool(name);
    }
    protected Sorts.IvyInt mkInt(String name) {
        return sorts.mkInt(name);
    }
    protected Sorts.IvyInt mkInt(String name, int min, int max) {
        return sorts.mkInt(name, min, max);
    }

    protected <J, Z extends Sort> Decls.IvyConst<J,Z> mkConst(String name, IvySort<J,Z> sort) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sort);
        return decls.mkConst(name, sort);
    }

    public Model solve() {
        Status s = slvr.check();
        if (s != Status.SATISFIABLE) {
            throw new RuntimeException(String.format("Got %s back from the solver", s.toString()));
        }
        return slvr.getModel();
    }

    public static <T> Runnable pipe(Supplier<T> source, Consumer<T> sink) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(sink);

        // TODO: truly no other way to compose these??
        // TODO: is there value in a subclass of Runnable that also contains metadata like the action name, etc
        return () -> sink.accept(source.get());
    }

    public static <I, S extends Specification<I>> Runnable pipe(Generator.UnitGenerator<I, S> source, Runnable sink) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(sink);

        return () -> {
            source.get();
            sink.run();
        };
    }
}

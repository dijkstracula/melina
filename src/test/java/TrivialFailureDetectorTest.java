import com.microsoft.z3.*;
import io.vavr.control.Either;
import ivy.Protocol;
import ivy.decls.Decls;
import ivy.exceptions.IvyExceptions;
import ivy.functions.Actions.Action1;
import ivy.sorts.Sorts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class TrivialFailureDetectorTest {

    /**
     * Morally, an isolate tracking availablity of nodes on [0, MAX_N).
     */
    private static class TrivialFailureDetector {
        public int MAX_N;

        private ArrayList<Boolean> statuses;

        public TrivialFailureDetector(int max_n) {
            MAX_N = max_n;
            statuses = new ArrayList<>(Collections.nCopies(max_n, true));
        }

        public Void isDown(int n) {
            System.out.println(String.format("Node %d is down", n));
            statuses.set(n, false);
            return null;
        }
    }

    public static class TrivialFailureDetectorProto extends Protocol {
        // Actions
        public final Supplier<Integer> isDownGen;
        public final Action1<Integer, Void> isDownExec;

        // Ghost state
        public final Decls.IvyConst<Integer, IntSort> node;

        public TrivialFailureDetectorProto(Random r, TrivialFailureDetector i) {
            super(r);
            Sorts.IvyInt nodeSort = sorts.mkInt("nodeSort", 0, i.MAX_N);
            node = decls.mkConst("node", nodeSort);

            isDownGen = nodeSort;
            isDownExec = Action1.from(i::isDown);
            addAction(isDownExec.pipe(isDownGen));
        }
    }

    private TrivialFailureDetector p;
    private TrivialFailureDetectorProto s;


    @BeforeEach
    public void setup() {
        Random r = new Random(42);
        p = new TrivialFailureDetector(42);
        s = new TrivialFailureDetectorProto(r, p);
    }

    @Test
    public void testGenAndExec() throws IvyExceptions.ConjectureFailure {
        for (int i = 0; i < 500; i++) {
            // Generating a value for p.n should put it within the right bounds.
            int n = s.isDownGen.get();
            assertTrue(0 <= n);
            assertTrue(n < p.MAX_N);

            // And executing it should have an observable effect on the implementation.
            assert(s.isDownExec.apply(n).isRight());
            assertFalse(p.statuses.get(n));
        }
    }

    @Test
    public void testPipe() throws IvyExceptions.ConjectureFailure {
        // Tests the same thing as testGenAndExec, but with the added layer of composing
        // the source and sink into a thunk (specifically a Void-producing Supplier).
        Supplier<Either<IvyExceptions.ActionException, Void>> pipe = s.isDownExec.pipe(s.isDownGen);
        for (int i = 0; i < 500; i++) {
            assert(pipe.get().isRight());
        }

        // Since piping the action values abstracts the particular choices away, we can
        // only observe that after a ~sufficient~ number of iterations, all nodes are
        // downed eventually.
        for (int i = 0; i < p.MAX_N; i++) {
            assertFalse(p.statuses.get(i));
        }
    }

    @Test
    public void testAct() throws IvyExceptions.ConjectureFailure {
        // Tests the same thing as testPipe, except that the single isDown action is
        // "nondeterministically" chosen by the Specification.
        for (int i = 0; i < 500; i++) {
            assert(s.takeAction().isRight());
        }

        // Since piping the action values abstracts the particular choices away, we can
        // only observe that after a ~sufficient~ number of iterations, all nodes are
        // downed eventually.
        for (int i = 0; i < p.MAX_N; i++) {
            assertFalse(p.statuses.get(i));
        }
    }
}

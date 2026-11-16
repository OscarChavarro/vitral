package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.function.Supplier;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

/**
A fallback result of {@code setOp} that is built only when the pipeline needs
it (Fable5.2Analysis.md Stage D step 4: "until then, build them lazily, only
when connect has failed"). Before, the axis-aligned cell and orthogonal
profile fallbacks were built for every eligible operand pair before the
preflights, whether used or not.

<p>{@link #peek()} builds on first call and returns the result until it is
taken; {@link #take()} returns it once and marks it used, as the former code
did by setting its local variable to null.</p>
 */
final class _PolyhedralBoundedSolidLazyFallback
{
    /** Builds performed on the calling thread (test hook). */
    private static final ThreadLocal<int[]> builds =
        ThreadLocal.withInitial(() -> new int[1]);

    static int getBuildCount()
    {
        return builds.get()[0];
    }

    private Supplier<PolyhedralBoundedSolid> builder;
    private PolyhedralBoundedSolid value;
    private boolean taken;

    _PolyhedralBoundedSolidLazyFallback(
        Supplier<PolyhedralBoundedSolid> builder)
    {
        this.builder = builder;
    }

    /** True when a builder exists (eligible operands); builds nothing. */
    boolean isAvailable()
    {
        return !taken && (builder != null || value != null);
    }

    PolyhedralBoundedSolid peek()
    {
        if ( taken ) {
            return null;
        }
        if ( builder != null ) {
            builds.get()[0]++;
            value = builder.get();
            builder = null;
        }
        return value;
    }

    PolyhedralBoundedSolid take()
    {
        PolyhedralBoundedSolid result = peek();
        taken = true;
        value = null;
        return result;
    }

    void discard()
    {
        taken = true;
        builder = null;
        value = null;
    }
}

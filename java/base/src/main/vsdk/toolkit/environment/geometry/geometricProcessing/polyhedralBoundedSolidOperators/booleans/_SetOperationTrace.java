package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

/**
Centralized, property-gated diagnostic tracing for the boolean set-operation
pipeline. Extracted in Stage 7 R4 from the per-class copies that previously
lived in {@link _PolyhedralBoundedSolidSetOperator},
{@link _PolyhedralBoundedSolidSetClassifier} and
{@link _PolyhedralBoundedSolidSetVertexVertexClassifier}.

<p>Tracing is enabled per category through JVM system properties, so it stays
inert in normal runs and is reachable for incident diagnosis (CLAUDE.md Visual
Diagnostics Policy):</p>
<ul>
  <li>{@code vsdk.setop.traceCoplanarTangential}</li>
  <li>{@code vsdk.setop.tracePipelineSummary}</li>
</ul>
 */
final class _SetOperationTrace
{
    private static final String TRACE_COPLANAR_TANGENTIAL_PROPERTY =
        "vsdk.setop.traceCoplanarTangential";
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";

    static boolean isCoplanarTangentialTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_COPLANAR_TANGENTIAL_PROPERTY);
    }

    static boolean isPipelineSummaryTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    static void traceCoplanarTangential(String message)
    {
        if ( !isCoplanarTangentialTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpCoplanarTrace] " + message);
    }

    static void tracePipelineSummary(String message)
    {
        if ( !isPipelineSummaryTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpPipelineTrace] " + message);
    }
}

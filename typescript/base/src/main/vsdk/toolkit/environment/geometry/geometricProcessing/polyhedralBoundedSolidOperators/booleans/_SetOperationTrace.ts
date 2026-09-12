import { Boolean as JavaBoolean } from "../../../../../../../java/lang/Boolean.js";

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

The properties are read through the `java.lang.Boolean.getBoolean` port, the
same system-property boundary used by every other Java call site.
 */
export class _SetOperationTrace {
    private static readonly TRACE_COPLANAR_TANGENTIAL_PROPERTY = "vsdk.setop.traceCoplanarTangential";
    private static readonly TRACE_PIPELINE_SUMMARY_PROPERTY = "vsdk.setop.tracePipelineSummary";

    public static isCoplanarTangentialTraceEnabled(): boolean {
        return JavaBoolean.getBoolean(_SetOperationTrace.TRACE_COPLANAR_TANGENTIAL_PROPERTY);
    }

    public static isPipelineSummaryTraceEnabled(): boolean {
        return JavaBoolean.getBoolean(_SetOperationTrace.TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    public static traceCoplanarTangential(message: string): void {
        if (!_SetOperationTrace.isCoplanarTangentialTraceEnabled()) {
            return;
        }
        console.log("[SetOpCoplanarTrace] " + message);
    }

    public static tracePipelineSummary(message: string): void {
        if (!_SetOperationTrace.isPipelineSummaryTraceEnabled()) {
            return;
        }
        console.log("[SetOpPipelineTrace] " + message);
    }
}

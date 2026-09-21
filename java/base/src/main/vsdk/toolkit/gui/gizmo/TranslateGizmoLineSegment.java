package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

public record TranslateGizmoLineSegment(Vector3Dd start, Vector3Dd end, ColorRgb color)
{
}

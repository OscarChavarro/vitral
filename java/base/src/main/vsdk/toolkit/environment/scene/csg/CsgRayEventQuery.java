package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

/**
Stable exact-t grouping; nearby values are not asserted coincident. References
are valid only with the unchanged leaf batch used for this query.
*/
public class CsgRayEventQuery {
    public static RaySolidQueryStatus group(CsgRayLeafBatch leaves, CsgRayEventBatch result) {
    result.clear();
    ArrayList<CsgRayEventCursor> cursors = new ArrayList<CsgRayEventCursor>((int)(leaves.size()>0 ? leaves.size() : 1));
    final RaySolidQueryStatus status=validate(leaves,cursors);
    if (status!=RaySolidQueryStatus.Supported) return status;
    CsgRayEventBatch candidate = new CsgRayEventBatch();
    ArrayList<CsgRayEventReference> references = new ArrayList<CsgRayEventReference>(8);
    double[] t = {0};
    while (nextGroup(leaves,cursors,t,references)) {
        CsgRayEventGroup group = new CsgRayEventGroup(t[0]);
        for (long k=0;k<references.size();++k)
            if (!group.add(references.get((int)(k)))) return RaySolidQueryStatus.Indeterminate;
        if (!candidate.add(group)) return RaySolidQueryStatus.Indeterminate;
    }
    result.assign(candidate);
    return RaySolidQueryStatus.Supported;
}

    // Streaming form over caller storage. validate checks every leaf and
    // resets one cursor per leaf; nextGroup then fills group with the next
    // exactly equal parameter's references in leaf order, or returns false
    // when every boundary has been consumed.
    public static RaySolidQueryStatus validate(CsgRayLeafBatch leaves, ArrayList<CsgRayEventCursor> cursors) {
    cursors.clear();
    for (long i=0;i<leaves.size();++i) {
        final RaySolidClassification c =leaves.get(i).getClassification();
        if (c.insideSegments.size()!=c.boundaries.size()+1) return RaySolidQueryStatus.Indeterminate;
        for (long j=0;j<c.boundaries.size();++j) {
            if (c.boundaries.get((int)(j)).geometry == null || !Double.isFinite(c.boundaries.get((int)(j)).t) ||
                (j>0 && c.boundaries.get((int)(j)).t<c.boundaries.get((int)(j-1)).t) || c.insideSegments.get((int)(j)).booleanValue()==c.insideSegments.get((int)(j+1)).booleanValue())
                return RaySolidQueryStatus.Indeterminate;
        }
        if (!cursors.add(new CsgRayEventCursor())) return RaySolidQueryStatus.Indeterminate;
    }
    return RaySolidQueryStatus.Supported;
}

    public static boolean nextGroup(CsgRayLeafBatch leaves, ArrayList<CsgRayEventCursor> cursors, double[] t, ArrayList<CsgRayEventReference> group) {
    group.clear();
    boolean found=false;
    for (long i=0;i<leaves.size() && i<cursors.size();++i) {
        final RaySolidClassification c =leaves.get(i).getClassification();
        if (cursors.get((int)(i)).index<c.boundaries.size() && (!found || c.boundaries.get((int)(cursors.get((int)(i)).index)).t<t[0])) {
            t[0]=c.boundaries.get((int)(cursors.get((int)(i)).index)).t; found=true;
        }
    }
    if (!found) return false;
    for (long i=0;i<leaves.size() && i<cursors.size();++i) {
        final RaySolidClassification c =leaves.get(i).getClassification();
        while (cursors.get((int)(i)).index<c.boundaries.size() && c.boundaries.get((int)(cursors.get((int)(i)).index)).t==t[0]) {
            if (!group.add(new CsgRayEventReference(i,cursors.get((int)(i)).index))) { group.clear(); return false; }
            ++cursors.get((int)(i)).index;
        }
    }
    return true;
}
}


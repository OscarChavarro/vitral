package vsdk.toolkit.render.jogl;

// Java classes
import java.util.HashMap;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.Geometry;

/**
The class `_JoglSimpleBodyRendererDisplayList` is used as an internal buffer
of JOGL/OpenGL display lists for Geometry/RendererConfiguration pairs. That is,
each different geometry/renderer config. has is own display list.

There is a trick in this class consisting on making two geometries
"comparable" (that is, to stablish a numbering or ordering on a set of
geometries), so this class should be used as a key on a hasmap-like
data structure. All geometries processed are numbered in succesive order.
*/
public class _JoglSimpleBodyRendererDisplayList extends JoglRenderer
implements Comparable<_JoglSimpleBodyRendererDisplayList>
{
    // Key
    private Geometry contentKey;
    private RendererConfiguration qualitySubset;

    // Value
    public int displayListId;

    // Internal numbering of geometries used to make them "comparable"
    private static HashMap<Geometry, Integer> geometryIds = null;
    private static int nextId = 1;

    public _JoglSimpleBodyRendererDisplayList(Geometry g, int id, RendererConfiguration q)
    {
        if ( geometryIds == null ) {
            geometryIds = new HashMap<Geometry, Integer>();
        }

        Integer gval;

        gval = geometryIds.get(g);

        if ( gval == null ) {
            geometryIds.put(g, Integer.valueOf(nextId));
            nextId++;
        }

        contentKey = g;
        displayListId = id;
        qualitySubset = new RendererConfiguration();
        qualitySubset.clone(q);
    }

    @Override
    public int compareTo(_JoglSimpleBodyRendererDisplayList other)
    {
        int thisId;
        int otherId;

        thisId = geometryIds.get(this.contentKey);
        otherId = geometryIds.get(other.contentKey);

        if ( thisId > otherId ) {
            return 1;
        }
        if ( thisId < otherId ) {
            return -1;
        }
        int result;
        result = this.qualitySubset.compareTo(other.qualitySubset);
        return result;
    }
}

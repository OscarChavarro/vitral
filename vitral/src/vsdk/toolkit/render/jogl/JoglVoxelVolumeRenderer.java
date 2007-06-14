//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 22 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL clases
import javax.media.opengl.GL;

// VitralSDK classes
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;

public class JoglVoxelVolumeRenderer extends JoglRenderer
{
    private static Box geometryInstance = null;

    private static int threshold = 127;

    public static void setThreshold(int t)
    {
        threshold = t;
    }

    public static int getThreshold()
    {
        return threshold;
    }

    public static void drawBinaryCubes(GL gl, VoxelVolume v,
        Camera c, RendererConfiguration q)
    {
    if ( geometryInstance == null ) {
            geometryInstance = new Box(2, 2, 2);
    }

        JoglGeometryRenderer.draw(gl, geometryInstance, c, q);

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, v, q);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, v, q);
        }

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================


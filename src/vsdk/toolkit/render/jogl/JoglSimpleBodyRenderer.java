//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 27 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

// JOGL clases
import javax.media.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;

/**
The class `_JoglSimpleBodyRendererDisplayList` is used as an internal buffer
of JOGL/OpenGL display lists for Geometry/RendererConfiguration pairs. That is,
each different geometry/renderer config. has is own display list.

There is a trick in this class consisting on making two geometries
"comparable" (that is, to stablish a numbering or ordering on a set of
geometries), so this class should be used as a key on a hasmap-like
data structure. All geometries processed are numbered in succesive order.
*/
class _JoglSimpleBodyRendererDisplayList extends JoglRenderer implements Comparable<_JoglSimpleBodyRendererDisplayList>
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
            geometryIds.put(g, new Integer(nextId));
            nextId++;
        }

        contentKey = g;
        displayListId = id;
        qualitySubset = new RendererConfiguration();
        qualitySubset.clone(q);
    }

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

/**
The `JoglSimpleBodyRenderer` class is a helper for the JOGL/OpenGL rendering
of geometries. Note that this class is responsible of:
  - Defining one object transforms
  - Activating global materials and maps (i.e. texture and normal maps)
  - Calling the specific geometry rendering.
Optionally, this class can do an automatic managing of display lists for
(geometry / renderer configuration)s.
*/
public class JoglSimpleBodyRenderer extends JoglRenderer {

    private static boolean usingDisplayLists = false;
    private static ArrayList<_JoglSimpleBodyRendererDisplayList> displayLists = null;

    public static void setAutomaticDisplayListManagement(boolean val)
    {
        usingDisplayLists = val;
    }

    public static boolean withAutomaticDisplayListManagement()
    {
        return usingDisplayLists;
    }

    private static int getDisplayList(Geometry thing, RendererConfiguration q)
    {
        if ( displayLists == null ) {
            return -1;
        }
        int prev;

        _JoglSimpleBodyRendererDisplayList key;

        key = new _JoglSimpleBodyRendererDisplayList(thing, -1, q);

        prev = Collections.binarySearch(displayLists, key);
        if ( prev < 0 ) {
            return -1;
        }
        return displayLists.get(prev).displayListId;
    }

    private static int createDisplayListId(GL2 gl, Geometry thing, RendererConfiguration q)
    {
        if ( displayLists == null ) {
            displayLists = new ArrayList<_JoglSimpleBodyRendererDisplayList>();
        }

        int id;

        _JoglSimpleBodyRendererDisplayList newElem;
        int prevIndex;

        newElem = new _JoglSimpleBodyRendererDisplayList(thing, -1, q);

        prevIndex = Collections.binarySearch(displayLists, newElem);

        if ( prevIndex >= 0 ) {
            return displayLists.get(prevIndex).displayListId;
        }

        id = gl.glGenLists(1);
        newElem.displayListId = id;
        displayLists.add(-prevIndex - 1, newElem);
        return id;
    }

    public static int getNumDisplayLists()
    {
        if ( displayLists == null ) {
            return 0;
        }
        return displayLists.size();
    }

    private static void drawCommon(GL2 gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        Vector3D scale;
        Vector3D p;

        p = b.getPosition();
        scale = b.getScale();

        gl.glTranslated(p.x, p.y, p.z);
        JoglMatrixRenderer.activate(gl, b.getRotation());
        gl.glScaled(scale.x, scale.y, scale.z);

        gl.glColor3d(1, 1, 1);
        JoglMaterialRenderer.activate(gl, b.getMaterial());

        //-----------------------------------------------------------------
        Image texture;

        texture = b.getTexture();

        if ( q.isTextureSet() ) {
            // Define texture parameters, including for further local
            // textures activated within JoglGeometryRenderers
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_GENERATE_MIPMAP,
                gl.GL_TRUE);
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER,
                gl.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER,
                gl.GL_LINEAR);
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S,
                gl.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T,
                gl.GL_CLAMP_TO_EDGE);
            gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE,
                gl.GL_MODULATE);

            // Activate global texture
            if ( (texture != null) ) {
                gl.glEnable(gl.GL_TEXTURE_2D);
                JoglImageRenderer.activate(gl, texture);
	    }
        }
        else {
            gl.glDisable(gl.GL_TEXTURE_2D);
        }

        //-----------------------------------------------------------------
        RGBImage nm = b.getNormalMapRgb();
        if ( q.isBumpMapSet() && (nm != null) ) {
            JoglImageRenderer.activateAsNormalMap(gl, nm, q);
        }
    }

    public static void draw(GL2 gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        activateNvidiaGpuParameters(gl, q,
            JoglRenderer.getCurrentVertexShader(), 
            JoglRenderer.getCurrentPixelShader());

        gl.glPushMatrix();

        drawCommon(gl, b, c, q);

        //-----------------------------------------------------------------
        if ( !usingDisplayLists ) {
            JoglGeometryRenderer.draw(gl, b.getGeometry(), c, q);
        }
        else {
            int id;
            id = getDisplayList(b.getGeometry(), q);

            if ( id >= 0 ) {
                gl.glCallList(id);
            }
            else {
                id = createDisplayListId(gl, b.getGeometry(), q);
                gl.glNewList(id, gl.GL_COMPILE);
                JoglGeometryRenderer.draw(gl, b.getGeometry(), c, q);
                gl.glEndList();
                if ( gl.glGetError() != 0 ) {
                    VSDK.reportMessage(null, VSDK.WARNING, "JoglSimpleBodyRenderer.draw", "Error compiling display list. Rendering could be wrong.");
                }
                gl.glCallList(id);
            }
        }

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        JoglImageRenderer.deactivate(gl, b.getTexture());
        deactivateNvidiaGpuParameters(gl, q);
    }

    public static void drawWithVertexArrays(GL2 gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        activateNvidiaGpuParameters(gl, q,
            JoglRenderer.getCurrentVertexShader(), 
            JoglRenderer.getCurrentPixelShader());

        gl.glPushMatrix();

        drawCommon(gl, b, c, q);

        //-----------------------------------------------------------------
        if ( !usingDisplayLists ) {
            JoglGeometryRenderer.drawWithVertexArrays(gl, b.getGeometry(), c, q);
        }
        else {
            int id;
            id = getDisplayList(b.getGeometry(), q);

            if ( id >= 0 ) {
                gl.glCallList(id);
            }
            else {
                id = createDisplayListId(gl, b.getGeometry(), q);
                gl.glNewList(id, gl.GL_COMPILE);
                JoglGeometryRenderer.drawWithVertexArrays(gl, b.getGeometry(), c, q);
                gl.glEndList();
                if ( gl.glGetError() != 0 ) {
                    VSDK.reportMessage(null, VSDK.WARNING, "JoglSimpleBodyRenderer.draw", "Error compiling display list. Rendering could be wrong.");
                }
                gl.glCallList(id);
            }
        }

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        JoglImageRenderer.deactivate(gl, b.getTexture());
        deactivateNvidiaGpuParameters(gl, q);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

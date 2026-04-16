package vsdk.toolkit.render.joglcg;

// Java classes
import java.util.ArrayList;
import java.util.Collections;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl._JoglSimpleBodyRendererDisplayList;

/**
The `JoglCgSimpleBodyRenderer` class is a helper for the JOGL/OpenGL rendering
of geometries. Note that this class is responsible of:
  - Defining one object transforms
  - Activating global materials and maps (i.e. texture and normal maps)
  - Calling the specific geometry rendering.
Optionally, this class can do an automatic managing of display lists for
(geometry / renderer configuration)s.
*/
public class JoglCgSimpleBodyRenderer extends JoglCgRenderer {

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
        JoglCgMaterialRenderer.activate(gl, b.getMaterial());

        //-----------------------------------------------------------------
        Image texture;

        texture = b.getTexture();

        if ( q.isTextureSet() ) {
            // Define texture parameters, including for further local
            // textures activated within JoglCgGeometryRenderers
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_GENERATE_MIPMAP,
                GL.GL_TRUE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
                GL.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
                GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
                GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
                GL.GL_CLAMP_TO_EDGE);
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
                GL2ES1.GL_MODULATE);

            // Activate global texture
            if ( (texture != null) ) {
                gl.glEnable(GL.GL_TEXTURE_2D);
                JoglCgImageRenderer.activate(gl, texture);
            }
        }
        else {
            gl.glDisable(GL.GL_TEXTURE_2D);
        }

        //-----------------------------------------------------------------
        RGBImage nm = b.getNormalMapRgb();
        if ( q.isBumpMapSet() && (nm != null) ) {
            JoglCgImageRenderer.activateAsNormalMap(gl, nm, q);
        }
    }

    public static void draw(GL2 gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        activateNvidiaGpuParameters(gl, q,
            JoglCgRenderer.getCurrentVertexShader(), 
            JoglCgRenderer.getCurrentPixelShader());

        gl.glPushMatrix();

        drawCommon(gl, b, c, q);

        //-----------------------------------------------------------------
        if ( !usingDisplayLists ) {
            JoglCgGeometryRenderer.draw(gl, b.getGeometry(), c, q);
        }
        else {
            int id;
            id = getDisplayList(b.getGeometry(), q);

            if ( id >= 0 ) {
                gl.glCallList(id);
            }
            else {
                id = createDisplayListId(gl, b.getGeometry(), q);
                gl.glNewList(id, GL2.GL_COMPILE);
                JoglCgGeometryRenderer.draw(gl, b.getGeometry(), c, q);
                gl.glEndList();
                if ( gl.glGetError() != 0 ) {
                    VSDK.reportMessage(null, VSDK.WARNING, "JoglSimpleBodyRenderer.draw", "Error compiling display list. Rendering could be wrong.");
                }
                gl.glCallList(id);
            }
        }

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        JoglCgImageRenderer.deactivate(gl, b.getTexture());
        deactivateNvidiaGpuParameters(gl, q);
    }

    public static void drawWithVertexArrays(GL2 gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        activateNvidiaGpuParameters(gl, q,
            JoglCgRenderer.getCurrentVertexShader(), 
            JoglCgRenderer.getCurrentPixelShader());

        gl.glPushMatrix();

        drawCommon(gl, b, c, q);

        //-----------------------------------------------------------------
        if ( !usingDisplayLists ) {
            JoglCgGeometryRenderer.drawWithVertexArrays(gl, b.getGeometry(), c, q);
        }
        else {
            int id;
            id = getDisplayList(b.getGeometry(), q);

            if ( id >= 0 ) {
                gl.glCallList(id);
            }
            else {
                id = createDisplayListId(gl, b.getGeometry(), q);
                gl.glNewList(id, GL2.GL_COMPILE);
                JoglCgGeometryRenderer.drawWithVertexArrays(gl, b.getGeometry(), c, q);
                gl.glEndList();
                if ( gl.glGetError() != 0 ) {
                    VSDK.reportMessage(null, VSDK.WARNING, "JoglSimpleBodyRenderer.draw", "Error compiling display list. Rendering could be wrong.");
                }
                gl.glCallList(id);
            }
        }

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        JoglCgImageRenderer.deactivate(gl, b.getTexture());
        deactivateNvidiaGpuParameters(gl, q);
    }
}

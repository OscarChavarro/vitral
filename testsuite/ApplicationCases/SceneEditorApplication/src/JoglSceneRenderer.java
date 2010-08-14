//===========================================================================

// Java basic classes
import java.util.ArrayList;

// JOGL classes
import javax.media.opengl.GL2;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyGroupRenderer;

public class JoglSceneRenderer
{
    /**
    Follows similar strategy to general JoglSimpleSceneRenderer, except that
    incorporates draw controlled under interface editor.
    */
    private static void drawBase(GL2 gl, Scene s, ModifyPanel modifyPanel)
    {
        //- Draw scene background -----------------------------------------
        JoglBackgroundRenderer.draw(gl,
            s.scene.getBackgrounds().get(s.scene.getActiveBackgroundIndex()));

        //- Activate camera -----------------------------------------------
        JoglCameraRenderer.activate(gl, s.activeCamera);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
        }

        //- Activate lights -----------------------------------------------
        int i;

        for ( i = 0; i < s.scene.getLights().size(); i++ ) {
            Light l = s.scene.getLights().get(i);
            JoglLightRenderer.activate(gl, l);
        }

        //- Draw scene bodies ---------------------------------------------
        SimpleBody gi;
        RendererConfiguration quality;

        if ( s.scene.getLights().size() > 0 ) {
            gl.glEnable(gl.GL_LIGHTING);
        }
        else {
            gl.glDisable(gl.GL_LIGHTING);
        }

        // Not working for NvidiaGPU!
        //JoglSimpleBodyRenderer.setAutomaticDisplayListManagement(true);

        for ( i = 0; i < s.scene.getSimpleBodies().size(); i++ ) {
            quality = s.qualityTemplate.clone();

            if ( s.selectedThings.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }
            gi = s.scene.getSimpleBodies().get(i);

            if ( modifyPanel == null || modifyPanel.getTarget() != gi ) {
                JoglSimpleBodyRenderer.draw(gl, gi, s.activeCamera, quality);
            }
            else {
                modifyPanel.draw(gl, s.activeCamera, quality);
            }
        }
    }

    public static void draw(GL2 gl, Scene s, SceneEditorApplication parent)
    {
        RendererConfiguration quality;
        SimpleBodyGroup ggi;
        int i;

        s.activateSelectedBackground();

        drawBase(gl, s, parent.modifyPanel);

        //- Draw 3D Gizmos ------------------------------------------------
        for ( i = 0; i < s.scene.getLights().size(); i++ ) {
            JoglLightRenderer.draw(gl, s.scene.getLights().get(i));
        }

        //- Draw visual debug entities (usually transparent) --------------
        for ( i = 0; i < s.debugThingGroups.size(); i++ ) {
            quality = s.qualityTemplate.clone();
            quality.setShadingType(quality.SHADING_TYPE_NOLIGHT);
            if ( s.selectedDebugThingGroups.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }
            ggi = s.debugThingGroups.get(i);
            if ( ggi.getBodies().get(0).getGeometry() instanceof Sphere ) {
                gl.glDisable(gl.GL_DEPTH_TEST);
            }
            JoglSimpleBodyGroupRenderer.draw(gl, ggi, s.activeCamera, quality);
            gl.glEnable(gl.GL_DEPTH_TEST);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

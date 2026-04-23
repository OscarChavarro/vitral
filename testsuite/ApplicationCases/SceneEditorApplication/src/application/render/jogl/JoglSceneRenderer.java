package application.render.jogl;

// Java basic classes

// JOGL classes
import com.jogamp.opengl.GL2;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.Jogl2BackgroundRenderer;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleBodyRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleBodyGroupRenderer;

// Application classes
import application.SceneEditorApplication;
import application.framework.Scene;
import application.gui.ModifyPanel;

public class JoglSceneRenderer
{
    /**
    Follows similar strategy to general Jogl2SimpleSceneRenderer, except that
    incorporates draw controlled under interface editor.
    */
    private static void drawBase(GL2 gl, Scene s, ModifyPanel modifyPanel)
    {
        //- Draw scene background -----------------------------------------
        Jogl2BackgroundRenderer.draw(gl,
            s.scene.getBackgrounds().get(s.scene.getActiveBackgroundIndex()));

        //- Activate camera -----------------------------------------------
        Jogl2CameraRenderer.activate(gl, s.activeCamera);

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        if ( s.showCorridor ) {
            s.corridor.drawGL(gl);
        }

        //- Activate lights -----------------------------------------------
        int i;

        for ( i = 0; i < s.scene.getLights().size(); i++ ) {
            Light l = s.scene.getLights().get(i);
            Jogl2LightRenderer.activate(gl, l);
        }

        //- Draw scene bodies ---------------------------------------------
        SimpleBody gi;
        RendererConfiguration quality;

        if ( s.scene.getLights().size() > 0 ) {
            gl.glEnable(GL2.GL_LIGHTING);
        }
        else {
            gl.glDisable(GL2.GL_LIGHTING);
        }

        // Not working for NvidiaGPU!
        //Jogl2SimpleBodyRenderer.setAutomaticDisplayListManagement(true);

        for ( i = 0; i < s.scene.getSimpleBodies().size(); i++ ) {
            try {
                quality = s.qualityTemplate.clone();
	    }
	    catch ( CloneNotSupportedException e ) {
                break;
	    }

            if ( s.selectedThings.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }
            gi = s.scene.getSimpleBodies().get(i);

            if ( modifyPanel == null || modifyPanel.getTarget() != gi ) {
                Jogl2SimpleBodyRenderer.draw(gl, gi, s.activeCamera, quality);
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
            Jogl2LightRenderer.draw(gl, s.scene.getLights().get(i));
        }

        //- Draw visual debug entities (usually transparent) --------------
        for ( i = 0; i < s.debugThingGroups.size(); i++ ) {
            try {
                quality = s.qualityTemplate.clone();
	    }
	    catch ( CloneNotSupportedException e ) {
                break;
	    }

            quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            if ( s.selectedDebugThingGroups.isSelected(i) ) {
                quality.setSelectionCorners(true);
            }
            else {
                quality.setSelectionCorners(false);
            }
            ggi = s.debugThingGroups.get(i);
            if ( ggi.getBodies().get(0).getGeometry() instanceof Sphere ) {
                gl.glDisable(GL2.GL_DEPTH_TEST);
            }
            Jogl2SimpleBodyGroupRenderer.draw(gl, ggi, s.activeCamera, quality);
            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }

}

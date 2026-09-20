package application.render.jogl;

// Java basic classes

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;

// VSDK classes
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl2BackgroundRenderer;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleBodyRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleBodyGroupRenderer;
import vsdk.toolkit.render.jogl.Jogl4SphereRenderer;

// Application classes
import application.SceneEditorApplication;
import application.framework.Scene;
import application.gui.ModifyPanel;

public class Jogl4SceneRenderer
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
                drawSimpleBody(gl, gi, s, quality);
            }
            else {
                modifyPanel.draw(gl, s.activeCamera, quality);
            }
        }
    }

    private static void drawSimpleBody(GL2 gl, SimpleBody body, Scene s,
                                       RendererConfiguration quality)
    {
        Geometry geometry = body.getGeometry();
        if ( geometry instanceof Sphere && gl.isGL4() ) {
            drawSphere(gl.getGL4(), (Sphere)geometry, body, s, quality);
        }
        else {
            Jogl2SimpleBodyRenderer.draw(gl, body, s.activeCamera, quality);
        }
    }

    private static void drawSphere(GL4 gl, Sphere sphere, SimpleBody body,
                                   Scene s, RendererConfiguration quality)
    {
        Light light = s.scene.getLights().isEmpty()
            ? null
            : s.scene.getLights().get(0);
        Image texture = body.getTexture();
        RGBImageUncompressed textureMap = texture instanceof RGBImageUncompressed
            ? (RGBImageUncompressed)texture
            : null;

        Jogl4SphereRenderer.draw(
            gl,
            sphere,
            s.activeCamera,
            light,
            body.getMaterial(),
            quality,
            textureMap,
            body.getNormalMapRgb(),
            body.getTransformationMatrix(),
            32,
            16);
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

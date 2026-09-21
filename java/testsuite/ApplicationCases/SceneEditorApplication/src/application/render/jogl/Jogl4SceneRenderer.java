package application.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL4;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4BackgroundRenderer;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.render.jogl.Jogl4MinMaxRenderer;
import vsdk.toolkit.render.jogl.Jogl4SelectionCornersRenderer;

// Application classes
import application.SceneEditorApplication;
import application.framework.Scene;
import application.gui.ModifyPanel;

/**
Draws the scene of the editor into the current viewport with the GL4 core
pipeline: every geometry goes through `Jogl4GeometryRenderer`, so all of them
honor the same bits of the `RendererConfiguration` of the viewport.
*/
public class Jogl4SceneRenderer
{
    /**
    Draws the background, the lights and the bodies of the scene.
    */
    private static void drawBase(GL4 gl, Scene s, ModifyPanel modifyPanel)
    {
        //- Draw scene background -----------------------------------------
        Jogl4BackgroundRenderer.draw(gl,
            s.scene.getBackgrounds().get(s.scene.getActiveBackgroundIndex()));

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);

        //- Draw scene bodies ---------------------------------------------
        Light light = s.scene.getLights().isEmpty()
            ? null
            : s.scene.getLights().get(0);
        SimpleBody gi;
        RendererConfiguration quality;
        int i;

        for ( i = 0; i < s.scene.getSimpleBodies().size(); i++ ) {
            try {
                quality = s.qualityTemplate.clone();
            }
            catch ( CloneNotSupportedException e ) {
                break;
            }

            quality.setSelectionCorners(s.selectedThings.isSelected(i));
            gi = s.scene.getSimpleBodies().get(i);

            if ( modifyPanel == null || modifyPanel.getTarget() != gi ) {
                drawBody(gl, gi, s.activeCamera, light, quality);
            }
            else {
                modifyPanel.draw(gl, s.activeCamera, light, quality);
            }
        }
    }

    /**
    Draws one body of the scene.

    @param gl OpenGL context
    @param body body to draw
    @param camera camera that views the body
    @param light light of the scene, or null for a light at the camera
    @param quality bits of rendering configuration
    */
    public static void drawBody(GL4 gl, SimpleBody body, Camera camera, Light light,
                                RendererConfiguration quality)
    {
        drawBody(gl, body, Matrix4x4d.identityMatrix(), camera, light, quality);
    }

    /**
    Draws all the bodies of a group, with the transformation of the group. The
    bounding volume and the selection corners are drawn once around the whole
    group, not around each body.

    @param gl OpenGL context
    @param group group to draw
    @param camera camera that views the group
    @param light light of the scene, or null for a light at the camera
    @param quality bits of rendering configuration
    */
    public static void drawBodyGroup(GL4 gl, SimpleBodyGroup group, Camera camera,
                                     Light light, RendererConfiguration quality)
    {
        RendererConfiguration memberQuality;

        try {
            memberQuality = quality.clone();
        }
        catch ( CloneNotSupportedException e ) {
            return;
        }
        memberQuality.setSelectionCorners(false);
        memberQuality.setBoundingVolume(false);

        for ( SimpleBody body : group.getBodies() ) {
            drawBody(gl, body, group.getTransformationMatrix(), camera, light, memberQuality);
        }
        if ( quality.isBoundingVolumeSet() ) {
            Jogl4MinMaxRenderer.draw(gl, group.getMinMax(), camera, group.getTransformationMatrix());
        }
        if ( quality.isSelectionCornersSet() ) {
            Jogl4SelectionCornersRenderer.draw(gl, group.getMinMax(), camera,
                group.getTransformationMatrix());
        }
    }

    private static void drawBody(GL4 gl, SimpleBody body, Matrix4x4d parentTransform,
                                 Camera camera, Light light, RendererConfiguration quality)
    {
        Matrix4x4d transform = parentTransform.multiply(body.getTransformationMatrix());
        Geometry geometry = body.getGeometry();
        Image texture = body.getTexture();
        RGBImageUncompressed textureMap = texture instanceof RGBImageUncompressed
            ? (RGBImageUncompressed)texture
            : null;

        Jogl4GeometryRenderer.draw(
            gl,
            geometry,
            camera,
            light,
            body.getMaterial(),
            quality,
            textureMap,
            body.getNormalMapRgb(),
            transform);
    }

    public static void draw(GL4 gl, Scene s, SceneEditorApplication parent)
    {
        RendererConfiguration quality;
        SimpleBodyGroup ggi;
        int i;

        s.activateSelectedBackground();

        drawBase(gl, s, parent.getAwtModel().getModifyPanel());

        //- Draw 3D Gizmos ------------------------------------------------
        for ( i = 0; i < s.scene.getLights().size(); i++ ) {
            Jogl4LightRenderer.draw(gl, s.scene.getLights().get(i), s.activeCamera);
        }

        //- Draw visual debug entities (usually transparent) --------------
        Light light = s.scene.getLights().isEmpty()
            ? null
            : s.scene.getLights().get(0);

        for ( i = 0; i < s.debugThingGroups.size(); i++ ) {
            try {
                quality = s.qualityTemplate.clone();
            }
            catch ( CloneNotSupportedException e ) {
                break;
            }

            quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            quality.setSelectionCorners(s.selectedDebugThingGroups.isSelected(i));
            ggi = s.debugThingGroups.get(i);
            if ( ggi.getBodies().get(0).getGeometry() instanceof Sphere ) {
                gl.glDisable(GL4.GL_DEPTH_TEST);
            }
            drawBodyGroup(gl, ggi, s.activeCamera, light, quality);
            gl.glEnable(GL4.GL_DEPTH_TEST);
        }
    }

}

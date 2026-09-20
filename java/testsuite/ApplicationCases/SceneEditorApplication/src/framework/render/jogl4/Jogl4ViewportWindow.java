//=   previous "JoglView" class at SceneEditorApplication example).         =

package framework.render.jogl4;

// Java basic classes
import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL2;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl2ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl2MatrixRenderer;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;

// Framework classes
import framework.model.Viewport;
import framework.model.ViewportSet;

/**
JOGL presentation of one `Viewport` of a `ViewportSet`: grid, reference base,
title and gizmo labels. It holds the JOGL/image resources needed for that
drawing, and reads everything else (cameras, areas, selection) from the
injected model. It does not depend on AWT/Swing: user interaction is processed
in the `application.gui` package, and label images are provided by the
injected `Jogl4LabelImageProvider`.
*/
public class Jogl4ViewportWindow
{
    private static final ColorRgb TITLE_COLOR = new ColorRgb(0.76, 0.76, 0.76);

    private final ViewportSet viewportSet;
    private final Viewport viewport;
    private final Jogl4LabelImageProvider labelImageProvider;

    private String title;
    private RGBAImageUncompressed titleImage;
    private final RGBAImageUncompressed xLabelImage;
    private final RGBAImageUncompressed yLabelImage;
    private final RGBAImageUncompressed zLabelImage;
    private final RGBAImageUncompressed xLabelImageSelected;
    private final RGBAImageUncompressed yLabelImageSelected;
    private final RGBAImageUncompressed zLabelImageSelected;

    // Each Jogl4ViewportWindow can call a different visualization algorithm
    public static final int RENDER_MODE_ZBUFFER = Viewport.RENDER_MODE_Z_BUFFER;
    public static final int RENDER_MODE_RAYTRACING = Viewport.RENDER_MODE_RAYTRACING;

    public Jogl4ViewportWindow(ViewportSet viewportSet,
                               Viewport viewport,
                               Jogl4LabelImageProvider labelImageProvider)
    {
        this.viewportSet = viewportSet;
        this.viewport = viewport;
        this.labelImageProvider = labelImageProvider;

        xLabelImage = labelImageProvider.createLabelImage("X", new ColorRgb(0.78, 0, 0));
        yLabelImage = labelImageProvider.createLabelImage("Y", new ColorRgb(0, 0.61, 0));
        zLabelImage = labelImageProvider.createLabelImage("Z", new ColorRgb(0, 0, 0.76));

        xLabelImageSelected = labelImageProvider.createLabelImage("X", new ColorRgb(1, 1, 0));
        yLabelImageSelected = labelImageProvider.createLabelImage("Y", new ColorRgb(1, 1, 0));
        zLabelImageSelected = labelImageProvider.createLabelImage("Z", new ColorRgb(1, 1, 0));

        updateTitleImage();
    }

    public Viewport getViewport()
    {
        return viewport;
    }

    public int getRenderMode()
    {
        return viewport.getRenderMode();
    }

    public int getViewportStartX()
    {
        return viewport.getPixelStartX();
    }

    public int getViewportStartY()
    {
        return viewport.getPixelStartY();
    }

    public int getViewportSizeX()
    {
        return viewport.getPixelSizeX();
    }

    public int getViewportSizeY()
    {
        return viewport.getPixelSizeY();
    }

    public boolean isSelected()
    {
        return viewportSet.isSelected(viewport);
    }

    public boolean isActive()
    {
        return viewport.isActive();
    }

    public Camera getCamera()
    {
        return viewport.getActiveCamera();
    }

    public RendererConfiguration getRendererConfiguration()
    {
        return viewport.getRendererConfiguration();
    }

    public void drawReferenceBase(GL2 gl)
    {
        //-----------------------------------------------------------------
        int basesize = 64;
        gl.glPushAttrib(GL2.GL_VIEWPORT_BIT);
        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glPushAttrib(GL2.GL_TEXTURE_2D);
        gl.glPushAttrib(GL2.GL_LIGHTING);
        gl.glViewport(viewport.getPixelStartX(), viewport.getPixelStartY(), basesize, basesize);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        Matrix4x4d R = viewport.getActiveCamera().getRotation();

        gl.glLoadIdentity();
        R = R.invert();
        gl.glRotated(90, -1, 0, 0);
        gl.glRotated(90, 0, 0, 1);
        Jogl2MatrixRenderer.activate(gl, R);

        gl.glPushMatrix();
        gl.glTranslated(2, 1, 0);
        drawTextureString3D(gl, xLabelImage);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(1, 2, 0);
        drawTextureString3D(gl, yLabelImage);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(1, 1, 1);
        drawTextureString3D(gl, zLabelImage);
        gl.glPopMatrix();

        //gl.glLoadIdentity();
        gl.glBegin(GL2.GL_LINES);
            gl.glColor3d(0.78, 0, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(1, 0, 0);
            gl.glColor3d(0, 0.61, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 1, 0);
            gl.glColor3d(0, 0, 0.76);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 1);
        gl.glEnd();

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
    }

    private void drawGridRectangle(GL2 gl)
    {
        gl.glPushMatrix();

        Matrix4x4d R;
        double yaw, pitch;

        R = viewport.getActiveCamera().getRotation();
        yaw = Math.toDegrees(R.obtainEulerYawAngle());
        pitch = Math.toDegrees(R.obtainEulerPitchAngle());

        if ( viewport.getActiveCamera().getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL &&
              (pitch > -45 && pitch < 45) ) {
            if ( (yaw > 45 && yaw < 135) ||
                 (yaw < -45 && yaw > -135) ) {
                gl.glRotated(90, 1, 0, 0);
            }
            else {
                gl.glRotated(90, 0, 1, 0);
            }
        }
        
        //-----------------------------------------------------------------
        int nx = 14; // Must be an even number
        int ny = 14; // Must be an even number
        double dx = 1.0;
        double dy = 1.0;
        int x, y;
        double minx = -(((double)nx)/2) * dx;
        double maxx = (((double)nx)/2) * dx;
        double miny = -(((double)ny)/2) * dy;
        double maxy = (((double)ny)/2) * dy;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3d(0.37, 0.37, 0.37);
        for ( x = 0; x <= nx; x++ ) {
            if ( x == nx/2 ) continue;
            gl.glVertex3d(minx + ((double)x)*dx, miny, 0);
            gl.glVertex3d(minx + ((double)x)*dx, maxy, 0);
        }
        for ( y = 0; y <= ny; y++ ) {
            if ( y == ny/2 ) continue;
            gl.glVertex3d(minx, minx + ((double)y)*dy, 0);
            gl.glVertex3d(maxx, minx + ((double)y)*dy, 0);
        }
        gl.glColor3d(0, 0, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, miny, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, maxy, 0);
        gl.glVertex3d(minx, minx + ((double)(ny/2))*dy, 0);
        gl.glVertex3d(maxx, minx + ((double)(ny/2))*dy, 0);

        gl.glEnd();
        gl.glPopMatrix();
    }

    public void toggleGrid()
    {
        viewport.toggleGrid();
    }

    public void drawGrid(GL2 gl)
    {
        //- Draw reference grid plane -------------------------------------
        if ( viewport.isShowGrid() ) drawGridRectangle(gl);
    }

    public void drawTextureString2D(GL2 gl, int x, int y, RGBAImageUncompressed i)
    {
        double dx;
        double dy;

        dx = ((double)(2*x)) / ((double)viewport.getPixelSizeX());
        dy = ((double)(2*(viewport.getPixelSizeY() - y))) / ((double)viewport.getPixelSizeY());

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslated(dx, dy, 0);

        drawTextureString3D(gl, i);

        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private void drawTextureString3D(GL2 gl, RGBAImageUncompressed i)
    {
        gl.glPushAttrib(GL2.GL_ENABLE_BIT);
        gl.glRasterPos3d(-1, -1, 0);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // Set texture parameters
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_GENERATE_MIPMAP,
            GL2.GL_TRUE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
            GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
            GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
            GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
            GL2.GL_CLAMP_TO_EDGE);

        // Calling this configuration with GL_BLEND here generates an error on
        // some Windows Vista machines with Intel graphics, as such on
        // Dell Inspiron 1525 laptop with Mobile Intel 965 (BIOS 1566).
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

        //float c[] = {1f, 1f, 1f, 1f};
        //gl.glTexEnvfv(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_COLOR, c, 0);

        Jogl2ImageRenderer.draw(gl, i);
        gl.glPopAttrib();
    }

    public void drawTitle(GL2 gl)
    {
        int borderx = 4;
        int bordery = 1;

        updateTitleImage();
        drawTextureString2D(gl, borderx, titleImage.getYSize() + bordery, titleImage);
    }

    /**
    The title is owned by the model viewport (it follows the active camera),
    so the image is regenerated whenever it changes.
    */
    private void updateTitleImage()
    {
        String currentTitle = viewport.getTitle();

        if ( titleImage == null || !currentTitle.equals(title) ) {
            title = currentTitle;
            titleImage = labelImageProvider.createLabelImage(title, TITLE_COLOR);
        }
    }

    public void drawLabelsForTranslateGizmo(GL2 gl, TranslateGizmo gizmo)
    {
        ArrayList<SimpleBody> things = gizmo.getElements();
        int i;
        Vector3Dd lv = new Vector3Dd();
        Vector3Dd p;
        Vector3Dd tp = new Vector3Dd();
        Matrix4x4d R;
        boolean yellow;
        ColorRgb c = new ColorRgb(1, 1, 0);

        for ( i = 0; i < things.size() && i < 3; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3Dd position;

            if ( g != null ) {
                gl.glPushMatrix();
                gl.glLoadIdentity();

                lv = new Vector3Dd(0, 0, lv.z());
                if ( g instanceof Arrow ) {
                    Arrow a = ((Arrow)g);
                    lv = lv.withZ((a.getHeadLength() + a.getBaseLength()) * 1.1);
                }
                else {
                    lv = lv.withZ(1);
                }

                R = new Matrix4x4d();
                R = R.translation(r.getPosition());
                R = R.multiply(r.getRotation());
                p = R.multiply(lv);
                viewport.getActiveCamera().projectPoint(p, tp);

                //---------------------------------------------
                yellow = false;
                if ( ColorRgb.distance(c, r.getMaterial().getDiffuse()) <
                     VSDK.EPSILON ) {
                yellow = true;
                }

                //---------------------------------------------
                switch ( i ) {
                  case 0:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            xLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            xLabelImage);
                    }
                    break;
                  case 1:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            yLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            yLabelImage);
                    }
                    break;
                  case 2:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            zLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            zLabelImage);
                    }
                    break;
                }
                gl.glPopMatrix();
            }
        }
    }
}

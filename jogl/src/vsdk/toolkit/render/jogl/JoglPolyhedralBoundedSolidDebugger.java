package vsdk.toolkit.render.jogl;

// Java classes
import java.io.File;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.render.PolyhedralBoundedSolidDebugger;

/**
Warning: check why this class implements a GLEventListener. Advanced debug
features?
*/
public class JoglPolyhedralBoundedSolidDebugger 
extends PolyhedralBoundedSolidDebugger
implements GLEventListener
{
    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLOffscreenAutoDrawable pbuffer;
    private RGBImage image;
    private String filename;
    private PolyhedralBoundedSolid solid;
    private Camera camera;
    private RendererConfiguration quality;
    private Material material;
    private Light light1;
    private Light light2;

    public JoglPolyhedralBoundedSolidDebugger()
    {
        init();
    }

    private void init()
    {
        GLProfile profile;

        profile = GLProfile.get(GLProfile.GL2);
        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(true);
        try {
            GLDrawableFactory creator = GLDrawableFactory.getFactory(profile);
            pbuffer = creator.createOffscreenAutoDrawable(
                null, pbCaps, null, imageWidth, imageHeight);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              System.exit(1);
        }
        pbuffer.addGLEventListener(this);

        //-----------------------------------------------------------------
        camera = new Camera();
        camera.setPosition(new Vector3D(2, -1, 2));
        Matrix4x4 R = new Matrix4x4();
        R = R.eulerAnglesRotation(Math.toRadians(135), Math.toRadians(-35), 0);
        camera.setRotation(R);
        camera.setFov(45.0);

        quality = new RendererConfiguration();
        quality.setSurfaces(false);
        quality.setPoints(true);
        quality.setWires(true);

        material = defaultMaterial();
        light1 = new Light(vsdk.toolkit.environment.LightType.POINT, new Vector3D(3, -3, 2), new ColorRgb(1, 1, 1));
        light2 = new Light(vsdk.toolkit.environment.LightType.POINT, new Vector3D(-2, 5, -2), new ColorRgb(0.9, 0.5, 0.5));
        light1.setId(0);
        light2.setId(1);
    }

    private Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.5, 0.9));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }

    @Override
    public void execute(PolyhedralBoundedSolid solid, String filename)
    {
        this.filename = filename;
        this.solid = solid;
        pbuffer.display();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        JoglCameraRenderer.activate(gl, camera);
        JoglMaterialRenderer.activate(gl, material);
        JoglLightRenderer.activate(gl, light1);
        JoglLightRenderer.draw(gl, light1);
        JoglLightRenderer.activate(gl, light2);
        JoglLightRenderer.draw(gl, light2);
        gl.glEnable(GL2.GL_LIGHTING);
        JoglPolyhedralBoundedSolidRenderer.draw(gl, solid, camera, quality);
        JoglPolyhedralBoundedSolidRenderer.drawDebugVertices(gl, solid, camera);
        
        image=JoglRGBImageRenderer.getImageJOGL(gl);
        ImagePersistence.exportPNG(new File(filename), image);
    }

    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
    }

    @Override
    public void init( GLAutoDrawable drawable ) {
    }
  
    /** Not used method, but needed to instanciate GLEventListener */
    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

}

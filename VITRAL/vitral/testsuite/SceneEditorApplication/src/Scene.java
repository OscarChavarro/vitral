import java.io.File;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.RGBAImageBuilder;

public class Scene
{
    //- 1. Camera ----------------------------------------------------------
    public Camera camera;
    public Camera activeCamera;

    //- 2. Lights ----------------------------------------------------------

    //- 3. Background ------------------------------------------------------
    public SimpleBackground simpleBackground;
    public CubemapBackground cubemapBackground;
    public int selectedBackground;

    //- 4. Objects ---------------------------------------------------------
    public SimpleCorridor corridor;
    public boolean showCorridor;

    Scene()
    {
        //-----------------------------------------------------------------
        camera = new Camera();
        activeCamera = camera;

        //-----------------------------------------------------------------
        simpleBackground = new SimpleBackground();
        simpleBackground.setColor(0, 0, 0);

        RGBAImage front, right, back, left, down, up;

        try {
            System.out.print("Loading background: 1");
            front = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno0_small.jpg"));
            System.out.print("2");
            right = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno1_small.jpg"));
            System.out.print("3");
            back = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno2_small.jpg"));
            System.out.print("4");
            left = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno3_small.jpg"));
            System.out.print("5");
            down = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno4_small.jpg"));
            System.out.print("6");
            up = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno5_small.jpg"));
            System.out.println(" OK!");

            cubemapBackground = 
                new CubemapBackground(camera, 
                                      front, right, back, left, down, up);
        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }

        selectedBackground = 0;

        //-----------------------------------------------------------------
        corridor = new SimpleCorridor();
        showCorridor = true;
    }
}

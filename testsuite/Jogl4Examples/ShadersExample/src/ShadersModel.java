import java.io.File;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.LightType;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;

public class ShadersModel
{
    private static final int MIN_SPHERE_MERIDIANS = 12;
    private static final int MIN_SPHERE_PARALLELS = 8;

    private Camera camera;
    private CameraController cameraController;
    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private Sphere sphere;
    private Light light;
    private Material material;
    private RGBImage textureMap;
    private RGBImage bumpMapHeightRgb;
    private boolean animationEnabled;
    private double sphereRotationAngleRadians;
    private int sphereMeridians;
    private int sphereParallels;

    public static ShadersModel createDefault()
    {
        ShadersModel model = new ShadersModel();
        model.initializeDefaults();
        return model;
    }

    private void initializeDefaults()
    {
        camera = new Camera();
        camera.setPosition(new Vector3D(0, -4, 0));
        Matrix4x4 rotation = new Matrix4x4().eulerAnglesRotation(Math.toRadians(90.0), 0, 0);
        camera.setRotation(rotation);
        camera.setFov(30.0);

        cameraController = new CameraControllerOrbiter(camera);

        quality = new RendererConfiguration();
        quality.setTexture(true);
        quality.setBumpMap(true);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
        qualityController = new RendererConfigurationController(quality);

        sphere = new Sphere(1.0);

        light = new Light(LightType.POINT, new Vector3D(1, -3, 1), new ColorRgb(1, 1, 1));
        light.setId(0);

        material = new Material();
        material.setAmbient(new ColorRgb(0.1, 0.1, 0.1));
        material.setDiffuse(new ColorRgb(1, 1, 1));
        material.setSpecular(new ColorRgb(1, 1, 1));
        material.setPhongExponent(40);

        try {
            textureMap = ImagePersistence.importRGB(new File("../../../etc/textures/miniearth.png"));

            IndexedColorImage bump = ImagePersistence.importIndexedColor(new File("../../../etc/bumpmaps/earth.bw"));
            // [BLIN1978b] Section 3:
            // bump mapping is driven by a scalar height function F(u,v).
            // The shader computes Fu/Fv with central differences directly
            // from this grayscale height map.
            bumpMapHeightRgb = bump.exportToRgbImage();
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed loading textures for ShadersExample", e);
        }

        animationEnabled = false;
        sphereRotationAngleRadians = 0.0;
        sphereMeridians = 64;
        sphereParallels = 32;
    }

    public Camera getCamera()
    {
        return camera;
    }

    public CameraController getCameraController()
    {
        return cameraController;
    }

    public RendererConfiguration getQuality()
    {
        return quality;
    }

    public RendererConfigurationController getQualityController()
    {
        return qualityController;
    }

    public Sphere getSphere()
    {
        return sphere;
    }

    public Light getLight()
    {
        return light;
    }

    public Material getMaterial()
    {
        return material;
    }

    public RGBImage getTextureMap()
    {
        return textureMap;
    }

    public RGBImage getBumpMapHeightRgb()
    {
        return bumpMapHeightRgb;
    }

    public boolean isAnimationEnabled()
    {
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled)
    {
        this.animationEnabled = animationEnabled;
    }

    public void toggleAnimationEnabled()
    {
        animationEnabled = !animationEnabled;
    }

    public double getSphereRotationAngleRadians()
    {
        return sphereRotationAngleRadians;
    }

    public void setSphereRotationAngleRadians(double sphereRotationAngleRadians)
    {
        this.sphereRotationAngleRadians = normalizeAngleRadians(sphereRotationAngleRadians);
    }

    public void advanceSphereRotationRadians(double deltaRadians)
    {
        setSphereRotationAngleRadians(sphereRotationAngleRadians + deltaRadians);
    }

    public int getSphereMeridians()
    {
        return sphereMeridians;
    }

    public int getSphereParallels()
    {
        return sphereParallels;
    }

    public void setSphereMeridians(int sphereMeridians)
    {
        this.sphereMeridians = Math.max(MIN_SPHERE_MERIDIANS, sphereMeridians);
    }

    public void setSphereParallels(int sphereParallels)
    {
        this.sphereParallels = Math.max(MIN_SPHERE_PARALLELS, sphereParallels);
    }

    public void changeSphereMeridians(int delta)
    {
        setSphereMeridians(sphereMeridians + delta);
    }

    public void changeSphereParallels(int delta)
    {
        setSphereParallels(sphereParallels + delta);
    }

    private static double normalizeAngleRadians(double angle)
    {
        double twoPi = 2.0 * Math.PI;
        double normalized = angle % twoPi;
        if ( normalized < 0.0 ) {
            normalized += twoPi;
        }
        return normalized;
    }
}

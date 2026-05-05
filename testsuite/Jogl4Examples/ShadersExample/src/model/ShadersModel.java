package model;

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
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBImageUncompressed;

public class ShadersModel
{
    private static final int MIN_SPHERE_MERIDIANS = 12;
    private static final int MIN_SPHERE_PARALLELS = 8;
    private static final Vector3D DEFAULT_BUMP_SCALE = new Vector3D(1.0, 1.0, 1.0);

    private Camera camera;
    private CameraController cameraController;
    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private Sphere sphere;
    private Light light;
    private Material material;
    private RGBImageUncompressed textureMap;
    private RGBImageUncompressed bumpMapHeightRgb;
    private RGBImageUncompressed softwareFrameImage;
    private ShaderOperationMode renderingMode;
    private boolean showHud;
    private boolean animationEnabled;
    private boolean lightAnimationEnabled;
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

            IndexedColorImageUncompressed bump = ImagePersistence.importIndexedColor(
                new File("../../../etc/bumpmaps/earth.bw"));
            NormalMap bumpNormalMap = new NormalMap();
            bumpNormalMap.importBumpMap(bump, DEFAULT_BUMP_SCALE);
            // Keep GLSL and CPU raytracer aligned: both consume the same
            // precomputed normal field extracted from the bump map.
            bumpMapHeightRgb = bumpNormalMap.exportToRgbImage();
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed loading textures for ShadersExample", e);
        }

        animationEnabled = false;
        lightAnimationEnabled = false;
        renderingMode = ShaderOperationMode.OPENGL_4_1;
        showHud = true;
        sphereRotationAngleRadians = 0.0;
        sphereMeridians = 100;
        sphereParallels = 50;
        updateSoftwareViewportAndCamera(
            (int)camera.getViewportXSize(),
            (int)camera.getViewportYSize());
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

    public RGBImageUncompressed getTextureMap()
    {
        return textureMap;
    }

    public RGBImageUncompressed getBumpMapHeightRgb()
    {
        return bumpMapHeightRgb;
    }

    public RGBImageUncompressed getSoftwareFrameImage()
    {
        return softwareFrameImage;
    }

    public ShaderOperationMode getRenderingMode()
    {
        return renderingMode;
    }

    public void setRenderingMode(ShaderOperationMode renderingMode)
    {
        this.renderingMode = renderingMode;
    }

    public void rotateRenderingMode()
    {
        renderingMode = renderingMode.next();
    }

    public boolean isShowHud()
    {
        return showHud;
    }

    public void setShowHud(boolean showHud)
    {
        this.showHud = showHud;
    }

    public void toggleShowHud()
    {
        showHud = !showHud;
    }

    public void updateSoftwareViewportAndCamera(int viewportWidth, int viewportHeight)
    {
        int width = Math.max(1, viewportWidth);
        int height = Math.max(1, viewportHeight);
        camera.updateViewportResize(width, height);
        if ( softwareFrameImage != null &&
             softwareFrameImage.getXSize() == width &&
             softwareFrameImage.getYSize() == height ) {
            return;
        }
        softwareFrameImage = new RGBImageUncompressed();
        if ( !softwareFrameImage.init(width, height) ) {
            throw new IllegalStateException(
                "Could not allocate software frame image " + width + "x" + height);
        }
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

    public boolean isLightAnimationEnabled()
    {
        return lightAnimationEnabled;
    }

    public void setLightAnimationEnabled(boolean lightAnimationEnabled)
    {
        this.lightAnimationEnabled = lightAnimationEnabled;
    }

    public void toggleLightAnimationEnabled()
    {
        lightAnimationEnabled = !lightAnimationEnabled;
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

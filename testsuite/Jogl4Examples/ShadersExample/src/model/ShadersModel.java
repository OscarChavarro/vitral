package model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.material.MicroFacetedMaterial;
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
    private static final String MICROFACET_CSV_FILE =
        "../../../etc/materials/microFacetMAterials.csv";
    private static final String COOK_TORRANCE_MATERIAL_NAME = "Copper";

    private Camera camera;
    private CameraController cameraController;
    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private Sphere sphere;
    private Light light;
    private SimpleMaterial material;
    private MicroFacetedMaterial cookTorranceCopperMaterial;
    private List<String> cookTorranceMaterialNames;
    private int cookTorranceMaterialIndex;
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

        material = new SimpleMaterial();
        material = material.withAmbient(new ColorRgb(0.1, 0.1, 0.1));
        material = material.withDiffuse(new ColorRgb(1, 1, 1));
        material = material.withSpecular(new ColorRgb(1, 1, 1));
        material = material.withPhongExponent(40);
        cookTorranceCopperMaterial = new MicroFacetedMaterial(
            MICROFACET_CSV_FILE,
            COOK_TORRANCE_MATERIAL_NAME);
        cookTorranceMaterialNames = loadMicroFacetMaterialNames(MICROFACET_CSV_FILE);
        cookTorranceMaterialIndex = indexOfMaterialName(
            cookTorranceMaterialNames,
            COOK_TORRANCE_MATERIAL_NAME);

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

    public SimpleMaterial getMaterial()
    {
        return material;
    }

    public SimpleMaterial getActiveMaterialForCurrentShading()
    {
        if ( quality.getShadingType() == RendererConfiguration.SHADING_TYPE_COOK_TERRANCE ) {
            return cookTorranceCopperMaterial;
        }
        return material;
    }

    public void cycleCookTorranceMaterial()
    {
        if ( cookTorranceMaterialNames == null || cookTorranceMaterialNames.isEmpty() ) {
            return;
        }
        cookTorranceMaterialIndex =
            (cookTorranceMaterialIndex + 1) % cookTorranceMaterialNames.size();
        String nextMaterialName = cookTorranceMaterialNames.get(cookTorranceMaterialIndex);
        cookTorranceCopperMaterial = new MicroFacetedMaterial(
            MICROFACET_CSV_FILE,
            nextMaterialName);
    }

    public String getCookTorranceMaterialLabel()
    {
        if ( cookTorranceCopperMaterial == null ||
             cookTorranceCopperMaterial.getName() == null ||
             cookTorranceCopperMaterial.getName().isBlank() ) {
            return COOK_TORRANCE_MATERIAL_NAME;
        }
        return cookTorranceCopperMaterial.getName();
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

    private static int indexOfMaterialName(
        List<String> materialNames,
        String targetName)
    {
        int i;
        for ( i = 0; i < materialNames.size(); i++ ) {
            if ( materialNames.get(i).equalsIgnoreCase(targetName) ) {
                return i;
            }
        }
        return 0;
    }

    private static List<String> loadMicroFacetMaterialNames(String csvPath)
    {
        ArrayList<String> materialNames = new ArrayList<String>();
        File csvFile = new File(csvPath);
        try {
            List<String> lines = Files.readAllLines(
                csvFile.toPath(),
                StandardCharsets.UTF_8);
            if ( lines.isEmpty() ) {
                materialNames.add(COOK_TORRANCE_MATERIAL_NAME);
                return materialNames;
            }
            int i;
            for ( i = 1; i < lines.size(); i++ ) {
                String line = lines.get(i).trim();
                if ( line.isEmpty() ) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if ( fields.length == 0 ) {
                    continue;
                }
                String materialName = fields[0].trim();
                if ( materialName.isEmpty() ) {
                    continue;
                }
                materialNames.add(materialName);
            }
        }
        catch ( IOException e ) {
            materialNames.clear();
        }
        if ( materialNames.isEmpty() ) {
            materialNames.add(COOK_TORRANCE_MATERIAL_NAME);
        }
        return materialNames;
    }
}

package vsdk.toolkit.render.raytracing;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.AmbientLight;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.io.geometry.ReaderMitScene;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.TraceWorkspace;
import vsdk.toolkit.render.shaders.ShadowTransmittance;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectNe;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.vitralRepositoryRoot;

/**
Port of `cpp/base/src/test/refraction_tests.cpp`; each test method is named
`<cpp suite>_<cpp test>`.
*/
class RefractionTest
{
    private static final String VITRAL_REPOSITORY_ROOT = vitralRepositoryRoot();

    private static SimpleBody createSphere(Vector3Dd position, SimpleMaterial material)
    {
        SimpleBody body = new SimpleBody();
        body.setGeometry(new Sphere(1.0));
        body.setPosition(position);
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());
        body.setMaterial(new SimpleMaterial(material));
        return body;
    }

    private static SimpleMaterial glass(double kt)
    {
        SimpleMaterial m = new SimpleMaterial();
        return m.withRefractionCoefficient(kt).withIndexOfRefraction(1.5);
    }

    @Test
    void SnellRefraction_given_normalIncidence_when_refracting_then_directionIsUnchanged()
    {
        Vector3Dd[] out = {new Vector3Dd()};
        assertThat(SnellRefraction.refract(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 1), 1.0 / 1.5, out)).isTrue();
        expectNear(0.0, out[0].x(), 1e-12);
        expectNear(0.0, out[0].y(), 1e-12);
        expectNear(-1.0, out[0].z(), 1e-12);
    }

    @Test
    void SnellRefraction_given_obliqueRayIntoGlass_when_refracting_then_snellLawHolds()
    {
        double s = Math.sqrt(0.5);
        Vector3Dd[] out = {new Vector3Dd()};
        // Incoming ray travels along (1, 0, -1); it comes from air into glass.
        assertThat(SnellRefraction.refract(new Vector3Dd(-s, 0, s), new Vector3Dd(0, 0, 1), 1.0 / 1.5, out)).isTrue();
        expectNear(1.0, out[0].length(), 1e-12);
        // n1 sin(theta1) == n2 sin(theta2)
        expectNear(1.0 * s, 1.5 * out[0].x(), 1e-12);
        expectLt(out[0].z(), 0.0);
    }

    @Test
    void SnellRefraction_given_rayBeyondCriticalAngle_when_refracting_then_totalInternalReflection()
    {
        // Glass to air (eta 1.5): the critical angle is about 41.8 degrees.
        double angle = 60.0 * Math.PI / 180.0;
        Vector3Dd towardViewer = new Vector3Dd(-Math.sin(angle), 0, Math.cos(angle));
        Vector3Dd[] out = {new Vector3Dd(7, 7, 7)};
        expectFalse(SnellRefraction.refract(towardViewer, new Vector3Dd(0, 0, 1), 1.5, out));
        expectDoubleEq(7.0, out[0].x());
        Vector3Dd mirror = SnellRefraction.reflect(towardViewer, new Vector3Dd(0, 0, 1));
        expectNear(Math.sin(angle), mirror.x(), 1e-12);
        expectNear(Math.cos(angle), mirror.z(), 1e-12);
    }

    @Test
    void ShadowTransmittance_given_blockers_when_evaluating_then_opaqueStopsAndGlassFilters()
    {
        SimpleScene scene = new SimpleScene();
        SimpleMaterial opaque = new SimpleMaterial();
        scene.addBody(createSphere(new Vector3Dd(0, 0, 3), glass(0.9)));
        scene.addBody(createSphere(new Vector3Dd(0, 0, 6), glass(0.5)));
        scene.addBody(createSphere(new Vector3Dd(0, 0, 20), opaque));
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        TraceWorkspace workspace = new TraceWorkspace();
        Ray shadowRay = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1));

        // One glass body before the light.
        expectDoubleEq(0.9, ShadowTransmittance.evaluate(shadowRay, 4.5, bodies, workspace, false));
        // Two glass bodies multiply their transmission coefficients.
        expectDoubleEq(0.45, ShadowTransmittance.evaluate(shadowRay, 10.0, bodies, workspace, false));
        // An opaque body stops the light.
        expectDoubleEq(0.0, ShadowTransmittance.evaluate(shadowRay, 30.0, bodies, workspace, false));
        // Bodies behind the light do not cast shadows.
        expectDoubleEq(1.0, ShadowTransmittance.evaluate(shadowRay, 1.0, bodies, workspace, false));
    }

    @Test
    void RefractionReader_given_mitSurfaceWithIndex_when_reading_then_materialKeepsIt() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/glass_lens.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(3);
        expectDoubleEq(0.0, bodies.get(1).getMaterial().getRefractionCoefficient());
        expectDoubleEq(0.9, bodies.get(2).getMaterial().getRefractionCoefficient());
        expectDoubleEq(1.5, bodies.get(2).getMaterial().getIndexOfRefraction());
    }

    @Test
    void RefractionRender_given_glassWithIndexOfAir_when_raytracing_then_sphereIsInvisible()
    {
        int width = 41;
        int height = 41;
        SimpleScene scene = new SimpleScene();
        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(-5, -5, 5));
        camera.setRotation(new Matrix4x4d().eulerAnglesRotation(
            Math.toRadians(45), Math.toRadians(-35), 0));
        SimpleBackground background = new SimpleBackground();
        background.setColor(0.2, 0.6, 0.4);
        scene.addCamera(camera);
        scene.addBackground(background);
        scene.addLight(new AmbientLight(new ColorRgb(1, 1, 1)));
        SimpleMaterial clear = new SimpleMaterial();
        clear = clear.withAmbient(new ColorRgb(0, 0, 0)).withDiffuse(new ColorRgb(0, 0, 0))
                     .withSpecular(new ColorRgb(0, 0, 0)).withRefractionCoefficient(1.0)
                     .withIndexOfRefraction(1.0);
        scene.addBody(createSphere(new Vector3Dd(0, 0, 0), clear));
        SimpleSceneSnapshot snapshot = scene.exportToSimpleSceneSnapshot(width, height);
        RendererConfiguration quality = new RendererConfiguration();
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(width, height);

        SimpleRaytracer raytracer = new SimpleRaytracer();
        raytracer.execute(image, quality, snapshot, null, null);

        RGBPixel corner = new RGBPixel();
        RGBPixel center = new RGBPixel();
        image.getPixelRgb(0, 0, corner);
        image.getPixelRgb(width / 2, height / 2, center);
        expectEq(corner.getR(), center.getR());
        expectEq(corner.getG(), center.getG());
        expectEq(corner.getB(), center.getB());

        // Control: the same sphere with a little ambient reflectance is seen at the center.
        scene.getSimpleBodies().get(0).setMaterial(new SimpleMaterial(clear.withAmbient(new ColorRgb(1, 0, 0))));
        snapshot = scene.exportToSimpleSceneSnapshot(width, height);
        raytracer.execute(image, quality, snapshot, null, null);
        image.getPixelRgb(width / 2, height / 2, center);
        expectNe(corner.getR(), center.getR());
    }
}

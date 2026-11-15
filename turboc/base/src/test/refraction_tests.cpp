#include "vsdk/toolkit/common/VSDK.h"
#include <math.h>
#include "VitralTest.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/AmbientLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/TraceWorkspace.h"
#include "vsdk/toolkit/render/raytracing/SimpleRaytracer.h"
#include "vsdk/toolkit/render/raytracing/SnellRefraction.h"
#include "vsdk/toolkit/render/shaders/ShadowTransmittance.h"

static SimpleBody* createSphere(const Vector3Dd& position, const SimpleMaterial& material)
{
    SimpleBody* body = new SimpleBody();
    body->setGeometry(new Sphere(1.0));
    body->setPosition(position);
    body->setRotation(Matrix4x4d());
    body->setRotationInverse(Matrix4x4d());
    body->setMaterial(new SimpleMaterial(material));
    return body;
}

static SimpleMaterial glass(double kt)
{
    SimpleMaterial m;
    return m.withRefractionCoefficient(kt).withIndexOfRefraction(1.5);
}

TEST(SnellRefraction, given_normalIncidence_when_refracting_then_directionIsUnchanged) {
    Vector3Dd out;
    ASSERT_TRUE(SnellRefraction::refract(Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 1), 1.0 / 1.5, &out));
    EXPECT_NEAR(0.0, out.x(), 1e-12);
    EXPECT_NEAR(0.0, out.y(), 1e-12);
    EXPECT_NEAR(-1.0, out.z(), 1e-12);
}

TEST(SnellRefraction, given_obliqueRayIntoGlass_when_refracting_then_snellLawHolds) {
    double s = sqrt(0.5);
    Vector3Dd out;
    // Incoming ray travels along (1, 0, -1); it comes from air into glass.
    ASSERT_TRUE(SnellRefraction::refract(Vector3Dd(-s, 0, s), Vector3Dd(0, 0, 1), 1.0 / 1.5, &out));
    EXPECT_NEAR(1.0, out.length(), 1e-12);
    // n1 sin(theta1) == n2 sin(theta2)
    EXPECT_NEAR(1.0 * s, 1.5 * out.x(), 1e-12);
    EXPECT_LT(out.z(), 0.0);
}

TEST(SnellRefraction, given_rayBeyondCriticalAngle_when_refracting_then_totalInternalReflection) {
    // Glass to air (eta 1.5): the critical angle is about 41.8 degrees.
    double angle = 60.0 * M_PI / 180.0;
    Vector3Dd towardViewer(-sin(angle), 0, cos(angle));
    Vector3Dd out(7, 7, 7);
    EXPECT_FALSE(SnellRefraction::refract(towardViewer, Vector3Dd(0, 0, 1), 1.5, &out));
    EXPECT_DOUBLE_EQ(7.0, out.x());
    Vector3Dd mirror = SnellRefraction::reflect(towardViewer, Vector3Dd(0, 0, 1));
    EXPECT_NEAR(sin(angle), mirror.x(), 1e-12);
    EXPECT_NEAR(cos(angle), mirror.z(), 1e-12);
}

TEST(ShadowTransmittance, given_blockers_when_evaluating_then_opaqueStopsAndGlassFilters) {
    SimpleScene scene;
    SimpleMaterial opaque;
    scene.addBody(createSphere(Vector3Dd(0, 0, 3), glass(0.9)));
    scene.addBody(createSphere(Vector3Dd(0, 0, 6), glass(0.5)));
    scene.addBody(createSphere(Vector3Dd(0, 0, 20), opaque));
    ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    TraceWorkspace workspace;
    Ray shadowRay(Vector3Dd(0, 0, 0), Vector3Dd(0, 0, 1));

    // One glass body before the light.
    EXPECT_DOUBLE_EQ(0.9, ShadowTransmittance::evaluate(shadowRay, 4.5, bodies, &workspace, FALSE));
    // Two glass bodies multiply their transmission coefficients.
    EXPECT_DOUBLE_EQ(0.45, ShadowTransmittance::evaluate(shadowRay, 10.0, bodies, &workspace, FALSE));
    // An opaque body stops the light.
    EXPECT_DOUBLE_EQ(0.0, ShadowTransmittance::evaluate(shadowRay, 30.0, bodies, &workspace, FALSE));
    // Bodies behind the light do not cast shadows.
    EXPECT_DOUBLE_EQ(1.0, ShadowTransmittance::evaluate(shadowRay, 1.0, bodies, &workspace, FALSE));
}

TEST(RefractionReader, given_mitSurfaceWithIndex_when_reading_then_materialKeepsIt) {
    SimpleScene scene;
    ReaderMitScene reader;
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT "/etc/geometry/mitscenes/glass_lens.ray", &scene);
    ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
    ASSERT_EQ(3, bodies.size());
    EXPECT_DOUBLE_EQ(0.0, bodies[1]->getMaterial()->getRefractionCoefficient());
    EXPECT_DOUBLE_EQ(0.9, bodies[2]->getMaterial()->getRefractionCoefficient());
    EXPECT_DOUBLE_EQ(1.5, bodies[2]->getMaterial()->getIndexOfRefraction());
}

TEST(RefractionRender, given_glassWithIndexOfAir_when_raytracing_then_sphereIsInvisible) {
    int width = 41;
    int height = 41;
    SimpleScene scene;
    Camera* camera = new Camera();
    camera->setPosition(Vector3Dd(-5, -5, 5));
    camera->setRotation(Matrix4x4d().eulerAnglesRotation(
        Math::toRadians(45), Math::toRadians(-35), 0));
    SimpleBackground* background = new SimpleBackground();
    background->setColor(0.2, 0.6, 0.4);
    scene.addCamera(camera);
    scene.addBackground(background);
    scene.addLight(new AmbientLight(ColorRgb(1, 1, 1)));
    SimpleMaterial clear;
    clear = clear.withAmbient(ColorRgb(0, 0, 0)).withDiffuse(ColorRgb(0, 0, 0))
                 .withSpecular(ColorRgb(0, 0, 0)).withRefractionCoefficient(1.0)
                 .withIndexOfRefraction(1.0);
    scene.addBody(createSphere(Vector3Dd(0, 0, 0), clear));
    SimpleSceneSnapshot* snapshot = scene.exportToSimpleSceneSnapshot(width, height);
    RendererConfiguration quality;
    RGBImageUncompressed image;
    image.init(width, height);

    SimpleRaytracer raytracer;
    raytracer.execute(&image, &quality, snapshot, 0, 0);

    RGBPixel corner;
    RGBPixel center;
    image.getPixelRgb(0, 0, &corner);
    image.getPixelRgb(width / 2, height / 2, &center);
    EXPECT_EQ(corner.getR(), center.getR());
    EXPECT_EQ(corner.getG(), center.getG());
    EXPECT_EQ(corner.getB(), center.getB());
    delete snapshot;

    // Control: the same sphere with a little ambient reflectance is seen at the center.
    scene.getSimpleBodies()[0]->setMaterial(new SimpleMaterial(clear.withAmbient(ColorRgb(1, 0, 0))));
    snapshot = scene.exportToSimpleSceneSnapshot(width, height);
    raytracer.execute(&image, &quality, snapshot, 0, 0);
    image.getPixelRgb(width / 2, height / 2, &center);
    EXPECT_NE(corner.getR(), center.getR());
    delete snapshot;
}

VITRAL_TEST_MAIN()

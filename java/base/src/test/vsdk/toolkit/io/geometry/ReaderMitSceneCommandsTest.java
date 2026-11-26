package vsdk.toolkit.io.geometry;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.background.CubemapBackground;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Prism;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.csg.CsgBody;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectThrow;
import static vsdk.toolkit.GTestAssertions.removeFile;
import static vsdk.toolkit.GTestAssertions.tempDir;
import static vsdk.toolkit.GTestAssertions.vitralRepositoryRoot;
import static vsdk.toolkit.GTestAssertions.writeFile;

/**
Port of `cpp/base/src/test/reader_mit_scene_commands_tests.cpp`; each test
method is named `<cpp suite>_<cpp test>`.
*/
class ReaderMitSceneCommandsTest
{
    private static final String VITRAL_REPOSITORY_ROOT = vitralRepositoryRoot();
    @Test
    void ReaderMitCommands_MeshPolyhedralAndTorusBodiesHaveRealHits() throws Exception
    {
        final String[] scenes = {"quadmesh_test.ray", "balls_featured_object.ray", "balls_torus.ray"};
        for (int i = 0; i < 3; ++i) {
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            final String path = VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/" + scenes[i];
            reader.importEnvironment(path, scene);
            ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
            assertThat(2 <= bodies.size()).as(scenes[i]).isTrue();
            SimpleBody center = bodies.get(1);
            if (i == 0) assertThat(center.getGeometry()).isInstanceOf(QuadMesh.class);
            if (i == 1) assertThat(center.getGeometry()).isInstanceOf(PolyhedralBoundedSolid.class);
            if (i == 2) assertThat(center.getGeometry()).isInstanceOf(Torus.class);
            // Every object sits at the origin and is hit from above.
            final double height = i == 2 ? 0.13 : 0.0;
            RayHit hit = new RayHit(RayHit.DETAIL_POINT, false);
            final Vector3Dd origin = i == 2 ? new Vector3Dd(0.36, 0, 5) : new Vector3Dd(0, 0, 5);
            assertThat(center.doIntersectionFirstHit(new Ray(origin, new Vector3Dd(0, 0, -1)), hit)).as(scenes[i]).isTrue();
            expectLt(hit.getHitDistance(), 5.0, scenes[i]);
            if (i == 2) expectNear(5 - height, hit.getHitDistance(), 1e-6);
        }
    }

    @Test
    void ReaderMitCommands_TexturesAndBumpMapsAreSharedSceneResources() throws Exception
    {
        final String path = tempDir() + "reader_mit_textures.ray";
        writeFile(path, "eye 0 -5 0\nup 0 0 1\nlookat 0 0 0\n" +
              "texture \"" + VITRAL_REPOSITORY_ROOT + "/etc/textures/miniearth.png\"\n" +
              "bumpmap \"" + VITRAL_REPOSITORY_ROOT + "/etc/bumpmaps/earth.bw\" 1 1 0.2\n" +
              "sphere -2 0 0 1\nsphere 2 0 0 1\n" +
              "texture \"" + VITRAL_REPOSITORY_ROOT + "/etc/textures/miniearth.png\"\n" +
              "notexture\nnobumpmap\nsphere 0 0 2 1\n");
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(path, scene);
        removeFile(path);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(3);
        assertThat(bodies.get(0).getTexture()).isNotNull();
        assertThat(bodies.get(1).getTexture()).isSameAs(bodies.get(0).getTexture());
        assertThat(bodies.get(0).getNormalMap()).isNotNull();
        assertThat(bodies.get(1).getNormalMap()).isSameAs(bodies.get(0).getNormalMap());
        assertThat(bodies.get(2).getTexture()).isNull();
        assertThat(bodies.get(2).getNormalMap()).isNull();
        // A repeated texture file is loaded once.
        expectEq(1, scene.getImages().size());
        expectEq(1, scene.getNormalMaps().size());
        // Removing a body leaves the shared resources to the scene.
        bodies.remove(0);
        assertThat(bodies.get(0).getTexture()).isSameAs(scene.getImages().get(0));
    }

    @Test
    void ReaderMitCommands_ResourcesResolveFromTheSceneDirectory() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        // bumpMapping.ray names its files relative to its own directory.
        reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/bumpMapping.ray", scene);
        assertThat(scene.getSimpleBodies().size()).isEqualTo(1);
        assertThat(scene.getSimpleBodies().get(0).getTexture()).isNotNull();
        assertThat(scene.getSimpleBodies().get(0).getNormalMap()).isNotNull();
    }

    @Test
    void ReaderMitCommands_UnknownAndMalformedCommandsAreRejected()
    {
        final String[] bodies = {"spheer 0 0 0 1\n", "cube 0 0 0 -1\n", "texture /nonexistent/file.png\n",
            "quads\nv 0 0 0\nq 0 1 2 3\nend\n", "rotation 1 2\n", "v 0 0 0\n",
            "light 1 1 1 spot 0 0 0\n", "appel 4 0 0 0\n", "quads\nv 0 0 0\n", "csg union\ntorus 0 0 0 1 0.2\nend\n"};
        final String path = tempDir() + "reader_mit_commands_malformed.ray";
        for (int i = 0; i < 10; ++i) {
            writeFile(path, "eye 0 -5 0\nup 0 0 1\nlookat 0 0 0\n" + bodies[i]);
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            expectThrow(Exception.class, () -> reader.importEnvironment(path, scene), "" + i);
        }
        removeFile(path);
    }

    @Test
    void ReaderMitCommands_PrismsAreParametricBodiesAndCsgOperands() throws Exception
    {
        SimpleScene scene = new SimpleScene();
        ReaderMitScene reader = new ReaderMitScene();
        reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/prism_csg.ray", scene);
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();
        assertThat(bodies.size()).isEqualTo(4);
        assertThat(bodies.get(1).getGeometry()).isInstanceOf(Prism.class);
        final Prism star = (Prism)bodies.get(1).getGeometry();
        expectEq(10, star.getNumberOfSides());
        expectDoubleEq(0.6, star.getHeight());
        expectDoubleEq(-2.3, bodies.get(1).getPosition().x());
        assertThat(bodies.get(2)).isInstanceOf(CsgBody.class);
        assertThat(bodies.get(3)).isInstanceOf(CsgBody.class);

        // The star prism is hit on its top cap; its notch between two points is not
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        assertThat(bodies.get(1).doIntersectionFirstHit(new Ray(new Vector3Dd(-2.3, 0, 5), new Vector3Dd(0, 0, -1)), hit)).isTrue();
        expectNear(5 - 0.6, hit.getHitDistance(), 1e-9);
        expectFalse(bodies.get(1).doIntersectionFirstHit(new Ray(new Vector3Dd(-2.3 - 0.411449, 0.566312, 5), new Vector3Dd(0, 0, -1)), hit));
        // The spherical bite of the difference: the ray down the axis reaches the
        // bottom of the bite, at z = 1.4 - 0.75
        assertThat(bodies.get(2).doIntersectionFirstHit(new Ray(new Vector3Dd(0, 0, 5), new Vector3Dd(0, 0, -1)), hit)).isTrue();
        expectNear(5 - 0.65, hit.getHitDistance(), 1e-9);
    }

    @Test
    void ReaderMitCommands_MalformedPrismsAreRejected()
    {
        final String[] lines = {"prism 0 0 0 1 2 0 0 1 0\n", "prism 0 0 0 -1 3 0 0 1 0 0 1\n",
            "prism 0 0 0 1 3 0 0 1 0\n", "prism 0 0 0 1 4 0 0 1 1 1 0 0 1\n",
            "prism 0 0 0 1 3 0 0 1 0 0 1 7\n"};
        final String path = tempDir() + "reader_mit_prism_malformed.ray";
        for (int i = 0; i < 5; ++i) {
            writeFile(path, "eye 0 -5 0\nup 0 0 1\nlookat 0 0 0\n" + lines[i]);
            SimpleScene scene = new SimpleScene();
            ReaderMitScene reader = new ReaderMitScene();
            expectThrow(Exception.class, () -> reader.importEnvironment(path, scene), "" + i);
        }
        removeFile(path);
    }
}

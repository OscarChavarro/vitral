package vsdk.toolkit.environment.geometry.volume;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector2Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.csg.CsgBody;
import vsdk.toolkit.environment.scene.csg.CsgBodyEvaluator;
import vsdk.toolkit.environment.scene.csg.CsgOperation;
import vsdk.toolkit.gui.editor.ControlSpecification;
import vsdk.toolkit.gui.editor.GenericEditor;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/prism_tests.cpp`; each test method is named
`<cpp suite>_<cpp test>`.

Exercises the parametric `Prism` (base polygon on z = 0, swept along +Z):
ray intersection and [ROTH1982] ray classification against convex and
concave bases (compared with `Box` for a rectangular base), CSG with the
Roth evaluator, point containment, the boundary representation and the
parameters exposed to generic editors.
*/
class PrismTest
{
    private static final double EPS = 1e-9;

    private static Ray ray(double ox, double oy, double oz, double dx,
        double dy, double dz)
    {
        return new Ray(new Vector3Dd(ox, oy, oz), new Vector3Dd(dx, dy, dz));
    }

    private static ArrayList<Vector2Dd> square(double half)
    {
        ArrayList<Vector2Dd> polygon = new ArrayList<Vector2Dd>();
        polygon.add(new Vector2Dd(-half, -half));
        polygon.add(new Vector2Dd(half, -half));
        polygon.add(new Vector2Dd(half, half));
        polygon.add(new Vector2Dd(-half, half));
        return polygon;
    }

    /** "U" shape: x in [0, 3], y in [0, 2], with the notch x in [1, 2], y > 1 */
    private static ArrayList<Vector2Dd> uShape()
    {
        ArrayList<Vector2Dd> polygon = new ArrayList<Vector2Dd>();
        polygon.add(new Vector2Dd(0, 0));
        polygon.add(new Vector2Dd(3, 0));
        polygon.add(new Vector2Dd(3, 2));
        polygon.add(new Vector2Dd(2, 2));
        polygon.add(new Vector2Dd(2, 1));
        polygon.add(new Vector2Dd(1, 1));
        polygon.add(new Vector2Dd(1, 2));
        polygon.add(new Vector2Dd(0, 2));
        return polygon;
    }

    private static ArrayList<Vector2Dd> star(int points, double outer,
        double inner)
    {
        ArrayList<Vector2Dd> polygon = new ArrayList<Vector2Dd>();
        for ( int i = 0; i < 2 * points; i++ ) {
            double a = Math.PI * i / points;
            double r = i % 2 == 0 ? outer : inner;
            polygon.add(new Vector2Dd(r * Math.cos(a), r * Math.sin(a)));
        }
        return polygon;
    }

    /**
    The glibc `srand` / `rand` generator (the additive feedback generator
    of `random_r`, TYPE_3), so the random rays are those of the C++ test
    built with GCC.
    */
    private static final class GlibcRand
    {
        static final int RAND_MAX = 2147483647;
        private final int[] state = new int[31];
        private int front;
        private int rear;

        GlibcRand(int seed)
        {
            if ( seed == 0 ) {
                seed = 1;
            }
            state[0] = seed;
            for ( int i = 1; i < 31; i++ ) {
                long word = (16807L * state[i - 1]) % 2147483647L;
                if ( word < 0 ) {
                    word += 2147483647L;
                }
                state[i] = (int)word;
            }
            front = 3;
            rear = 0;
            for ( int i = 0; i < 310; i++ ) {
                next();
            }
        }

        int next()
        {
            state[front] += state[rear];
            int result = state[front] >>> 1;
            front = (front + 1) % 31;
            rear = (rear + 1) % 31;
            return result;
        }
    }

    private static GlibcRand generator;

    private static double random(double low, double high)
    {
        return low + (high - low) * (generator.next() / (double)GlibcRand.RAND_MAX);
    }

    /** Minimal editor without widgets, to use the generic accessors. */
    private static final class AccessorEditor extends GenericEditor
    {
        @Override
        protected void beginBuild(String title)
        {
        }

        @Override
        protected void addControl(ControlSpecification specification,
            String value)
        {
        }

        @Override
        protected void endBuild()
        {
        }

        @Override
        protected void showValidationMessage(String message)
        {
        }

        @Override
        protected void setControlValue(ControlSpecification specification,
            String value)
        {
        }

        @Override
        protected void clearControls(String message)
        {
        }
    }

    private static ControlSpecification findSpecification(Prism prism,
        String name)
    {
        for ( String text : prism.getControlSpecifications() ) {
            ControlSpecification specification = ControlSpecification.parse(text);
            if ( specification != null && specification.getName().equals(name) ) {
                return specification;
            }
        }
        return null;
    }

    @Test
    void PrismTest_given_squarePrism_when_randomRaysClassified_then_matchesBox()
    {
        Prism prism = new Prism(square(1.0), 2.0);
        Box box = new Box(new Vector3Dd(-1, -1, 0), new Vector3Dd(1, 1, 2));
        // Java-only guard: the generator reproduces glibc `rand` after `srand(12345)`
        expectEq(new GlibcRand(12345).next(), 383100999);
        generator = new GlibcRand(12345);
        int compared = 0;

        for ( int i = 0; i < 2000; i++ ) {
            // The C++ test draws the components as constructor arguments,
            // whose evaluation order is unspecified; GCC evaluates them from
            // the last to the first, and so does this port to get its rays
            double oz = random(-3, 5);
            double oy = random(-4, 4);
            double ox = random(-4, 4);
            Vector3Dd origin = new Vector3Dd(ox, oy, oz);
            double dz = random(-1, 1);
            double dy = random(-1, 1);
            double dx = random(-1, 1);
            Vector3Dd direction = new Vector3Dd(dx, dy, dz);
            if ( direction.length() < 0.1 ) {
                continue;
            }
            Ray r = new Ray(origin, direction);
            RayCrossingQuery query = new RayCrossingQuery(0.0, 100.0);
            RaySolidClassification fromPrism = new RaySolidClassification();
            RaySolidClassification fromBox = new RaySolidClassification();
            assertThat(prism.classifyRay(r, query, fromPrism))
                .isEqualTo(RaySolidQueryStatus.Supported);
            assertThat(box.classifyRay(r, query, fromBox))
                .isEqualTo(RaySolidQueryStatus.Supported);
            assertThat(fromPrism.boundaries.size()).isEqualTo(fromBox.boundaries.size());
            assertThat(fromPrism.insideSegments.size()).isEqualTo(fromBox.insideSegments.size());
            for ( int j = 0; j < fromBox.boundaries.size(); j++ ) {
                expectNear(fromPrism.boundaries.get(j).t, fromBox.boundaries.get(j).t, 1e-9);
            }
            for ( int j = 0; j < fromBox.insideSegments.size(); j++ ) {
                expectEq(fromPrism.insideSegments.get(j), fromBox.insideSegments.get(j));
            }

            RayHit prismHit = new RayHit(RayHit.DETAIL_ALL, true);
            RayHit boxHit = new RayHit(RayHit.DETAIL_ALL, true);
            RayFirstHitStatus prismStatus = prism.queryFirstHit(r, query, prismHit);
            RayFirstHitStatus boxStatus = box.queryFirstHit(r, query, boxHit);
            assertThat(prismStatus).isEqualTo(boxStatus);
            if ( prismStatus == RayFirstHitStatus.Hit ) {
                expectNear(prismHit.getHitDistance(), boxHit.getHitDistance(), 1e-9);
                expectNear(prismHit.normal.dotProduct(boxHit.normal), 1.0, 1e-9);
                compared++;
            }
        }
        expectGt(compared, 100);
    }

    @Test
    void PrismTest_given_concavePrism_when_rayCrossesNotch_then_twoInteriorIntervals()
    {
        Prism prism = new Prism(uShape(), 1.0);
        // Along x at y = 1.5, z = 0.5: inside [0,1] and [2,3]
        RaySolidClassification result = new RaySolidClassification();
        assertThat(prism.classifyRay(ray(-1, 1.5, 0.5, 1, 0, 0),
            new RayCrossingQuery(-10, 10), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries.size()).isEqualTo(4);
        expectNear(result.boundaries.get(0).t, 1.0, EPS);
        expectNear(result.boundaries.get(1).t, 2.0, EPS);
        expectNear(result.boundaries.get(2).t, 3.0, EPS);
        expectNear(result.boundaries.get(3).t, 4.0, EPS);
        expectFalse(result.insideSegments.get(0));
        expectTrue(result.insideSegments.get(1));
        expectFalse(result.insideSegments.get(2));
        expectTrue(result.insideSegments.get(3));
        expectFalse(result.insideSegments.get(4));

        // The first hit from inside the notch is the inner wall of the right arm
        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);
        assertThat(prism.queryFirstHit(ray(1.5, 1.5, 0.5, 1, 0, 0),
            new RayCrossingQuery(0, 10), hit)).isEqualTo(RayFirstHitStatus.Hit);
        expectNear(hit.getHitDistance(), 0.5, EPS);
        expectNear(hit.normal.x(), -1.0, EPS);
    }

    @Test
    void PrismTest_given_concavePrism_when_rayPassesThroughVertices_then_intervalsAreConsistent()
    {
        Prism prism = new Prism(uShape(), 1.0);
        RaySolidClassification result = new RaySolidClassification();

        // Line x + y = 2: enters at the convex corner (0,2), goes through the
        // reflex corner (1,1) without leaving the base, and leaves at (2,0)
        assertThat(prism.classifyRay(ray(-1, 3, 0.5, 1, -1, 0),
            new RayCrossingQuery(-10, 10), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries.size()).isEqualTo(2);
        // (Ray normalizes its direction: t is a distance)
        expectNear(result.boundaries.get(0).t, 1.0 * Math.sqrt(2.0), EPS);
        expectNear(result.boundaries.get(1).t, 3.0 * Math.sqrt(2.0), EPS);

        // Line y = x: inside from the corner (0,0) to the reflex corner (1,1),
        // where it enters the notch; it touches the corner (2,2) of the right arm
        assertThat(prism.classifyRay(ray(-1, -1, 0.5, 1, 1, 0),
            new RayCrossingQuery(-10, 10), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries.size()).isEqualTo(2);
        expectNear(result.boundaries.get(0).t, 1.0 * Math.sqrt(2.0), EPS);
        expectNear(result.boundaries.get(1).t, 2.0 * Math.sqrt(2.0), EPS);

        // Line touching the convex corner (3,0) only: no interior
        assertThat(prism.classifyRay(ray(2, -1, 0.5, 1, 1, 0),
            new RayCrossingQuery(-10, 10), result)).isEqualTo(RaySolidQueryStatus.Supported);
        expectEq(result.boundaries.size(), 0);
        assertThat(result.insideSegments.size()).isEqualTo(1);
        expectFalse(result.insideSegments.get(0));
    }

    @Test
    void PrismTest_given_prism_when_rayRunsAlongAFace_then_isIndeterminate()
    {
        Prism prism = new Prism(square(1.0), 2.0);
        RaySolidClassification result = new RaySolidClassification();
        // On the wall x = 1
        expectEq(prism.classifyRay(ray(1, -5, 1, 0, 1, 0),
            new RayCrossingQuery(0, 10), result), RaySolidQueryStatus.Indeterminate);
        // On the top cap plane, over the base
        expectEq(prism.classifyRay(ray(-5, 0, 2, 1, 0, 0),
            new RayCrossingQuery(0, 10), result), RaySolidQueryStatus.Indeterminate);
        // On the plane of the top cap, but away from the base: a miss
        expectEq(prism.classifyRay(ray(-5, 3, 2, 1, 0, 0),
            new RayCrossingQuery(0, 10), result), RaySolidQueryStatus.Supported);
        expectEq(result.boundaries.size(), 0);
    }

    @Test
    void PrismTest_given_prism_when_verticalRays_then_capsOrNothingAreHit()
    {
        Prism prism = new Prism(uShape(), 1.0);
        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);

        assertThat(prism.queryFirstHit(ray(0.5, 1.5, 5, 0, 0, -1),
            new RayCrossingQuery(0, 10), hit)).isEqualTo(RayFirstHitStatus.Hit);
        expectNear(hit.getHitDistance(), 4.0, EPS);
        expectNear(hit.normal.z(), 1.0, EPS);
        // Over the notch
        expectEq(prism.queryFirstHit(ray(1.5, 1.5, 5, 0, 0, -1),
            new RayCrossingQuery(0, 10), hit), RayFirstHitStatus.Miss);
        // From below, the bottom cap
        assertThat(prism.queryFirstHit(ray(2.5, 0.5, -1, 0, 0, 1),
            new RayCrossingQuery(0, 10), hit)).isEqualTo(RayFirstHitStatus.Hit);
        expectNear(hit.getHitDistance(), 1.0, EPS);
        expectNear(hit.normal.z(), -1.0, EPS);
    }

    @Test
    void PrismTest_given_clockwiseBase_when_constructed_then_isStoredCounterclockwiseWithOutwardWalls()
    {
        ArrayList<Vector2Dd> clockwise = new ArrayList<Vector2Dd>();
        ArrayList<Vector2Dd> counterclockwise = square(1.0);
        for ( int i = counterclockwise.size() - 1; i >= 0; i-- ) {
            clockwise.add(counterclockwise.get(i));
        }
        Prism prism = new Prism(clockwise, 1.0);
        assertThat(prism.hasValidParameters()).isTrue();

        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);
        assertThat(prism.doIntersectionFirstHit(ray(5, 0, 0.5, -1, 0, 0), hit)).isTrue();
        expectNear(hit.getHitDistance(), 4.0, EPS);
        expectNear(hit.normal.x(), 1.0, EPS);
    }

    @Test
    void PrismTest_given_invalidBases_when_constructed_then_haveNoValidParameters()
    {
        ArrayList<Vector2Dd> bowtie = new ArrayList<Vector2Dd>();
        bowtie.add(new Vector2Dd(0, 0));
        bowtie.add(new Vector2Dd(1, 1));
        bowtie.add(new Vector2Dd(1, 0));
        bowtie.add(new Vector2Dd(0, 1));
        expectFalse(new Prism(bowtie, 1.0).hasValidParameters());

        ArrayList<Vector2Dd> collinear = new ArrayList<Vector2Dd>();
        collinear.add(new Vector2Dd(0, 0));
        collinear.add(new Vector2Dd(1, 0));
        collinear.add(new Vector2Dd(2, 0));
        expectFalse(new Prism(collinear, 1.0).hasValidParameters());

        expectFalse(new Prism(square(1.0), 0.0).hasValidParameters());
        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);
        expectEq(new Prism(bowtie, 1.0).queryFirstHit(ray(-5, 0.5, 0.5, 1, 0, 0),
            new RayCrossingQuery(0, 10), hit), RayFirstHitStatus.InvalidInput);
    }

    @Test
    void PrismTest_given_concavePrism_when_pointsClassified_then_notchAndCapsAreRespected()
    {
        Prism prism = new Prism(uShape(), 1.0);
        final double tol = 0.01;
        expectEq(prism.doContainmentTest(new Vector3Dd(0.5, 1.5, 0.5), tol), Geometry.INSIDE);
        expectEq(prism.doContainmentTest(new Vector3Dd(1.5, 1.5, 0.5), tol), Geometry.OUTSIDE);
        expectEq(prism.doContainmentTest(new Vector3Dd(1.0, 1.5, 0.5), tol), Geometry.LIMIT);
        expectEq(prism.doContainmentTest(new Vector3Dd(2.5, 0.5, 1.0), tol), Geometry.LIMIT);
        expectEq(prism.doContainmentTest(new Vector3Dd(2.5, 0.5, 1.5), tol), Geometry.OUTSIDE);
        expectEq(prism.classifyPoint(new Vector3Dd(2.5, 0.5, 0.5), tol), GeometryContainment.Inside);

        GeometryBounds bounds = new GeometryBounds();
        prism.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        expectEq(bounds.getMinimum().x(), 0.0);
        expectEq(bounds.getMaximum().x(), 3.0);
        expectEq(bounds.getMaximum().y(), 2.0);
        expectEq(bounds.getMaximum().z(), 1.0);
    }

    @Test
    void PrismTest_given_prismsAsCsgOperands_when_rothEvaluated_then_differenceHasTheNotch()
    {
        // A square slab minus a smaller square prism through it
        CsgBody body = new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.ROTH);
        SimpleBody outer = new SimpleBody();
        outer.setGeometry(new Prism(square(2.0), 1.0));
        SimpleBody inner = new SimpleBody();
        inner.setGeometry(new Prism(4, 1.0, 3.0));
        inner.setPosition(new Vector3Dd(0, 0, -1));
        assertThat(body.addLeaf(0, outer)).isTrue();
        assertThat(body.addLeaf(0, inner)).isTrue();

        // Along x at z = 0.5, y = 0: the inner prism is a diamond of
        // circumradius 1, so the solid is [-2, -1] U [1, 2]
        RaySolidClassification result = new RaySolidClassification();
        assertThat(body.classifyRay(ray(-5, 0, 0.5, 1, 0, 0),
            new RayCrossingQuery(0, 20), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries.size()).isEqualTo(4);
        expectNear(result.boundaries.get(0).t, 3.0, EPS);
        expectNear(result.boundaries.get(1).t, 4.0, EPS);
        expectNear(result.boundaries.get(2).t, 6.0, EPS);
        expectNear(result.boundaries.get(3).t, 7.0, EPS);

        // From the hole, the first hit is the diamond wall, facing the hole
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        assertThat(body.doIntersectionFirstHit(ray(0, 0, 0.5, 1, 0, 0), hit)).isTrue();
        expectNear(hit.getHitDistance(), 1.0, EPS);
        expectLt(hit.normal.x(), 0.0);
    }

    @Test
    void PrismTest_given_starPrism_when_exportedToBrep_then_isValidSolid()
    {
        int points = 5;
        Prism prism = new Prism(star(points, 2.0, 0.8), 1.5);
        PolyhedralBoundedSolid solid = prism.exportToPolyhedralBoundedSolid();
        int n = 2 * points;

        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary.from(solid);
        expectEq(topology.getVertexCount(), 2 * n);
        expectEq(topology.getEdgeCount(), 3 * n);
        expectEq(topology.getFaceCount(), n + 2);
        expectEq(topology.getShellCount(), 1);
        StringBuilder msg = new StringBuilder();
        expectTrue(PolyhedralBoundedSolidValidationEngine.validateStrict(solid,
            msg), msg.toString());
    }

    @Test
    void PrismTest_given_concaveBase_when_triangulated_then_trianglesCoverTheBase()
    {
        Prism prism = new Prism(star(6, 2.0, 0.7), 1.0);
        ArrayList<Integer> triangles = new ArrayList<Integer>();
        assertThat(prism.triangulateBase(triangles)).isEqualTo(10);
        assertThat(triangles.size()).isEqualTo(30);

        double polygonArea = 0;
        ArrayList<Vector2Dd> base = prism.getBase();
        for ( int i = 0; i < base.size(); i++ ) {
            Vector2Dd a = base.get(i);
            Vector2Dd b = base.get((i + 1) % base.size());
            polygonArea += 0.5 * (a.x * b.y - b.x * a.y);
        }
        double trianglesArea = 0;
        for ( int i = 0; i < triangles.size(); i += 3 ) {
            Vector2Dd a = base.get(triangles.get(i));
            Vector2Dd b = base.get(triangles.get(i + 1));
            Vector2Dd c = base.get(triangles.get(i + 2));
            double area = 0.5 * ((b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y));
            expectGt(area, 0.0);
            trianglesArea += area;
        }
        expectNear(trianglesArea, polygonArea, 1e-12);
    }

    @Test
    void PrismTest_given_prism_when_editedThroughControlAccessors_then_parametersChange()
    {
        Prism prism = new Prism(6, 1.0, 2.0);
        List<String> specifications = prism.getControlSpecifications();
        assertThat(specifications.size()).isEqualTo(3);

        ControlSpecification sides = findSpecification(prism, "numberOfSides");
        ControlSpecification radius = findSpecification(prism, "circumradius");
        ControlSpecification height = findSpecification(prism, "height");
        assertThat(sides).isNotNull();
        assertThat(radius).isNotNull();
        assertThat(height).isNotNull();
        expectEq(sides.getType(), "int");

        AccessorEditor editor = new AccessorEditor();
        editor.build(prism);
        String text = editor.readValue(sides);
        assertThat(text).isNotNull();
        expectEq(text, "6");
        assertThat(editor.updateValue(sides, "8")).isTrue();
        expectEq(prism.getNumberOfSides(), 8);
        expectNear(prism.getCircumradius(), 1.0, EPS);
        assertThat(editor.updateValue(radius, "2.5")).isTrue();
        expectNear(prism.getCircumradius(), 2.5, EPS);
        expectEq(prism.getNumberOfSides(), 8);
        assertThat(editor.updateValue(height, "4")).isTrue();
        expectEq(prism.getHeight(), 4.0);
        editor.detach();

        Geometry copy = prism.copyGeometry();
        assertThat(copy).isNotNull();
        expectEq(((Prism)copy).getNumberOfSides(), 8);
    }
}

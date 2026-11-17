package application.commands;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.geometricProcessing.Voxelization;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.geometry.volume.BlobComponent;
import vsdk.toolkit.environment.geometry.volume.BlobSolid;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.PolynomialSurface;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.gui.feedback.ProgressMonitorConsole;
import vsdk.toolkit.media.RGBAImageUncompressed;

// Application classes
import model.ApplicationModel;
import model.Scene;

class GeometryCreationCommandsExecutor
{
    private final ApplicationModel model;
    private final Presenter presenter;

    GeometryCreationCommandsExecutor(ApplicationModel model,
                                     Presenter presenter)
    {
        this.model = model;
        this.presenter = presenter;
    }

    private Scene scene() {
        return model.getScene();
    }

    CommandResult execute(String label)
    {
        if ( label.equals("IDC_CREATE_SPHERE") ) {
            scene().addThing(new Sphere(1.0));
        }
        else if ( label.equals("IDC_CREATE_CONE") ) {
            scene().addThing(new Cone(1, 0, 2));
        }
        else if ( label.equals("IDC_CREATE_CYLINDER") ) {
            scene().addThing(new Cone(1, 1, 2));
        }
        else if ( label.equals("IDC_CREATE_TUBE") ) {
            scene().addThing(new Tube(0.6, 1, 2));
        }
        else if ( label.equals("IDC_CREATE_WASHER") ) {
            // A tube of height 0: a circular lamina with a hole
            SimpleBody body = scene().addThing(new Tube(0.6, 1, 0));
            body.setMaterial(body.getMaterial().withDoubleSided(true));
        }
        else if ( label.equals("IDC_CREATE_CUBE") ) {
            scene().addThing(new Box(1, 1, 1));
        }
        else if ( label.equals("IDC_CREATE_BOX") ) {
            scene().addThing(new Box(1, 3, 2));
        }
        else if ( label.equals("IDC_CREATE_ARROW") ) {
            scene().addThing(new Arrow(0.7, 0.3, 0.05, 0.1));
        }
        else if ( label.equals("IDC_CREATE_TORUS") ) {
            scene().addThing(new Torus(2, 1));
        }
        else if ( label.equals("IDC_CREATE_BLOB") ) {
            createBlob();
        }
        else if ( label.equals("IDC_CREATE_QUADRIC") ) {
            createQuadric();
        }
        else if ( label.equals("IDC_CREATE_POLYNOMIAL") ) {
            createPolynomial();
        }
        else if ( label.equals("IDC_CREATE_HEIGHTFIELD") ) {
            createHeightField();
        }
        else if ( label.equals("IDC_CREATE_HALFSPACE") ) {
            scene().addThing(new PlanarHalfSpace(0, 0, 1, 0));
        }
        else if ( label.equals("IDC_CREATE_PLANE") ) {
            createPlane();
        }
        else if ( label.equals("IDC_CREATE_SPHERE_HARMONIC") ) {
            createSphericalHarmonicDebugSpheres();
        }
        else if ( label.equals("IDC_CREATE_VOLUME") ) {
            if ( !createVolume() ) {
                return CommandResult.FAILED;
            }
        }
        else if ( label.equals("IDC_CREATE_BREP") ) {
            createBrep();
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICCUBICCURVE") ) {
            createParametricCubicCurve();
        }
        else if ( label.equals("IDC_CREATE_FUNCTIONALEXPLICITSURFACE") ) {
            createFunctionalExplicitSurface();
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICBICUBICPATCH") ) {
            createParametricBiCubicPatch();
        }
        else {
            return CommandResult.NOT_HANDLED;
        }
        return CommandResult.DONE;
    }

    /**
    A blob shaped as a triangular molecule with a hole: three components at
    the vertices of an equilateral triangle (at distance 1 from its center,
    on the plane z = 0). Each component reaches 1.25 units, so neighbors
    overlap; the threshold 0.46 is above the density at the center of the
    triangle (0.39) and below the one half way between two components
    (0.54), so the three of them merge in a ring around an empty center
    (of radius 0.25, wide enough to be kept by the default polygonization).
    */
    private void createBlob()
    {
        BlobSolid blob = new BlobSolid(0.46, false);
        int i;

        for ( i = 0; i < 3; i++ ) {
            double angle = Math.PI / 2 + 2 * Math.PI * i / 3;
            blob.addComponent(new BlobComponent(
                new Vector3Dd(Math.cos(angle), Math.sin(angle), 0), 1.25, 1.0));
        }
        scene().addThing(blob);
    }

    /**
    An ellipsoid of semiaxes 1.5, 1 and 0.7: x^2/1.5^2 + y^2 + z^2/0.7^2 - 1
    <= 0.
    */
    private void createQuadric()
    {
        scene().addThing(new QuadricSurface(
            new Vector3Dd(1.0 / (1.5 * 1.5), 1.0, 1.0 / (0.7 * 0.7)),
            new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 0), -1.0));
    }

    /**
    The quartic "tangle cube" x^4 - 5x^2 + y^4 - 5y^2 + z^4 - 5z^2 + 11.8 < 0,
    a solid of genus 5 that fits in a cube of side 4.6, scaled to 0.4.
    */
    private void createPolynomial()
    {
        int order = 4;
        PolynomialSurface polynomial = new PolynomialSurface(order, false);
        double[] c = polynomial.getCoefficients();

        c[PolynomialSurface.getTermIndex(order, 4, 0, 0)] = 1;
        c[PolynomialSurface.getTermIndex(order, 0, 4, 0)] = 1;
        c[PolynomialSurface.getTermIndex(order, 0, 0, 4)] = 1;
        c[PolynomialSurface.getTermIndex(order, 2, 0, 0)] = -5;
        c[PolynomialSurface.getTermIndex(order, 0, 2, 0)] = -5;
        c[PolynomialSurface.getTermIndex(order, 0, 0, 2)] = -5;
        c[PolynomialSurface.getTermIndex(order, 0, 0, 0)] = 11.8;
        SimpleBody body = scene().addThing(polynomial);
        body.setScale(new Vector3Dd(0.4, 0.4, 0.4));
    }

    /**
    A terrain of 17 x 17 heights (waves between 0.7 and 2.3) over a box of
    height 3. Height fields grow along their Y axis: the body is rotated so
    they grow along Z, scaled to 0.25 and centered at the origin.
    */
    private void createHeightField()
    {
        int n = 17;
        HeightFieldSolid field = new HeightFieldSolid(n, n,
            new Vector3Dd(0, 0, 0), new Vector3Dd(n - 1, 3, n - 1));
        int x;
        int z;

        for ( z = 0; z < n; z++ ) {
            for ( x = 0; x < n; x++ ) {
                field.setHeight(x, z,
                    1.5 + 0.8 * Math.sin(x / 2.5) * Math.cos(z / 3.0));
            }
        }
        SimpleBody body = scene().addThing(field);
        Matrix4x4d rotation = new Matrix4x4d();
        rotation = rotation.axisRotation(Math.PI / 2, 1, 0, 0);
        double scale = 0.25;
        body.setRotation(rotation);
        body.setScale(new Vector3Dd(scale, scale, scale));
        // Rotated, the center of the grid (8, y, 8) goes to (8, -8, y)
        body.setPosition(new Vector3Dd(-scale * (n - 1) / 2.0,
            scale * (n - 1) / 2.0, 0));
    }

    private void createPlane()
    {
        InfinitePlane plane;
        plane = new InfinitePlane(new Vector3Dd(-0.2, 0, 1), new Vector3Dd(0, 0, -1));
        System.out.println(plane);
        scene().addThing(plane);
    }

    private void createSphericalHarmonicDebugSpheres()
    {
        SimpleBody voxelBody = null;
        int selectedThing = scene().selectedThings.firstSelected();

        Geometry referenceGeometry = null;

        if ( selectedThing >= 0 ) {
            voxelBody = scene().scene.getSimpleBodies().get(selectedThing);
            referenceGeometry = voxelBody.getGeometry();
        }

        if ( referenceGeometry == null ||
             !(referenceGeometry instanceof VoxelVolume) ) {
            presenter.showStatusMessage("ERROR: A VoxelVolume must be selected for spherical harmonic debugging sphere to be created");
        }
        else {
            //- Calculate the VoxelVolume's center of mass ---------------
            VoxelVolume vv = (VoxelVolume)referenceGeometry;
            Vector3Dd cm = vv.doCenterOfMass();

            //- Calculate average distance from nonzero voxels to cm -----
            // This accounts for scale normalization as in [FUNK2003].4.1.
            int numberOfNonZeroVoxels = 0;
            double d; // Distance between a given voxel and center of mass
            Vector3Dd p; // Position of voxel
            double averageDistance = 0;
            int x, y, z;

            for ( x = 0; x < vv.getXSize(); x++ ) {
                for ( y = 0; y < vv.getYSize(); y++ ) {
                    for ( z = 0; z < vv.getZSize(); z++ ) {
                        if ( vv.getVoxel(x, y, z) != 0 ) {
                            p = vv.getVoxelPosition(x, y, z);
                            averageDistance += Vector3Dd.distance(cm, p);
                            numberOfNonZeroVoxels++;
                        }
                    }
                }
            }
            averageDistance /= (double)numberOfNonZeroVoxels;

            //- Create spheres -------------------------------------------
            SimpleBody body;
            SimpleBodyGroup group = new SimpleBodyGroup();
            int i;
            for ( i = 0; i < 32; i++ ) {
                body = addDebugSphere(voxelBody, i, cm, averageDistance);
                group.getBodies().add(body);
            }
            scene().debugThingGroups.add(group);
            // Subspheres account for translation & scale
            group.setRotation(voxelBody.getRotation());
        }
    }

    private boolean createVolume()
    {
        //- The volume is built from the selected object -------------------
        int selectedThing = scene().selectedThings.firstSelected();

        if ( selectedThing < 0 ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage(
                "IDM_CREATE_VOLUME_NO_SELECTION"));
            return false;
        }
        SimpleBody thing = scene().scene.getSimpleBodies().get(selectedThing);
        Geometry referenceGeometry = thing.getGeometry();

        //- Calculate transform matrix ------------------------------------
        double minmax[] = referenceGeometry.getMinMax();
        Matrix4x4d M; // Transform from voxelspace to geometry minmax space

        M = VoxelVolume.getTransformFromVoxelFrameToMinMax(minmax);

        //- Auxiliary variables -------------------------------------------
        int nx = 64, ny = 64, nz = 64;

        //- Primitive rasterization ---------------------------------------
        VoxelVolume vv = new VoxelVolume();
        vv.init(nx, ny, nz);

        ProgressMonitorConsole reporter = new ProgressMonitorConsole();
        Voxelization.doVoxelization(referenceGeometry, vv, M, reporter);

        if ( !hasFilledVoxels(vv) ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage(
                "IDM_CREATE_VOLUME_EMPTY"));
            return false;
        }

        //- Append newly created volume to scene, matching reference form -
        // The body of the volume composes the transformation of the
        // selected body with M (a translation to the center of the minmax
        // box and a uniform scale): its rotation is the one of the selected
        // body, its scale the product of both scales, and the center is
        // moved by the rotation and scale of the selected body
        Vector3Dd center = M.extractTranslation();
        Vector3Dd thingScale = thing.getScale();
        double voxelSpaceScale = M.get(0, 0);
        Vector3Dd scaledCenter = new Vector3Dd(center.x() * thingScale.x(),
            center.y() * thingScale.y(), center.z() * thingScale.z());

        SimpleBody newThing = scene().addThing(vv);
        newThing.setRotation(thing.getRotation());
        newThing.setScale(thingScale.multiply(voxelSpaceScale));
        newThing.setPosition(thing.getPosition().add(
            thing.getRotation().multiply(scaledCenter)));
        return true;
    }

    private static boolean hasFilledVoxels(VoxelVolume vv)
    {
        int x, y, z;

        for ( z = 0; z < vv.getZSize(); z++ ) {
            for ( y = 0; y < vv.getYSize(); y++ ) {
                for ( x = 0; x < vv.getXSize(); x++ ) {
                    if ( vv.isFilled(x, y, z) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void createBrep()
    {
        scene().addThing(
            SimpleTestGeometryLibrary.createTestObjectMANT1986_1());
    }

    private void createParametricCubicCurve()
    {
        ParametricCurve curve;

        // Case 1: curve hard-coded in source
        Vector3Dd pointParameters[];

        curve = new ParametricCurve();
        // Note that an HERMITE curve uses tangent vectors, BEZIER curves
        // uses control points (tangent vectors are control point minus
        // knot position)
        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(0, 0, 0); // Position 0
        pointParameters[1] = new Vector3Dd(0, 0, 0); // Not used
        pointParameters[2] = new Vector3Dd(0, 1, 0); // Salient tangent end
        curve.addPoint(pointParameters, ParametricCurve.BEZIER);

        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(1, 1, 0); // Position 1
        pointParameters[1] = new Vector3Dd(0, 1, 0); // Entry tangent end
        pointParameters[2] = new Vector3Dd(2, 1, 0); // Salient tangent end
        curve.addPoint(pointParameters, ParametricCurve.BEZIER);

        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(2, 0, 1); // Position 2
        pointParameters[1] = new Vector3Dd(2, 0, 0); // Entry tangent end
        pointParameters[2] = new Vector3Dd(0, 0, 0); // Not used
        curve.addPoint(pointParameters, ParametricCurve.BEZIER);

        scene().addThing(curve);
    }

    private void createFunctionalExplicitSurface()
    {
        SimpleBody newThing;
        FunctionalExplicitSurface functionalSurface;
        functionalSurface = new FunctionalExplicitSurface("cos((PI*x)/2)");
        functionalSurface.setBounds(-10, -10, -10, 10, 10, 10);
        functionalSurface.setTesselationHint(100, 100);
        newThing = scene().addThing(functionalSurface);
        newThing.setMaterial(newThing.getMaterial().withDoubleSided(true));
    }

    private void createParametricBiCubicPatch()
    {
        //- Create a Ferguson patch ---------------------------------------
        ParametricCurve contourHermiteLine;
        Vector3Dd pointParameters[];

        contourHermiteLine = new ParametricCurve();
        // Note that an HERMITE curve uses tangent vectors, BEZIER curves
        // uses control points (tangent vectors are control point minus
        // knot position)
        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(0, 0, 0);  // Position 0
        pointParameters[1] = new Vector3Dd(0, -1, 0); // Entry tangent
        pointParameters[2] = new Vector3Dd(1, 0, 0);  // Salient tangent
        contourHermiteLine.addPoint(pointParameters, ParametricCurve.HERMITE);

        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(1, 0, 0);  // Position 1
        pointParameters[1] = new Vector3Dd(1, 0, 0);  // Entry tangent
        pointParameters[2] = new Vector3Dd(0, 1, 0);  // Salient tangent
        contourHermiteLine.addPoint(pointParameters, ParametricCurve.HERMITE);

        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(1, 1, 0.4);  // Position 2
        pointParameters[1] = new Vector3Dd(0, 1, 0);  // Entry tangent
        pointParameters[2] = new Vector3Dd(-1, 0, 0);  // Salient tangent
        contourHermiteLine.addPoint(pointParameters, ParametricCurve.HERMITE);

        pointParameters = new Vector3Dd[3];
        pointParameters[0] = new Vector3Dd(0, 1, 0);  // Position 3
        pointParameters[1] = new Vector3Dd(-1, 0, 0);  // Entry tangent
        pointParameters[2] = new Vector3Dd(0, -1, 0);  // Salient tangent
        contourHermiteLine.addPoint(pointParameters, ParametricCurve.HERMITE);

        contourHermiteLine.addPoint(contourHermiteLine.getPoint(0), ParametricCurve.HERMITE);

        ParametricBiCubicPatch patch;
        patch = new ParametricBiCubicPatch();
        patch.buildFergusonPatch(contourHermiteLine);
        patch.setApproximationSteps(20);

        SimpleBody newThing;
        newThing = scene().addThing(patch);
        newThing.setMaterial(newThing.getMaterial().withDoubleSided(true));
    }

    private SimpleBody addDebugSphere(SimpleBody voxelBody, int groupIndex,
                                      Vector3Dd cm, double averageDistance)
    {
        double r = (((double)groupIndex) / 31.0) * (2 * averageDistance);
        VoxelVolume vv = (VoxelVolume)voxelBody.getGeometry();
        Sphere sphere;
        RGBAImageUncompressed texture;
        SimpleBody body;
        double tetha, phi;
        int s, t;
        int voxelValue;
        Vector3Dd p = new Vector3Dd();
        Vector3Dd pos;
        Vector3Dd scale, cm2;
        Matrix4x4d S = new Matrix4x4d();

        sphere = new Sphere(r);
        body = new SimpleBody();
        body.setGeometry(sphere);
        body.setMaterial(scene().defaultMaterial());
        body.setMaterial(body.getMaterial().withDoubleSided(true));
        scale = voxelBody.getScale();
        S = S.scale(scale.x(), scale.y(), scale.z());
        scale = scale.multiply(r);
        body.setScale(scale);
        cm2 = S.multiply(cm);
        pos = voxelBody.getPosition().add(cm2);
        body.setPosition(pos);
        body.setRotation(new Matrix4x4d());
        body.setRotationInverse(new Matrix4x4d());
        body.setName("Debug sphere for harmonics " + groupIndex);

        texture = new RGBAImageUncompressed();
        texture.init(64, 64);

        //- Build sphere's texture map from voxel grid --------------------
        for ( s = 0; s < texture.getXSize(); s++ ) {
            for ( t = 0; t < texture.getYSize(); t++ ) {
                tetha =
             ((double)s) / ((double)texture.getXSize()) * Math.PI * 2;
                phi =
             ((double)t) / ((double)texture.getYSize()) * Math.PI;
                p = Vector3Dd.fromSpherical(r, tetha, phi);
                p = cm.add(p);
                voxelValue = vv.getVoxelAtPosition(p.x(), p.y(), p.z());
                if ( voxelValue < 128 ) {
                    texture.putPixel(s, t, (byte)0, (byte)0, (byte)0, (byte)0);
                }
                else {
                    texture.putPixel(s, t, (byte)0, (byte)0, (byte)0, (byte)255);
                }
            }
        }

        //-----------------------------------------------------------------
        body.setTexture(texture);
        return body;
    }
}

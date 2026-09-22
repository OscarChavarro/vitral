package application;

// Java basic classes
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.geometricProcessing.Voxelization;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.gui.CommandListener;
import vsdk.toolkit.gui.feedback.ProgressMonitorConsole;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.media.RGBAImageUncompressed;

// Application classes
import model.ApplicationModel;
import model.InteractionMode;
import model.Scene;

/**
Executes the commands of the GUI of the editor (identified by the `IDC_*`
names of the I18N GUI definition) that only work over the application model:
creation of objects and lights, capture requests, interaction modes, viewport
and debugging toggles. It also offers the persistence operations whose files
are chosen by the GUI. It does not depend on any GUI or rendering technology:
each GUI technology executes here what it does not present itself (file
dialogs, windows, look and feel...), and presents the status messages
requested through its `Presenter`.
*/
public class GuiEventExecutor extends CommandListener
{
    /**
    Result of executing a command.
    */
    public enum CommandResult
    {
        /** The command was executed */
        DONE,
        /** The command was recognized, but it could not be executed */
        FAILED,
        /** The command is not executed by this class */
        NOT_HANDLED
    }

    /**
    Formats to export the objects of the scene.
    */
    public enum ExportFormat
    {
        OBJ,
        GTS,
        VTK
    }

    /**
    What the GUI technology presents for this executor.
    */
    public interface Presenter
    {
        /**
        @param message text to show in the status bar
        */
        void showStatusMessage(String message);
    }

    private final ApplicationModel model;
    private final Presenter presenter;

    /**
    @param model application model the commands work over
    @param presenter presents the status messages of the commands
    */
    public GuiEventExecutor(ApplicationModel model, Presenter presenter)
    {
        this.model = model;
        this.presenter = presenter;
    }

    private Scene scene() {
        return model.getScene();
    }

    /**
    @param label identifier of the command (`IDC_*`)
    @return true if the command was executed
    */
    @Override
    public boolean executeCommand(String label)
    {
        return execute(label) == CommandResult.DONE;
    }

    /**
    @param label identifier of the command (`IDC_*`)
    @return whether the command was executed, failed, or is not one of the
    commands of this class
    */
    public CommandResult execute(String label)
    {
        Light light;

        //- CREATE --------------------------------------------------------
        if ( label.equals("IDC_CREATE_SPHERE") ) {
            scene().addThing(new Sphere(1.0));
        }
        else if ( label.equals("IDC_CREATE_CONE") ) {
            scene().addThing(new Cone(1, 0, 2));
        }
        else if ( label.equals("IDC_CREATE_CYLINDER") ) {
            scene().addThing(new Cone(1, 1, 2));
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
        else if ( label.equals("IDC_CREATE_PLANE") ) {
            InfinitePlane plane;
            plane = new InfinitePlane(new Vector3Dd(-0.2, 0, 1), new Vector3Dd(0, 0, -1));
            System.out.println(plane);
            scene().addThing(plane);

/*
            scene().activeCamera.updateVectors();
            InfinitePlane planes[];
            planes = scene().activeCamera.getBoundingPlanes();
            for ( int i = 0; i < 6; i++ ) {
                scene().addThing(planes[i]);
            }
*/
        }
        else if ( label.equals("IDC_CREATE_SPHERE_HARMONIC") ) {
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
        else if ( label.equals("IDC_CREATE_PROJECTED_VIEWS") ) {
            model.getDrawingArea().setProjectedViewsDebugRequested(true);
        }
        else if ( label.equals("IDC_CREATE_VOLUME") ) {
            //- Select current object, if empty selection take a temp. sphere -
            int selectedThing = scene().selectedThings.firstSelected();
            Geometry referenceGeometry;
            SimpleBody thing = null;

            if ( selectedThing < 0 ) {
                referenceGeometry = new Sphere(0.5);
            }
            else {
                thing = scene().scene.getSimpleBodies().get(selectedThing);
                referenceGeometry = thing.getGeometry();
            }

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

            //- Append newly created volume to scene, matching reference form -
            SimpleBody newThing = scene().addThing(vv);
            Vector3Dd pos = M.extractTranslation();
            if ( thing != null ) {
                pos = pos.add(thing.getPosition());
                newThing.setRotation(thing.getRotation());
                newThing.setScale(thing.getScale());
            }
            newThing.setPosition(pos);
            Vector3Dd size = new Vector3Dd(M.get(0, 0), M.get(1, 1), M.get(2, 2));
            newThing.setScale(size);
        }
        else if ( label.equals("IDC_CREATE_BREP") ) {
            PolyhedralBoundedSolid brep;

            brep = (new Box(0.9, 0.9, 0.9)).exportToPolyhedralBoundedSolid();
            Matrix4x4d R = new Matrix4x4d();
            R = R.translation(0.55, 0.55, 0.55);
            PolyhedralBoundedSolidModeler.applyTransformation(brep, R);
            //- Cube modification to holed box ----------------------------
            PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 5, 9, new Vector3Dd(0.3, 0.3, 1));
            PolyhedralBoundedSolidEulerOperators.kemr(brep, 6, 6, 5, 9, 9, 5);
            PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 9, 10, new Vector3Dd(0.8, 0.3, 1));
            PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 10, 11, new Vector3Dd(0.8, 0.8, 1));
            PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 11, 12, new Vector3Dd(0.3, 0.8, 1));
            PolyhedralBoundedSolidEulerOperators.mef(brep, 6, 6, 9, 10, 12, 11, 7);

            //- Box extrusion ---------------------------------------------
            PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 9, 13, new Vector3Dd(0.3, 0.3, 0.1));
            PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 10, 14, new Vector3Dd(0.8, 0.3, 0.1));
            PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 13, 9, 14, 10, 8);
            PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 11, 15, new Vector3Dd(0.8, 0.8, 0.1));
            PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 14, 10, 15, 11, 9);
            PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 12, 16, new Vector3Dd(0.3, 0.8, 0.1));
            PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 15, 11, 16, 12, 10);
            PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 13, 14, 16, 12, 11);

            //- Hole creation ---------------------------------------------
            PolyhedralBoundedSolidEulerOperators.kfmrh(brep, 2, 11);

            R = R.translation(-0.55, -0.55, -0.55);
            PolyhedralBoundedSolidModeler.applyTransformation(brep, R);
            PolyhedralBoundedSolidValidationEngine.validateIntermediate(brep);

            //brep = createCircle(0.5, 0.5, 0.5, 0.1, 12);

            //
            PolyhedralBoundedSolidValidationEngine.validateIntermediate(brep);
            scene().addThing(brep);
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICCUBICCURVE") ) {
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

/*
            try {
                XmlManager.exportXml(curve, "curveTest.xml",
                                     "../../../../etc/xml/vsdk.dtd");
            } catch (XmlException ex1) {
                System.out.println("EXPORT:XmlException:" +ex1);
            }
*/
/*
            // Case 2: curve read from a previous existing data file
            try {
                curve = (ParametricCurve)XmlManager.importXml(
                          "curveTest.xml");
                scene().addThing(curve);
              }
              catch (XmlException ex1) {
                System.out.println("IMPORT:XmlException:" + ex1);
            }
*/
        }
        else if ( label.equals("IDC_CREATE_FUNCTIONALEXPLICITSURFACE") ) {
            SimpleBody newThing;
            FunctionalExplicitSurface functionalSurface;
            functionalSurface = new FunctionalExplicitSurface("cos((PI*x)/2)");
            functionalSurface.setBounds(-10, -10, -10, 10, 10, 10);
            functionalSurface.setTesselationHint(100, 100);
            newThing = scene().addThing(functionalSurface);
            newThing.setMaterial(newThing.getMaterial().withDoubleSided(true));
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICBICUBICPATCH") ) {
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
            //scene().addThing(contourHermiteLine);
            SimpleBody newThing;
            newThing = scene().addThing(patch);
            newThing.setMaterial(newThing.getMaterial().withDoubleSided(true));
            //-----------------------------------------------------------------

/*
            //- Create a Bezier patch -----------------------------------------
            // Create control points 4x4 matrix
            Vector3Dd cp[][] = new Vector3Dd[4][4];
            for ( int j = 0; j < 4; j++ ) {
                for ( int i = 0; i < 4; i++ ) {
                    cp[i][j] = new Vector3Dd();
                    cp[i][j].x = ((double)i)/3-0.5;
                    cp[i][j].y = ((double)j)/3-0.5;
                    if ( i > 0 && i < 3 && j > 0 && j < 3 ) {
                        cp[i][j].z = 1;                        
                    }
                    else {
                        cp[i][j].z = 0;
                    }
                }
            }

            // Create a Bezier patch
            patch = new ParametricBiCubicPatch();
            patch.buildBezierPatch(cp);
            patch.setApproximationSteps(20);
            SimpleBody newThing;
            newThing = scene().addThing(patch);
            newThing.setMaterial(newThing.getMaterial().withDoubleSided(true));
            //-----------------------------------------------------------------
*/

/*
            //- Save a previously created patch -------------------------------
            try {
                XmlManager.exportXml(patch, "patchTest.xml",
                                     "../../../../etc/xml/vsdk.dtd");
              }
              catch (XmlException ex2) {
                 System.out.println("EXPORT:XmlException:" +ex2);
            }
            //-----------------------------------------------------------------
*/
/*
            //- Load a previously saved patch ---------------------------------
            // Case 2: patch read from a previous existing data file
            try {
                patch = (ParametricBiCubicPatch) XmlManager.importXml(
                         "patchTest.xml");
                scene().addThing(patch);
              }
              catch (XmlException ex1) {
                System.out.println("IMPORT:XmlException:" +ex1);
            }
            //-----------------------------------------------------------------
*/
        }
        else if ( label.equals("IDC_CREATE_OMNILIGHT") ) {
            light = model.addNewLight();
            if ( light == null ) {
                Logger.reportMessage(this, VSDK.WARNING, "execute", "No visible viewport where to create the light");
                return CommandResult.FAILED;
            }
        }
        //- RENDERING -----------------------------------------------------
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERIMAGE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_PENDING_ZBUFFER_COLOR_IMAGE"));
            model.getDrawingArea().setColorCaptureRequested(true);
        }
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_PENDING_ZBUFFER_DEPTH"));
            model.getDrawingArea().setDepthCaptureRequested(true);
        }
        else if ( label.equals("IDC_RENDERING_OBTAINCONTOURNS") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_PENDING_CONTOURNS"));
            model.getDrawingArea().setDepthCaptureRequested(true);
            model.getDrawingArea().setContoursRequested(true);
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_OTHERS_CYCLE_BACKGROUND") ) {
            scene().rotateBackground();
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_TEST_CORRIDOR") ) {
            if ( scene().showCorridor == true ) {
                scene().showCorridor = false;
            }
            else {
                scene().showCorridor = true;
            }
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_GRID") ) {
            model.getDrawingArea().toggleSelectedViewportGrid();
        }
        else if ( label.equals("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE") ) {
            scene().print();
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_TOOLS_CAMERA") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_CAMERA_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.CAMERA);
        }
        else if ( label.equals("IDC_TOOLS_SELECT") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_SELECTION_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.SELECT);
        }
        else if ( label.equals("IDC_TOOLS_TRANSLATE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_TRANSLATION_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.TRANSLATE);
        }
        else if ( label.equals("IDC_TOOLS_ROTATE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_ROTATION_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.ROTATE);
        }
        else if ( label.equals("IDC_TOOLS_SCALE") ) {
            presenter.showStatusMessage(model.getI18nContext().getMessage("IDM_SCALE_MODE"));
            model.getDrawingArea().setInteractionMode(InteractionMode.SCALE);
        }
        else if ( label.equals("IDC_TOOLS_RAY") ) {
            model.setWithVisualDebugRay(
                !model.isWithVisualDebugRay());
        }
        else if ( label.equals("IDC_NEW_VIEW") ) {
            model.getDrawingArea().addViewport();
        }
        else if ( label.equals("IDC_DEL_VIEW") ) {
            model.getDrawingArea().removeLastViewport();
        }
        else {
            return CommandResult.NOT_HANDLED;
        }
        return CommandResult.DONE;
    }

    /**
    Adds the objects of a file to the scene.
    @param file 3ds, vtk, gts, obj or ply file
    @throws Exception if the file can not be read
    */
    public void importObjects(File file) throws Exception
    {
        EnvironmentPersistence.importEnvironment(file, scene().scene);
    }

    /**
    Writes the objects of the scene to a file.
    @param file destination file
    @param format format of the file
    @throws Exception if the file can not be written
    */
    public void exportObjects(File file, ExportFormat format) throws Exception
    {
        FileOutputStream fos;
        fos = new FileOutputStream(file);

        switch ( format ) {
          case OBJ:
            EnvironmentPersistence.exportEnvironmentObj(fos, scene().scene);
            break;
          case GTS:
            EnvironmentPersistence.exportEnvironmentGts(fos, scene().scene);
            break;
          default:
            EnvironmentPersistence.exportEnvironmentVtk(fos, scene().scene);
            break;
        }

        fos.close();
    }

    /**
    Replaces the palette used to present depth maps.
    @param file Gimp palette (gpl) file
    @throws Exception if the file can not be read
    */
    public void loadPalette(File file) throws Exception
    {
        model.setPalette(
            RGBColorPalettePersistence.importGimpPalette(
                new FileReader(file.getAbsolutePath())));
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

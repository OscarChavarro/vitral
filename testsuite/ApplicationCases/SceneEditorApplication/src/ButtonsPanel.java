//===========================================================================

// Java basic classes
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.ArrayList;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.XmlException;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.io.image.ImagePersistence;

// Internal classes
import vsdk.transition.gui.GuiCache;
import vsdk.transition.io.presentation.GuiCachePersistence;
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

public class ButtonsPanel extends JPanel implements ActionListener
{
    private SceneEditorApplication parent;
    private String currentFilePathForReading;
    private String currentFilePathForWriting;

    public ButtonsPanel(SceneEditorApplication parent, int group)
    {
        //-------------------------------------------------------------------
        this.parent = parent;

        if ( group < 100 ) {
            // This is a button group inside right tab panels
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border empty = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            this.setBorder(empty);
        }
        else {
            // This is a button group part of an icon bar
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
            this.setBorder(empty);
        }

        currentFilePathForReading = (new File("")).getAbsolutePath() + "/../../../etc/geometry";
        currentFilePathForWriting = ".";

        //-------------------------------------------------------------------
        JPanel internal = null;

        switch ( group ) {
          case 1:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "CREATION", this);
            break;
          case 2:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "GUI", this);
            break;
          case 3:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "OTHER", this);
            break;
          case 4:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "RENDER", this);
            break;
          case 101:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "GLOBAL", this);
            break;
        }

        if ( internal != null ) {
            this.add(internal, BorderLayout.WEST);
        }

        //-------------------------------------------------------------------
    }

    private SimpleBody addDebugSphere(SimpleBody voxelBody, int groupIndex,
                                      Vector3D cm, double averageDistance)
    {
        double r = (((double)groupIndex) / 31.0) * (2 * averageDistance);
        VoxelVolume vv = (VoxelVolume)voxelBody.getGeometry();
        Sphere sphere;
        RGBAImage texture;
        SimpleBody body;
        double tetha, phi;
        int s, t;
        int voxelValue;
        Vector3D p = new Vector3D();
        Vector3D pos;
        Vector3D scale, cm2;
        Matrix4x4 S = new Matrix4x4();

        sphere = new Sphere(r);
        body = new SimpleBody();
        body.setGeometry(sphere);
        body.setMaterial(parent.theScene.defaultMaterial());
        body.getMaterial().setDoubleSided(true);
        scale = voxelBody.getScale();
        S.scale(scale.x, scale.y, scale.z);
        scale = scale.multiply(r);
        body.setScale(scale);
        cm2 = S.multiply(cm);
        pos = voxelBody.getPosition().add(cm2);
        body.setPosition(pos);
        body.setRotation(new Matrix4x4());
        body.setRotationInverse(new Matrix4x4());
        body.setName("Debug sphere for harmonics " + groupIndex);

        texture = new RGBAImage();
        texture.init(64, 64);

        //- Build sphere's texture map from voxel grid --------------------
        for ( s = 0; s < texture.getXSize(); s++ ) {
            for ( t = 0; t < texture.getYSize(); t++ ) {
                tetha =
             ((double)s) / ((double)texture.getXSize()) * Math.PI * 2;
                phi =
             ((double)t) / ((double)texture.getYSize()) * Math.PI;
                p.setSphericalCoordinates(r, tetha, phi);
                p = cm.add(p);
                voxelValue = vv.getVoxelAtPosition(p.x, p.y, p.z);
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

    private static PolyhedralBoundedSolid createCircle(
        double cx, double cy, double rad, double h, int n)
    {
        PolyhedralBoundedSolid solid;

        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(cx + rad, cy, h), 1, 1);
        addArc(solid, 1, 1, cx, cy, rad, h, 0, 
            ((double)(n-1))*360.0/((double)n), n-1);
        solid.smef(1, n, 1, 2);
        solid.validateModel();
        return solid;
    }

    private static void addArc(PolyhedralBoundedSolid solid,
        int faceId, int vertexId,
        double cx, double cy, double rad, double h, double phi1, double phi2,
        int n)
    {
        double x, y, angle, inc;
        int prev, i, nextVertexId;

        angle = Math.toRadians(phi1);
        inc = Math.toRadians(((phi2 - phi1) / ((double)n)));
        prev = vertexId;
        for ( i = 0; i < n; i++ ) {
            angle += inc;
            x = cx + rad * Math.cos(angle);
            y = cy + rad * Math.sin(angle);
            nextVertexId = solid.getMaxVertexId() + 1;
            solid.smev(faceId, prev, nextVertexId, new Vector3D(x, y, h));
            prev = nextVertexId;
        }
        solid.validateModel();
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        // This makes event compatible with ButtonGroup scheme of event
        // handling
        if ( ev.getSource() instanceof JButton ) {
            JButton origin = (JButton)ev.getSource();
            label = origin.getName();
        }
        executeCommand(label);
    }

    public void executeCommand(String label)
    {
        Light light;

        //- FILE ----------------------------------------------------------
        if ( label.equals("IDC_FILE_QUIT") ) {
            System.exit(0);
        }
        //- EDIT ----------------------------------------------------------
        //- CREATE --------------------------------------------------------
        else if ( label.equals("IDC_CREATE_SPHERE") ) {
            parent.theScene.addThing(new Sphere(1.0));
        }
        else if ( label.equals("IDC_CREATE_CONE") ) {
            parent.theScene.addThing(new Cone(1, 0, 2));
        }
        else if ( label.equals("IDC_CREATE_CYLINDER") ) {
            parent.theScene.addThing(new Cone(1, 1, 2));
        }
        else if ( label.equals("IDC_CREATE_CUBE") ) {
            parent.theScene.addThing(new Box(1, 1, 1));
        }
        else if ( label.equals("IDC_CREATE_BOX") ) {
            parent.theScene.addThing(new Box(1, 3, 2));
        }
        else if ( label.equals("IDC_CREATE_ARROW") ) {
            parent.theScene.addThing(new Arrow(0.7, 0.3, 0.05, 0.1));
        }
        else if ( label.equals("IDC_CREATE_PLANE") ) {
            InfinitePlane plane;
            plane = new InfinitePlane(new Vector3D(-0.2, 0, 1), new Vector3D(0, 0, -1));
            System.out.println(plane);
            parent.theScene.addThing(plane);

/*
            parent.theScene.activeCamera.updateVectors();
            InfinitePlane planes[];
            planes = parent.theScene.activeCamera.getBoundingPlanes();
            for ( int i = 0; i < 6; i++ ) {
                parent.theScene.addThing(planes[i]);
            }
*/
        }
        else if ( label.equals("IDC_CREATE_SPHERE_HARMONIC") ) {
            SimpleBody voxelBody = null;
            int selectedThing = parent.theScene.selectedThings.firstSelected();

            Geometry referenceGeometry = null;

            if ( selectedThing >= 0 ) {
                voxelBody = parent.theScene.scene.getSimpleBodies().get(selectedThing);
                referenceGeometry = voxelBody.getGeometry();
            }

            if ( referenceGeometry == null ||
                 !(referenceGeometry instanceof VoxelVolume) ) {
                parent.statusMessage.setText("ERROR: A VoxelVolume must be selected for spherical harmonic debugging sphere to be created");
            }
            else {
                //- Calculate the VoxelVolume's center of mass ---------------
                VoxelVolume vv = (VoxelVolume)referenceGeometry;
                Vector3D cm = vv.doCenterOfMass();

                //- Calculate average distance from nonzero voxels to cm -----
                // This accounts for scale normalization as in [FUNK2003].4.1.
                int numberOfNonZeroVoxels = 0;
                double d; // Distance between a given voxel and center of mass
                Vector3D p; // Position of voxel
                double averageDistance = 0;
                int x, y, z;

                for ( x = 0; x < vv.getXSize(); x++ ) {
                    for ( y = 0; y < vv.getYSize(); y++ ) {
                        for ( z = 0; z < vv.getZSize(); z++ ) {
                            if ( vv.getVoxel(x, y, z) != 0 ) {
                                p = vv.getVoxelPosition(x, y, z);
                                averageDistance += VSDK.vectorDistance(cm, p);
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
                parent.theScene.debugThingGroups.add(group);
                // Subspheres account for translation & scale
                group.setRotation(voxelBody.getRotation());
            }

        }
        else if ( label.equals("IDC_CREATE_PROJECTED_VIEWS") ) {
            parent.drawingArea.wantToDebugProjectedViews = true;
        }
        else if ( label.equals("IDC_CREATE_VOLUME") ) {
            //- Select current object, if empty selection take a temp. sphere -
            int selectedThing = parent.theScene.selectedThings.firstSelected();
            Geometry referenceGeometry = null;
            SimpleBody thing = null;

            if ( selectedThing < 0 ) {
                referenceGeometry = new Sphere(0.5);
            }
            else {
                thing = parent.theScene.scene.getSimpleBodies().get(selectedThing);
                referenceGeometry = thing.getGeometry();
            }

            //- Calculate transform matrix ------------------------------------
            double minmax[] = referenceGeometry.getMinMax();
            Matrix4x4 M; // Transform from voxelspace to geometry minmax space

            M = VoxelVolume.getTransformFromVoxelFrameToMinMax(minmax);

            //- Auxiliary variables -------------------------------------------
            int nx = 64, ny = 64, nz = 64;

            //- Primitive rasterization ---------------------------------------
            VoxelVolume vv = new VoxelVolume();
            vv.init(nx, ny, nz);

            ProgressMonitorConsole reporter = new ProgressMonitorConsole();
            referenceGeometry.doVoxelization(vv, M, reporter);

            //- Append newly created volume to scene, matching reference form -
            SimpleBody newThing = parent.theScene.addThing(vv);
            Vector3D pos = new Vector3D(M.M[0][3], M.M[1][3], M.M[2][3]);
            if ( thing != null ) {
                pos = pos.add(thing.getPosition());
                newThing.setRotation(thing.getRotation());
                newThing.setScale(thing.getScale());
            }
            newThing.setPosition(pos);
            Vector3D size = new Vector3D(M.M[0][0], M.M[1][1], M.M[2][2]);
            newThing.setScale(size);
        }
        else if ( label.equals("IDC_CREATE_BREP") ) {
            PolyhedralBoundedSolid brep;

            brep = (new Box(0.9, 0.9, 0.9)).exportToPolyhedralBoundedSolid();
            Matrix4x4 R = new Matrix4x4();
            R.translation(0.55, 0.55, 0.55);
            brep.applyTransformation(R);
            //- Cube modification to holed box ----------------------------
            brep.smev(6, 5, 9, new Vector3D(0.3, 0.3, 1));
            brep.kemr(6, 6, 5, 9, 9, 5);
            brep.smev(6, 9, 10, new Vector3D(0.8, 0.3, 1));
            brep.smev(6, 10, 11, new Vector3D(0.8, 0.8, 1));
            brep.smev(6, 11, 12, new Vector3D(0.3, 0.8, 1));
            brep.mef(6, 6, 9, 10, 12, 11, 7);

            //- Box extrusion ---------------------------------------------
            brep.smev(7, 9, 13, new Vector3D(0.3, 0.3, 0.1));
            brep.smev(7, 10, 14, new Vector3D(0.8, 0.3, 0.1));
            brep.mef(7, 7, 13, 9, 14, 10, 8);
            brep.smev(7, 11, 15, new Vector3D(0.8, 0.8, 0.1));
            brep.mef(7, 7, 14, 10, 15, 11, 9);
            brep.smev(7, 12, 16, new Vector3D(0.3, 0.8, 0.1));
            brep.mef(7, 7, 15, 11, 16, 12, 10);
            brep.mef(7, 7, 13, 14, 16, 12, 11);

            //- Hole creation ---------------------------------------------
            brep.kfmrh(2, 11);

            R.translation(-0.55, -0.55, -0.55);
            brep.applyTransformation(R);
            brep.validateModel();

            //brep = createCircle(0.5, 0.5, 0.5, 0.1, 12);

            //
            brep.validateModel();
            parent.theScene.addThing(brep);
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICCUBICCURVE") ) {
            ParametricCurve curve;

            // Case 1: curve hard-coded in source
            Vector3D pointParameters[];

            curve = new ParametricCurve();
            // Note that an HERMITE curve uses tangent vectors, BEZIER curves
            // uses control points (tangent vectors are control point minus
            // knot position)
            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0, 0, 0); // Position 0
            pointParameters[1] = new Vector3D(0, 0, 0); // Not used
            pointParameters[2] = new Vector3D(0, 1, 0); // Salient tangent end
            curve.addPoint(pointParameters, curve.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(1, 1, 0); // Position 1
            pointParameters[1] = new Vector3D(0, 1, 0); // Entry tangent end
            pointParameters[2] = new Vector3D(2, 1, 0); // Salient tangent end
            curve.addPoint(pointParameters, curve.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(2, 0, 1); // Position 2
            pointParameters[1] = new Vector3D(2, 0, 0); // Entry tangent end
            pointParameters[2] = new Vector3D(0, 0, 0); // Not used
            curve.addPoint(pointParameters, curve.BEZIER);

            parent.theScene.addThing(curve);

/*
            try {
                XmlManager.exportXml(curve, "curveTest.xml",
                                     "../../../etc/xml/vsdk.dtd");
            } catch (XmlException ex1) {
                System.out.println("EXPORT:XmlException:" +ex1);
            }
*/
/*
            // Case 2: curve read from a previous existing data file
            try {
                curve = (ParametricCurve)XmlManager.importXml(
                          "curveTest.xml");
                parent.theScene.addThing(curve);
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
            newThing = parent.theScene.addThing(functionalSurface);
            newThing.getMaterial().setDoubleSided(true);
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICBICUBICPATCH") ) {
            //- Create a Ferguson patch ---------------------------------------
            ParametricCurve contourHermiteLine;
            Vector3D pointParameters[];

            contourHermiteLine = new ParametricCurve();
            // Note that an HERMITE curve uses tangent vectors, BEZIER curves
            // uses control points (tangent vectors are control point minus
            // knot position)
            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0, 0, 0);  // Position 0
            pointParameters[1] = new Vector3D(0, -1, 0); // Entry tangent
            pointParameters[2] = new Vector3D(1, 0, 0);  // Salient tangent
            contourHermiteLine.addPoint(pointParameters, contourHermiteLine.HERMITE);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(1, 0, 0);  // Position 1
            pointParameters[1] = new Vector3D(1, 0, 0);  // Entry tangent
            pointParameters[2] = new Vector3D(0, 1, 0);  // Salient tangent
            contourHermiteLine.addPoint(pointParameters, contourHermiteLine.HERMITE);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(1, 1, 0.4);  // Position 2
            pointParameters[1] = new Vector3D(0, 1, 0);  // Entry tangent
            pointParameters[2] = new Vector3D(-1, 0, 0);  // Salient tangent
            contourHermiteLine.addPoint(pointParameters, contourHermiteLine.HERMITE);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0, 1, 0);  // Position 3
            pointParameters[1] = new Vector3D(-1, 0, 0);  // Entry tangent
            pointParameters[2] = new Vector3D(0, -1, 0);  // Salient tangent
            contourHermiteLine.addPoint(pointParameters, contourHermiteLine.HERMITE);

            contourHermiteLine.addPoint(contourHermiteLine.getPoint(0), contourHermiteLine.HERMITE);

            ParametricBiCubicPatch patch;
            patch = new ParametricBiCubicPatch();
            patch.buildFergusonPatch(contourHermiteLine);
            patch.setApproximationSteps(20);
            //parent.theScene.addThing(contourHermiteLine);
            SimpleBody newThing;
            newThing = parent.theScene.addThing(patch);
            newThing.getMaterial().setDoubleSided(true);
            //-----------------------------------------------------------------

/*
            //- Create a Bezier patch -----------------------------------------
            // Create control points 4x4 matrix
            Vector3D cp[][] = new Vector3D[4][4];
            for ( int j = 0; j < 4; j++ ) {
                for ( int i = 0; i < 4; i++ ) {
                    cp[i][j] = new Vector3D();
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
            newThing = parent.theScene.addThing(patch);
            newThing.getMaterial().setDoubleSided(true);
            //-----------------------------------------------------------------
*/

/*
            //- Save a previously created patch -------------------------------
            try {
                XmlManager.exportXml(patch, "patchTest.xml",
                                     "../../../etc/xml/vsdk.dtd");
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
                parent.theScene.addThing(patch);
              }
              catch (XmlException ex1) {
                System.out.println("IMPORT:XmlException:" +ex1);
            }
            //-----------------------------------------------------------------
*/
        }
        else if ( label.equals("IDC_IMPORT_OBJECTS_FROM_FILE") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser(currentFilePathForReading);
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new MyFilter("3ds", "3ds Kinetix/Discreet 3DStudio/3DStudioMax binary scene file"));
            jfc.addChoosableFileFilter(new MyFilter("vtk", "vtk Kitware vtk legacy binary file (mesh only)"));
            jfc.addChoosableFileFilter(new MyFilter("gts", "gts Gts mesh ASCII file"));
            jfc.addChoosableFileFilter(new MyFilter("obj", "obj Alias/Wavefront text mesh"));
            jfc.addChoosableFileFilter(new MyFilter("ply", "ply Ply mesh"));

            int opc = jfc.showOpenDialog(new JPanel());
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();

                    EnvironmentPersistence.importEnvironment(file,
                        parent.theScene.scene);

                    currentFilePathForReading = file.getParentFile().getAbsolutePath();

                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to read file...\n" + ex);
                    ex.printStackTrace();
                    return;
                }
            }

        }
        else if ( label.equals("IDC_EXPORT_OBJECTS_TO_OBJ") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser(currentFilePathForWriting);
            jfc.removeChoosableFileFilter(jfc.getFileFilter());

            int opc = jfc.showOpenDialog(new JPanel());
            if ( opc == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = jfc.getSelectedFile();
                    FileOutputStream fos = new FileOutputStream(file);

                    EnvironmentPersistence.exportEnvironmentObj(fos,
                        parent.theScene.scene);

                    fos.close();

                    currentFilePathForWriting = file.getParentFile().getAbsolutePath();

                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to write file...\n" + ex);
                    return;
                }
            }

        }
        else if ( label.equals("IDC_EXPORT_OBJECTS_TO_GTS") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser(currentFilePathForWriting);
            jfc.removeChoosableFileFilter(jfc.getFileFilter());

            int opc = jfc.showOpenDialog(new JPanel());
            if ( opc == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = jfc.getSelectedFile();
                    FileOutputStream fos = new FileOutputStream(file);

                    EnvironmentPersistence.exportEnvironmentGts(fos,
                        parent.theScene.scene);

                    fos.close();

                    currentFilePathForWriting = file.getParentFile().getAbsolutePath();

                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to write file...\n" + ex);
                    ex.printStackTrace();
                    return;
                }
            }
        }
        else if ( label.equals("IDC_EXPORT_OBJECTS_TO_VTK") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser(currentFilePathForWriting);
            jfc.removeChoosableFileFilter(jfc.getFileFilter());

            int opc = jfc.showOpenDialog(new JPanel());
            if ( opc == JFileChooser.APPROVE_OPTION ) {
                try {
                    File file = jfc.getSelectedFile();
                    FileOutputStream fos = new FileOutputStream(file);

                    EnvironmentPersistence.exportEnvironmentVtk(fos,
                        parent.theScene.scene);

                    fos.close();

                    currentFilePathForWriting = file.getParentFile().getAbsolutePath();

                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to write file...\n" + ex);
                    ex.printStackTrace();
                    return;
                }
            }
        }
        else if ( label.equals("IDC_CREATE_OMNILIGHT") ) {
            light = new Light(Light.POINT, new Vector3D(-10, -9, 8), new ColorRgb(1, 1, 1));
            //light = new Light(Light.POINT, new Vector3D(0, -4, 0), new ColorRgb(1, 1, 1));
            parent.theScene.scene.getLights().add(light);
        }
        //- RENDERING -----------------------------------------------------
        else if ( label.equals("Select palette for depthmap display") ||
                  label.equals("IDC_RENDERING_SELECTPALETTEDEPTH") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../../etc/palettes");
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new MyFilter("gpl", "gpl Gimp Palettes"));
            int opc = jfc.showOpenDialog(new JPanel());
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();
                    parent.palette = 
                        RGBColorPalettePersistence.importGimpPalette(
                            new java.io.FileReader(file.getAbsolutePath()));
                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to read file");
                    return;
                }
            }
        }
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERIMAGE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_PENDING_ZBUFFER_COLOR_IMAGE"));
            parent.drawingArea.wantToGetColor = true;
        }
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_PENDING_ZBUFFER_DEPTH"));
            parent.drawingArea.wantToGetDepth = true;
        }
        else if ( label.equals("IDC_RENDERING_OBTAINCONTOURNS") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_PENDING_CONTOURNS"));
            parent.drawingArea.wantToGetDepth = true;
            parent.drawingArea.wantToGetContourns = true;
        }
        else if ( label.equals("IDC_RENDERING_RAYTRACING") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_COMPUTING_RAYTRACING"));
            parent.doRaytracedImage();
            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.raytracedImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.raytracedImage);
            }
            parent.imageControlWindow.redrawImage();
        }
        //- CUSTOMIZE -----------------------------------------------------
        else if ( label.equals("IDC_CUSTOMIZE_LAF_MOTIF") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_JAVA") ) {
            parent.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_GTK") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_WINDOWS") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LANGUAGE_ENGLISH") ) {
            parent.setGuiLanguage("./etc/english.gui");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LANGUAGE_SPANISH") ) {
            parent.setGuiLanguage("./etc/spanish.gui");
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_OTHERS_CYCLE_BACKGROUND") ) {
            parent.drawingArea.rotateBackground();
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_TEST_CORRIDOR") ) {
            if ( parent.theScene.showCorridor == true ) {
                parent.theScene.showCorridor = false;
            }
            else {
                parent.theScene.showCorridor = true;
            }
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_GRID") ) {
            parent.drawingArea.toggleGrid();
        }
        else if ( label.equals("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE") ) {
            parent.theScene.print();
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_TOOLS_CAMERA") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_CAMERA_MODE"));
            parent.drawingArea.interactionMode = 
                parent.drawingArea.CAMERA_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_SELECT") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_SELECTION_MODE"));
            parent.drawingArea.interactionMode = 
            parent.drawingArea.SELECT_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_TRANSLATE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_TRANSLATION_MODE"));
            parent.drawingArea.interactionMode = 
            parent.drawingArea.TRANSLATE_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_ROTATE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_ROTATION_MODE"));
            parent.drawingArea.interactionMode = 
                parent.drawingArea.ROTATE_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_SCALE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_SCALE_MODE"));
            parent.drawingArea.interactionMode = 
                parent.drawingArea.SCALE_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_RAY") ) {
            if ( parent.withVisualDebugRay ) {
                parent.withVisualDebugRay = false;
            }
            else {
                parent.withVisualDebugRay = true;
            }
        }
        else if ( label.equals("IDC_VOICECOMMAND_CLIENT") ) {
            parent.switchVoiceCommandClient();
        }
        else if ( label.equals("IDC_NEW_VIEW") ) {
            parent.drawingArea.newView();
        }
        else if ( label.equals("IDC_DEL_VIEW") ) {
            parent.drawingArea.delView();
        }

        //-----------------------------------------------------------------
        parent.drawingArea.canvas.repaint();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

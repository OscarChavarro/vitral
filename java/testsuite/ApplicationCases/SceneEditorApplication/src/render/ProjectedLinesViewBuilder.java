//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          Meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package render;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.geometricProcessing.GeometryTriangulator;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.render.SelectionCorners;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

import model.Scene;

/**
Builds the drawing of a viewport as projected 2D lines, computed entirely in
the processor: the "calligraphic" approach of vector displays and plotters
[FOLE1992].1, where a 3D scene is presented without any 3D graphics API. The
result is a list of `ProjectedLineLayer`s, so this class does not depend on
the 2D technology that finally draws them (see `render.awt`).

The bodies follow the `RendererConfiguration` of the viewport:
  - With surfaces, the bodies that are (or can be exported to) a
    `PolyhedralBoundedSolid` are drawn with hidden line removal, by the
    quantitative invisibility algorithm of [APPE1967] (`HiddenLineRenderer`):
    contour lines are thicker than the other visible lines, and hidden lines
    are dashed if wires are also asked for. The occlusion is computed among
    all those solids.
  - Without surfaces, with wires, every body is a plain wireframe: the edges
    of its boundary representation, or of its triangle mesh.
  - Bodies with no boundary representation nor mesh are always drawn as
    wireframes (with surfaces they neither hide nor are hidden), and bodies
    with none of them are represented by their bounding box.
  - Points, bounding volumes and the selection corners of selected bodies
    are added over them.

The reference grid, the light gizmos, the visual debug entities and the
feedback of the body editor are also presented as lines.
*/
public class ProjectedLinesViewBuilder
{
    private static final ColorRgb GRID_COLOR = new ColorRgb(0.37, 0.37, 0.37);
    private static final ColorRgb GRID_AXIS_COLOR = new ColorRgb(0, 0, 0);
    private static final ColorRgb VISIBLE_LINE_COLOR = new ColorRgb(0, 0, 0);
    private static final ColorRgb HIDDEN_LINE_COLOR = new ColorRgb(0.32, 0.32, 0.32);
    private static final ColorRgb BOUNDING_VOLUME_COLOR = new ColorRgb(0.6, 0.9, 1.0);
    private static final ColorRgb POINT_COLOR = new ColorRgb(1.0, 0.0, 0.0);
    private static final ColorRgb LIGHT_COLOR = new ColorRgb(1.0, 1.0, 1.0);
    private static final ColorRgb SELECTED_LIGHT_COLOR = new ColorRgb(1.0, 1.0, 0.0);
    private static final ColorRgb DEBUG_COLOR = new ColorRgb(0.2, 0.8, 1.0);
    private static final ColorRgb EDIT_FEEDBACK_COLOR = new ColorRgb(1.0, 0.55, 0.0);
    private static final double LIGHT_GIZMO_SIZE = 0.15;
    /// Distance an orthogonal camera is moved back for the Appel lines of sight
    private static final double ORTHOGONAL_EYE_DISTANCE = 1000.0;

    /**
    Builds the lines presenting the scene in a viewport.
    @param scene scene to present
    @param viewport viewport whose active camera, rendering configuration and
    grid are used; its camera must have its viewport size updated
    @param editor editor of the selected body, whose feedback is presented, or
    null if there is none
    @param withBodies true to present the bodies and the grid (hidden lines
    render mode), false to present only the annotations that go over an image
    of the scene computed by other means (raytracing render mode)
    @return the layers, in drawing order
    */
    public List<ProjectedLineLayer> buildView(Scene scene, Viewport viewport,
                                              BodyEditFeedbackProvider editor,
                                              boolean withBodies)
    {
        List<ProjectedLineLayer> layers = new ArrayList<ProjectedLineLayer>();
        Camera camera = viewport.getActiveCamera();
        RendererConfiguration quality = viewport.getRendererConfiguration();
        ClipSpaceLineProjector projector = new ClipSpaceLineProjector(camera);

        if ( withBodies ) {
            if ( viewport.isShowGrid() ) {
                addGrid(layers, projector, camera);
            }
            addBodies(layers, projector, scene, camera, quality);
        }
        addAnnotations(layers, projector, scene, quality, editor);
        addDebugEntities(layers, projector, scene);
        addLights(layers, projector, scene);
        return layers;
    }

    //= Grid ==============================================================

    /**
    Same reference grid as the OpenGL presentation: 14x14 unit cells on the
    plane z = 0, or on the vertical plane facing the camera for orthogonal
    side views.
    */
    private void addGrid(List<ProjectedLineLayer> layers,
                         ClipSpaceLineProjector projector, Camera camera)
    {
        Matrix4x4d gridTransform = new Matrix4x4d();
        Matrix4x4d rotation = camera.getRotation();
        double yaw = Math.toDegrees(rotation.obtainEulerYawAngle());
        double pitch = Math.toDegrees(rotation.obtainEulerPitchAngle());

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL &&
             (pitch > -45 && pitch < 45) ) {
            if ( (yaw > 45 && yaw < 135) || (yaw < -45 && yaw > -135) ) {
                gridTransform = gridTransform.axisRotation(Math.toRadians(90), 1, 0, 0);
            }
            else {
                gridTransform = gridTransform.axisRotation(Math.toRadians(90), 0, 1, 0);
            }
        }

        ProjectedLineLayer grid = new ProjectedLineLayer(GRID_COLOR, 1.0,
            ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer axes = new ProjectedLineLayer(GRID_AXIS_COLOR, 1.0,
            ProjectedLineLayer.Style.SOLID);
        int n = 14;
        double half = n / 2.0;
        int i;

        for ( i = 0; i <= n; i++ ) {
            double c = -half + i;
            ProjectedLineLayer target = (i == n / 2) ? axes : grid;
            projector.addSegment(target.getLines(), gridTransform,
                new Vector3Dd(c, -half, 0), new Vector3Dd(c, half, 0));
            projector.addSegment(target.getLines(), gridTransform,
                new Vector3Dd(-half, c, 0), new Vector3Dd(half, c, 0));
        }
        addIfNotEmpty(layers, grid);
        addIfNotEmpty(layers, axes);
    }

    //= Bodies ============================================================

    private void addBodies(List<ProjectedLineLayer> layers,
                           ClipSpaceLineProjector projector, Scene scene,
                           Camera camera, RendererConfiguration quality)
    {
        List<SimpleBody> bodies = scene.scene.getSimpleBodies();
        List<SimpleBody> solids = new ArrayList<SimpleBody>();
        ProjectedLineLayer points = new ProjectedLineLayer(POINT_COLOR, 3.0,
            ProjectedLineLayer.Style.POINTS);

        for ( SimpleBody body : bodies ) {
            Geometry geometry = body.getGeometry();
            if ( geometry == null ) {
                continue;
            }
            PolyhedralBoundedSolid brep = exportToBrep(geometry);
            if ( quality.isSurfacesSet() && brep != null ) {
                // Drawn below, all at once: they occlude each other
                solids.add(brepBody(body, brep));
            }
            else if ( quality.isSurfacesSet() || quality.isWiresSet() ) {
                ProjectedLineLayer wires = new ProjectedLineLayer(
                    wireColor(body.getMaterial()), 1.0,
                    ProjectedLineLayer.Style.SOLID);
                addWireframe(wires, projector, geometry, brep,
                    body.getTransformationMatrix());
                addIfNotEmpty(layers, wires);
            }
            if ( quality.isPointsSet() ) {
                addPoints(points, projector, geometry, brep,
                    body.getTransformationMatrix());
            }
        }

        if ( !solids.isEmpty() ) {
            addHiddenLineDrawing(layers, solids, camera, quality.isWiresSet());
        }
        addIfNotEmpty(layers, points);
    }

    /**
    Presents a set of solids with the hidden line removal of [APPE1967].
    */
    private void addHiddenLineDrawing(List<ProjectedLineLayer> layers,
                                      List<SimpleBody> solids, Camera camera,
                                      boolean withHiddenLines)
    {
        ProjectedLineLayer contours = new ProjectedLineLayer(VISIBLE_LINE_COLOR,
            2.0, ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer visible = new ProjectedLineLayer(VISIBLE_LINE_COLOR,
            1.0, ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer hidden = new ProjectedLineLayer(HIDDEN_LINE_COLOR,
            1.0, ProjectedLineLayer.Style.DASHED);

        HiddenLineRenderer.executeAppelAlgorithm(solids,
            lineOfSightCamera(camera), contours.getLines(), visible.getLines(),
            hidden.getLines());

        if ( withHiddenLines ) {
            addIfNotEmpty(layers, hidden);
        }
        addIfNotEmpty(layers, visible);
        addIfNotEmpty(layers, contours);
    }

    /**
    The quantitative invisibility of [APPE1967] is measured along the line of
    sight from the eye to each point. For an orthogonal camera all the lines
    of sight are parallel, so the eye is moved far back along the viewing
    direction (with its far plane, so the projection does not change).
    @param camera camera of the view
    @return the camera to give to the hidden line algorithm
    */
    private static Camera lineOfSightCamera(Camera camera)
    {
        if ( camera.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            return camera;
        }
        Camera eye = new Camera(camera);
        Vector3Dd front = camera.getFront().normalized();
        eye.setPosition(camera.getPosition().subtract(front.multiply(ORTHOGONAL_EYE_DISTANCE)));
        eye.setNearPlaneDistance(camera.getNearPlaneDistance() + ORTHOGONAL_EYE_DISTANCE);
        eye.setFarPlaneDistance(camera.getFarPlaneDistance() + ORTHOGONAL_EYE_DISTANCE);
        return eye;
    }

    /**
    The quantitative invisibility is answered by the geometry of each body,
    and only a `PolyhedralBoundedSolid` answers it: a body whose geometry is
    exported to one (i.e. a `Box` or a `Sphere`) is replaced, for the hidden
    line algorithm, by a body with the same transformation and the exported
    boundary representation as geometry.
    @param body body of the scene
    @param brep boundary representation of its geometry
    @return the body to give to the hidden line algorithm
    */
    private static SimpleBody brepBody(SimpleBody body, PolyhedralBoundedSolid brep)
    {
        if ( body.getGeometry() == brep ) {
            return body;
        }
        SimpleBody surrogate = new SimpleBody();
        surrogate.setName(body.getName());
        surrogate.setGeometry(brep);
        surrogate.setPosition(body.getPosition());
        surrogate.setRotation(body.getRotation());
        surrogate.setScale(body.getScale());
        return surrogate;
    }

    /**
    @param geometry
    @return the boundary representation of the geometry, or null if it is
    not a volume that can be exported to one
    */
    private static PolyhedralBoundedSolid exportToBrep(Geometry geometry)
    {
        if ( geometry instanceof PolyhedralBoundedSolid ) {
            return (PolyhedralBoundedSolid)geometry;
        }
        if ( geometry instanceof Volume ) {
            return ((Volume)geometry).exportToPolyhedralBoundedSolid();
        }
        return null;
    }

    /**
    Adds the wireframe of a geometry: the edges of its boundary
    representation, else the edges of the triangles of its mesh, else its
    bounding box.
    @param brep boundary representation of the geometry, or null
    */
    private void addWireframe(ProjectedLineLayer layer,
                              ClipSpaceLineProjector projector,
                              Geometry geometry, PolyhedralBoundedSolid brep,
                              Matrix4x4d transform)
    {
        if ( brep != null ) {
            int i;
            for ( i = 0; i < brep.getEdgesList().size(); i++ ) {
                _PolyhedralBoundedSolidEdge edge = brep.getEdgesList().get(i);
                if ( edge.getStartingVertexId() < 0 || edge.getEndingVertexId() < 0 ||
                     edge.rightHalf == null || edge.leftHalf == null ) {
                    continue;
                }
                Vector3Dd p0 = edge.rightHalf.startingVertex.position;
                Vector3Dd p1 = edge.leftHalf.startingVertex.position;
                if ( p0 != null && p1 != null ) {
                    projector.addSegment(layer.getLines(), transform, p0, p1);
                }
            }
            return;
        }

        TriangleMeshGroup meshes = GeometryTriangulator.exportToTriangleMeshGroup(geometry);
        if ( meshes != null ) {
            for ( TriangleMesh mesh : meshes.getMeshes() ) {
                addMeshWireframe(layer, projector, mesh, transform);
            }
            return;
        }

        addBox(layer, projector, geometry.getMinMax(), transform);
    }

    private void addMeshWireframe(ProjectedLineLayer layer,
                                  ClipSpaceLineProjector projector,
                                  TriangleMesh mesh, Matrix4x4d transform)
    {
        double[] v = mesh.getVertexPositions();
        int[] triangles = mesh.getTriangleIndexes();
        int t;

        if ( v == null || triangles == null ) {
            return;
        }
        for ( t = 0; t < mesh.getNumTriangles(); t++ ) {
            int i;
            for ( i = 0; i < 3; i++ ) {
                int a = triangles[3*t + i];
                int b = triangles[3*t + (i + 1) % 3];
                projector.addSegment(layer.getLines(), transform,
                    new Vector3Dd(v[3*a], v[3*a + 1], v[3*a + 2]),
                    new Vector3Dd(v[3*b], v[3*b + 1], v[3*b + 2]));
            }
        }
    }

    private void addPoints(ProjectedLineLayer layer,
                           ClipSpaceLineProjector projector,
                           Geometry geometry, PolyhedralBoundedSolid brep,
                           Matrix4x4d transform)
    {
        if ( brep != null ) {
            int i;
            for ( i = 0; i < brep.getVerticesList().size(); i++ ) {
                _PolyhedralBoundedSolidVertex vertex = brep.getVerticesList().get(i);
                if ( vertex.position != null ) {
                    projector.addPoint(layer.getLines(), transform.multiply(vertex.position));
                }
            }
            return;
        }

        TriangleMeshGroup meshes = GeometryTriangulator.exportToTriangleMeshGroup(geometry);
        if ( meshes == null ) {
            return;
        }
        for ( TriangleMesh mesh : meshes.getMeshes() ) {
            double[] v = mesh.getVertexPositions();
            int i;
            for ( i = 0; v != null && i < mesh.getNumVertices(); i++ ) {
                projector.addPoint(layer.getLines(), transform.multiply(
                    new Vector3Dd(v[3*i], v[3*i + 1], v[3*i + 2])));
            }
        }
    }

    private static ColorRgb wireColor(SimpleMaterial material)
    {
        if ( material == null ) {
            return VISIBLE_LINE_COLOR;
        }
        return material.getDiffuse();
    }

    //= Annotations =======================================================

    private void addAnnotations(List<ProjectedLineLayer> layers,
                                ClipSpaceLineProjector projector, Scene scene,
                                RendererConfiguration quality,
                                BodyEditFeedbackProvider editor)
    {
        List<SimpleBody> bodies = scene.scene.getSimpleBodies();
        ProjectedLineLayer boxes = new ProjectedLineLayer(BOUNDING_VOLUME_COLOR,
            1.0, ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer corners = new ProjectedLineLayer(
            SelectionCorners.getDefaultColor(), 1.0, ProjectedLineLayer.Style.SOLID);
        int i;

        scene.selectedThings.sync();
        for ( i = 0; i < bodies.size(); i++ ) {
            SimpleBody body = bodies.get(i);
            Geometry geometry = body.getGeometry();
            if ( geometry == null ) {
                continue;
            }
            if ( quality.isBoundingVolumeSet() ) {
                addBox(boxes, projector, geometry.getMinMax(),
                    body.getTransformationMatrix());
            }
            if ( scene.selectedThings.isSelected(i) ) {
                addSelectionCorners(corners, projector, geometry.getMinMax(),
                    body.getTransformationMatrix());
            }
        }
        addIfNotEmpty(layers, boxes);
        addIfNotEmpty(layers, corners);

        if ( editor != null && editor.getTarget() != null ) {
            ProjectedLineLayer feedback = new ProjectedLineLayer(
                EDIT_FEEDBACK_COLOR, 1.0, ProjectedLineLayer.Style.SOLID);
            for ( RenderPrimitive primitive : editor.buildEditFeedback() ) {
                Geometry geometry = primitive.getGeometry();
                if ( geometry != null ) {
                    addWireframe(feedback, projector, geometry,
                        exportToBrep(geometry), primitive.getTransform());
                }
            }
            addIfNotEmpty(layers, feedback);
        }
    }

    private void addSelectionCorners(ProjectedLineLayer layer,
                                     ClipSpaceLineProjector projector,
                                     double[] minmax, Matrix4x4d transform)
    {
        Vector3Dd[] segments = SelectionCorners.buildSegments(minmax);
        int i;

        for ( i = 0; i + 1 < segments.length; i += 2 ) {
            projector.addSegment(layer.getLines(), transform, segments[i], segments[i + 1]);
        }
    }

    /**
    Adds the twelve edges of an axis aligned box.
    @param minmax minimum x, y, z followed by maximum x, y, z
    */
    private void addBox(ProjectedLineLayer layer,
                        ClipSpaceLineProjector projector, double[] minmax,
                        Matrix4x4d transform)
    {
        if ( minmax == null || minmax.length < 6 ) {
            return;
        }
        for ( double limit : minmax ) {
            // i.e. an infinite plane
            if ( Double.isInfinite(limit) || Double.isNaN(limit) ) {
                return;
            }
        }
        Vector3Dd[] c = new Vector3Dd[8];
        int i;

        for ( i = 0; i < 8; i++ ) {
            c[i] = new Vector3Dd(
                (i & 1) != 0 ? minmax[3] : minmax[0],
                (i & 2) != 0 ? minmax[4] : minmax[1],
                (i & 4) != 0 ? minmax[5] : minmax[2]);
        }
        for ( i = 0; i < 8; i++ ) {
            int bit;
            for ( bit = 1; bit < 8; bit <<= 1 ) {
                if ( (i & bit) == 0 ) {
                    projector.addSegment(layer.getLines(), transform, c[i], c[i | bit]);
                }
            }
        }
    }

    //= Debug entities and lights =========================================

    private void addDebugEntities(List<ProjectedLineLayer> layers,
                                  ClipSpaceLineProjector projector, Scene scene)
    {
        ProjectedLineLayer debug = new ProjectedLineLayer(DEBUG_COLOR, 1.0,
            ProjectedLineLayer.Style.SOLID);

        for ( SimpleBodyGroup group : scene.debugThingGroups ) {
            Matrix4x4d groupTransform = group.getTransformationMatrix();
            for ( SimpleBody body : group.getBodies() ) {
                Geometry geometry = body.getGeometry();
                if ( geometry != null ) {
                    addWireframe(debug, projector, geometry, exportToBrep(geometry),
                        groupTransform.multiply(body.getTransformationMatrix()));
                }
            }
        }
        addIfNotEmpty(layers, debug);
    }

    /**
    Each light is presented as a small star of three axis aligned segments
    and four diagonals, centered at its position.
    */
    private void addLights(List<ProjectedLineLayer> layers,
                           ClipSpaceLineProjector projector, Scene scene)
    {
        List<Light> lights = scene.scene.getLights();
        ProjectedLineLayer normal = new ProjectedLineLayer(LIGHT_COLOR, 1.0,
            ProjectedLineLayer.Style.SOLID);
        ProjectedLineLayer selected = new ProjectedLineLayer(SELECTED_LIGHT_COLOR,
            2.0, ProjectedLineLayer.Style.SOLID);
        double s = LIGHT_GIZMO_SIZE * scene.getLightGizmoScale();
        double d = s / Math.sqrt(3.0);
        Vector3Dd[] directions = {
            new Vector3Dd(s, 0, 0), new Vector3Dd(0, s, 0), new Vector3Dd(0, 0, s),
            new Vector3Dd(d, d, d), new Vector3Dd(-d, d, d),
            new Vector3Dd(d, -d, d), new Vector3Dd(d, d, -d)
        };
        int i;

        scene.selectedLights.sync();
        for ( i = 0; i < lights.size(); i++ ) {
            Vector3Dd center = lights.get(i).getPosition();
            ProjectedLineLayer target = scene.selectedLights.isSelected(i) ? selected : normal;
            if ( center == null ) {
                continue;
            }
            for ( Vector3Dd direction : directions ) {
                projector.addSegment(target.getLines(), center.subtract(direction),
                    center.add(direction));
            }
        }
        addIfNotEmpty(layers, normal);
        addIfNotEmpty(layers, selected);
    }

    private static void addIfNotEmpty(List<ProjectedLineLayer> layers,
                                      ProjectedLineLayer layer)
    {
        if ( !layer.isEmpty() ) {
            layers.add(layer);
        }
    }
}

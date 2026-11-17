#include <stddef.h>
#include <stdlib.h>
#include <math.h>

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"

const double CameraControlGizmo::CORNER_FRACTION = 0.15;
const double CameraControlGizmo::GAP = 0.04;
const double CameraControlGizmo::COMPASS_INTERNAL_RADIUS = 1.5;
const double CameraControlGizmo::COMPASS_EXTERNAL_RADIUS = 1.8;
const double CameraControlGizmo::COMPASS_HEIGHT = 0.08;
const double CameraControlGizmo::COMPASS_BASE_Z = -1.0;
const double CameraControlGizmo::HUD_HALF_EXTENT = 2.05;
const double CameraControlGizmo::HUD_CAMERA_DISTANCE = 10.0;
const double CameraControlGizmo::DEFAULT_INACTIVE_OPACITY = 0.55;

static const char* const FACE_LABELS[6] = {
    "LEFT", "RIGHT", "FRONT", "BACK", "BOTTOM", "TOP"
};

CameraControlGizmo::CameraControlGizmo() :
    edgeSegments(NULL),
    faceColor(0.86, 0.88, 0.92),
    edgeColor(0.74, 0.77, 0.82),
    cornerColor(0.66, 0.69, 0.75),
    compassColor(0.55, 0.57, 0.62),
    highlightColor(0.35, 0.62, 1.0),
    lineColor(0.15, 0.15, 0.2),
    labelColor(0.1, 0.1, 0.15)
{
    buildModel();

    camera = NULL;
    hudCamera.setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
    hudCamera.setOrthogonalZoom(1.0 / HUD_HALF_EXTENT);
    hudCamera.setNearPlaneDistance(0.1);
    hudCamera.setFarPlaneDistance(2 * HUD_CAMERA_DISTANCE);
    hudCamera.setName("Camera control gizmo");

    baseSizeInPixels = DEFAULT_BASE_SIZE_IN_PIXELS;
    baseMarginInPixels = DEFAULT_BASE_MARGIN_IN_PIXELS;
    sizeInPixels = baseSizeInPixels;
    marginInPixels = baseMarginInPixels;
    hudX = marginInPixels;
    hudY = marginInPixels;
    visible = TRUE;
    inactiveOpacity = DEFAULT_INACTIVE_OPACITY;
    active = FALSE;
    volatileSelection = NO_REGION;
    updateHudCamera();
    updatePartMaterials();
}

CameraControlGizmo::~CameraControlGizmo()
{
    for ( long i = 0; i < parts.size(); i++ ) {
        delete parts[i];
    }
    delete[] edgeSegments;
}

//= Model construction ====================================================

void CameraControlGizmo::band(int v, double out[2])
{
    double c = 2 * CORNER_FRACTION;
    double g = GAP / 2;

    switch ( v ) {
      case -1:
        out[0] = -1;
        out[1] = -1 + c - g;
        break;
      case 1:
        out[0] = 1 - c + g;
        out[1] = 1;
        break;
      default:
        out[0] = -1 + c + g;
        out[1] = 1 - c - g;
        break;
    }
}

void CameraControlGizmo::buildModel()
{
    int region = 0;
    int i;
    int j;
    int k;

    for ( i = -1; i <= 1; i++ ) {
        for ( j = -1; j <= 1; j++ ) {
            for ( k = -1; k <= 1; k++ ) {
                if ( i == 0 && j == 0 && k == 0 ) {
                    continue;
                }
                regionDirections[region][0] = i;
                regionDirections[region][1] = j;
                regionDirections[region][2] = k;
                double bx[2];
                double by[2];
                double bz[2];
                band(i, bx);
                band(j, by);
                band(k, bz);
                // Centered boxes, placed by the position of the part
                // (renderers tessellate boxes centered at the origin)
                Vector3Dd half((bx[1] - bx[0]) / 2,
                    (by[1] - by[0]) / 2, (bz[1] - bz[0]) / 2);
                Box* box = new Box(half.multiply(-1), half);
                parts.add(createPart(box, Vector3Dd(
                    (bx[0] + bx[1]) / 2, (by[0] + by[1]) / 2,
                    (bz[0] + bz[1]) / 2), regionName(region)));
                region++;
            }
        }
    }

    Tube* compass = new Tube(COMPASS_INTERNAL_RADIUS,
        COMPASS_EXTERNAL_RADIUS, COMPASS_HEIGHT);
    parts.add(createPart(compass, Vector3Dd(0, 0, COMPASS_BASE_Z),
        "Compass"));
}

SimpleBody* CameraControlGizmo::createPart(Geometry* geometry,
    const Vector3Dd& position, const String& name)
{
    SimpleBody* body = new SimpleBody();

    body->setGeometry(geometry);
    body->setPosition(position);
    body->setRotation(Matrix4x4d());
    body->setRotationInverse(Matrix4x4d());
    body->setScale(Vector3Dd(1, 1, 1));
    body->setMaterial(new SimpleMaterial());
    body->setName(name);
    return body;
}

//= Model queries =========================================================

const ArrayList<SimpleBody*>& CameraControlGizmo::getParts() const
{
    return parts;
}

PolyhedralBoundedSolid* CameraControlGizmo::exportPartToPolyhedralBoundedSolid(
    int part) const
{
    SimpleBody* body = parts[part];
    PolyhedralBoundedSolid* brep;
    Geometry* geometry = body->getGeometry();

    if ( geometry != NULL && geometry->instanceOf(Tube::CLASS) ) {
        Tube* tube = (Tube*)geometry;
        brep = tube->exportToPolyhedralBoundedSolid((int)COMPASS_SLICES);
    }
    else {
        brep = ((Volume*)geometry)->exportToPolyhedralBoundedSolid();
    }
    return brep;
}

// Port note: the array is created and filled here, not in
// getEdgeSegments, as gcc 2.7.2 has an internal compiler error with a member
// that stores and tests a `new T[]` in the same function
ArrayList<float>* CameraControlGizmo::createEdgeSegments()
{
    ArrayList<float>* segments = new ArrayList<float>[NUMBER_OF_PARTS];

    for ( int p = 0; p < NUMBER_OF_PARTS; p++ ) {
        PolyhedralBoundedSolid* brep = exportPartToPolyhedralBoundedSolid(p);
        Vector3Dd offset = parts[p]->getPosition();
        long n = brep->getEdgesList().size();
        ArrayList<float>& positions = segments[p];
        for ( long e = 0; e < n; e++ ) {
            _PolyhedralBoundedSolidEdge* edge = brep->getEdgesList().get(e);
            Vector3Dd a = edge->rightHalf->startingVertex->position.add(offset);
            Vector3Dd b = edge->leftHalf->startingVertex->position.add(offset);
            positions.add((float)a.x());
            positions.add((float)a.y());
            positions.add((float)a.z());
            positions.add((float)b.x());
            positions.add((float)b.y());
            positions.add((float)b.z());
        }
        delete brep;
    }
    return segments;
}

ArrayList<float> CameraControlGizmo::getEdgeSegments(int part)
{
    if ( edgeSegments == NULL ) {
        edgeSegments = createEdgeSegments();
    }
    return edgeSegments[part];
}

void CameraControlGizmo::getRegionDirectionComponents(int region,
    int out[3]) const
{
    out[0] = regionDirections[region][0];
    out[1] = regionDirections[region][1];
    out[2] = regionDirections[region][2];
}

Vector3Dd CameraControlGizmo::getRegionDirection(int region) const
{
    const int* d = regionDirections[region];
    return Vector3Dd(d[0], d[1], d[2]).normalized();
}

int CameraControlGizmo::findRegion(int i, int j, int k) const
{
    for ( int r = 0; r < NUMBER_OF_CUBE_REGIONS; r++ ) {
        const int* d = regionDirections[r];
        if ( d[0] == i && d[1] == j && d[2] == k ) {
            return r;
        }
    }
    return NO_REGION;
}

int CameraControlGizmo::getRegionKind(int region) const
{
    if ( region == COMPASS_REGION ) {
        return KIND_COMPASS;
    }
    if ( region < 0 || region >= NUMBER_OF_CUBE_REGIONS ) {
        return KIND_NONE;
    }
    const int* d = regionDirections[region];
    int nonZero = abs(d[0]) + abs(d[1]) + abs(d[2]);
    switch ( nonZero ) {
      case 1:
        return KIND_FACE;
      case 2:
        return KIND_EDGE;
      default:
        return KIND_CORNER;
    }
}

String CameraControlGizmo::getFaceLabel(int region) const
{
    if ( getRegionKind(region) != KIND_FACE ) {
        return String();
    }
    const int* d = regionDirections[region];
    for ( int axis = 0; axis < 3; axis++ ) {
        if ( d[axis] != 0 ) {
            return String(FACE_LABELS[2 * axis + (d[axis] > 0 ? 1 : 0)]);
        }
    }
    return String();
}

String CameraControlGizmo::regionName(int region) const
{
    if ( region == COMPASS_REGION ) {
        return String("Compass");
    }
    if ( region < 0 || region >= NUMBER_OF_CUBE_REGIONS ) {
        return String("None");
    }
    const int* d = regionDirections[region];
    String sb;
    // Vertical first, as in "TOP FRONT RIGHT"
    static const int order[3] = {2, 1, 0};
    for ( int o = 0; o < 3; o++ ) {
        int axis = order[o];
        if ( d[axis] != 0 ) {
            if ( sb.length() > 0 ) {
                sb += " ";
            }
            sb += FACE_LABELS[2 * axis + (d[axis] > 0 ? 1 : 0)];
        }
    }
    return sb;
}

ArrayList<int> CameraControlGizmo::getFaceRegions() const
{
    ArrayList<int> faces;
    for ( int r = 0; r < NUMBER_OF_CUBE_REGIONS; r++ ) {
        if ( getRegionKind(r) == KIND_FACE ) {
            faces.add(r);
        }
    }
    return faces;
}

Vector3Dd CameraControlGizmo::getFaceLabelPosition(int region) const
{
    const int* d = regionDirections[region];
    return Vector3Dd(d[0], d[1], d[2]);
}

//= Views =================================================================

BOOLEAN CameraControlGizmo::createCameraRotation(const Vector3Dd& front,
    const Vector3Dd& upHint, Matrix4x4d& out)
{
    Vector3Dd f = front.normalized();
    Vector3Dd left = upHint.crossProduct(f);

    if ( left.length() < VSDK::EPSILON ) {
        return FALSE;
    }
    left = left.normalized();
    Vector3Dd up = f.crossProduct(left).normalized();

    out = Matrix4x4d()
        .withVal(0, 0, f.x()).withVal(0, 1, left.x()).withVal(0, 2, up.x())
        .withVal(1, 0, f.y()).withVal(1, 1, left.y()).withVal(1, 2, up.y())
        .withVal(2, 0, f.z()).withVal(2, 1, left.z()).withVal(2, 2, up.z());
    return TRUE;
}

BOOLEAN CameraControlGizmo::calculateViewRotation(int region,
    const Matrix4x4d& currentRotation, Matrix4x4d& out) const
{
    Vector3Dd front = getRegionDirection(region).multiply(-1);
    const int* d = regionDirections[region];

    if ( d[0] != 0 || d[1] != 0 ) {
        return createCameraRotation(front, Vector3Dd(0, 0, 1), out);
    }

    Vector3Dd currentFront(currentRotation.get(0, 0),
        currentRotation.get(1, 0), currentRotation.get(2, 0));
    Vector3Dd currentUp(currentRotation.get(0, 2),
        currentRotation.get(1, 2), currentRotation.get(2, 2));
    // Tilting down to look at the TOP, what was ahead goes up on the
    // screen; tilting up to look at the BOTTOM, it goes down
    Vector3Dd hint = currentUp.add(currentFront.multiply(d[2]));
    Vector3Dd candidates[4];
    candidates[0] = Vector3Dd(1, 0, 0);
    candidates[1] = Vector3Dd(-1, 0, 0);
    candidates[2] = Vector3Dd(0, 1, 0);
    candidates[3] = Vector3Dd(0, -1, 0);
    Vector3Dd best = candidates[2];
    double bestDot = Double::NEGATIVE_INFINITY;

    for ( int i = 0; i < 4; i++ ) {
        const Vector3Dd& c = candidates[i];
        double dot = c.dotProduct(hint);
        if ( dot > bestDot + VSDK::EPSILON ) {
            bestDot = dot;
            best = c;
        }
    }
    return createCameraRotation(front, best, out);
}

//= Presentation ==========================================================

void CameraControlGizmo::setCamera(Camera* camera)
{
    this->camera = camera;
    updateHudCamera();
}

Camera* CameraControlGizmo::getCamera() const
{
    return camera;
}

Camera* CameraControlGizmo::getHudCamera()
{
    return &hudCamera;
}

const Camera* CameraControlGizmo::getHudCamera() const
{
    return &hudCamera;
}

void CameraControlGizmo::updateHudCamera()
{
    Matrix4x4d rotation;
    if ( camera != NULL ) {
        rotation = camera->getRotation();
    }
    else {
        createCameraRotation(Vector3Dd(1, 1, -1), Vector3Dd(0, 0, 1),
            rotation);
    }
    Vector3Dd front = Vector3Dd(rotation.get(0, 0), rotation.get(1, 0),
        rotation.get(2, 0)).normalized();

    hudCamera.setRotation(rotation);
    hudCamera.setPosition(front.multiply(-HUD_CAMERA_DISTANCE));
    hudCamera.updateViewportResize(sizeInPixels, sizeInPixels);
    hudCamera.updateVectors();
}

void CameraControlGizmo::placeInViewport(int viewportSizeX, int titleAreaEndX,
    int titleAreaEndY, const ViewportElementScaler* scaler)
{
    if ( scaler != NULL ) {
        sizeInPixels = Math::max(16, scaler->scaleSize(baseSizeInPixels));
        marginInPixels = scaler->scaleSize(baseMarginInPixels);
    }
    else {
        sizeInPixels = baseSizeInPixels;
        marginInPixels = baseMarginInPixels;
    }
    hudX = viewportSizeX - marginInPixels - sizeInPixels;
    hudY = marginInPixels;
    if ( hudX < titleAreaEndX + marginInPixels ) {
        hudY = titleAreaEndY + marginInPixels;
    }
    updateHudCamera();
}

void CameraControlGizmo::placeInViewport(const Viewport* viewport,
    const ViewportElementScaler* scaler)
{
    camera = viewport->getActiveCamera();
    placeInViewport(viewport->getPixelSizeX(),
        viewport->getTitleAreaStartX() + viewport->getTitleAreaSizeX(),
        viewport->getTitleAreaStartY() + viewport->getTitleAreaSizeY(),
        scaler);
}

int CameraControlGizmo::getBaseSizeInPixels() const
{
    return baseSizeInPixels;
}

void CameraControlGizmo::setBaseSizeInPixels(int baseSizeInPixels)
{
    this->baseSizeInPixels = baseSizeInPixels;
}

int CameraControlGizmo::getSizeInPixels() const
{
    return sizeInPixels;
}

int CameraControlGizmo::getHudX() const
{
    return hudX;
}

int CameraControlGizmo::getHudY() const
{
    return hudY;
}

BOOLEAN CameraControlGizmo::containsPixel(int x, int y) const
{
    return visible && x >= hudX && x < hudX + sizeInPixels &&
        y >= hudY && y < hudY + sizeInPixels;
}

BOOLEAN CameraControlGizmo::isVisible() const
{
    return visible;
}

void CameraControlGizmo::setVisible(BOOLEAN visible)
{
    this->visible = visible;
}

void CameraControlGizmo::projectToViewport(const Vector3Dd& p,
    double out[2]) const
{
    Vector4Dd clip = hudCamera.calculateProjectionMatrix().multiply(Vector4Dd(p));
    Vector4Dd ndc = clip.dividedByW();

    out[0] = hudX + (ndc.x() + 1.0) / 2.0 * sizeInPixels;
    out[1] = hudY + (1.0 - ndc.y()) / 2.0 * sizeInPixels;
}

double CameraControlGizmo::faceVisibility(int region) const
{
    Vector3Dd towardsViewer = hudCamera.getFront().multiply(-1).normalized();
    return getRegionDirection(region).dotProduct(towardsViewer);
}

//= Picking ===============================================================

int CameraControlGizmo::pickRegion(int x, int y) const
{
    if ( !containsPixel(x, y) ) {
        return NO_REGION;
    }
    Ray ray = hudCamera.generateRay(x - hudX, y - hudY);
    return pickRegion(ray);
}

int CameraControlGizmo::pickRegion(const Ray& ray) const
{
    int nearest = NO_REGION;
    double nearestT = Double::POSITIVE_INFINITY;

    for ( int r = 0; r < parts.size(); r++ ) {
        RayHit hit(RayHit::DETAIL_NONE, TRUE);
        if ( parts[r]->doIntersectionFirstHit(ray, &hit) ) {
            double t = hit.getRay() != NULL ? hit.getRay()->getT() :
                hit.getHitDistance();
            if ( t < nearestT ) {
                nearestT = t;
                nearest = r;
            }
        }
    }
    return nearest;
}

double CameraControlGizmo::compassAngleAt(int x, int y,
    const Camera& viewCamera)
{
    Ray ray = viewCamera.generateRay(x, y);
    Vector3Dd d = ray.getDirection();
    if ( Math::abs(d.normalized().z()) < 0.1 ) {
        return Double::NaN;
    }
    double zc = COMPASS_BASE_Z + COMPASS_HEIGHT / 2;
    double t = (zc - ray.getOrigin().z()) / d.z();
    Vector3Dd p = ray.getOrigin().add(d.multiply(t));
    return atan2(p.y(), p.x());
}

//= State and its materials ===============================================

BOOLEAN CameraControlGizmo::isActive() const
{
    return active;
}

void CameraControlGizmo::setActive(BOOLEAN active)
{
    if ( this->active != active ) {
        this->active = active;
        updatePartMaterials();
    }
}

int CameraControlGizmo::getVolatileSelection() const
{
    return volatileSelection;
}

void CameraControlGizmo::setVolatileSelection(int region)
{
    if ( volatileSelection != region ) {
        volatileSelection = region;
        updatePartMaterials();
    }
}

double CameraControlGizmo::getInactiveOpacity() const
{
    return inactiveOpacity;
}

void CameraControlGizmo::setInactiveOpacity(double inactiveOpacity)
{
    this->inactiveOpacity = inactiveOpacity;
    updatePartMaterials();
}

double CameraControlGizmo::getOpacity() const
{
    return active ? 1.0 : inactiveOpacity;
}

ColorRgb CameraControlGizmo::getLineColor() const
{
    return lineColor;
}

ColorRgb CameraControlGizmo::getLabelColor() const
{
    return labelColor;
}

ColorRgb CameraControlGizmo::getHighlightColor() const
{
    return highlightColor;
}

ColorRgb CameraControlGizmo::getPartColor(int region) const
{
    if ( region == volatileSelection ) {
        return highlightColor;
    }
    switch ( getRegionKind(region) ) {
      case KIND_FACE:
        return faceColor;
      case KIND_EDGE:
        return edgeColor;
      case KIND_CORNER:
        return cornerColor;
      default:
        return compassColor;
    }
}

void CameraControlGizmo::updatePartMaterials()
{
    double opacity = getOpacity();

    for ( int r = 0; r < parts.size(); r++ ) {
        ColorRgb c = getPartColor(r);
        SimpleMaterial m = SimpleMaterial()
            .withAmbient(c.multiply(0.45))
            .withDiffuse(c)
            .withSpecular(ColorRgb(0.15, 0.15, 0.15))
            .withPhongExponent(20)
            .withOpacity(opacity);
        parts[r]->setMaterial(new SimpleMaterial(m));
    }
}

const Class CameraControlGizmo::CLASS = { "CameraControlGizmo", &Gizmo::CLASS };

const Class *
CameraControlGizmo::getClass() const
{
    return &CLASS;
}

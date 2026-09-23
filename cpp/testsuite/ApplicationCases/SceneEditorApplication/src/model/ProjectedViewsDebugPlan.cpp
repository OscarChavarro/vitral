#include <cmath>

#include "vsdk/toolkit/environment/camera/Camera.h"
#include "model/ProjectedViewsDebugPlan.h"

namespace {
double toRadians(double degrees)
{
    return degrees * M_PI / 180.0;
}
}

Matrix4x4d ProjectedViewsDebugPlan::rotation(double degrees, double x,
                                             double y, double z)
{
    return Matrix4x4d().axisRotation(toRadians(degrees), Vector3Dd(x, y, z));
}

Matrix4x4d ProjectedViewsDebugPlan::composed(const Matrix4x4d& first,
                                             const Matrix4x4d& second)
{
    return second.multiply(first);
}

Vector3Dd ProjectedViewsDebugPlan::diagonalPosition(double x, double y,
                                                    double z)
{
    return Vector3Dd(x, y, z).normalized().multiply(1.5);
}

bool ProjectedViewsDebugPlan::getPlacement(int view,
                                           ViewPlacement* outPlacement)
{
    Vector3Dd unit(1, 1, 1);
    Vector3Dd half(0.5, 0.5, 0.5);

    switch ( view ) {
      case 1:
        *outPlacement = ViewPlacement(Vector3Dd(0, -2, 0), unit,
            rotation(90, 1, 0, 0));
        return true;
      case 2:
        *outPlacement = ViewPlacement(Vector3Dd(-2, 0, 0), unit,
            composed(rotation(90, 0, 0, -1), rotation(90, 0, -1, 0)));
        return true;
      case 3:
        *outPlacement = ViewPlacement(Vector3Dd(0, 0, -2), unit,
            rotation(180, 0, 1, 0));
        return true;
      case 4:
        *outPlacement = ViewPlacement(diagonalPosition(-1, -1, 1), half,
            composed(rotation(45, 0, 0, -1), rotation(35, 1, -1, 0)));
        return true;
      case 5:
        *outPlacement = ViewPlacement(diagonalPosition(1, -1, 1), half,
            composed(rotation(45, 0, 0, 1), rotation(35, 1, 1, 0)));
        return true;
      case 6:
        *outPlacement = ViewPlacement(diagonalPosition(1, 1, 1), half,
            composed(rotation(135, 0, 0, 1), rotation(35, -1, 1, 0)));
        return true;
      case 7:
        *outPlacement = ViewPlacement(diagonalPosition(-1, 1, 1), half,
            composed(rotation(135, 0, 0, -1), rotation(35, -1, -1, 0)));
        return true;
      case 8:
        *outPlacement = ViewPlacement(diagonalPosition(0, 1, -1), half,
            composed(rotation(180, 0, 0, 1), rotation(135, -1, 0, 0)));
        return true;
      case 9:
        *outPlacement = ViewPlacement(diagonalPosition(-1, 0, -1), half,
            composed(rotation(90, 0, 0, -1), rotation(135, 0, -1, 0)));
        return true;
      case 10:
        *outPlacement = ViewPlacement(diagonalPosition(0, -1, -1), half,
            rotation(135, 1, 0, 0));
        return true;
      case 11:
        *outPlacement = ViewPlacement(diagonalPosition(1, 0, -1), half,
            composed(rotation(90, 0, 0, 1), rotation(135, 0, 1, 0)));
        return true;
      case 12:
        *outPlacement = ViewPlacement(diagonalPosition(1, -1, 0), half,
            composed(rotation(90, 1, 0, 0), rotation(45, 0, 0, 1)));
        return true;
      case 13:
        *outPlacement = ViewPlacement(diagonalPosition(1, 1, 0), half,
            composed(rotation(90, 1, 0, 0), rotation(135, 0, 0, 1)));
        return true;
      default:
        return false;
    }
}

Camera* ProjectedViewsDebugPlan::createCamera(int view)
{
    Camera* camera = new Camera();
    camera->setFov(90);
    camera->setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
    camera->setNearPlaneDistance(2);
    camera->setFarPlaneDistance(20);

    Vector3Dd position(0, 0, 0);
    Matrix4x4d R;
    double cornerAngle;

    Vector3Dd down(0, 0, -1);
    Vector3Dd cornerReference(10, 10, -10);
    cornerReference = cornerReference.normalized();
    cornerAngle = (M_PI/2-(std::acos(down.dotProduct(cornerReference))));
    switch ( view ) {
      case 1:
        position = Vector3Dd(0, -10, 0);
        R = R.eulerAnglesRotation(toRadians(90), 0, 0);
        break;
      case 2:
        position = Vector3Dd(-10, 0, 0);
        break;
      case 3:
        position = Vector3Dd(0, 0, -10);
        R = R.eulerAnglesRotation(toRadians(-90), toRadians(90), 0);
        break;
      case 4:
        position = Vector3Dd(-10, -10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(45), -cornerAngle, 0);
        break;
      case 5:
        position = Vector3Dd(10, -10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(135), -cornerAngle, 0);
        break;
      case 6:
        position = Vector3Dd(10, 10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(-135), -cornerAngle, 0);
        break;
      case 7:
        position = Vector3Dd(-10, 10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(-45), -cornerAngle, 0);
        break;
      case 8:
        position = Vector3Dd(0, 10, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(-90), toRadians(45), 0);
        break;
      case 9:
        position = Vector3Dd(-10, 0, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(0), toRadians(45), 0);
        break;
      case 10:
        position = Vector3Dd(0, -10, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(90), toRadians(45), 0);
        break;
      case 11:
        position = Vector3Dd(10, 0, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(180), toRadians(45), 0);
        break;
      case 12:
        position = Vector3Dd(10, -10, 0).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(135), 0, 0);
        break;
      case 13:
        position = Vector3Dd(10, 10, 0).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(180+45), 0, 0);
        break;
      default:
        break;
    }
    camera->setPosition(position);
    camera->setRotation(R);
    return camera;
}

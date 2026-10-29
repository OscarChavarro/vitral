#include <math.h>
#include <stdio.h>
#include <string.h>

#include "java/io/File.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#ifndef TURBO_C_8086_REAL_MODE_ENABLED
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#endif
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/AmbientLight.h"
#include "vsdk/toolkit/environment/light/DirectionalLight.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
ReaderMitScene::ReaderMitScene()
{
}

static String trimLine(const String& in)
{
    size_t a = 0;
    while ( a < (size_t)in.size() && (in[a] == ' ' || in[a] == '\t' || in[a] == '\r' || in[a] == '\n') ) a++;
    size_t b = in.size();
    while ( b > a && (in[b-1] == ' ' || in[b-1] == '\t' || in[b-1] == '\r' || in[b-1] == '\n') ) b--;
    return in.substr(a, b - a);
}

void ReaderMitScene::importEnvironment(const char* fileName, SimpleScene* outScene)
{
    if ( outScene == 0 ) {
        Logger::reportMessage("ReaderMitScene", Logger::ERROR, "importEnvironment", "outScene is null");
        throw VSDKFatalException("ReaderMitScene::importEnvironment: outScene is null");
    }

    const String platformName = File::toPlatformName(fileName);
    FILE* file = fopen(platformName.toCString(), "r");
    if ( !file ) {
        Logger::reportMessage("ReaderMitScene", Logger::ERROR, "importEnvironment", "could not open file");
        throw VSDKFatalException("ReaderMitScene::importEnvironment: could not open file");
    }

    outScene->clearOwnedElements();

    Camera* camera =
        new Camera();
    SimpleBackground* background = new SimpleBackground();
    outScene->addCamera(camera);
    outScene->addBackground(background);
    outScene->setActiveCameraIndex(0);
    outScene->setActiveBackgroundIndex(0);

    Vector3Dd eye = Vector3Dd(0, 0, 10);
    Vector3Dd up = Vector3Dd(0, 1, 0);
    Vector3Dd lookat = camera->getFocusedPosition();
    BOOLEAN hasEye = FALSE;
    BOOLEAN hasUp = FALSE;
    BOOLEAN hasLookat = FALSE;
    BOOLEAN hasViewport = FALSE;
    int viewportXSize = 320;
    int viewportYSize = 240;
    double importedHorizontalFov = 30.0;

    SimpleMaterial* currentMaterial = new SimpleMaterial();
    Matrix4x4d currentRotation;

    char lineBuf[1024];
    while ( fgets(lineBuf, sizeof(lineBuf), file) ) {
        // Remove comment
        char* sharp = strchr(lineBuf, '#');
        if ( sharp ) {
            *sharp = '\0';
        }
        String javaLine(lineBuf);
        javaLine = trimLine(javaLine);
        if ( javaLine.empty() ) {
            continue;
        }

        char cmdStr[256] = {0};
        sscanf(javaLine.toCString(), "%255s", cmdStr);
        String cmd(cmdStr);

        if ( cmd == "viewport" ) {
            int w, h;
            if ( sscanf(javaLine.toCString(), "%*s %d %d", &w, &h) == 2 ) {
                viewportXSize = w;
                viewportYSize = h;
                hasViewport = TRUE;
            }
        }
        else if ( cmd == "eye" ) {
            double x, y, z;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                eye = Vector3Dd(x, y, z);
                hasEye = TRUE;
            }
        }
        else if ( cmd == "up" ) {
            double x, y, z;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                up = Vector3Dd(x, y, z);
                hasUp = TRUE;
            }
        }
        else if ( cmd == "lookat" ) {
            double x, y, z;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                lookat = Vector3Dd(x, y, z);
                hasLookat = TRUE;
            }
        }
        else if ( cmd == "fov" ) {
            double fov;
            if ( sscanf(javaLine.toCString(), "%*s %lf", &fov) == 1 ) {
                importedHorizontalFov = fov;
            }
        }
        else if ( cmd == "background" ) {
            double r, g, b;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &r, &g, &b) == 3 ) {
                background->setColor(r, g, b);
            }
        }
        else if ( cmd == "surface" ) {
            double r, g, b;
            double ka, kd, ks;
            double ns;
            double kr, kt, index;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        &r, &g, &b, &ka, &kd, &ks, &ns, &kr, &kt, &index) == 10 ) {
                delete currentMaterial;
                currentMaterial = new SimpleMaterial(
                    "surface",
                    ColorRgb(r * ka, g * ka, b * ka),
                    ColorRgb(r * kd, g * kd, b * kd),
                    ColorRgb(ks, ks, ks),
                    TRUE,
                    kr,
                    kt,
                    1.0,
                    ns);
            }
        }
        else if ( cmd == "rotation" ) {
            double yaw, pitch, roll;
            if (sscanf(javaLine.toCString(), "%*s %lf %lf %lf", &yaw, &pitch, &roll) == 3)
                currentRotation = Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
        }
#ifndef TURBO_C_8086_REAL_MODE_ENABLED
        else if ( cmd == "appel" ) {
            int model;
            double x, y, z, scale;
            if (sscanf(javaLine.toCString(), "%*s %d %lf %lf %lf %lf",
                       &model, &x, &y, &z, &scale) != 5 || model < 1 || model > 3) {
                fclose(file);
                throw VSDKFatalException("ReaderMitScene: invalid Appel model");
            }
            printf("Building PolyhedralBoundedSolid: Appel %d...\n", model);
            PolyhedralBoundedSolid* solid = model == 1 ?
                SimpleTestGeometryLibrary::createTestObjectAPPE1967_1() : model == 2 ?
                SimpleTestGeometryLibrary::createTestObjectAPPE1967_2() :
                SimpleTestGeometryLibrary::createTestObjectAPPE1967_3();
            if (!solid) {
                fclose(file);
                throw VSDKFatalException("ReaderMitScene: Appel construction failed");
            }
            SimpleBody* body = new SimpleBody();
            body->setGeometry(solid);
            body->setPosition(Vector3Dd(x, y, z));
            body->setScale(Vector3Dd(scale, scale, scale));
            body->setRotation(currentRotation);
            body->setMaterial(new SimpleMaterial(*currentMaterial));
            outScene->addBody(body);
            printf("BREP loaded: %ld vertices, %ld edges, %ld faces.\n",
                solid->getVerticesList().size(), solid->getEdgesList().size(),
                solid->getPolygonsList().size());
        }
#endif
        else if ( cmd == "sphere" ) {
            double x, y, z, r;
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf %lf", &x, &y, &z, &r) == 4 ) {
                SimpleBody* b = new SimpleBody();
                b->setGeometry(new Sphere(r));
                b->setPosition(Vector3Dd(x, y, z));
                b->setMaterial(new SimpleMaterial(*currentMaterial));
                outScene->addBody(b);
            }
        }
        else if ( cmd == "light" ) {
            double r, g, b;
            char typeStr[256] = {0};
            if ( sscanf(javaLine.toCString(), "%*s %lf %lf %lf %255s", &r, &g, &b, typeStr) == 4 ) {
                String type(typeStr);
                if ( type == "ambient" ) {
                    outScene->addLight(new AmbientLight(ColorRgb(r,g,b)));
                }
                else if ( type == "point" ) {
                    double x, y, z;
                    if ( sscanf(javaLine.toCString(), "%*s %*f %*f %*f %*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                        outScene->addLight(new PointLight(Vector3Dd(x,y,z), ColorRgb(r,g,b)));
                    }
                }
                else if ( type == "directional" ) {
                    double x, y, z;
                    if ( sscanf(javaLine.toCString(), "%*s %*f %*f %*f %*s %lf %lf %lf", &x, &y, &z) == 3 ) {
                        outScene->addLight(new DirectionalLight(Vector3Dd(x,y,z), ColorRgb(r,g,b)));
                    }
                }
            }
        }
#ifdef TURBO_C_8086_REAL_MODE_ENABLED
        // BREP/model commands are deliberately ignored in the DOS reader.
        // The raytracer supports analytic spheres only, and not constructing
        // a PolyhedralBoundedSolid keeps the executable within DOS limits.
        else if ( cmd == "brep" || cmd == "model" || cmd == "object" ) {
            continue;
        }
#endif
    }

    fclose(file);

    if ( hasEye ) {
        camera->setPosition(eye);
    }
    camera->setUpDirect(up);
    if ( hasLookat ) {
        camera->setFocusedPositionMaintainingOrthogonality(lookat);
    }

    if ( viewportXSize > 0 && viewportYSize > 0 ) {
        double aspect = ((double)viewportXSize) / ((double)viewportYSize);
        double horizontalHalfAngle = importedHorizontalFov * M_PI / 360.0;
        double verticalHalfAngle = atan(tan(horizontalHalfAngle) / aspect);
        double verticalFov = (verticalHalfAngle * 360.0) / M_PI;
        camera->setFov(verticalFov);
    }
    if ( hasViewport ) {
        camera->updateViewportResize(viewportXSize, viewportYSize);
    }
    else {
        camera->updateVectors();
    }

    delete currentMaterial;
}

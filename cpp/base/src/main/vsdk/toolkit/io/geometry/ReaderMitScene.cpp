#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "java/lang/String.h"

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "java/lang/String.h"

#include <sstream>
#include "java/lang/String.h"
#include <stdexcept>
#include "java/lang/String.h"
#include <cmath>
#include "java/lang/String.h"

ReaderMitScene::ReaderMitScene()
{
}

static java::String trimLine(const java::String& in)
{
    size_t a = 0;
    while ( a < in.size() && (in[a] == ' ' || in[a] == '\t' || in[a] == '\r' || in[a] == '\n') ) a++;
    size_t b = in.size();
    while ( b > a && (in[b-1] == ' ' || in[b-1] == '\t' || in[b-1] == '\r' || in[b-1] == '\n') ) b--;
    return in.substr(a, b - a);
}

void ReaderMitScene::importEnvironment(std::istream& is, SimpleScene* outScene)
{
    if ( outScene == 0 ) {
        throw std::runtime_error("ReaderMitScene::importEnvironment: outScene is null");
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
    bool hasEye = false;
    bool hasUp = false;
    bool hasLookat = false;
    bool hasViewport = false;
    int viewportXSize = 320;
    int viewportYSize = 240;
    double importedHorizontalFov = 30.0;

    SimpleMaterial* currentMaterial = new SimpleMaterial();

    std::string line;
    while ( std::getline(is, line) ) {
        size_t sharp = line.find('#');
        if ( sharp != std::string::npos ) {
            line = line.substr(0, sharp);
        }
        java::String javaLine(line.c_str());
        javaLine = trimLine(javaLine);
        if ( javaLine.empty() ) {
            continue;
        }

        std::istringstream ss(javaLine.toCString());
        std::string cmdStr;
        ss >> cmdStr;
        java::String cmd(cmdStr.c_str());

        if ( cmd == "viewport" ) {
            int w, h;
            if ( ss >> w >> h ) {
                viewportXSize = w;
                viewportYSize = h;
                hasViewport = true;
            }
        }
        else if ( cmd == "eye" ) {
            double x, y, z;
            if ( ss >> x >> y >> z ) {
                eye = Vector3Dd(x, y, z);
                hasEye = true;
            }
        }
        else if ( cmd == "up" ) {
            double x, y, z;
            if ( ss >> x >> y >> z ) {
                up = Vector3Dd(x, y, z);
                hasUp = true;
            }
        }
        else if ( cmd == "lookat" ) {
            double x, y, z;
            if ( ss >> x >> y >> z ) {
                lookat = Vector3Dd(x, y, z);
                hasLookat = true;
            }
        }
        else if ( cmd == "fov" ) {
            double fov;
            if ( ss >> fov ) {
                importedHorizontalFov = fov;
            }
        }
        else if ( cmd == "background" ) {
            double r, g, b;
            if ( ss >> r >> g >> b ) {
                background->setColor(r, g, b);
            }
        }
        else if ( cmd == "surface" ) {
            double r, g, b;
            double ka, kd, ks;
            double ns;
            double kr, kt, index;
            if ( ss >> r >> g >> b >> ka >> kd >> ks >> ns >> kr >> kt >> index ) {
                delete currentMaterial;
                currentMaterial = new SimpleMaterial(
                    "surface",
                    ColorRgb(r * ka, g * ka, b * ka),
                    ColorRgb(r * kd, g * kd, b * kd),
                    ColorRgb(ks, ks, ks),
                    true,
                    kr,
                    kt,
                    1.0,
                    ns);
            }
        }
        else if ( cmd == "sphere" ) {
            double x, y, z, r;
            if ( ss >> x >> y >> z >> r ) {
                SimpleBody* b = new SimpleBody();
                b->setGeometry(new Sphere(r));
                b->setPosition(Vector3Dd(x, y, z));
                b->setMaterial(new SimpleMaterial(*currentMaterial));
                outScene->addBody(b);
            }
        }
        else if ( cmd == "light" ) {
            double r, g, b;
            std::string typeStr;
            if ( !(ss >> r >> g >> b >> typeStr) ) {
                continue;
            }
            java::String type(typeStr.c_str());
            if ( type == "ambient" ) {
                outScene->addLight(new Light(LightType::AMBIENT, Vector3Dd(0,0,0), ColorRgb(r,g,b)));
            }
            else if ( type == "point" ) {
                double x, y, z;
                if ( ss >> x >> y >> z ) {
                    outScene->addLight(new Light(LightType::POINT, Vector3Dd(x,y,z), ColorRgb(r,g,b)));
                }
            }
            else if ( type == "directional" ) {
                double x, y, z;
                if ( ss >> x >> y >> z ) {
                    outScene->addLight(new Light(LightType::DIRECTIONAL, Vector3Dd(x,y,z), ColorRgb(r,g,b)));
                }
            }
        }
    }

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
        double verticalHalfAngle = std::atan(std::tan(horizontalHalfAngle) / aspect);
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

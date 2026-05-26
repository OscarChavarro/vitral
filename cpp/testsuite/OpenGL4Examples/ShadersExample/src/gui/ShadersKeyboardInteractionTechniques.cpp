#include "ShadersKeyboardInteractionTechniques.h"
#include "../model/ShadersModel.h"
#include <java/lang/Math.h>

bool ShadersKeyboardInteractionTechniques::processPressed(const KeyEvent& event, ShadersModel* model, Actions* actions)
{
    if (model == 0) return false;
    if (event.keycode == KeyEvent::KEY_ESC) {
        if (actions) actions->requestExit();
        return false;
    }

    bool repaint = false;
    if (model->cameraController->processKeyPressedEvent(event)) repaint = true;
    if (model->qualityController->processKeyPressedEvent(event)) repaint = true;

    Vector3Dd lp = model->light->getPosition();
    switch (event.keycode) {
        case KeyEvent::KEY_h:
        case KeyEvent::KEY_H: model->toggleShowHud(); repaint = true; break;
        case KeyEvent::KEY_l:
        case KeyEvent::KEY_L: model->light->setPosition(lp.withX(lp.x() - 0.1)); repaint = true; break;
        case KeyEvent::KEY_k:
        case KeyEvent::KEY_K: model->light->setPosition(lp.withX(lp.x() + 0.1)); repaint = true; break;
        case KeyEvent::KEY_j:
        case KeyEvent::KEY_J: model->light->setPosition(lp.withZ(lp.z() - 0.1)); repaint = true; break;
        case KeyEvent::KEY_u:
        case KeyEvent::KEY_U: model->light->setPosition(lp.withZ(lp.z() + 0.1)); repaint = true; break;
        case KeyEvent::KEY_9: model->light->setPosition(lp.withY(lp.y() - 0.1)); repaint = true; break;
        case KeyEvent::KEY_0: model->light->setPosition(lp.withY(lp.y() + 0.1)); repaint = true; break;
        case KeyEvent::KEY_g:
        case KeyEvent::KEY_G: model->quality.setShadingType(RendererConfiguration::SHADING_TYPE_GOURAUD); repaint = true; break;
        case KeyEvent::KEY_p:
        case KeyEvent::KEY_P: model->quality.setShadingType(RendererConfiguration::SHADING_TYPE_PHONG); repaint = true; break;
        case KeyEvent::KEY_n:
        case KeyEvent::KEY_N: model->quality.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT); repaint = true; break;
        case KeyEvent::KEY_t:
        case KeyEvent::KEY_T: model->quality.changeTexture(); repaint = true; break;
        case KeyEvent::KEY_b:
        case KeyEvent::KEY_B: model->quality.changeBumpMap(); repaint = true; break;
        case KeyEvent::KEY_m:
        case KeyEvent::KEY_M: model->cycleCookTorranceMaterial(); repaint = true; break;
        case KeyEvent::KEY_q: model->changeSphereMeridians(-1); repaint = true; break;
        case KeyEvent::KEY_Q: model->changeSphereMeridians(1); repaint = true; break;
        case KeyEvent::KEY_w: model->changeSphereParallels(-1); repaint = true; break;
        case KeyEvent::KEY_W: model->changeSphereParallels(1); repaint = true; break;
        case KeyEvent::KEY_r:
        case KeyEvent::KEY_R: model->toggleAnimationEnabled(); if (actions) actions->animationStateChanged(); repaint = true; break;
        case KeyEvent::KEY_SPACE: model->toggleLightAnimationEnabled(); if (actions) actions->animationStateChanged(); repaint = true; break;
        case KeyEvent::KEY_PERIOD: model->rotateRenderingMode(); repaint = true; break;
    }
    return repaint;
}

bool ShadersKeyboardInteractionTechniques::processReleased(const KeyEvent& event, ShadersModel* model)
{
    if (!model) return false;
    return model->cameraController->processKeyReleasedEvent(event);
}

bool ShadersKeyboardInteractionTechniques::processPressedForApp(
    const KeyEvent& event,
    CameraControllerAquynza* cameraController,
    RendererConfigurationController* qualityController,
    Light* light,
    RendererConfiguration* quality,
    int* meridians,
    int* parallels,
    bool* showHud,
    bool* animationEnabled,
    bool* lightAnimationEnabled,
    ShaderOperationMode* renderingMode,
    Actions* actions)
{
    if (event.keycode == KeyEvent::KEY_ESC) {
        if (actions) actions->requestExit();
        return false;
    }

    bool repaint = false;
    if (cameraController && cameraController->processKeyPressedEvent(event)) repaint = true;
    if (qualityController && qualityController->processKeyPressedEvent(event)) repaint = true;

    Vector3Dd lp = light ? light->getPosition() : Vector3Dd(0, 0, 0);
    switch (event.keycode) {
        case KeyEvent::KEY_h:
        case KeyEvent::KEY_H: if (showHud) *showHud = !*showHud; repaint = true; break;
        case KeyEvent::KEY_l:
        case KeyEvent::KEY_L: if (light) light->setPosition(lp.withX(lp.x() - 0.1)); repaint = true; break;
        case KeyEvent::KEY_k:
        case KeyEvent::KEY_K: if (light) light->setPosition(lp.withX(lp.x() + 0.1)); repaint = true; break;
        case KeyEvent::KEY_j:
        case KeyEvent::KEY_J: if (light) light->setPosition(lp.withZ(lp.z() - 0.1)); repaint = true; break;
        case KeyEvent::KEY_u:
        case KeyEvent::KEY_U: if (light) light->setPosition(lp.withZ(lp.z() + 0.1)); repaint = true; break;
        case KeyEvent::KEY_9: if (light) light->setPosition(lp.withY(lp.y() - 0.1)); repaint = true; break;
        case KeyEvent::KEY_0: if (light) light->setPosition(lp.withY(lp.y() + 0.1)); repaint = true; break;
        case KeyEvent::KEY_g:
        case KeyEvent::KEY_G: if (quality) quality->setShadingType(RendererConfiguration::SHADING_TYPE_GOURAUD); repaint = true; break;
        case KeyEvent::KEY_p:
        case KeyEvent::KEY_P: if (quality) quality->setShadingType(RendererConfiguration::SHADING_TYPE_PHONG); repaint = true; break;
        case KeyEvent::KEY_n:
        case KeyEvent::KEY_N: if (quality) quality->setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT); repaint = true; break;
        case KeyEvent::KEY_t:
        case KeyEvent::KEY_T: if (quality) quality->changeTexture(); repaint = true; break;
        case KeyEvent::KEY_b:
        case KeyEvent::KEY_B: if (quality) quality->changeBumpMap(); repaint = true; break;
        case KeyEvent::KEY_q: if (meridians) *meridians = java::Math::max(12, *meridians - 1); repaint = true; break;
        case KeyEvent::KEY_Q: if (meridians) *meridians = *meridians + 1; repaint = true; break;
        case KeyEvent::KEY_w: if (parallels) *parallels = java::Math::max(8, *parallels - 1); repaint = true; break;
        case KeyEvent::KEY_W: if (parallels) *parallels = *parallels + 1; repaint = true; break;
        case KeyEvent::KEY_r:
        case KeyEvent::KEY_R:
            if (animationEnabled) *animationEnabled = !*animationEnabled;
            if (actions) actions->animationStateChanged();
            repaint = true;
            break;
        case KeyEvent::KEY_SPACE:
            if (lightAnimationEnabled) *lightAnimationEnabled = !*lightAnimationEnabled;
            if (actions) actions->animationStateChanged();
            repaint = true;
            break;
        case KeyEvent::KEY_PERIOD:
            if (renderingMode) *renderingMode = nextShaderOperationMode(*renderingMode);
            repaint = true;
            break;
    }
    return repaint;
}

bool ShadersKeyboardInteractionTechniques::processReleasedForApp(
    const KeyEvent& event,
    CameraControllerAquynza* cameraController)
{
    if (!cameraController) return false;
    return cameraController->processKeyReleasedEvent(event);
}

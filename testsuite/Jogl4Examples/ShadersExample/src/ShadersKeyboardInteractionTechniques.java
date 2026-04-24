import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.gui.KeyEvent;

public class ShadersKeyboardInteractionTechniques
{
    public interface Actions
    {
        void requestExit();
        void reportQuality(RendererConfiguration quality);
        void animationToggled(boolean enabled);
    }

    public boolean processPressed(
        KeyEvent event,
        ShadersModel model,
        Actions actions)
    {
        if ( event.keycode == KeyEvent.KEY_ESC ) {
            actions.requestExit();
            return false;
        }

        Light light = model.getLight();
        RendererConfiguration quality = model.getQuality();

        boolean repaint = false;

        if ( model.getCameraController().processKeyPressedEvent(event) ) {
            repaint = true;
        }

        if ( model.getQualityController().processKeyPressedEvent(event) ) {
            actions.reportQuality(quality);
            repaint = true;
        }

        Vector3D lp = light.getPosition();
        switch ( event.keycode ) {
            case KeyEvent.KEY_h:
            case KeyEvent.KEY_H:
                light.setPosition(lp.withX(lp.x() - 0.1));
                repaint = true;
                break;
            case KeyEvent.KEY_k:
            case KeyEvent.KEY_K:
                light.setPosition(lp.withX(lp.x() + 0.1));
                repaint = true;
                break;
            case KeyEvent.KEY_j:
            case KeyEvent.KEY_J:
                light.setPosition(lp.withZ(lp.z() - 0.1));
                repaint = true;
                break;
            case KeyEvent.KEY_u:
            case KeyEvent.KEY_U:
                light.setPosition(lp.withZ(lp.z() + 0.1));
                repaint = true;
                break;
            case KeyEvent.KEY_9:
                light.setPosition(lp.withY(lp.y() - 0.1));
                repaint = true;
                break;
            case KeyEvent.KEY_0:
                light.setPosition(lp.withY(lp.y() + 0.1));
                repaint = true;
                break;
            case KeyEvent.KEY_g:
            case KeyEvent.KEY_G:
                quality.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);
                repaint = true;
                break;
            case KeyEvent.KEY_p:
            case KeyEvent.KEY_P:
                quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
                repaint = true;
                break;
            case KeyEvent.KEY_n:
            case KeyEvent.KEY_N:
                quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
                repaint = true;
                break;
            case KeyEvent.KEY_t:
            case KeyEvent.KEY_T:
                quality.changeTexture();
                repaint = true;
                break;
            case KeyEvent.KEY_b:
            case KeyEvent.KEY_B:
                quality.changeBumpMap();
                repaint = true;
                break;
            case KeyEvent.KEY_q:
                model.changeSphereMeridians(-1);
                repaint = true;
                break;
            case KeyEvent.KEY_Q:
                model.changeSphereMeridians(1);
                repaint = true;
                break;
            case KeyEvent.KEY_w:
                model.changeSphereParallels(-1);
                repaint = true;
                break;
            case KeyEvent.KEY_W:
                model.changeSphereParallels(1);
                repaint = true;
                break;
            case KeyEvent.KEY_SPACE:
                model.toggleAnimationEnabled();
                actions.animationToggled(model.isAnimationEnabled());
                repaint = true;
                break;
            default:
                break;
        }

        return repaint;
    }

    public boolean processReleased(KeyEvent event, ShadersModel model)
    {
        return model.getCameraController().processKeyReleasedEvent(event);
    }
}

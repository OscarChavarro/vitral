package vsdk.external.jogl.effects;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import vsdk.framework.render.*;

public abstract class JoglRenderService extends RenderService
{
    public abstract void initService(GL gl, GLU glu);

}

/*
 * JoglScene.java
 *
 * Created on 25 de agosto de 2005, 11:49 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package vitral.toolkits.visual.jogl;

import net.java.games.jogl.GL;
import net.java.games.jogl.GLU;

/**
 *
 * @author usuario
 */
public interface JoglScene 
{
    /** Creates a new instance of JoglScene */
    public void drawScene(GL gl, GLU glu);
    public void initScene(GL gl, GLU glu);
    public SceneController getSceneController();
}

/*
 * TestMesh.java
 *
 * Created on 13 de septiembre de 2005, 01:20 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package test.vsdk.toolkit.environment.geometry.mesh;

import vsdk.framework.presentation.PresentationException;
import vsdk.framework.presentation.frames.SimpleFrame;
import vsdk.framework.presentation.panels.PresentationPanel;

import vsdk.external.jogl.JoglFactory;

import java.awt.*;
/**
 *
 * @author usuario
 */
public class TestMesh
{
    public static void main(String args[]) throws PresentationException
    {
        ObjectImportScene ois=new ObjectImportScene();
        PresentationPanel principal=JoglFactory.createPresentationPanel();
        
        SimpleFrame sf=new SimpleFrame();
        sf.setSceneController(ois.getSceneController());
        sf.setPresentationPanel(principal);
                
        principal.getRenderService().setData(ois.getData());        
    }
}
//
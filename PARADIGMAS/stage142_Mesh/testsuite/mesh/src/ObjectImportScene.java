import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import net.java.games.jogl.*;
import vitral.toolkits.common.*;
import vitral.toolkits.environment.*;
import vitral.toolkits.geometry.*;
import vitral.toolkits.visual.jogl.*;

public class ObjectImportScene
    implements JoglScene, SceneController {
  private JPanel panelConfig = new JPanel();

  private JRadioButton wire = new JRadioButton("wire frame");
  private JRadioButton solid = new JRadioButton("solid");
  private JRadioButton smooth = new JRadioButton("smooth");
  private JRadioButton textured = new JRadioButton("textured");
  private JRadioButton bump = new JRadioButton("bump mapped");
  private ButtonGroup bg = new ButtonGroup();

  private Light light;

  private JCheckBox drawNormals = new JCheckBox("draw Pont normals");
  private JCheckBox drawFaceNormals = new JCheckBox("draw Face normals");
  private JCheckBox drawPoints = new JCheckBox("draw points");
  private JCheckBox drawBounding = new JCheckBox("draw bounding");

  private JMenuItem miCargar = new JMenuItem("cargar objeto");

  private MeshGroup objetos;

  /** Creates a new instance of ObjectImportScene */
  public ObjectImportScene() {
    light = new Light(Light.DIRECCIONAL, new Vector3D(0, -1, 1),
                      new ColorRgb(1, 1, 1));
    light.setAmbient(new ColorRgb(1, 1, 1));
    light.setDiffuse(new ColorRgb(1, 1, 1));
    light.setSpecular(new ColorRgb(1, 1, 1));

    //    objetos=new ArrayList<Mesh>();
    bg.add(wire);
    bg.add(solid);
    bg.add(smooth);
    bg.add(textured);
    bg.add(bump);

    JPanel pNorth = new JPanel();
    JPanel pSouth = new JPanel();

    pNorth.add(wire);
    pNorth.add(solid);
    pNorth.add(smooth);
    pNorth.add(textured);
    pNorth.add(bump);

    pSouth.add(drawPoints);
    pSouth.add(drawNormals);
    pSouth.add(drawFaceNormals);
    pSouth.add(drawBounding);

    panelConfig.setLayout(new BorderLayout());
    panelConfig.add(pNorth, BorderLayout.NORTH);
    panelConfig.add(pSouth, BorderLayout.SOUTH);

    miCargar.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileFilter() {
          public boolean accept(File f) {
            if (f.isDirectory()) {
              return true;
            }
            String fName = f.getName();
            StringTokenizer st = new StringTokenizer(fName, ".");
            int numTokens = st.countTokens();
            for (int i = 0; i < numTokens - 1; i++) {
              st.nextToken();
            }
            String ext = st.nextToken();
            if (ext.equalsIgnoreCase("obj")) {
              return true;
            }
            return false;
          }

          public String getDescription() {
            return "*.obj";
          }
        };

        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(panelConfig);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File selectedFile = chooser.getSelectedFile();
          /*     try
               {
           MeshGroup meshGroup objs = ReaderObj.read(selectedFile.getAbsolutePath());
                   for(int i=0; i<objs.size(); i++)
                   {
                       objetos.add(objs.get(i));
                   }
                   sortByAlpha();
               }
               catch(IOException ioe)
               {
                   JOptionPane.showMessageDialog(panelConfig, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
               }*/
        }
      }
    }
    );
  }

  private void sortByAlpha() {
    /*    for(int i=0; i<objetos.size(); i++)
        {
            for(int j=i; j<objetos.size(); j++)
            {
     if(objetos.get(i).material.getAlpha()<objetos.get(j).material.getAlpha())
                {
                    Mesh objAux=objetos.get(i);
                    objetos.set(i, objetos.get(j));
                    objetos.set(j, objAux);
                }
            }
        }*/
  }

  public void drawScene(GL gl, GLU glu) {
    /*     for(int i=0; i<objetos.size(); i++)
         {
             if(wire.isSelected())
             {
                 gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
                 {
                     gl.glDisable(gl.GL_LIGHTING);
                     gl.glDisable(gl.GL_DEPTH_TEST);
                     gl.glDisable(gl.GL_BLEND);
                     gl.glDepthMask(false);
                     gl.glColor3f(1,1,1);
                     JoglMeshRenderer.drawWires(gl, objetos.get(i));
                 }
                 gl.glPopAttrib();
             }
             else if(solid.isSelected())
             {
                 JoglMeshRenderer.drawSurfacesSolid(gl, objetos.get(i));
             }
             else if(smooth.isSelected())
             {
                 JoglMeshRenderer.drawSurfacesSmooth(gl, objetos.get(i));
             }
             else if(textured.isSelected())
             {
                 JoglMeshRenderer.drawTexture(gl, objetos.get(i));
             }

             if(drawPoints.isSelected())
             {
                 gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
                 {
                     gl.glDisable(gl.GL_LIGHTING);
                     gl.glDisable(gl.GL_DEPTH_TEST);
                     gl.glDisable(gl.GL_BLEND);
                     gl.glDepthMask(false);
                     gl.glColor3f(1,1,1);

                     JoglMeshRenderer.drawPoints(gl, objetos.get(i));
                 }
                 gl.glPopAttrib();
             }

             if(drawNormals.isSelected())
             {
                 gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
                 {
                     gl.glDisable(gl.GL_LIGHTING);
                     gl.glDisable(gl.GL_DEPTH_TEST);
                     gl.glDisable(gl.GL_BLEND);
                     gl.glDepthMask(false);
                     gl.glColor3f(0,1,0);

                     JoglMeshRenderer.drawNormals(gl, objetos.get(i));
                 }
                 gl.glPopAttrib();
             }

             if(drawFaceNormals.isSelected())
             {
                 gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
                 {
                     gl.glDisable(gl.GL_LIGHTING);
                     gl.glDisable(gl.GL_DEPTH_TEST);
                     gl.glDisable(gl.GL_BLEND);
                     gl.glDepthMask(false);
                     gl.glColor3f(1,0,0);

                     JoglMeshRenderer.drawNormalsTriangles(gl, objetos.get(i));
                 }
                 gl.glPopAttrib();
             }

             if(drawBounding.isSelected())
             {
                 gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
                 {
                     gl.glDisable(gl.GL_LIGHTING);
                     gl.glDisable(gl.GL_DEPTH_TEST);
                     gl.glDisable(gl.GL_BLEND);
                     gl.glDepthMask(false);
                     gl.glColor3f(0,0,1);

                     JoglMeshRenderer.drawBoundingVolume(gl, objetos.get(i));
                 }
                 gl.glPopAttrib();
             }
         }*/
  }

  public void initScene(GL gl, GLU glu) {
    gl.glEnable(gl.GL_BLEND);
    gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);

    gl.glEnable(gl.GL_LIGHTING);
    gl.glEnable(gl.GL_LIGHT0);

    JoglToolkitInterface.setLight(gl, light, gl.GL_LIGHT0);

    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    gl.glClearDepth(1.0);

    gl.glEnable(gl.GL_STENCIL_TEST);
    gl.glClearStencil(0x0);

    gl.glDepthFunc(gl.GL_LESS);

    gl.glEnable(gl.GL_DEPTH_TEST);

    gl.glShadeModel(gl.GL_SMOOTH);
  }

  public SceneController getSceneController() {
    return this;
  }

  public JPanel getControlPaneNorth() {
    return panelConfig;
  }

  public JPanel getControlPaneSouth() {
    return null;
  }

  public JPanel getControlPaneWest() {
    return null;
  }

  public JPanel getControlPaneEast() {
    return null;
  }

  public ArrayList<JMenuItem> getFileMenus() {
    ArrayList<JMenuItem> items = new ArrayList<JMenuItem> ();
    items.add(miCargar);
    return items;
  }

  public ArrayList<JMenuItem> getHelpMenus() {
    return null;
  }

  public ArrayList<JMenu> getMenus() {
    return null;
  }
}

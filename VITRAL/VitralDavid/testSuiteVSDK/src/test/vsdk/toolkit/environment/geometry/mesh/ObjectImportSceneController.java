package test.vsdk.toolkit.environment.geometry.mesh;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileFilter;

import vsdk.framework.presentation.util.LightControlFrame;
import vsdk.framework.presentation.util.SceneController;
import vsdk.framework.render.util.TrivialRenderizable;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.io.geometry.ReaderObj;

public class ObjectImportSceneController implements SceneController, ActionListener
{
    private JPanel panelConfig=new JPanel();
    
    private JRadioButton wire=new JRadioButton("wire frame");
    private JRadioButton solid=new JRadioButton("solid");
    private JRadioButton smooth=new JRadioButton("smooth");
    private JRadioButton texturedSolid=new JRadioButton("textured Solid");
    private JRadioButton textured=new JRadioButton("textured Smooth");
    private JRadioButton bump=new JRadioButton("bump mapped");
    private ButtonGroup bg=new ButtonGroup();
    
    private TrivialRenderizable objects;
    private QualitySelection quality;
    
    private Light light;
    private LightControlFrame contLuz;
    private JMenuItem miContLuz=new JMenuItem("configurar luz");
    private JFrame fContLuz;
            
    private JCheckBox drawNormals=new JCheckBox("draw Pont normals");
    private JCheckBox drawFaceNormals=new JCheckBox("draw Face normals");
    private JCheckBox drawPoints=new JCheckBox("draw points");
    private JCheckBox drawBounding=new JCheckBox("draw bounding");
    
    private JMenuItem miCargar=new JMenuItem("cargar objeto");
    
    public ObjectImportSceneController()
    {
        contLuz=new LightControlFrame(light);
        
        miContLuz.addActionListener
        (
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        contLuz.setVisible(true);
                    }
                }
        );
        
        wire.addActionListener(this);
        solid.addActionListener(this);
        smooth.addActionListener(this);
        texturedSolid.addActionListener(this);
        textured.addActionListener(this);
        bump.addActionListener(this);

        drawPoints.addActionListener(this);
        drawNormals.addActionListener(this);
        drawFaceNormals.addActionListener(this);
        drawBounding.addActionListener(this);
        
        bg.add(wire);
        bg.add(solid);
        bg.add(smooth);
        bg.add(texturedSolid);
        bg.add(textured);
        bg.add(bump);
        
        JPanel pNorth=new JPanel();
        JPanel pSouth=new JPanel();
        
        pNorth.add(wire);
        pNorth.add(solid);
        pNorth.add(smooth);
        pNorth.add(texturedSolid);
        pNorth.add(textured);
        pNorth.add(bump);
        
        pSouth.add(drawPoints);
        pSouth.add(drawNormals);
        pSouth.add(drawFaceNormals);
        pSouth.add(drawBounding);
        
        panelConfig.setLayout(new BorderLayout());
        panelConfig.add(pNorth, BorderLayout.NORTH);
        panelConfig.add(pSouth, BorderLayout.SOUTH);
        
        miCargar.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File("../etc"));
                FileFilter filter = new FileFilter()
                {
                    public boolean accept(File f)
                    {
                        if(f.isDirectory())
                        {
                            return true;
                        }
                        String fName=f.getName();
                        StringTokenizer st=new StringTokenizer(fName, ".");
                        int numTokens=st.countTokens();
                        for(int i=0; i<numTokens-1;i++)
                        {
                            st.nextToken();
                        }
                        String ext=st.nextToken();
                        if(ext.equalsIgnoreCase("obj"))
                        {
                            return true;
                        }
                        return false;
                    }
                    
                    public String getDescription()
                    {
                        return "*.obj";
                    }
                };
                
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(panelConfig);
                if(returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File selectedFile=chooser.getSelectedFile();
                    try
                    {
                        objects.addObject(ReaderObj.read(selectedFile.getAbsolutePath()));
                        //sortByAlpha();
                    }
                    catch(IOException ioe)
                    {
                        JOptionPane.showMessageDialog(panelConfig, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        );
    }
    
    public void setLight(Light l)
    {
        light=l;
        contLuz.setLight(l);
    }

    public void setTrivialRenderizable(TrivialRenderizable tr)
    {
        objects=tr;
    }
    
    public void setQualitySelection(QualitySelection q)
    {
        quality=q;
    }
    
    public JPanel getControlPaneNorth()
    {
        return panelConfig;
    }
     
    public JPanel getControlPaneSouth()
    {
        return null;
    }
     
    public JPanel getControlPaneWest()
    {
        return null;
    }
     
    public JPanel getControlPaneEast()
    {
        return null;
    }
     
    public ArrayList<JMenuItem> getFileMenus()
    {
        ArrayList<JMenuItem> items=new ArrayList<JMenuItem>();
        items.add(miCargar);
        items.add(miContLuz);
        return items;
    }
    
    public ArrayList<JMenuItem> getHelpMenus()
    {
        return null;
    }
    
    public ArrayList<JMenu> getMenus()
    {
        return null;
    }

    public void actionPerformed(ActionEvent arg0)
    {
        if(wire.isSelected())
        {
            quality.setWires(true);
            quality.setBumpMap(false);
            quality.setSurfaces(false);
            quality.setTexture(false);
            quality.setShadingType(quality.SHADING_TYPE_FLAT);
        }
        else if(solid.isSelected())
        {
            quality.setWires(false);
            quality.setBumpMap(false);
            quality.setSurfaces(true);
            quality.setTexture(false);
            quality.setShadingType(quality.SHADING_TYPE_FLAT);
        }
        else if(smooth.isSelected())
        {
            quality.setWires(false);
            quality.setBumpMap(false);
            quality.setSurfaces(true);
            quality.setTexture(false);
            quality.setShadingType(quality.SHADING_TYPE_PHONG);
        }
        else if(texturedSolid.isSelected())
        {
            quality.setWires(false);
            quality.setBumpMap(false);
            quality.setSurfaces(false);
            quality.setTexture(true);
            quality.setShadingType(quality.SHADING_TYPE_FLAT);
        }
        else if(textured.isSelected())
        {
            quality.setWires(false);
            quality.setBumpMap(false);
            quality.setSurfaces(false);
            quality.setTexture(true);
            quality.setShadingType(quality.SHADING_TYPE_PHONG);
        }
        else if(bump.isSelected())
        {
            quality.setWires(false);
            quality.setBumpMap(true);
            quality.setSurfaces(false);
            quality.setTexture(false);
            quality.setShadingType(quality.SHADING_TYPE_PHONG);
        }
        
        if(drawPoints.isSelected())
        {
            quality.setPoints(true);
        }
        else
        {
            quality.setPoints(false);
        }
        
        if(drawNormals.isSelected())
        {
            quality.setNormals(true);
        }
        else
        {
            quality.setNormals(false);
        }
        
        if(drawFaceNormals.isSelected())
        {
            quality.setTrianglesNormals(true);
        }
        else
        {
            quality.setTrianglesNormals(false);
        }
        
        if(drawBounding.isSelected())
        {
            quality.setBoundingVolume(true);
        }
        else
        {
            quality.setBoundingVolume(false);
        }
    }
}

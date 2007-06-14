package vsdk.framework.presentation.frames;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

import vsdk.framework.presentation.util.*;

import vsdk.framework.presentation.panels.*;

import vsdk.framework.presentation.*;

public class SimpleFrame extends JFrame implements ActionListener
{
    private JMenuBar mbPrincipal=new JMenuBar();
    private JMenu mFile=new JMenu("File");
    private JMenu mHelp=new JMenu("Help");
    
    private JMenuItem miDispMode=new JMenuItem("display mode");
    
    private Component principal;
    
    public SimpleFrame()
    {
        super();
        mbPrincipal.add(mFile);
        
        mFile.add(miDispMode);
        miDispMode.addActionListener(this);
        
        mbPrincipal.add(mHelp);
        
        this.setJMenuBar(mbPrincipal);
        
        this.addWindowListener
        (
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    System.exit(0);
                }
            }
        );
        
    }
    
    public void setSceneController(SceneController sc)
    {
        if(sc.getControlPaneNorth()!=null)
        {
            this.getContentPane().add(sc.getControlPaneNorth(), BorderLayout.NORTH);
        }
        if(sc.getControlPaneSouth()!=null)
        {
            this.getContentPane().add(sc.getControlPaneSouth(), BorderLayout.SOUTH);
        }
        if(sc.getControlPaneWest()!=null)
        {
            this.getContentPane().add(sc.getControlPaneWest(), BorderLayout.WEST);
        }
        if(sc.getControlPaneEast()!=null)
        {
            this.getContentPane().add(sc.getControlPaneEast(), BorderLayout.EAST);
        }
        
        if(sc.getFileMenus()!=null)
        {
            for(JMenuItem jmiAct : sc.getFileMenus())
            {    
                mFile.add(jmiAct);
            }
        }
        
        if(sc.getHelpMenus()!=null)
        {
            for(JMenuItem jmiAct : sc.getHelpMenus())
            {        
                mHelp.add(jmiAct);
            }
        }
        
        if(sc.getMenus()!=null)
        {
            for(JMenu jmAct : sc.getMenus())
            {
                mbPrincipal.add(jmAct);
            }
        }
        
        pack();
        setVisible(true);
    }
    
    public void setPresentationPanel(PresentationPanel p)throws PresentationException
    {
        if(!(p instanceof Component))
        {
            throw new PresentationException("Incompatible type");
        }
        principal=(Component)p;
        this.getContentPane().add(principal, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource()==miDispMode)
        {
            String[] options=((PresentationPanel)principal).getDisplayModes();
            int choosen = JOptionPane.showOptionDialog(this, "Choose the display mode", "Dispaly Mode", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            try
            {
                ((PresentationPanel)principal).setDisplayMode(options[choosen]);
            }
            catch(Exception ex)
            {}
        }
    }
}

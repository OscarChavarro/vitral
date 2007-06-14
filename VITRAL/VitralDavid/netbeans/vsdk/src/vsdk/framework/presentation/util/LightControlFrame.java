package vsdk.framework.presentation.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.common.ColorRgb;

/**
 *
 * @author usuario
 */
public class LightControlFrame extends JFrame implements ChangeListener, ItemListener, ActionListener
{                                                                     
    private Light light;
    
    private double[] centLuz={0,0,0};
    
    //1.0f, 0.11f, 0.0f
    
    private double ate1=1.0;
    private double ate2=0.11;
    private double ate3=0.0;
    
    private JRadioButton rbAmbient=new JRadioButton("Ambiente");
    private JRadioButton rbDireccional=new JRadioButton("Direccional");
    private JRadioButton rbPuntual=new JRadioButton("Puntual");
    
    private ButtonGroup bg=new ButtonGroup();
        
    private JSlider slColAmbientR=new JSlider(0, 255);
    private JSlider slColAmbientG=new JSlider(0, 255);
    private JSlider slColAmbientB=new JSlider(0, 255);
    
    private JSlider slColDiffuseR=new JSlider(0, 255);
    private JSlider slColDiffuseG=new JSlider(0, 255);
    private JSlider slColDiffuseB=new JSlider(0, 255);
    
    private JSlider slColSpecularR=new JSlider(0, 255);
    private JSlider slColSpecularG=new JSlider(0, 255);
    private JSlider slColSpecularB=new JSlider(0, 255);
    
    private JSlider slPosX=new JSlider(-100, 100);
    private JSlider slPosY=new JSlider(-100, 100);
    private JSlider slPosZ=new JSlider(-100, 100);
    
    private JButton bCentLuz=new JButton("Centrar Luz");
    
    private JSlider slAte1=new JSlider(0, 100);
    private JSlider slAte2=new JSlider(0, 100);
    private JSlider slAte3=new JSlider(0, 100);
    
    private JButton bAceptar=new JButton("Aceptar");
    
    /** Creates a new instance of LightControlPanel */
    public LightControlFrame(Light l)
    {
        light=l;
        
        setLayout(new GridLayout(7,1));
        
        bg.add(rbAmbient);
        bg.add(rbDireccional);
        bg.add(rbPuntual);
        
        JPanel pTipo=new JPanel();
        pTipo.setBorder(BorderFactory.createTitledBorder("Tipo de la luz"));
        pTipo.add(rbAmbient);
        pTipo.add(rbDireccional);
        pTipo.add(rbPuntual);
        
        rbAmbient.addItemListener(this);
        rbDireccional.addItemListener(this);
        rbPuntual.addItemListener(this);
                
        JPanel pColAmb=new JPanel();
        pColAmb.setLayout(new GridLayout(3,1));
        pColAmb.setBorder(BorderFactory.createTitledBorder("ambiente"));
        pColAmb.add(slColAmbientR);
        pColAmb.add(slColAmbientG);
        pColAmb.add(slColAmbientB);
        
        slColAmbientR.addChangeListener(this);
        slColAmbientG.addChangeListener(this);
        slColAmbientB.addChangeListener(this);
        
        JPanel pColDif=new JPanel();
        pColDif.setLayout(new GridLayout(3,1));
        pColDif.setBorder(BorderFactory.createTitledBorder("diffuese"));
        pColDif.add(slColDiffuseR);
        pColDif.add(slColDiffuseG);
        pColDif.add(slColDiffuseB);
        
        slColDiffuseR.addChangeListener(this);
        slColDiffuseG.addChangeListener(this);
        slColDiffuseB.addChangeListener(this);
        
        
        JPanel pColSpe=new JPanel();
        pColSpe.setLayout(new GridLayout(3,1));
        pColSpe.setBorder(BorderFactory.createTitledBorder("specular"));
        pColSpe.add(slColSpecularR);
        pColSpe.add(slColSpecularG);
        pColSpe.add(slColSpecularB);
        
        slColSpecularR.addChangeListener(this);
        slColSpecularG.addChangeListener(this);
        slColSpecularB.addChangeListener(this);
        
        JPanel pAte=new JPanel();
        pAte.setLayout(new GridLayout(3,1));
        pAte.setBorder(BorderFactory.createTitledBorder("atenuacion"));
        pAte.add(slAte1);
        pAte.add(slAte2);
        pAte.add(slAte3);
        
        slAte1.addChangeListener(this);
        slAte2.addChangeListener(this);
        slAte3.addChangeListener(this);
        
        bCentLuz.addActionListener(this);
        JPanel pPos=new JPanel();
        pPos.setLayout(new GridLayout(4,1));
        pPos.setBorder(BorderFactory.createTitledBorder("posicion"));
        pPos.add(slPosX);
        pPos.add(slPosY);
        pPos.add(slPosZ);
        pPos.add(bCentLuz);
        
        slPosX.addChangeListener(this);
        slPosY.addChangeListener(this);
        slPosZ.addChangeListener(this);

        bAceptar.addActionListener(this);
        
        getContentPane().add(pTipo);
        getContentPane().add(pColAmb);
        getContentPane().add(pColDif);
        getContentPane().add(pColSpe);
        getContentPane().add(pAte);
        getContentPane().add(pPos);
        getContentPane().add(bAceptar);
        
        pack();
    }
    
    public void stateChanged(ChangeEvent e)
    {
        if(e.getSource()==slColAmbientR)
        {
            ColorRgb color=light.getAmbient();
            color.r=((double)slColAmbientR.getValue())/255.0;
            light.setAmbient(color);
        }
        if(e.getSource()==slColAmbientG)
        {
            ColorRgb color=light.getAmbient();
            color.g=((double)slColAmbientG.getValue())/255.0;
            light.setAmbient(color);
        }
        if(e.getSource()==slColAmbientB)
        {
            ColorRgb color=light.getAmbient();
            color.b=((double)slColAmbientB.getValue())/255.0;
            light.setAmbient(color);
        }
        if(e.getSource()==slColDiffuseR)
        {
            ColorRgb color=light.getDiffuse();
            color.r=((double)slColDiffuseR.getValue())/255.0;
            light.setDiffuse(color);
        }
        if(e.getSource()==slColDiffuseG)
        {
            ColorRgb color=light.getDiffuse();
            color.g=((double)slColDiffuseG.getValue())/255.0;
            light.setDiffuse(color);
        }
        if(e.getSource()==slColDiffuseB)
        {
            ColorRgb color=light.getDiffuse();
            color.b=((double)slColDiffuseB.getValue())/255.0;
            light.setDiffuse(color);
        }
        if(e.getSource()==slColSpecularR)
        {
            ColorRgb color=light.getSpecular();
            color.r=((double)slColSpecularR.getValue())/255.0;
            light.setSpecular(color);
        }
        if(e.getSource()==slColSpecularG)
        {
            ColorRgb color=light.getSpecular();
            color.g=((double)slColSpecularG.getValue())/255.0;
            light.setSpecular(color);
        }
        if(e.getSource()==slColSpecularB)
        {
            ColorRgb color=light.getSpecular();
            color.b=((double)slColSpecularB.getValue())/255.0;
            light.setSpecular(color);
        }
        
        if(e.getSource()==slAte1)
        {
            light.setConstantAtenuation(((double)slAte1.getValue())/50.0);
        }
        
        if(e.getSource()==slAte2)
        {
            light.setLinearAtenuation(((double)slAte2.getValue())/100.0);
        }
        
        if(e.getSource()==slAte3)
        {
            light.setQuadricAtenuation(((double)slAte3.getValue())/100.0);
        }
        
        if(e.getSource()==slPosX)
        {
            light.lvec.x=centLuz[0]+((double)slPosX.getValue())/10.0;
        }
        if(e.getSource()==slPosY)
        {
            light.lvec.y=centLuz[1]+((double)slPosY.getValue())/10.0;
        }
        if(e.getSource()==slPosZ)
        {
            light.lvec.z=centLuz[2]+((double)slPosZ.getValue())/10.0;
        }
    }
    
    public void itemStateChanged(ItemEvent ise)
    {
        if(ise.getSource()==rbAmbient)
        {
            light.tipo_de_luz=Light.AMBIENT;
        }
        if(ise.getSource()==rbDireccional)
        {
            light.tipo_de_luz=Light.DIRECTIONAL;
        }
        if(ise.getSource()==rbPuntual)
        {
            light.tipo_de_luz=Light.POINT;
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource()==bCentLuz)
        {
            centLuz[0]+=((double)slPosX.getValue())/10.0;
            centLuz[1]+=((double)slPosY.getValue())/10.0;
            centLuz[2]+=((double)slPosZ.getValue())/10.0;
            
            slPosX.setValue(0);
            slPosY.setValue(0);
            slPosZ.setValue(0);
        }
        
        if(e.getSource()==bAceptar)
        {
            setVisible(false);
        }
    }
    
    public float getAte1()
    {
        return (float) ate1;
    }
    
    public float getAte2()
    {
        return (float) ate2;
    }
    
    public float getAte3()
    {
        return (float) ate3;
    }
    
    public void setLight(Light l)
    {
        light=l;
    }
    
}

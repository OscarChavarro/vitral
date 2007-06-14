/*
 * JoglToolkitInterface.java
 *
 * Created on 26 de agosto de 2005, 11:56 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import javax.media.opengl.GL;
/**
 *
 * @author usuario
 */
public class JoglToolkitInterface
{
    public static void setMaterial(GL gl, Material mat)
    {
        if(mat.getAlpha()<0.95)
        {
            gl.glDisable(gl.GL_CULL_FACE);
        }
        else
        {
            gl.glEnable(gl.GL_CULL_FACE);
        }
        
        float shine = (float)mat.getPhongExponent();
        float diffuse[] = toVect(mat.getDiffuse(), mat.getAlpha());
        float ambient[] = toVect(mat.getAmbient(), mat.getAlpha());
        float specular[]  = toVect(mat.getSpecular(), mat.getAlpha());
        float emission[] = toVect(mat.getEmission(), mat.getAlpha());
        
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_AMBIENT, ambient, 0);
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, specular, 0);
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_DIFFUSE, diffuse, 0);
        //gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_EMISSION, emission, 0);
        gl.glMaterialf(gl.GL_FRONT_AND_BACK, gl.GL_SHININESS, shine);
    }
    
    public static void setLight(GL gl, Light light, int lightNumber, float constantAtenuation, float linearAtenuation, float quadricAtenuation)
    {
        float[] lightPosition=toVect(light.lvec);
        if(light.tipo_de_luz==Light.DIRECTIONAL)
        {
            lightPosition[3]=0;
        }
        
        gl.glLightfv(lightNumber, gl.GL_POSITION, lightPosition, 0);
        gl.glLightfv(lightNumber, gl.GL_AMBIENT, toVect(light.getAmbient(), 1), 0);
        gl.glLightfv(lightNumber, gl.GL_DIFFUSE, toVect(light.getDiffuse(), 1), 0);
        gl.glLightfv(lightNumber, gl.GL_SPECULAR, toVect(light.getSpecular(), 1), 0);
        
        gl.glLightf(lightNumber, gl.GL_CONSTANT_ATTENUATION, constantAtenuation);
        gl.glLightf(lightNumber, gl.GL_LINEAR_ATTENUATION, linearAtenuation);
        gl.glLightf(lightNumber, gl.GL_QUADRATIC_ATTENUATION, quadricAtenuation);
    }
    
    public static void setLight(GL gl, Light light, int lightNumber)
    {
        float[] lightPosition=toVect(light.lvec);
        if(light.tipo_de_luz==Light.DIRECTIONAL)
        {
            lightPosition[3]=0;
        }
        
        gl.glLightfv(lightNumber, gl.GL_POSITION, lightPosition, 0);
        gl.glLightfv(lightNumber, gl.GL_AMBIENT, toVect(light.getAmbient(), 1), 0);
        gl.glLightfv(lightNumber, gl.GL_DIFFUSE, toVect(light.getDiffuse(), 1), 0);
        gl.glLightfv(lightNumber, gl.GL_SPECULAR, toVect(light.getSpecular(), 1), 0);
    }
    
    private static float[] toVect(ColorRgb c, double alpha)
    {
        float[] ret={(float)c.r, (float)c.g, (float)c.b, (float)alpha};
        return ret;
    }
    
    private static float[] toVect(Vector3D v)
    {
        float[] ret={(float)v.x, (float)v.y, (float)v.z, 1};
        return ret;
    }
}

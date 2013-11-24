//===========================================================================
package vsdk.toolkit.render.jogl;

// JOGL Classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Torus;

/**
@author Leidy Alexandra Lozano Jacome
*/

public class JoglTorusRenderer extends JoglRenderer {
    //= PROGRAM PART 1/2: ATTRIBUTES ==========================================
    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL2 gl, Torus t, Camera c, RendererConfiguration q) {
        // Parameters to draw de torus (slices  and stacks)
        int N=10;
        int n=10;
        
        draw(gl, t, c, q, n, N);
    }  
    
    public static void draw(GL2 gl, Torus t, Camera c, RendererConfiguration q,
                            int n, int N)
    {
        if ( q.isSurfacesSet() ) {
            drawQuads(gl,t, n,N);
        }
        if ( q.isWiresSet() ) {
            drawLines (gl,t, n,N);
        }
        if ( q.isPointsSet() ) {
            drawPoint (gl,t, n,N);
        }
        if ( q.isNormalsSet() ) {
            drawVertexNormals(gl,t, n,N);
        }
        if ( q.isBoundingVolumeSet() ) {
            drawBox(gl,t, n,N);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, t, q);
        }
    }

    //= PROGRAM PART 2/2: PROCEDURES ==========================================
    private static void drawPoint(GL2 gl,Torus t, int n,int N)
    {
        double R = t.getrMajor();
        double r = t.getrMinor();
        double dv = 2*Math.PI/n;
        double dw = 2*Math.PI/N;
        double v = 0.0f;
        double w;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3d(1, 0, 0);

        for ( w = 0.0; w < 2 * Math.PI + dw; w+=dw ) {
            v=0.0f;
            gl.glBegin(GL.GL_POINTS);
            for ( v = 0.0f; v < 2 * Math.PI + dv; v += dv ) {
                gl.glColor3d(1, 0, 0);
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),
                              (R+r*Math.cos(v))*Math.sin(w),
                              r*Math.sin(v));                             
            }
            gl.glEnd();
        }
    }
    
    private static void drawLines (GL2 gl,Torus t, int n,int N)
    {
        double R = t.getrMajor();
        double r = t.getrMinor();
        
        gl.glColor3d(1, 1, 1);
        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_LIGHTING);

        double dv = 2*Math.PI/n;
        double dw = 2*Math.PI/N;
        double v = 0.0f;
        double w;
        
        for ( w = 0.0; w < 2 * Math.PI + dw; w += dw ) {
            gl.glBegin(GL.GL_LINES);
            for (v=0.0f; v<2*Math.PI+dv; v+=dv) {
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),
                              (R+r*Math.cos(v))*Math.sin(w),
                              r*Math.sin(v));
                gl.glVertex3d((R+r*Math.cos(v+dv))*Math.cos(w),
                              (R+r*Math.cos(v+dv))*Math.sin(w),
                              r*Math.sin(v+dv));                
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),
                              (R+r*Math.cos(v))*Math.sin(w),
                              r*Math.sin(v));
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w+dw),
                              (R+r*Math.cos(v))*Math.sin(w+dw),
                              r*Math.sin(v));
            }                
            gl.glEnd();
        }
    }
    
    private static void drawQuads(GL2 gl,Torus t, int n,int N)
    {
        double R=t.getrMajor();
        double r=t.getrMinor();

        double rr=1.5f*r;

        double dv=2*Math.PI/n;
        double dw=2*Math.PI/N;
        double v=0.0f;
        double w;
        
        Vector3D normal = new Vector3D();

        //--------------------------------------------------------------------
        for ( w = 0.0; w<2*Math.PI+dw; w+=dw ) {
            v=0.0f;
            //gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
           
            gl.glBegin(GL2.GL_QUADS);
            for ( v = 0.0f; v < 2*Math.PI + dv; v += dv ) {              
                normal.x = (R+rr*Math.cos(v))*Math.cos(w) -
                           (R+r*Math.cos(v))*Math.cos(w);
                normal.y = (R+rr*Math.cos(v))*Math.sin(w) - 
                           (R+r*Math.cos(v))*Math.sin(w);
                normal.z = (rr*Math.sin(v)-r*Math.sin(v));
                normal.normalize();
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),
                              (R+r*Math.cos(v))*Math.sin(w),
                              r*Math.sin(v));

                normal.x = (R+rr*Math.cos(v))*Math.cos(w+dw) - 
                           (R+r*Math.cos(v))*Math.cos(w+dw);
                normal.y = (R+rr*Math.cos(v))*Math.sin(w+dw) -
                           (R+r*Math.cos(v))*Math.sin(w+dw);
                normal.z = (rr*Math.sin(v)-r*Math.sin(v));
                normal.normalize();
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w+dw),
                              (R+r*Math.cos(v))*Math.sin(w+dw),
                              r*Math.sin(v));
                
                normal.x = (R+rr*Math.cos(v+dv))*Math.cos(w+dw) -
                           (R+r*Math.cos(v+dv))*Math.cos(w+dw);
                normal.y = (R+rr*Math.cos(v+dv))*Math.sin(w+dw) - 
                           (R+r*Math.cos(v+dv))*Math.sin(w+dw);
                normal.z = (rr*Math.sin(v+dv)-r*Math.sin(v+dv));
                normal.normalize();
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d((R+r*Math.cos(v+dv))*Math.cos(w+dw),
                              (R+r*Math.cos(v+dv))*Math.sin(w+dw),
                              r*Math.sin(v+dv));

                normal.x = (R+rr*Math.cos(v+dv))*Math.cos(w) - 
                           (R+r*Math.cos(v+dv))*Math.cos(w);
                normal.y = (R+rr*Math.cos(v+dv))*Math.sin(w) -
                           (R+r*Math.cos(v+dv))*Math.sin(w);
                normal.z = (rr*Math.sin(v+dv)-r*Math.sin(v+dv));
                normal.normalize();
                gl.glNormal3d(normal.x, normal.y, normal.z);
            
                gl.glVertex3d((R+r*Math.cos(v+dv))*Math.cos(w),
                              (R+r*Math.cos(v+dv))*Math.sin(w),
                              r*Math.sin(v+dv));
            }
            gl.glEnd();
            
        }
    }
    
    private static void drawBox(GL2 gl,Torus t, int n,int N)
    {
        double R=t.getrMajor() + t.getrMinor();
        double r=t.getrMinor();
        
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_LIGHTING);
        
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(-R, -R, -r);
        gl.glVertex3d( R, -R, -r);
        gl.glVertex3d( R,  R, -r);
        gl.glVertex3d(-R,  R, -r);
        gl.glVertex3d(-R, -R, -r);

        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(-R, -R, r);
        gl.glVertex3d( R, -R, r);
        gl.glVertex3d( R,  R, r);
        gl.glVertex3d(-R,  R, r);
        gl.glVertex3d(-R, -R, r);
        gl.glEnd();


        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(-R, -R,  r);
        gl.glVertex3d( R, -R,  r);
        gl.glVertex3d( R, -R, -r);
        gl.glVertex3d(-R, -R, -r);
        gl.glVertex3d(-R, -R,  r);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d( R, R,  r);
        gl.glVertex3d(-R, R,  r);
        gl.glVertex3d(-R, R, -r);
        gl.glVertex3d( R, R, -r);
        gl.glVertex3d( R, R,  r);
        gl.glEnd();
    }
    
    /**
    <IMG ="torusNormals.jpg"></IMG>
    */
    private static void drawVertexNormals(GL2 gl,Torus t, int n,int N) {        
        double R=t.getrMajor();
        double r=t.getrMinor();
        
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_LIGHTING);

        int maxn= 1000; // max precision
        n=Math.min(n,maxn-1);
        N=Math.min(N,maxn-1);
        double rr=1.5f*r;
        double dv=2*Math.PI/n;
        double dw=2*Math.PI/N;
        double v=0.0f;
        double w;
                
        Vector3D normal = new Vector3D();
        Vector3D p = new Vector3D();

        double f=10;
        
        // outer loop
        for ( w = 0.0; w < 2 * Math.PI + dw; w += dw ) {
            gl.glBegin(GL.GL_LINES);//_STRIP);
          
            for ( v = 0.0f; v < 2 * Math.PI + dv; v += dv ) {
                normal.x = (R+rr*Math.cos(v))*Math.cos(w) - 
                           (R+r*Math.cos(v))*Math.cos(w);              
                normal.y = (R+rr*Math.cos(v))*Math.sin(w) - 
                           (R+r*Math.cos(v))*Math.sin(w);
                normal.z = (rr*Math.sin(v)-r*Math.sin(v));
                normal.normalize();
                
                p.x = (R+r*Math.cos(v))*Math.cos(w);
                p.y = (R+r*Math.cos(v))*Math.sin(w);
                p.z = r*Math.sin(v);
                
                gl.glVertex3d(p.x,p.y,p.z);
                gl.glVertex3d(p.x+normal.x/f, p.y+normal.y/f, p.z+normal.z/f);
            }
            gl.glEnd();
        }
    }
}    

//===========================================================================
//= EOF                                                                     =
//===========================================================================

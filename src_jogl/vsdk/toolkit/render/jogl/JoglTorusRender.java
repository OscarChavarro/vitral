package vsdk.toolkit.render.jogl;




import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Torus;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Leidy Alexandra Lozano Jácome
 */
public class JoglTorusRender {
  
    
 //= PROGRAM PART 1/2: ATTRIBUTES ============================================
    
   static Light l;
   static Material m;
   
   static 
   {
       l=new Light(Light.POINT,new Vector3D(1,1,2),new ColorRgb(1,1,1));
       m=new Material();
   }
    
    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL2 gl, Torus t, Camera c, RendererConfiguration q)
    {
        //Parameters to draw de torus (slices  and stacks)
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

 //= PROGRAM PART 2/2: PROCEDURES ============================================
    static void drawPoint (GL2 gl,Torus t, int n,int N)
    {
        double R=t.getrMajor();
        double r=t.getrMinor();
        

        double dv=2*Math.PI/n;
        double dw=2*Math.PI/N;
        double v=0.0f;
        double w=0.0f;
        
        // outer loop
        while(w<2*Math.PI+dw)
        {
            v=0.0f;
            gl.glBegin(GL.GL_POINTS);//_STRIP);
            // inner loop
            for (v=0.0f; v<2*Math.PI+dv; v+=dv)
            {
                gl.glColor3d(1, 0, 0);
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),(R+r*Math.cos(v))*Math.sin(w),r*Math.sin(v));
                             
              
            } // inner loop
            gl.glEnd();
            w+=dw;
            } //outer loop
        }
    
    static void drawLines (GL2 gl,Torus t, int n,int N)
    {
        double R=t.getrMajor();
        double r=t.getrMinor();
        
        gl.glColor3d(1, 1, 1);
        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_LIGHTING);

        double dv=2*Math.PI/n;
        double dw=2*Math.PI/N;
        double v=0.0f;
        double w=0.0f;
        
        // outer loop
        while(w<2*Math.PI+dw)
        {
            // inner loop
                  
            gl.glBegin(GL.GL_LINES);
            for (v=0.0f; v<2*Math.PI+dv; v+=dv)
            {
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),(R+r*Math.cos(v))*Math.sin(w),r*Math.sin(v));
                gl.glVertex3d((R+r*Math.cos(v+dv))*Math.cos(w),(R+r*Math.cos(v+dv))*Math.sin(w),r*Math.sin(v+dv));
                
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),(R+r*Math.cos(v))*Math.sin(w),r*Math.sin(v));
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w+dw),(R+r*Math.cos(v))*Math.sin(w+dw),r*Math.sin(v));
            } // inner loop
    
            
            gl.glEnd();
            
            w+=dw;
            } //outer loop
        }
    
    static void drawQuads(GL2 gl,Torus t, int n,int N)
    {
        double R=t.getrMajor();
        double r=t.getrMinor();

         double rr=1.5f*r;

        double dv=2*Math.PI/n;
        double dw=2*Math.PI/N;
        double v=0.0f;
        double w=0.0f;
        
        
       Vector3D Normal = new Vector3D();
       //--------------------------------------------------------------------
        
        
        JoglLightRenderer.activate(gl, l);
        gl.glEnable(GL2.GL_LIGHTING);
        
        
        
        JoglMaterialRenderer.activate(gl, m);
        
        //--------------------------------------------------------------------
       
        // outer loop
        while(w<2*Math.PI+dw)
        {
            v=0.0f;
      //      gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
           
            gl.glBegin(GL2.GL_QUADS);
            // inner loop
            for (v=0.0f; v<2*Math.PI+dv; v+=dv)
            {
              
                Normal.x=(R+rr*Math.cos(v))*Math.cos(w)-(R+r*Math.cos(v))*Math.cos(w);
                Normal.y=(R+rr*Math.cos(v))*Math.sin(w)-(R+r*Math.cos(v))*Math.sin(w);
                Normal.z=(rr*Math.sin(v)-r*Math.sin(v));
                Normal.normalize();
                gl.glNormal3d(
                        Normal.x,
                        Normal.y,
                        Normal.z);
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w),(R+r*Math.cos(v))*Math.sin(w),r*Math.sin(v));
                
                Normal.x=(R+rr*Math.cos(v))*Math.cos(w+dw)-(R+r*Math.cos(v))*Math.cos(w+dw);
                Normal.y=(R+rr*Math.cos(v))*Math.sin(w+dw)-(R+r*Math.cos(v))*Math.sin(w+dw);
                Normal.z=(rr*Math.sin(v)-r*Math.sin(v));
                Normal.normalize();
                gl.glNormal3d(
                        Normal.x,
                        Normal.y,
                        Normal.z);
                gl.glVertex3d((R+r*Math.cos(v))*Math.cos(w+dw),(R+r*Math.cos(v))*Math.sin(w+dw),r*Math.sin(v));
                
                Normal.x=(R+rr*Math.cos(v+dv))*Math.cos(w+dw)-(R+r*Math.cos(v+dv))*Math.cos(w+dw);
                Normal.y=(R+rr*Math.cos(v+dv))*Math.sin(w+dw)-(R+r*Math.cos(v+dv))*Math.sin(w+dw);
                Normal.z=(rr*Math.sin(v+dv)-r*Math.sin(v+dv));
                Normal.normalize();
                gl.glNormal3d(
                        Normal.x,
                        Normal.y,
                        Normal.z);
        
                gl.glVertex3d((R+r*Math.cos(v+dv))*Math.cos(w+dw),(R+r*Math.cos(v+dv))*Math.sin(w+dw),r*Math.sin(v+dv));
                
                 Normal.x=(R+rr*Math.cos(v+dv))*Math.cos(w)-(R+r*Math.cos(v+dv))*Math.cos(w);
                Normal.y=(R+rr*Math.cos(v+dv))*Math.sin(w)-(R+r*Math.cos(v+dv))*Math.sin(w);
                Normal.z=(rr*Math.sin(v+dv)-r*Math.sin(v+dv));
                Normal.normalize();
                gl.glNormal3d(
                        Normal.x,
                        Normal.y,
                        Normal.z);
            
                gl.glVertex3d((R+r*Math.cos(v+dv))*Math.cos(w),(R+r*Math.cos(v+dv))*Math.sin(w),r*Math.sin(v+dv));
                
            } // inner loop
            gl.glEnd();
            w+=dw;
            } //outer loop
        gl.glDisable(GL2.GL_LIGHTING);
        }
    
    static void drawBox(GL2 gl,Torus t, int n,int N)
    {
        double R=t.getrMajor();
        double r=t.getrMinor();
        
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_LIGHTING);
        
                // White side - BACK
         gl.glBegin(GL2.GL_LINE_STRIP);
         gl.glVertex3d( -R, -R, -r );
         gl.glVertex3d(  R, -R, -r );
         gl.glVertex3d(  R,  R, -r );
         gl.glVertex3d( -R,  R, -r );
         gl.glVertex3d( -R,  -R, -r );

         gl.glEnd();

         gl.glBegin(GL2.GL_LINE_STRIP);
         gl.glVertex3d( -R, -R, r );
         gl.glVertex3d(  R, -R, r );
         gl.glVertex3d(  R,  R, r );
         gl.glVertex3d( -R,  R, r );
         gl.glVertex3d( -R, -R, r );

         gl.glEnd();


         gl.glBegin(GL2.GL_LINE_STRIP);
         gl.glVertex3d( -R, -R, r );
         gl.glVertex3d(  R, -R, r );
         gl.glVertex3d(  R, -R, -r );
         gl.glVertex3d( -R, -R, -r );
         gl.glVertex3d( -R, -R, r );


         gl.glEnd();

         gl.glBegin(GL2.GL_LINE_STRIP);
//            gl.glColor3d(   1.0,  1.0, 1.0 );
         gl.glVertex3d(  R,  R, r );
         gl.glVertex3d( -R,  R, r );
         gl.glVertex3d( -R,  R, -r );
         gl.glVertex3d(  R,  R, -r );
         gl.glVertex3d(  R,  R, r );

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
        double w=0.0f;
        
        
        Vector3D normal = new Vector3D();
        Vector3D vertice = new Vector3D();

        
        double f=10;
        
         // outer loop
        while(w<2*Math.PI+dw)
        {
            // inner loop
                  
            gl.glBegin(GL.GL_LINES);//_STRIP);
          
            for (v=0.0f; v<2*Math.PI+dv; v+=dv)
            {
                normal.x = (R+rr*Math.cos(v))*Math.cos(w)-(R+r*Math.cos(v))*Math.cos(w);              
                normal.y = (R+rr*Math.cos(v))*Math.sin(w)-(R+r*Math.cos(v))*Math.sin(w);
                normal.z = (rr*Math.sin(v)-r*Math.sin(v));
                
                normal.normalize();
                
                vertice.x=(R+r*Math.cos(v))*Math.cos(w);
                vertice.y=(R+r*Math.cos(v))*Math.sin(w);
                vertice.z=r*Math.sin(v);
                
                gl.glVertex3d(vertice.x,vertice.y,vertice.z);
                gl.glVertex3d(vertice.x+normal.x/f, vertice.y+normal.y/f, vertice.z+normal.z/f);
                
      
            } // inner loop
    
            
            gl.glEnd();
            
            w+=dw;
            } //outer loop
    }
    

    static void drawFloor(GL2 gl,double dx,double dy, int nx,int nj)
    {
        //--------------------------------------------------------------------
        
        
        JoglLightRenderer.activate(gl, l);
        gl.glEnable(GL2.GL_LIGHTING);
        
        
        
        JoglMaterialRenderer.activate(gl, m);
        
        //--------------------------------------------------------------------
           double x1,x2,y1,y2;
        x1=0;
        x2=x1+dx;
        y1=0;
        y2=y1+dy;
        
        gl.glBegin(GL2.GL_QUADS);
            
        
        double delta=0;
        
        gl.glNormal3d(0, 0, 1);
        
        for(int j=0;j<nj;j++)
            {
                for(int i=0;i<nx;i++)
                {
                    gl.glVertex3d(x1 , y1,0);
                    gl.glVertex3d(x2-delta , y1,0);
                    gl.glVertex3d(x2-delta , y2-delta,0);
                    gl.glVertex3d(x1 , y2-delta,0);
                    
                    

                    x1=x2;
                    x2=x1+dx;

                    }
                 x1=0;
                 x2=x1+dx;
                 y1=y2;
                y2=y1+dy;
         
        }  
         
        gl.glEnd();
        gl.glDisable(GL2.GL_LIGHTING);
    }
    }    

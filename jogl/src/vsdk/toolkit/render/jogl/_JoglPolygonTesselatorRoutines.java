//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 21 2007 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellatorCallback;

public class _JoglPolygonTesselatorRoutines extends JoglRenderer 
implements GLUtessellatorCallback
{
    private GL2 gl;
    private GLU glu;
    public _JoglPolygonTesselatorRoutines(GL2 gl, GLU glu) {
        this.gl = gl;
        this.glu = glu;
    }

    @Override
    public void begin(int type) {
        gl.glBegin(type);
    }

    @Override
    public void end() {
        gl.glEnd();
    }

    @Override
    public void vertex(Object vertexData) {
        double[] pointer;
        if ( vertexData instanceof double[] ) {
            pointer = (double[]) vertexData;
            gl.glVertex3dv(pointer, 0);
        }

    }

    @Override
    public void vertexData(Object vertexData, Object polygonData) {
    }

    /* combineCallback is used to create a new vertex when edges intersect.
    coordinate location is trivial to calculate, but weight[4] may be
    used to average color, normal, or texture coordinate data. In this
    program, color is weighted. */
    @Override
    public void combine(double[] coords, Object[] data, 
                        float[] weight, Object[] outData) {
        double[] vertex = new double[6];
        int i;

        vertex[0] = coords[0];
        vertex[1] = coords[1];
        vertex[2] = coords[2];
        for (i = 3; i < 6/* 7OutOfBounds from C! */; i++)
            vertex[i] = weight[0]
                * ((double[]) data[0])[i] + weight[1]
                * ((double[]) data[1])[i] + weight[2]
                * ((double[]) data[2])[i] + weight[3]
                * ((double[]) data[3])[i];
        outData[0] = vertex;
    }

    @Override
    public void combineData(double[] coords, Object[] data, //
                            float[] weight, Object[] outData, Object polygonData) {
    }

    @Override
    public void error(int errnum) {
        String estring;

        try {
            estring = glu.gluErrorString(errnum);
        }
        catch ( Exception e ) {
            estring = "" + e;
        }

        System.err.println("Tessellation Error: " + estring);
        //System.exit(0);
    }

    @Override
    public void beginData(int type, Object polygonData) {
    }

    @Override
    public void endData(Object polygonData) {
    }

    @Override
    public void edgeFlag(boolean boundaryEdge) {
    }

    @Override
    public void edgeFlagData(boolean boundaryEdge, Object polygonData) {
    }

    @Override
    public void errorData(int errnum, Object polygonData) {
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

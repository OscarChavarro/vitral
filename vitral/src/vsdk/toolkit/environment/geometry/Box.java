//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 12 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.common.Ray;

public class Box extends Solid {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private Vector3D size;

    GeometryIntersectionInformation lastInfo;

    public Box(double dx, double dy, double dz) {
        size = new Vector3D(dx, dy, dz);

        lastInfo = new GeometryIntersectionInformation();
    }

    public Box(Vector3D s) {
        size = new Vector3D(s);

        lastInfo = new GeometryIntersectionInformation();
    }

    /**
     Check the general interface contract in superclass method
     Geometry.doIntersection.
    */
    public boolean
    doIntersection(Ray inOutRay) {
        double t, min_t = Double.MAX_VALUE;
        double x2 = size.x/2;  // OJO: Esto deberia venir precalculado
        double y2 = size.y/2;  // OJO: Esto deberia venir precalculado
        double z2 = size.z/2;  // OJO: Esto deberia venir precalculado
        Vector3D p = new Vector3D();
        GeometryIntersectionInformation info = 
            new GeometryIntersectionInformation();

        inOutRay.direction.normalize();

        // Plano superior: Z = size.z/2
        if ( Math.abs(inOutRay.direction.z) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Z=size.z/2
            t = (z2-inOutRay.origin.z)/inOutRay.direction.z;
            if ( t > -VSDK.EPSILON ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -x2 && p.x <= x2 && 
                     p.y >= -y2 && p.y <= y2 ) {
                    info.n = new Vector3D(0, 0, 1);
                    info.p = new Vector3D(p);
                    min_t = t;
                }
            }
        }

        // Plano inferior: Z = -size.z/2
        if ( Math.abs(inOutRay.direction.z) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Z=-size.z/2
            t = (-z2-inOutRay.origin.z)/inOutRay.direction.z;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -x2 && p.x <= x2 && 
                     p.y >= -y2 && p.y <= y2 ) {
                    info.n = new Vector3D(0, 0, -1);
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano frontal: Y = size.y/2
        if ( Math.abs(inOutRay.direction.y) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Y=size.y/2
            t = (y2-inOutRay.origin.y)/inOutRay.direction.y;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -x2 && p.x <= x2 && 
                     p.z >= -z2 && p.z <= z2 ) {
                    info.n.x = info.n.z = 0;
                    info.n.y = 1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano posterior: Y = -size.y/2
        if ( Math.abs(inOutRay.direction.y) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Y=-size.y/2
            t = (-y2-inOutRay.origin.y)/inOutRay.direction.y;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -x2 && p.x <= x2 && 
                     p.z >= -z2 && p.z <= z2 ) {
                    info.n.x = info.n.z = 0;
                    info.n.y = -1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano X = size.x/2
        if ( Math.abs(inOutRay.direction.x) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano X=size.x/2
            t = (x2-inOutRay.origin.x)/inOutRay.direction.x;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.y >= -y2 && p.y <= y2 && 
                     p.z >= -z2 && p.z <= z2 ) {
                    info.n.y = info.n.z = 0;
                    info.n.x = 1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano X = -size.x/2
        if ( Math.abs(inOutRay.direction.x) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano X=-size.x/2
            t = (-x2-inOutRay.origin.x)/inOutRay.direction.x;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.y >= -y2 && p.y <= y2 && 
                     p.z >= -z2 && p.z <= z2 ) {
                    info.n.y = info.n.z = 0;
                    info.n.x = -1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        if ( min_t < Double.MAX_VALUE ) {
            inOutRay.t = min_t;
            lastInfo = new GeometryIntersectionInformation(info);
            return true;
        }
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    */
    public void doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        outData.p = lastInfo.p;
        outData.n = lastInfo.n;
        outData.n.normalize();
    }

    public double[] getMinMax()
    {
        // TODO!
        double [] minmax = new double[6];

        minmax[0] = -size.x/2;
        minmax[1] = -size.y/2;
        minmax[2] = -size.z/2;
        minmax[3] = size.x/2;
        minmax[4] = size.y/2;
        minmax[5] = size.z/2;

        return minmax;
    }

    public Vector3D getSize()
    {
        return size;
    }

    public void setSize(double dx, double dy, double dz) {
        size = new Vector3D(dx, dy, dz);
    }

    public void setSize(Vector3D s) {
        size = new Vector3D(s);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

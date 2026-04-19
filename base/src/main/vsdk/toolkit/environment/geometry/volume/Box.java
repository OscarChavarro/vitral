//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume;
import java.io.Serial;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class Box extends Solid {
    @Serial private static final long serialVersionUID = 20060502L;

    private Vector3D size;

    private GeometryIntersectionInformation lastInfo;
    private int lastPlane;
    private PolyhedralBoundedSolid brepCache;

    public Box(double dx, double dy, double dz) {
        size = new Vector3D(dx, dy, dz);

        lastInfo = new GeometryIntersectionInformation();
        lastPlane = 0;
    }

    public Box(Vector3D s) {
        size = new Vector3D(s);

        lastInfo = new GeometryIntersectionInformation();
        lastPlane = 0;
    }

    public int getLastIntersectedPlane()
    {
        return lastPlane;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay
    @return true if given ray intersects current Box
    */
    @Override
    public Ray
    doIntersection(Ray inOutRay) {
        double t, min_t = Double.MAX_VALUE;
        double x2 = size.x()/2;  // OJO: Esto deberia venir precalculado
        double y2 = size.y()/2;  // OJO: Esto deberia venir precalculado
        double z2 = size.z()/2;  // OJO: Esto deberia venir precalculado
        Vector3D p;
        GeometryIntersectionInformation info = 
            new GeometryIntersectionInformation();

        inOutRay = inOutRay.withDirection(inOutRay.direction().normalized());

        // (1) Plano superior: Z = size.z()/2
        if ( Math.abs(inOutRay.direction().z()) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Z=size.z()/2
            t = (z2-inOutRay.origin().z())/inOutRay.direction().z();
            if ( t > -VSDK.EPSILON ) {
                p = inOutRay.origin().add(inOutRay.direction().multiply(t));
                if ( p.x() >= -x2 && p.x() <= x2 && 
                     p.y() >= -y2 && p.y() <= y2 ) {
                    info.p = new Vector3D(p);
                    min_t = t;
                    lastPlane = 1;
                }
            }
        }

        // (2) Plano inferior: Z = -size.z()/2
        if ( Math.abs(inOutRay.direction().z()) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Z=-size.z()/2
            t = (-z2-inOutRay.origin().z())/inOutRay.direction().z();
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin().add(inOutRay.direction().multiply(t));
                if ( p.x() >= -x2 && p.x() <= x2 && 
                     p.y() >= -y2 && p.y() <= y2 ) {
                    info.p = p;
                    min_t = t;
                    lastPlane = 2;
                }
            }
        }

        // (3) Plano frontal: Y = size.y()/2
        if ( Math.abs(inOutRay.direction().y()) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Y=size.y()/2
            t = (y2-inOutRay.origin().y())/inOutRay.direction().y();
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin().add(inOutRay.direction().multiply(t));
                if ( p.x() >= -x2 && p.x() <= x2 && 
                     p.z() >= -z2 && p.z() <= z2 ) {
                    info.p = p;
                    min_t = t;
                    lastPlane = 3;
                }
            }
        }

        // (4) Plano posterior: Y = -size.y()/2
        if ( Math.abs(inOutRay.direction().y()) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Y=-size.y()/2
            t = (-y2-inOutRay.origin().y())/inOutRay.direction().y();
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin().add(inOutRay.direction().multiply(t));
                if ( p.x() >= -x2 && p.x() <= x2 && 
                     p.z() >= -z2 && p.z() <= z2 ) {
                    info.p = p;
                    min_t = t;
                    lastPlane = 4;
                }
            }
        }

        // (5) Plano X = size.x()/2
        if ( Math.abs(inOutRay.direction().x()) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano X=size.x()/2
            t = (x2-inOutRay.origin().x())/inOutRay.direction().x();
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin().add(inOutRay.direction().multiply(t));
                if ( p.y() >= -y2 && p.y() <= y2 && 
                     p.z() >= -z2 && p.z() <= z2 ) {
                    info.p = p;
                    min_t = t;
                    lastPlane = 5;
                }
            }
        }

        // (6) Plano X = -size.x()/2
        if ( Math.abs(inOutRay.direction().x()) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano X=-size.x()/2
            t = (-x2-inOutRay.origin().x())/inOutRay.direction().x();
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin().add(inOutRay.direction().multiply(t));
                if ( p.y() >= -y2 && p.y() <= y2 && 
                     p.z() >= -z2 && p.z() <= z2 ) {
                    info.p = p;
                    min_t = t;
                    lastPlane = 6;
                }
            }
        }

        if ( min_t < Double.MAX_VALUE ) {
            lastInfo.clone(info);
            return inOutRay.withT(min_t);
        }
        return null;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        double t;
        double minT = Double.MAX_VALUE;
        int hitPlane = 0;
        double x2 = size.x()/2;
        double y2 = size.y()/2;
        double z2 = size.z()/2;
        Ray normalizedRay = inRay.withDirection(inRay.direction().normalized());
        Vector3D p = null;

        if ( Math.abs(normalizedRay.direction().z()) > VSDK.EPSILON ) {
            t = (z2-normalizedRay.origin().z())/normalizedRay.direction().z();
            if ( t > -VSDK.EPSILON ) {
                Vector3D candidate = normalizedRay.origin().add(normalizedRay.direction().multiply(t));
                if ( candidate.x() >= -x2 && candidate.x() <= x2 &&
                     candidate.y() >= -y2 && candidate.y() <= y2 ) {
                    p = candidate;
                    minT = t;
                    hitPlane = 1;
                }
            }
        }

        if ( Math.abs(normalizedRay.direction().z()) > VSDK.EPSILON ) {
            t = (-z2-normalizedRay.origin().z())/normalizedRay.direction().z();
            if ( t > -VSDK.EPSILON && t < minT ) {
                Vector3D candidate = normalizedRay.origin().add(normalizedRay.direction().multiply(t));
                if ( candidate.x() >= -x2 && candidate.x() <= x2 &&
                     candidate.y() >= -y2 && candidate.y() <= y2 ) {
                    p = candidate;
                    minT = t;
                    hitPlane = 2;
                }
            }
        }

        if ( Math.abs(normalizedRay.direction().y()) > VSDK.EPSILON ) {
            t = (y2-normalizedRay.origin().y())/normalizedRay.direction().y();
            if ( t > -VSDK.EPSILON && t < minT ) {
                Vector3D candidate = normalizedRay.origin().add(normalizedRay.direction().multiply(t));
                if ( candidate.x() >= -x2 && candidate.x() <= x2 &&
                     candidate.z() >= -z2 && candidate.z() <= z2 ) {
                    p = candidate;
                    minT = t;
                    hitPlane = 3;
                }
            }
        }

        if ( Math.abs(normalizedRay.direction().y()) > VSDK.EPSILON ) {
            t = (-y2-normalizedRay.origin().y())/normalizedRay.direction().y();
            if ( t > -VSDK.EPSILON && t < minT ) {
                Vector3D candidate = normalizedRay.origin().add(normalizedRay.direction().multiply(t));
                if ( candidate.x() >= -x2 && candidate.x() <= x2 &&
                     candidate.z() >= -z2 && candidate.z() <= z2 ) {
                    p = candidate;
                    minT = t;
                    hitPlane = 4;
                }
            }
        }

        if ( Math.abs(normalizedRay.direction().x()) > VSDK.EPSILON ) {
            t = (x2-normalizedRay.origin().x())/normalizedRay.direction().x();
            if ( t > -VSDK.EPSILON && t < minT ) {
                Vector3D candidate = normalizedRay.origin().add(normalizedRay.direction().multiply(t));
                if ( candidate.y() >= -y2 && candidate.y() <= y2 &&
                     candidate.z() >= -z2 && candidate.z() <= z2 ) {
                    p = candidate;
                    minT = t;
                    hitPlane = 5;
                }
            }
        }

        if ( Math.abs(normalizedRay.direction().x()) > VSDK.EPSILON ) {
            t = (-x2-normalizedRay.origin().x())/normalizedRay.direction().x();
            if ( t > -VSDK.EPSILON && t < minT ) {
                Vector3D candidate = normalizedRay.origin().add(normalizedRay.direction().multiply(t));
                if ( candidate.y() >= -y2 && candidate.y() <= y2 &&
                     candidate.z() >= -z2 && candidate.z() <= z2 ) {
                    p = candidate;
                    minT = t;
                    hitPlane = 6;
                }
            }
        }

        if ( minT == Double.MAX_VALUE ) {
            return false;
        }

        if ( outHit != null ) {
            outHit.setRay(inRay.withT(minT));
            outHit.p = p;
            outHit.u = 0;
            outHit.v = 0;
            switch ( hitPlane ) {
              case 1:
                outHit.n = new Vector3D(0, 0, 1);
                outHit.u = outHit.p.y() / size.y() - 0.5;
                outHit.v = 1-(outHit.p.x() / size.x() - 0.5);
                outHit.t = new Vector3D(0, 1, 0);
                break;
              case 2:
                outHit.n = new Vector3D(0, 0, -1);
                outHit.u = outHit.p.y() / size.y() - 0.5;
                outHit.v = outHit.p.x() / size.x() - 0.5;
                outHit.t = new Vector3D(0, 1, 0);
                break;
              case 3:
                outHit.n = new Vector3D(0, 1, 0);
                outHit.u = 1-(outHit.p.x() / size.x() - 0.5);
                outHit.v = outHit.p.z() / size.z() - 0.5;
                outHit.t = new Vector3D(-1, 0, 0);
                break;
              case 4:
                outHit.n = new Vector3D(0, -1, 0);
                outHit.u = outHit.p.x() / size.x() - 0.5;
                outHit.v = outHit.p.z() / size.z() - 0.5;
                outHit.t = new Vector3D(1, 0, 0);
                break;
              case 5:
                outHit.n = new Vector3D(1, 0, 0);
                outHit.u = outHit.p.y() / size.y() - 0.5;
                outHit.v = outHit.p.z() / size.z() - 0.5;
                outHit.t = new Vector3D(0, 1, 0);
                break;
              case 6:
                outHit.n = new Vector3D(-1, 0, 0);
                outHit.u = 1-(outHit.p.y() / size.y() - 0.5);
                outHit.v = outHit.p.z() / size.z() - 0.5;
                outHit.t = new Vector3D(0, -1, 0);
                break;
              default:
                outHit.n = new Vector3D();
                outHit.t = new Vector3D();
                break;
            }
        }

        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    @Override
    public void
    doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        outData.p = lastInfo.p;

        switch ( lastPlane ) {
          case 1:
            outData.n = outData.n.withX(0);
            outData.n = outData.n.withY(0);
            outData.n = outData.n.withZ(1);
            outData.u = outData.p.y() / size.y() - 0.5;
            outData.v = 1-(outData.p.x() / size.x() - 0.5);
            outData.t = outData.t.withX(0);
            outData.t = outData.t.withY(1);
            outData.t = outData.t.withZ(0);
            break;
          case 2:
            outData.n = outData.n.withX(0);
            outData.n = outData.n.withY(0);
            outData.n = outData.n.withZ(-1);
            outData.u = outData.p.y() / size.y() - 0.5;
            outData.v = outData.p.x() / size.x() - 0.5;
            outData.t = outData.t.withX(0);
            outData.t = outData.t.withY(1);
            outData.t = outData.t.withZ(0);
            break;
          case 3:
            outData.n = outData.n.withX(0);
            outData.n = outData.n.withZ(0);
            outData.n = outData.n.withY(1);
            outData.u = 1-(outData.p.x() / size.x() - 0.5);
            outData.v = outData.p.z() / size.z() - 0.5;
            outData.t = outData.t.withX(-1);
            outData.t = outData.t.withY(0);
            outData.t = outData.t.withZ(0);
            break;
          case 4:
            outData.n = outData.n.withX(0);
            outData.n = outData.n.withZ(0);
            outData.n = outData.n.withY(-1);
            outData.u = outData.p.x() / size.x() - 0.5;
            outData.v = outData.p.z() / size.z() - 0.5;
            outData.t = outData.t.withX(1);
            outData.t = outData.t.withY(0);
            outData.t = outData.t.withZ(0);
            break;
          case 5:
            outData.n = outData.n.withX(1);
            outData.n = outData.n.withY(0);
            outData.n = outData.n.withZ(0);
            outData.u = outData.p.y() / size.y() - 0.5;
            outData.v = outData.p.z() / size.z() - 0.5;
            outData.t = outData.t.withX(0);
            outData.t = outData.t.withY(1);
            outData.t = outData.t.withZ(0);
            break;
          case 6:
            outData.n = outData.n.withX(-1);
            outData.n = outData.n.withY(0);
            outData.n = outData.n.withZ(0);
            outData.u = 1-(outData.p.y() / size.y() - 0.5);
            outData.v = outData.p.z() / size.z() - 0.5;
            outData.t = outData.t.withX(0);
            outData.t = outData.t.withY(-1);
            outData.t = outData.t.withZ(0);
            break;
          default:
            outData.u = 0;
            outData.v = 0;
            break;
        }
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax()
    {
        // TODO!
        double [] minmax = new double[6];

        minmax[0] = -size.x()/2;
        minmax[1] = -size.y()/2;
        minmax[2] = -size.z()/2;
        minmax[3] = size.x()/2;
        minmax[4] = size.y()/2;
        minmax[5] = size.z()/2;

        return minmax;
    }

    public Vector3D getSize()
    {
        return size;
    }

    public void setSize(double dx, double dy, double dz) {
        setSize(new Vector3D(dx, dy, dz));
    }

    public void setSize(Vector3D s) {
        size = new Vector3D(s);
    }

    @Override
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        if ( brepCache == null ) {
            brepCache = buildPolyhedralBoundedSolid();
        }
        return brepCache;
    }

    /**
    Current method creates a polyhedral boundary representation for
    current box, following the strategy for Euler operators presented
    at sections [MANT1988].9.3., [MANT1988].12.3.1., as depicted in
    figure [MANT1988].9.11. and following the structure of the program
    [MANT1988].12.4.
    */
    private PolyhedralBoundedSolid buildPolyhedralBoundedSolid()
    {
        PolyhedralBoundedSolid solid;
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(
            new Vector3D(-size.x()/2, -size.y()/2, -size.z()/2), 1, 1);
        solid.smev(1, 1, 4,
            new Vector3D(-size.x()/2, size.y()/2, -size.z()/2));
        solid.smev(1, 4, 3,
            new Vector3D(size.x()/2, size.y()/2, -size.z()/2));
        solid.smev(1, 3, 2,
            new Vector3D(size.x()/2, -size.y()/2, -size.z()/2));
        solid.mef(1, 1, 1, 4, 2, 3, 2);
        
        solid.smev(1, 1, 5,
            new Vector3D(-size.x()/2, -size.y()/2, size.z()/2));
        solid.smev(1, 2, 6,
            new Vector3D(size.x()/2, -size.y()/2, size.z()/2));
        solid.mef(1, 1, 5, 1, 6, 2, 3);
        solid.smev(1, 3, 7,
            new Vector3D(size.x()/2, size.y()/2, size.z()/2));
        solid.mef(1, 1, 6, 2, 7, 3, 4);
        solid.smev(1, 4, 8,
            new Vector3D(-size.x()/2, size.y()/2, size.z()/2));
        solid.mef(1, 1, 7, 3, 8, 4, 5);
        solid.mef(1, 1, 5, 6, 8, 4, 6);
        return solid;
    }
}

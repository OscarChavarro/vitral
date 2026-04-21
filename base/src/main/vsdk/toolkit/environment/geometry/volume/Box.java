//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume;
import java.io.Serial;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class Box extends Solid {
    @Serial private static final long serialVersionUID = 20060502L;
    private static final Vector3D NORMAL_POS_Z = new Vector3D(0, 0, 1);
    private static final Vector3D NORMAL_NEG_Z = new Vector3D(0, 0, -1);
    private static final Vector3D NORMAL_POS_Y = new Vector3D(0, 1, 0);
    private static final Vector3D NORMAL_NEG_Y = new Vector3D(0, -1, 0);
    private static final Vector3D NORMAL_POS_X = new Vector3D(1, 0, 0);
    private static final Vector3D NORMAL_NEG_X = new Vector3D(-1, 0, 0);
    private static final Vector3D TANGENT_POS_Y = new Vector3D(0, 1, 0);
    private static final Vector3D TANGENT_NEG_Y = new Vector3D(0, -1, 0);
    private static final Vector3D TANGENT_NEG_X = new Vector3D(-1, 0, 0);
    private static final Vector3D TANGENT_POS_X = new Vector3D(1, 0, 0);
    private static final Vector3D ZERO_VECTOR = new Vector3D();

    private Vector3D size;

    public Box(double dx, double dy, double dz) {
        size = new Vector3D(dx, dy, dz);
    }

    public Box(Vector3D s) {
        size = new Vector3D(s);
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay
    @return true if given ray intersects current Box
    */
    public Ray
    doIntersection(Ray inOutRay) {
        RayHit hit = new RayHit();
        if ( doIntersection(inOutRay, hit) ) {
            return hit.ray();
        }
        return null;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        double minT = Double.MAX_VALUE;
        int hitPlane = 0;
        double x2 = size.x()/2;
        double y2 = size.y()/2;
        double z2 = size.z()/2;
        double ox = inRay.origin().x();
        double oy = inRay.origin().y();
        double oz = inRay.origin().z();
        double dx = inRay.direction().x();
        double dy = inRay.direction().y();
        double dz = inRay.direction().z();

        if ( Math.abs(dz) > VSDK.EPSILON ) {
            double t = (z2 - oz) / dz;
            if ( t > -VSDK.EPSILON ) {
                double cx = ox + dx*t;
                double cy = oy + dy*t;
                if ( cx >= -x2 && cx <= x2 &&
                     cy >= -y2 && cy <= y2 ) {
                    minT = t;
                    hitPlane = 1;
                }
            }
        }

        if ( Math.abs(dz) > VSDK.EPSILON ) {
            double t = (-z2 - oz) / dz;
            if ( t > -VSDK.EPSILON && t < minT ) {
                double cx = ox + dx*t;
                double cy = oy + dy*t;
                if ( cx >= -x2 && cx <= x2 &&
                     cy >= -y2 && cy <= y2 ) {
                    minT = t;
                    hitPlane = 2;
                }
            }
        }

        if ( Math.abs(dy) > VSDK.EPSILON ) {
            double t = (y2 - oy) / dy;
            if ( t > -VSDK.EPSILON && t < minT ) {
                double cx = ox + dx*t;
                double cz = oz + dz*t;
                if ( cx >= -x2 && cx <= x2 &&
                     cz >= -z2 && cz <= z2 ) {
                    minT = t;
                    hitPlane = 3;
                }
            }
        }

        if ( Math.abs(dy) > VSDK.EPSILON ) {
            double t = (-y2 - oy) / dy;
            if ( t > -VSDK.EPSILON && t < minT ) {
                double cx = ox + dx*t;
                double cz = oz + dz*t;
                if ( cx >= -x2 && cx <= x2 &&
                     cz >= -z2 && cz <= z2 ) {
                    minT = t;
                    hitPlane = 4;
                }
            }
        }

        if ( Math.abs(dx) > VSDK.EPSILON ) {
            double t = (x2 - ox) / dx;
            if ( t > -VSDK.EPSILON && t < minT ) {
                double cy = oy + dy*t;
                double cz = oz + dz*t;
                if ( cy >= -y2 && cy <= y2 &&
                     cz >= -z2 && cz <= z2 ) {
                    minT = t;
                    hitPlane = 5;
                }
            }
        }

        if ( Math.abs(dx) > VSDK.EPSILON ) {
            double t = (-x2 - ox) / dx;
            if ( t > -VSDK.EPSILON && t < minT ) {
                double cy = oy + dy*t;
                double cz = oz + dz*t;
                if ( cy >= -y2 && cy <= y2 &&
                     cz >= -z2 && cz <= z2 ) {
                    minT = t;
                    hitPlane = 6;
                }
            }
        }

        if ( minT == Double.MAX_VALUE ) {
            return false;
        }

        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() || outHit.needsAnySurfaceData() ) {
                outHit.setRay(inRay.withT(minT));
            }
            else {
                outHit.setHitDistance(minT);
            }

            if ( outHit.needsAnySurfaceData() ) {
                double hitX = ox + dx*minT;
                double hitY = oy + dy*minT;
                double hitZ = oz + dz*minT;

                if ( outHit.needsPoint() ) {
                    outHit.p = new Vector3D(hitX, hitY, hitZ);
                }

                if ( outHit.needsTextureCoordinates() ) {
                    outHit.u = 0;
                    outHit.v = 0;
                    switch ( hitPlane ) {
                      case 1:
                        outHit.u = hitY / size.y() - 0.5;
                        outHit.v = 1-(hitX / size.x() - 0.5);
                        break;
                      case 2:
                        outHit.u = hitY / size.y() - 0.5;
                        outHit.v = hitX / size.x() - 0.5;
                        break;
                      case 3:
                        outHit.u = 1-(hitX / size.x() - 0.5);
                        outHit.v = hitZ / size.z() - 0.5;
                        break;
                      case 4:
                        outHit.u = hitX / size.x() - 0.5;
                        outHit.v = hitZ / size.z() - 0.5;
                        break;
                      case 5:
                        outHit.u = hitY / size.y() - 0.5;
                        outHit.v = hitZ / size.z() - 0.5;
                        break;
                      case 6:
                        outHit.u = 1-(hitY / size.y() - 0.5);
                        outHit.v = hitZ / size.z() - 0.5;
                        break;
                      default:
                        break;
                    }
                }

                if ( outHit.needsNormal() ) {
                    outHit.n = planeNormal(hitPlane);
                }
                if ( outHit.needsTangent() ) {
                    outHit.t = planeTangent(hitPlane);
                }
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
    public void
    doExtraInformation(Ray inRay, double inT, 
                                  RayHit outData) {
        double hitX = inRay.origin().x() + inRay.direction().x()*inT;
        double hitY = inRay.origin().y() + inRay.direction().y()*inT;
        double hitZ = inRay.origin().z() + inRay.direction().z()*inT;
        int hitPlane = classifyHitPlane(hitX, hitY, hitZ);

        if ( outData.needsPoint() ) {
            outData.p = new Vector3D(hitX, hitY, hitZ);
        }
        if ( outData.needsNormal() ) {
            outData.n = planeNormal(hitPlane);
        }
        if ( outData.needsTangent() ) {
            outData.t = planeTangent(hitPlane);
        }

        if ( outData.needsTextureCoordinates() ) {
            switch ( hitPlane ) {
              case 1:
                outData.u = hitY / size.y() - 0.5;
                outData.v = 1-(hitX / size.x() - 0.5);
                break;
              case 2:
                outData.u = hitY / size.y() - 0.5;
                outData.v = hitX / size.x() - 0.5;
                break;
              case 3:
                outData.u = 1-(hitX / size.x() - 0.5);
                outData.v = hitZ / size.z() - 0.5;
                break;
              case 4:
                outData.u = hitX / size.x() - 0.5;
                outData.v = hitZ / size.z() - 0.5;
                break;
              case 5:
                outData.u = hitY / size.y() - 0.5;
                outData.v = hitZ / size.z() - 0.5;
                break;
              case 6:
                outData.u = 1-(hitY / size.y() - 0.5);
                outData.v = hitZ / size.z() - 0.5;
                break;
              default:
                outData.u = 0;
                outData.v = 0;
                break;
            }
        }
    }

    private static Vector3D planeNormal(int hitPlane)
    {
        switch ( hitPlane ) {
          case 1: return NORMAL_POS_Z;
          case 2: return NORMAL_NEG_Z;
          case 3: return NORMAL_POS_Y;
          case 4: return NORMAL_NEG_Y;
          case 5: return NORMAL_POS_X;
          case 6: return NORMAL_NEG_X;
          default: return ZERO_VECTOR;
        }
    }

    private static Vector3D planeTangent(int hitPlane)
    {
        switch ( hitPlane ) {
          case 1:
          case 2:
          case 5:
            return TANGENT_POS_Y;
          case 3:
            return TANGENT_NEG_X;
          case 4:
            return TANGENT_POS_X;
          case 6:
            return TANGENT_NEG_Y;
          default:
            return ZERO_VECTOR;
        }
    }

    private int classifyHitPlane(double x, double y, double z)
    {
        double x2 = size.x() / 2;
        double y2 = size.y() / 2;
        double z2 = size.z() / 2;
        double dxPlus = Math.abs(x - x2);
        double dxMinus = Math.abs(x + x2);
        double dyPlus = Math.abs(y - y2);
        double dyMinus = Math.abs(y + y2);
        double dzPlus = Math.abs(z - z2);
        double dzMinus = Math.abs(z + z2);

        double min = dzPlus;
        int plane = 1;
        if ( dzMinus < min ) { min = dzMinus; plane = 2; }
        if ( dyPlus < min ) { min = dyPlus; plane = 3; }
        if ( dyMinus < min ) { min = dyMinus; plane = 4; }
        if ( dxPlus < min ) { min = dxPlus; plane = 5; }
        if ( dxMinus < min ) { plane = 6; }
        return plane;
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
        return buildPolyhedralBoundedSolid();
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

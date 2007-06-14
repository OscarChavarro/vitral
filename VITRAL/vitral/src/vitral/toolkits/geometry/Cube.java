//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 12 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vitral.toolkits.geometry;

import vitral.toolkits.common.VSDK;
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.geometry.Geometry;
import vitral.toolkits.geometry.GeometryIntersectionInformation;
import vitral.toolkits.common.Ray;

public class Cube extends Geometry {
    private double side;

    GeometryIntersectionInformation lastInfo;

    public Cube(double l) {
        side = l;

        lastInfo = new GeometryIntersectionInformation();
    }

    /**
    Dado un Ray `inout_rayo`, esta operaci&oacute;n determina si el rayo se
    intersecta con la superficie de este objeto o no. Si el rayo no intersecta
    al objeto se retorna false, y de lo contrario se retorna true y la 
    distancia desde el origin del rayo hasta el punto de interseccion se
    copia dentro del rayo.

    En caso de intersecci&oacute;n, se modifica `inout_rayo.t` para que 
    contenga la distancia entre el punto de intersecci&oacute;n y el origin
    del `inout_rayo`.

    Precondici&oacute;n:
    \f[
        \mathbf{Q} := \side > \varepsilon
    \f]

    OJO: Revisar que puede hacerce en beneficio de la eficiencia
    */
    public boolean
    doIntersection(Ray inOutRay) {
        double t, min_t = Double.MAX_VALUE;
        double l2 = side/2;  // OJO: Esto deberia venir precalculado
        Vector3D p = new Vector3D();
        GeometryIntersectionInformation info = 
            new GeometryIntersectionInformation();

        inOutRay.direction.normalize();

        // Plano superior: Z = side/2
        if ( Math.abs(inOutRay.direction.z) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Z=side/2
            t = (l2-inOutRay.origin.z)/inOutRay.direction.z;
            if ( t > -VSDK.EPSILON ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -l2 && p.x <= l2 && 
                     p.y >= -l2 && p.y <= l2 ) {
                    info.n = new Vector3D(0, 0, 1);
                    info.p = new Vector3D(p);
                    min_t = t;
                }
            }
        }

        // Plano inferior: Z = -side/2
        if ( Math.abs(inOutRay.direction.z) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Z=-side/2
            t = (-l2-inOutRay.origin.z)/inOutRay.direction.z;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -l2 && p.x <= l2 && 
                     p.y >= -l2 && p.y <= l2 ) {
                    info.n = new Vector3D(0, 0, -1);
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano frontal: Y = side/2
        if ( Math.abs(inOutRay.direction.y) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Y=side/2
            t = (l2-inOutRay.origin.y)/inOutRay.direction.y;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -l2 && p.x <= l2 && 
                     p.z >= -l2 && p.z <= l2 ) {
                    info.n.x = info.n.z = 0;
                    info.n.y = 1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano posterior: Y = -side/2
        if ( Math.abs(inOutRay.direction.y) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano Y=-side/2
            t = (-l2-inOutRay.origin.y)/inOutRay.direction.y;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.x >= -l2 && p.x <= l2 && 
                     p.z >= -l2 && p.z <= l2 ) {
                    info.n.x = info.n.z = 0;
                    info.n.y = -1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano X = side/2
        if ( Math.abs(inOutRay.direction.x) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano X=side/2
            t = (l2-inOutRay.origin.x)/inOutRay.direction.x;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.y >= -l2 && p.y <= l2 && 
                     p.z >= -l2 && p.z <= l2 ) {
                    info.n.y = info.n.z = 0;
                    info.n.x = 1;
                    info.p = p;
                    min_t = t;
                }
            }
        }

        // Plano X = -side/2
        if ( Math.abs(inOutRay.direction.x) > VSDK.EPSILON ) {
            // El rayo no es paralelo al plano X=-side/2
            t = (-l2-inOutRay.origin.x)/inOutRay.direction.x;
            if ( t > -VSDK.EPSILON && t < min_t ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                if ( p.y >= -l2 && p.y <= l2 && 
                     p.z >= -l2 && p.z <= l2 ) {
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
    Dado un `in_rayo` que se intersecta con este objeto (a una distancia `in_t`
    desde su origin), este m&eacute;todo escribe en los vectores `out_p` y 
    `out_n` la siguiente informacio&nacute;n:
      - out_p: las coordenadas del punto en el que el `in_rayo` intersecta al 
           objeto.
      - out_n: un vector unitario con la normal de la superficie del objeto en
           el punto de intersecci&oacute;n `out_p`.

    PRE: Las referencias a Vector3D out_p y out_n YA deben existir. El 
         m&eacute;todo solo coloca informaci&oacute;n dentro de ellos.
    */
    public void doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        outData.p = lastInfo.p;
        outData.n = lastInfo.n;
        outData.n.normalize();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

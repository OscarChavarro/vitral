//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - March 14 2006 - Oscar Chavarro: Get/set interface                     =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.Geometry;

public class Sphere extends Geometry {
    private double _radio;
    private double _radio_al_cuadrado;
    private Vector3D _static_delta;

    public Sphere(double r) {
        _radio = r;
        _radio_al_cuadrado = _radio*_radio;
        _static_delta = new Vector3D();
    }

    /**
    Dado un Ray `inout_rayo`, esta operaci&oacute;n determina si el rayo se
    intersecta con la superficie de este objeto o no. Si el rayo no intersecta
    al objeto se retorna 0, y de lo contrario se retorna la distancia desde
    el origen del rayo hasta el punto de interseccion.

    En caso de intersecci&oacute;n, se modifica `inout_rayo.t` para que 
    contenga la distancia entre el punto de intersecci&oacute;n y el origen
    del `inout_rayo`.
    */
    public boolean
    doIntersection(Ray inout_rayo) {
        /* OJO: Como en Java, a diferencia de C no hay sino objetos por
                referencia, no se puede hacer una declaraci&oacute;n 
                est&aacute;tica de un objeto, y poder hacerla es importante 
                porque la constructora Vector3D::Vector3D se ejecuta muchas veces, 
                y no se debe gastar tiempo creando objetos (i.e. haciento 
                Vector3D delta; delta = new Vector3D(); ...).  El c&oacute;digo
                original de MIT resolvi&oacute; &eacute;sto usando unos 
                flotantes double dx, dy, dz; e implementando una versi&oacute;n
                adicional de Vector3D::dotProduct() que recibe 3 doubles.  
                Se considera que esa soluci&oacute;n es "fea" y que al ofrecer
                el nuevo m&eacute;todo `Vector3D::dotProduct` se le 
                desorganiza la mente al usuario/programador.  Por eso, se 
                resolvi&oacute; implementar otra soluci&oacute;n (tal vez 
                igual de fea): a&ntilde;adir un nuevo atributo de clase en
                Sphere, y utilizarlo como su fuese una variable est&aacute;tica
                de tipo Vector3D dentro del m&eacute;todo.  Esto no es bueno 
                porque gasta memoria, pero ... que m&aacute;s podr&aacute; 
                hacerse? Al menos el tiempo de ejecuci&oacute;n se mantiene 
                igual respecto al c&oacute;digo original de MIT.
                NOTA: Comparar este m&eacute;todo modificado con la 
                      versi&oacute;n original en la etapa 1, con la 
                      ayuda de un profiler. ... */
        _static_delta.x = -inout_rayo.origin.x;
        _static_delta.y = -inout_rayo.origin.y;
        _static_delta.z = -inout_rayo.origin.z;
        double v = inout_rayo.direction.dotProduct(_static_delta);

        // Do the following quick check to see if there is even a chance
        // that an intersection here might be closer than a previous one
        if ( v - _radio > inout_rayo.t ) {
            return false;
        }

        // Test if the inout_rayo actually intersects the sphere
        double t = _radio_al_cuadrado + v*v 
                  - _static_delta.x*_static_delta.x 
                  - _static_delta.y*_static_delta.y 
                  - _static_delta.z*_static_delta.z;
        if ( t < 0 ) {
            return false;
        }

        // Test if the intersection is in the positive
        // inout_rayo direction and it is the closest so far
        t = v - Math.sqrt(t);
        if ( (t > inout_rayo.t) || (t < 0) ) {
            return false;
        }

        inout_rayo.t = t;
        return true;
    }

    /**
    Dado un `in_rayo` que se intersecta con este objeto (a una distancia `in_t`
    desde su origen), este m&eacute;todo escribe en los vectores `out_p` y 
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
        outData.p.x = inRay.origin.x + inT*inRay.direction.x;
        outData.p.y = inRay.origin.y + inT*inRay.direction.y;
        outData.p.z = inRay.origin.z + inT*inRay.direction.z;

        outData.n.x = outData.p.x;
        outData.n.y = outData.p.y;
        outData.n.z = outData.p.z;
        outData.n.normalize();
    }

    public double getRadius()
    {
        return _radio;
    }

    public void setRadius(double r)
    {
        _radio = r;
        _radio_al_cuadrado = r*r;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

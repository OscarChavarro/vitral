//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - February 13 2006 - Oscar Chavarro: updated raytracing code to manage  =
//=   rayable objects, with geometries implementing intersection operation  =
//=   in an object-coordinate basis.                                        =
//===========================================================================

package vsdk.toolkit.render;

import java.util.ArrayList;
import java.util.Iterator;

import vsdk.toolkit.common.ProgressMonitor;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.geometry.RayableObject;


/**
Esta clase solo provee un m&eacute;todo que encapsula un algoritmo de
control de visualizaci&oacute;n. Su raz&oacute;n de ser (y por la cual
NO poner ese m&eacute;todo en la clase cliente que la utiliza) es facilitar
la abstracci&oacute;n de la operaci&oacute;n de visualizaci&oacute;n, la
cual puede ser cambiada por otro algoritmo de visualizaci&oacute;n (i.e.
zbuffer o radiosidad), pero manteniendo el mismo modelo de escena 3D.
*/
public class Raytracer {
    private static final float INFINITO = Float.MAX_VALUE;
    private static final float TINY = 0.001f;
    private Vector3D static_tmp;

    public Raytracer()
    {
        // Machete similar al descrito en Sphere::intersect
        static_tmp = new Vector3D();
    }

    /*
    the point of intersection (p)
    a unit-length surface normal (n)
    a unit-length vector towards the ray's origin (v)

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations. (This must be taken
    into account in the reflection and refraction calculations)
    */
    public ColorRgb modelo_de_iluminacion(Vector3D p, Vector3D n, Vector3D v, 
        ArrayList lights, ArrayList objects, Background fondo,
        Material m) {

        ColorRgb resultado = new ColorRgb();
        RayableObject nearestObject;
        ColorRgb color_de_fondo = fondo.colorInDireccion(n);
        ColorRgb ambient;
        ColorRgb diffuse;
        ColorRgb specular;
        ColorRgb lightEmission;
        Ray myRay;
        Vector3D po;
        Matrix4x4 R, Ri;

        for ( Iterator i = lights.iterator();
              i.hasNext(); ) {
            Light luz = (Light)i.next();
            lightEmission = luz.getSpecular();
            if ( luz.tipo_de_luz == Light.AMBIENT ) {
                ambient = m.getAmbient();
                resultado.r += ambient.r*lightEmission.r;
                resultado.g += ambient.g*lightEmission.g;
                resultado.b += ambient.b*lightEmission.b;
              } 
              else {
                Vector3D l;
                if ( luz.tipo_de_luz == Light.POINT ) {
                    l = new Vector3D(luz.lvec.x - p.x, 
                                   luz.lvec.y - p.y, 
                                   luz.lvec.z - p.z);
                    l.normalize();
                  } 
                  else {
                    l = new Vector3D(-luz.lvec.x, -luz.lvec.y, -luz.lvec.z);
                }

                // Check if the surface point is in shadow
                Vector3D poffset = 
                    new Vector3D(p.x + TINY*l.x, p.y + TINY*l.y, p.z + TINY*l.z);
                Ray rayo_sombra = new Ray(poffset, l);
                nearestObject =
                    trazar_rayo_en_escena(rayo_sombra, objects);
                if ( nearestObject != null ) {
                    continue;
                }

                double lambert = n.dotProduct(l);
                if ( lambert > 0 ) {
                    diffuse = m.getDiffuse();
                    if ( (diffuse.r + diffuse.g + diffuse.b) > 0 ) {
                        resultado.r += lambert*diffuse.r*lightEmission.r;
                        resultado.g += lambert*diffuse.g*lightEmission.g;
                        resultado.b += lambert*diffuse.b*lightEmission.b;
                    }
                    specular = m.getSpecular();
                    if ( (specular.r + specular.g + specular.b) > 0 ) {
                        lambert *= 2;

                        static_tmp.x = lambert*n.x - l.x;
                        static_tmp.y = lambert*n.y - l.y;
                        static_tmp.z = lambert*n.z - l.z;
                        double spec = 
                            v.dotProduct(static_tmp);

                        if ( spec > 0 ) {
                            // OJO: Raro...
                            spec = ((specular.r + specular.g + specular.b)/3)*(
                                (float) Math.pow((double) spec, (double)m.getPhongExponent()));
                            resultado.r += spec*lightEmission.r;
                            resultado.g += spec*lightEmission.g;
                            resultado.b += spec*lightEmission.b;
                        }
                    }
                }
            }
        }

        // Compute illumination due to reflection
        double kr = m.getReflectionCoefficient();
        if ( kr > 0 ) {
            double t = v.dotProduct(n);
            if ( t > 0 ) {
                t *= 2;
                Vector3D reflect = new Vector3D(t*n.x - v.x, 
                                                t*n.y - v.y, 
                                                t*n.z - v.z);
                Vector3D poffset = new Vector3D(p.x + TINY*reflect.x, 
                                                p.y + TINY*reflect.y, 
                                                p.z + TINY*reflect.z);
                Ray rayo_reflejado = new Ray(poffset, reflect);
                nearestObject = 
                    trazar_rayo_en_escena(rayo_reflejado, objects);
                if ( nearestObject != null ) {
                    Vector3D rv = new Vector3D();
                    Vector3D rp, rn;
                    GeometryIntersectionInformation info = 
                        new GeometryIntersectionInformation();

                    //--------------------------------------------------------
                    po = nearestObject.getPosition();
                    R = nearestObject.getRotation();
                    Ri = nearestObject.getRotationInverse();
                    myRay = new Ray ( 
                        Ri.multiply(rayo_reflejado.origin.substract(po) ),
                        Ri.multiply(rayo_reflejado.direction)
                    );
                    myRay.t = rayo_reflejado.t;

                    nearestObject.getGeometry().doExtraInformation(
                        myRay, myRay.t, info);

                    rp = R.multiply(info.p).add(po);
                    rn = R.multiply(info.n);

                    //--------------------------------------------------------

                    rv.x = -rayo_reflejado.direction.x;
                    rv.y = -rayo_reflejado.direction.y;
                    rv.z = -rayo_reflejado.direction.z;                    
                    ColorRgb rcolor =
                        modelo_de_iluminacion(rp, rn, rv, lights, objects, fondo, m);

                    resultado.r += kr*rcolor.r;
                    resultado.g += kr*rcolor.g;
                    resultado.b += kr*rcolor.b;
                  } 
                  else {
                    resultado.r += kr*color_de_fondo.r;
                    resultado.g += kr*color_de_fondo.g;
                    resultado.b += kr*color_de_fondo.b;
                }
            }
        }

        // Add code for refraction here
        // <No implementado>

        // Clamp resultado to MAX 1.0 intensity.
        resultado.r = (resultado.r > 1) ? 1 : resultado.r;
        resultado.g = (resultado.g > 1) ? 1 : resultado.g;
        resultado.b = (resultado.b > 1) ? 1 : resultado.b;
        return resultado;
    }

    /**
    This method intersect the `inRay` with all of the geometries contained
    in `inRayableObjectArray`. If none of the geometries is intersected
    `null` is returned, otherwise a reference to the containing RayableObject
    is returned.

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations.
    */
    public RayableObject 
    trazar_rayo_en_escena(Ray inOut_Ray, ArrayList inRayableObjectArray) {
        Iterator i;
        RayableObject gi;
        RayableObject nearestObject;
        double nearestDistance;

        nearestDistance = INFINITO;
        nearestObject = null;
        for ( i = inRayableObjectArray.iterator(); i.hasNext(); ) {
            inOut_Ray.t = INFINITO;
            gi = (RayableObject)i.next();
            if ( gi.doIntersection(inOut_Ray) && 
                 inOut_Ray.t < nearestDistance ) {
                nearestDistance = inOut_Ray.t;
                nearestObject = gi;
            }
        }
        inOut_Ray.t = nearestDistance;
        return nearestObject;
    }

    /**
    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations.
    */
    private ColorRgb seguimiento_rayo(Ray inRay, ArrayList in_objetos, 
                         ArrayList in_luces, Background in_fondo)
    {
        ColorRgb c;
        Vector3D v = new Vector3D();
        Vector3D p, n;
        RayableObject nearestObject;
        Ray myRay;
        Vector3D po;
        Matrix4x4 R, Ri;
        GeometryIntersectionInformation info = 
            new GeometryIntersectionInformation();

        nearestObject = trazar_rayo_en_escena(inRay, in_objetos);
        if ( nearestObject != null ) {
            //------------------------------------------------------------
            po = nearestObject.getPosition();
            R = nearestObject.getRotation();
            Ri = nearestObject.getRotationInverse();
            myRay = new Ray ( 
                Ri.multiply(inRay.origin.substract(po) ),
                Ri.multiply(inRay.direction)
            );
            myRay.t = inRay.t;

            nearestObject.getGeometry().doExtraInformation(myRay, 
                                                                myRay.t,
                                                                info);
            //------------------------------------------------------------
            v.x = -inRay.direction.x;
            v.y = -inRay.direction.y;
            v.z = -inRay.direction.z;

            p = R.multiply(info.p).add(po);
            n = R.multiply(info.n);

            c = modelo_de_iluminacion(
                p, n, v, in_luces, in_objetos, in_fondo, 
                nearestObject.getMaterial());
          }
          else {
            c = in_fondo.colorInDireccion(inRay.direction);
        }
        return c;
    }

    /**
    Macroalgoritmo de control para raytracing. Este m&eacute;todo recibe
    el modelo de una escena 3D previamente construida en memoria y una
    imagen, y modifica la imagen de tal forma que contiene una visualizacion
    de la escena, resultado de aplicar la t&eacute;cnica de raytracing.

    PAR&Aacute;METROS:
    - `inout_viewport`: imagen RGB en donde el algoritmo calcular&aacute; su
       resultado.
    - `in_objetos`: arreglo din&aacute;mico de RayableObjects que constituyen los
       objetos visibles de la escena.
    - `in_luces`: arreglo din&aacute;mico de Light'es (luces puntuales)
    - `in_fondo`: especificaci&oacute;n de un color de fondo para la escena
      (i.e. el color que se ve si no se ve ning&uacute;n objeto!)
    - `in_camara`: especificaci&oacute;n de la transformaci&oacute;n de
      proyecci&oacute;n 3D a 2D que se lleva a cabo en el proceso de 
      visualizaci&oacute;n.

    PRE:
    - Todas las referencias estan creadas, asi sea que apunten a estructuras
      vac&iacute;as.
    - La imagen `inout_viewport` esta creada, y es de el tama&ntilde;o que
      el usuario desea para su visualizaci&oacute;n.

    POST:
    - `inout_viewport` contiene una representaci&oacute;n visual de la
       escena 3D (`in_objetos`, `in_luces`, `in_fondo`), tal que corresponde a
       una proyecci&oacute;n 3D a 2D controlada por la c&aacute;mara
       virtual `in_camara`.

    NOTA: Este algoritmo se inici&oacute; como una modificaci&oacute;n del 
          raytracer del curso 6.837 (computaci&oacute;n gr&aacute;fica) de MIT,
          original de Leonard McMillan y Tomas Lozano Perez, pero puede 
          considerarse que es una re-escritura y re-estructuraci&oacute;n 
          completa de Oscar Chavarro.
    */
    public void execute(RGBImage inoutViewport, 
                         ArrayList inRayableObjectArray,
                         ArrayList in_arr_luces,
                         Background in_fondo,
                         Camera in_camara,
                         ProgressMonitor report)
    {
        int x, y;
        Ray rayo;
        ColorRgb color;

        in_camara.updateVectors();

        report.begin();
        for ( y = 0; y < inoutViewport.getYSize(); y++ ) {
            report.update(0, inoutViewport.getYSize(), y);
            for ( x = 0; x < inoutViewport.getXSize(); x++ ) {
                //- Trazado individual de un rayo --------------------------
                // Es importante que la operacion generateRay sea inline
                // (i.e. "final")
                rayo = in_camara.generateRay(x, y);
                color = seguimiento_rayo(rayo, inRayableObjectArray, 
                    in_arr_luces, in_fondo);

                //- Exporto el resultado de color del pixel ----------------
                inoutViewport.putPixel(x, y, (byte)(255 * color.r), 
                                              (byte)(255 * color.g), 
                                              (byte)(255 * color.b));
            }
        }
        report.end();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

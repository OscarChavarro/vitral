//===========================================================================

package vitral_transition.framework.visual;

import java.util.Vector;
import java.util.Enumeration;

import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.environment.Ray;
import vitral.toolkits.environment.Camera;
import vitral.toolkits.environment.Material;
import vitral_transition.toolkits.environment.Light2;
import vitral.toolkits.environment.Background;
import vitral.toolkits.geometry.Geometry;
import vitral_transition.toolkits.media.RGBImage;

/**
Esta clase solo provee un m&eacute;todo que encapsula un algoritmo de
control de visualizaci&oacute;n. Su raz&oacute;n de ser (y por la cual
NO poner ese m&eacute;todo en la clase cliente que la utiliza) es facilitar
la abstracci&oacute;n de la operaci&oacute;n de visualizaci&oacute;n, la
cual puede ser cambiada por otro algoritmo de visualizaci&oacute;n (i.e.
zbuffer o radiosidad), pero manteniendo el mismo modelo de escena 3D.
*/
public class RaytracerMIT {
    private static final float INFINITO = Float.MAX_VALUE;
    private static final float TINY = 0.001f;
    private Vector3D static_tmp;

    public RaytracerMIT()
    {
        // Machete similar al descrito en Sphere::intersect
        static_tmp = new Vector3D();
    }

    //   the point of intersection (p)
    //   a unit-length surface normal (n)
    //   a unit-length vector towards the ray's origin (v)
    public ColorRgb modelo_de_iluminacion(Vector3D p, Vector3D n, Vector3D v, 
        Vector lights, Vector objects, Background fondo,
        Material m) {

        ColorRgb resultado = new ColorRgb();
        Geometry objeto_mas_cercano;
    ColorRgb color_de_fondo = fondo.color_en_direccion(n);
        ColorRgb ambient;
        ColorRgb diffuse;
        ColorRgb specular;

        for ( Enumeration lightSources = lights.elements();
              lightSources.hasMoreElements(); ) {
            Light2 luz = (Light2)lightSources.nextElement();
            if ( luz.tipo_de_luz == Light2.AMBIENTE ) {
                ambient = m.getAmbient();
                resultado.r += ambient.r*luz.ir;
                resultado.g += ambient.g*luz.ig;
                resultado.b += ambient.b*luz.ib;
              } 
              else {
                Vector3D l;
                if ( luz.tipo_de_luz == Light2.PUNTUAL ) {
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
                objeto_mas_cercano =
                    trazar_rayo_en_escena(rayo_sombra, objects);
                if ( objeto_mas_cercano != null ) {
                    continue;
                }

                double lambert = n.dotProduct(l);
                if ( lambert > 0 ) {
                    diffuse = m.getDiffuse();
                    if ( (diffuse.r + diffuse.g + diffuse.b) > 0 ) {
                        resultado.r += lambert*diffuse.r*luz.ir;
                        resultado.g += lambert*diffuse.g*luz.ig;
                        resultado.b += lambert*diffuse.b*luz.ib;
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
                            resultado.r += spec*luz.ir;
                            resultado.g += spec*luz.ig;
                            resultado.b += spec*luz.ib;
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
                objeto_mas_cercano = 
                    trazar_rayo_en_escena(rayo_reflejado, objects);
                if ( objeto_mas_cercano != null ) {
                    Vector3D rp = new Vector3D();
                    Vector3D rn = new Vector3D();
                    Vector3D rv = new Vector3D();

                    objeto_mas_cercano.informacion_extra(
                        rayo_reflejado, rayo_reflejado.t, rp, rn);
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
        resultado.r = (resultado.r > 1f) ? 1f : resultado.r;
        resultado.g = (resultado.g > 1f) ? 1f : resultado.g;
        resultado.b = (resultado.b > 1f) ? 1f : resultado.b;
        return resultado;
    }

    /**
    Si el `in_rayo` se intersecta con al menos uno de los `in_arr_objetos`,
    se retorna una referencia al objeto mas cercano de los intersectados.
    De lo contrario se retorna null.
    */
    public Geometry 
    trazar_rayo_en_escena(Ray in_rayo, Vector in_arr_objetos) {
        Enumeration i;
        Geometry gi;
        Geometry objeto_mas_cercano;

        in_rayo.t = INFINITO;
        objeto_mas_cercano = null;

        for ( i = in_arr_objetos.elements(); i.hasMoreElements(); ) {
            gi = (Geometry)i.nextElement();
            if ( gi.interseccion(in_rayo) ) {
                objeto_mas_cercano = gi;
            }
        }
        return objeto_mas_cercano;
    }

    private ColorRgb seguimiento_rayo(Ray rayo, Vector in_objetos, 
                         Vector in_luces, Background in_fondo)
    {
        ColorRgb c;
        Vector3D p = new Vector3D();
        Vector3D n = new Vector3D();
        Vector3D v = new Vector3D();
        Geometry objeto_mas_cercano;

        objeto_mas_cercano = trazar_rayo_en_escena(rayo, in_objetos);
        if ( objeto_mas_cercano != null ) {
            objeto_mas_cercano.informacion_extra(rayo, rayo.t,
                                                      p, n);
            v.x = -rayo.direction.x;
            v.y = -rayo.direction.y;
            v.z = -rayo.direction.z;
            c = modelo_de_iluminacion(
                p, n, v, in_luces, in_objetos, in_fondo, 
                objeto_mas_cercano.material);
          }
          else {
            c = in_fondo.color_en_direccion(rayo.direction);
        }
        return c;
    }

/*
    // SIN MODELO DE ILUMINACION: SOLO Light AMBIENTE (Borrar luego de aqui,
    // dejar solo en el estudio paso a paso de las etapas!
    private ColorRgb seguimiento_rayo(Ray rayo, Vector in_objetos, 
                         Vector in_luces, ColorRgb in_fondo)
    {
        ColorRgb c;
        Geometry objeto_mas_cercano;

        c = in_fondo;
        objeto_mas_cercano = trazar_rayo_en_escena(rayo, in_objetos);
        if ( objeto_mas_cercano != null ) {
            c.r = objeto_mas_cercano.material.ir;
            c.g = objeto_mas_cercano.material.ig;
            c.b = objeto_mas_cercano.material.ib;
        }
        return c;
    }
*/

    /**
    Macroalgoritmo de control para raytracing. Este m&eacute;todo recibe
    el modelo de una escena 3D previamente construida en memoria y una
    imagen, y modifica la imagen de tal forma que contiene una visualizacion
    de la escena, resultado de aplicar la t&eacute;cnica de raytracing.

    PAR&Aacute;METROS:
    - `inout_viewport`: imagen RGB en donde el algoritmo calcular&aacute; su
       resultado.
    - `in_objetos`: arreglo din&aacute;mico de Geometrys que constituyen los
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
    public void ejecutar(RGBImage inout_viewport, 
             Vector in_arr_objetos,
             Vector in_arr_luces,
             Background in_fondo,
             Camera in_camara)
    {
        int x, y;
        Ray rayo;
        ColorRgb color;

        in_camara.updateVectors();

        //System.out.println(in_camara);

        System.out.print("[");

        for ( y = 0; y < inout_viewport.ytam(); y++ ) {
            if ( y % 10 == 0 ) {
                System.out.print(".");
            }
            for ( x = 0; x < inout_viewport.xtam(); x++ ) {
                //- Trazado individual de un rayo --------------------------
                // Es importante que la operacion generateRay sea inline
                // (i.e. "final")
                rayo = in_camara.generateRay(x, y);
                color = seguimiento_rayo(rayo, in_arr_objetos, 
                    in_arr_luces, in_fondo);

                //- Exporto el resultado de color del pixel ----------------
                inout_viewport.putpixel(x, y, (byte)(255 * color.r), 
                                              (byte)(255 * color.g), 
                                              (byte)(255 * color.b));
            }
        }
        System.out.println("]");
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

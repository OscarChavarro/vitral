//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [WHIT1980] Whitted, Turner. "An Improved Illumination Model for Shaded  =
//=            Display", 1980.                                              =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - February 13 2006 - Oscar Chavarro: updated raytracing code to manage  =
//=   rayable objects, with geometries implementing intersection operation  =
//=   in an object-coordinate basis.                                        =
//= - May 16 2006 - Alfonso Barbosa: modify to manage ZBuffers              =
//= - November 1 2006 - Alfonso Barbosa / Diana Reyes: exceute generalized  =
//=   for inclusion of sub-viewport spec.                                   =
//= - November 19 2006 - Oscar Chavarro: material handling supporting       =
//=   submaterials inside geometry.                                         =
//===========================================================================

package vsdk.toolkit.render;

import java.util.ArrayList;
import java.util.Iterator;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ProgressMonitor;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.scene.SimpleBody;


/**
This class provides an encaptulation for a rendering algorithm, 
implementing simple recursive raytracing as presented in [WHIT1980].
This class is appropiate to play a role of "concrete strategy" in
a "Strategy" design pattern.
*/
public class Raytracer {
    private Vector3D static_tmp;
    private static final double TINY = 0.0001;

    public Raytracer()
    {
        // Machete similar al descrito en Sphere::intersect
        static_tmp = new Vector3D();
    }

    /*
    @param p the point of intersection
    @param n unit-length surface normal
    @param v unit-length vector towards the ray's origin

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations. (This must be taken
    into account in the reflection and refraction calculations)
    */
    private ColorRgb evaluateIlluminationModel(Vector3D p, Vector3D n, Vector3D v, 
        ArrayList lights, ArrayList objects, Background background,
        Material material) {

        SimpleBody nearestObject;
        ColorRgb result = new ColorRgb();
        ColorRgb backgroundColor = background.colorInDireccion(n);
        ColorRgb ambient;
        ColorRgb diffuse;
        ColorRgb specular;
        ColorRgb lightEmission;
        Ray myRay;
        Vector3D po;
        Matrix4x4 R, Ri;

        for ( Iterator i = lights.iterator(); i.hasNext(); ) {
            Light light = (Light)i.next();
            lightEmission = light.getSpecular();

            if ( light.tipo_de_luz == Light.AMBIENT ) {
                ambient = material.getAmbient();
                result.r += ambient.r*lightEmission.r;
                result.g += ambient.g*lightEmission.g;
                result.b += ambient.b*lightEmission.b;
              } 
              else {
                Vector3D l;
                if ( light.tipo_de_luz == Light.POINT ) {
                    l = new Vector3D(light.lvec.x - p.x, 
                                     light.lvec.y - p.y, 
                                     light.lvec.z - p.z);
                    l.normalize();
                  } 
                  else {
                    l = new Vector3D(-light.lvec.x, -light.lvec.y, -light.lvec.z);
                }

                // Check if the surface point is in shadow
                Vector3D poffset = 
                    new Vector3D(p.x + VSDK.EPSILON*l.x, p.y + VSDK.EPSILON*l.y, p.z + VSDK.EPSILON*l.z);
                Ray rayo_sombra = new Ray(poffset, l);
                nearestObject = selectNearestThingInRayDirection(rayo_sombra, objects);
                if ( nearestObject != null ) {
                    continue;
                }

                double lambert = n.dotProduct(l);
                if ( lambert > 0 ) {
                    diffuse = material.getDiffuse();
                    if ( (diffuse.r + diffuse.g + diffuse.b) > 0 ) {
                        result.r += lambert*diffuse.r*lightEmission.r;
                        result.g += lambert*diffuse.g*lightEmission.g;
                        result.b += lambert*diffuse.b*lightEmission.b;
                    }
                    specular = material.getSpecular();
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
                                (float) Math.pow((double) spec, (double)material.getPhongExponent()));
                            result.r += spec*lightEmission.r;
                            result.g += spec*lightEmission.g;
                            result.b += spec*lightEmission.b;
                        }
                    }
                }
              } // else case of "if ( light.tipo_de_luz == Light.AMBIENT )" conditional
        } // for ( Iterator i = lights.iterator(); i.hasNext(); )

        // Compute illumination due to reflection
        double kr = material.getReflectionCoefficient();
        if ( kr > 0 ) {
            double t = v.dotProduct(n);
            if ( t > 0 ) {
                t *= 2;
                Vector3D reflect = new Vector3D(t*n.x - v.x, 
                                                t*n.y - v.y, 
                                                t*n.z - v.z);
                Vector3D poffset = new Vector3D(p.x + VSDK.EPSILON*reflect.x, 
                                                p.y + VSDK.EPSILON*reflect.y, 
                                                p.z + VSDK.EPSILON*reflect.z);
                Ray rayo_reflejado = new Ray(poffset, reflect);
                nearestObject = 
                    selectNearestThingInRayDirection(rayo_reflejado, objects);
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
                        evaluateIlluminationModel(rp, rn, rv, lights, objects, 
                            background, material);

                    result.r += kr*rcolor.r;
                    result.g += kr*rcolor.g;
                    result.b += kr*rcolor.b;
                  } 
                  else {
                    result.r += kr*backgroundColor.r;
                    result.g += kr*backgroundColor.g;
                    result.b += kr*backgroundColor.b;
                }
            }
        }

        // Add code for refraction here
        // <TODO>

        // Clamp result to MAX 1.0 intensity.
        result.r = (result.r > 1) ? 1 : result.r;
        result.g = (result.g > 1) ? 1 : result.g;
        result.b = (result.b > 1) ? 1 : result.b;

        return result;
    }

    /**
    This method intersect the `inOut_Ray` with all of the geometries contained
    in `inSimpleBodyArray`. If none of the geometries is intersected
    `null` is returned, otherwise a reference to the containing SimpleBody
    is returned.

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations.
    */
    private SimpleBody 
    selectNearestThingInRayDirection(Ray inOut_Ray, ArrayList inSimpleBodyArray) {
        Iterator i;
        SimpleBody gi;
        SimpleBody nearestObject;
        double nearestDistance;

        nearestDistance = Double.MAX_VALUE;
        nearestObject = null;
        for ( i = inSimpleBodyArray.iterator(); i.hasNext(); ) {
            inOut_Ray.t = Double.MAX_VALUE;
            gi = (SimpleBody)i.next();
            if ( gi.doIntersection(inOut_Ray) && 
                 inOut_Ray.t < nearestDistance &&
                 inOut_Ray.t > VSDK.EPSILON ) {
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
    private ColorRgb followRayPath(Ray inRay, ArrayList in_objetos, 
                         ArrayList in_luces, Background in_background)
    {
        ColorRgb c;
        Vector3D v = new Vector3D();
        Vector3D p, n;
        SimpleBody nearestObject;
        Ray myRay;
        Vector3D po;
        Matrix4x4 R, Ri;
        GeometryIntersectionInformation info = 
            new GeometryIntersectionInformation();

        nearestObject = selectNearestThingInRayDirection(inRay, in_objetos);
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

            Material material;
            if ( info.material != null ) {
                material = info.material;
            }
            else {
                material = nearestObject.getMaterial();
            }

            c = evaluateIlluminationModel(
                    p, n, v, in_luces, in_objetos, in_background, material);
          }
          else {
            c = in_background.colorInDireccion(inRay.direction);
        }
        return c;
    }

    public void execute(RGBImage inoutViewport, 
                        ArrayList inSimpleBodyArray,
                        ArrayList in_arr_luces,
                        Background in_background,
                        Camera in_camara,
                        ProgressMonitor report)
    {
        execute(inoutViewport, inSimpleBodyArray, in_arr_luces,
                in_background, in_camara, report, null, 0, 0,
                inoutViewport.getXSize(), inoutViewport.getYSize());
    }

    public void execute(RGBImage inoutViewport, 
                        ArrayList inSimpleBodyArray,
                        ArrayList in_arr_luces,
                        Background in_background,
                        Camera in_camara,
                        ProgressMonitor report,
                        ZBuffer depthmap)
    {
        execute(inoutViewport, inSimpleBodyArray, in_arr_luces,
                in_background, in_camara, report, depthmap, 0, 0,
                inoutViewport.getXSize(), inoutViewport.getYSize());
    }

    /**
    Macroalgoritmo de control para raytracing. Este m&eacute;todo recibe
    el modelo de una escena 3D previamente construida en memoria y una
    imagen, y modifica la imagen de tal forma que contiene una visualizacion
    de la escena, result de aplicar la t&eacute;cnica de raytracing.

    PAR&Aacute;METROS:
    - `inout_viewport`: imagen RGB en donde el algoritmo calcular&aacute; su
       result.
    - `in_objetos`: arreglo din&aacute;mico de SimpleBodys que constituyen los
       objetos visibles de la escena.
    - `in_luces`: arreglo din&aacute;mico de Light'es (luces puntuales)
    - `in_background`: especificaci&oacute;n de un color de fondo para la escena
      (i.e. el color que se ve si no se ve ning&uacute;n objeto!)
    - `in_camara`: especificaci&oacute;n de la transformaci&oacute;n de
      proyecci&oacute;n 3D a 2D que se lleva a cabo en el proceso de 
      visualizaci&oacute;n.
    - `depthmap`: can be null or a reference to a ZBuffer. If it is null,
      nothing is done with this parameter. If it is not null, the associated
      ZBuffer is filled with depth values corresponding to distances 
      calculated in world space coordinates from ray intersections.
      Note that depth values are not scaled neither clamped to any specific
      range, so post-processing should be done if wanting to combine that
      with other depth maps, as those generated from OpenGL's ZBuffer.


    PRE:
    - Todas las referencias estan creadas, asi sea que apunten a estructuras
      vac&iacute;as.
    - La imagen `inout_viewport` esta creada, y es de el tama&ntilde;o que
      el usuario desea para su visualizaci&oacute;n.
    - In the case the ZBuffer depthmap is not null, the ZBuffer must be
      initialized to the same size of the image inoutViewport.

    POST:
    - `inout_viewport` contiene una representaci&oacute;n visual de la
       escena 3D (`in_objetos`, `in_luces`, `in_background`), tal que corresponde a
       una proyecci&oacute;n 3D a 2D controlada por la c&aacute;mara
       virtual `in_camara`.

    NOTA: Este algoritmo se inici&oacute; como una modificaci&oacute;n del 
          raytracer del curso 6.837 (computaci&oacute;n gr&aacute;fica) de MIT,
          original de Leonard McMillan y Tomas Lozano Perez, pero puede 
          considerarse que es una re-escritura y re-estructuraci&oacute;n 
          completa de Oscar Chavarro.
    */
    public void execute(RGBImage inoutViewport,
                        ArrayList inSimpleBodyArray,
                        ArrayList in_arr_luces,
                        Background in_background,
                        Camera in_camara,
                        ProgressMonitor report,
                        ZBuffer depthmap,
                        int limx1, int limy1,
                        int limx2, int limy2)
    {
        int x, y;
        int relativeX;
        int relativeY;
        Ray rayo;
        ColorRgb color;

        in_camara.updateVectors();

        report.begin();
        for ( y = limy1, relativeY = 0; y < limy2; y++, relativeY++ ) {
            report.update(0, inoutViewport.getYSize(), y);
            for ( x = limx1, relativeX = 0; x < limx2; x++, relativeX++ ) {
                //- Trazado individual de un rayo --------------------------
                // Es importante que la operacion generateRay sea inline
                // (i.e. "final")
                rayo = in_camara.generateRay(x, y);
                color = followRayPath(rayo, inSimpleBodyArray,
                    in_arr_luces, in_background);
                if ( depthmap != null ) {
                    depthmap.setZ(x, y, (float)rayo.t);
                }
                //- Exporto el result de color del pixel ----------------
                inoutViewport.putPixel(relativeX, relativeY,
                                              (byte)(255 * color.r),
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

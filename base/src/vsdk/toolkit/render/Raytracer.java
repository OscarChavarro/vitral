//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//= [WHIT1980] Whitted, Turner. "An Improved Illumination Model for Shaded  =
//=            Display", 1980.                                              =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - February 13 2006 - Oscar Chavarro: updated raytracing code to manage  =
//=   rayable objects, with geometries implementing intersection operation  =
//=   in an object-coordinate basis.                                        =
//= - May 16 2006 - Alfonso Barbosa: modify to manage ZBuffers              =
//= - November 1 2006 - Alfonso Barbosa / Diana Reyes: execute generalized  =
//=   for inclusion of sub-viewport spec.                                   =
//= - November 19 2006 - Oscar Chavarro: material handling supporting       =
//=   submaterials inside geometry.                                         =
//= - December 20 2006 - Oscar Chavarro: bump mapping added                 =
//===========================================================================

package vsdk.toolkit.render;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.scene.SimpleBody;

import vsdk.toolkit.gui.ProgressMonitor;

/**
This class provides an encaptulation for a rendering algorithm, 
implementing simple recursive raytracing as presented in [WHIT1980].
Includes a normal perturbation for the simulation of wrinkled surfaces,
as described in [BLIN1978b].
This class is appropiate to play a role of "concrete strategy" in
a "Strategy" design pattern.

@todo Upgrade ArrayList management to Java 1.5 code style (typed templates)
*/
public class Raytracer extends RenderingElement {
    private Vector3D static_tmp;
    private static final double TINY = 0.0001;

    public Raytracer()
    {
        // Machete similar al descrito en Sphere::intersect
        static_tmp = new Vector3D();
    }

    /*
    @param info.p the point of intersection
    @param info.n unit-length surface normal
    @param v unit-length vector towards the ray's origin

    Note: The info datastructure must contain point and normal in world
    coordinates.

    Warning: This method includes the use of the ray transformation technique
    that permits the representation of geometries centered in its origin,
    and its combination with geometric transformations. (This must be taken
    into account in the reflection and refraction calculations)

    @todo Check the inconsistent use of tangent vector in bump mapping
    calculation... it is non sense to always be <0, 1, 0>.
    */
    private ColorRgb evaluateIlluminationModel(
        GeometryIntersectionInformation info, Vector3D v, 
        ArrayList <Light> lights, ArrayList <SimpleBody> objects,
        Background background,
        Material material, RendererConfiguration inQualitySelection) {
        //-----------------------------------------------------------------
        SimpleBody nearestObject;
        ColorRgb result = new ColorRgb();
        ColorRgb backgroundColor = background.colorInDireccion(info.n);
        ColorRgb ambient;
        ColorRgb diffuse;
        ColorRgb specular;
        ColorRgb lightEmission;

        //- Normal perturbation / bump mapping ----------------------------
        // This code follows the variable name convention used on equation
        // [FOLE1992].16.23, section [FOLE1992].16.3.3.
        //-----------------------------------------------------------------
        if ( info.normalMap != null ) {
            // Information inherent to current geometry
            Vector3D N;                      // Normal vector on surface
            Vector3D Ps;                     // Tangent vector on surface
            Vector3D Pt;                     // Binormal vector on surface
            // Information extracted from precomputed normal map (after F)
            Vector3D normalVariation;        // Normal variation for point
                                             // at texture coordinates (u, v)
            double Bu;                       // dF/du for bumpmap F
            double Bv;                       // dF/dv for bumpmap F
            // Auxiliary variables
            Vector3D normalPerturbation;
            Vector3D NxPt;
            Vector3D NxPs;

            normalVariation = info.normalMap.getNormal(info.u, 1-info.v);
            if ( normalVariation != null ) {
                // Evaluation of [BLIN1978b]/[FOLE1992].16.23 equation
                N = info.n;    N.normalize();
                Ps = info.t;   Ps.normalize();

                // This is non-sense, but it works! Currently not using
                // tangent vector from geometry! Explain this!
                Ps.x = 0; Ps.y = 1; Ps.z = 0;

                Pt = N.crossProduct(Ps);
                NxPt = N.crossProduct(Pt);
                NxPs = N.crossProduct(Ps);
                Bu = normalVariation.x;
                Bv = normalVariation.y;
                // Note: this only works when `N` is a unit vector. If not,
                //      `normalPerturbation` must be divided by N's length
                normalPerturbation =
                    NxPt.multiply(Bu).substract(NxPs.multiply(Bv));
                info.n = info.n.add(normalPerturbation);
                info.n.normalize();
            }
        }

        //-----------------------------------------------------------------
        //-----------------------------------------------------------------
        int i;
        for ( i = 0; i< lights.size(); i++ ) {
            Light light = lights.get(i);
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
                    l = new Vector3D(light.lvec.x - info.p.x, 
                                     light.lvec.y - info.p.y, 
                                     light.lvec.z - info.p.z);
                    l.normalize();
                  } 
                  else {
                    l = new Vector3D(-light.lvec.x, -light.lvec.y, -light.lvec.z);
                }

                // Check if the surface point is in shadow
                Vector3D poffset = 
                    new Vector3D(info.p.x + VSDK.EPSILON*l.x, info.p.y + VSDK.EPSILON*l.y, info.p.z + VSDK.EPSILON*l.z);
                Ray shadowRay = new Ray(poffset, l);
                nearestObject = selectNearestThingInRayDirection(shadowRay, objects);
                if ( nearestObject != null ) {
                    continue;
                }

                double lambert = info.n.dotProduct(l);
                if ( lambert > 0 ) {
                    diffuse = material.getDiffuse();
                    if ( info.texture != null ) {
                        diffuse.modulate(
                            info.texture.getColorRgbBiLinear(info.u, 1-info.v));
                    }
                    if ( (diffuse.r + diffuse.g + diffuse.b) > 0 ) {
                        result.r += lambert*diffuse.r*lightEmission.r;
                        result.g += lambert*diffuse.g*lightEmission.g;
                        result.b += lambert*diffuse.b*lightEmission.b;
                    }
                    specular = material.getSpecular();
                    if ( (specular.r + specular.g + specular.b) > 0 ) {
                        lambert *= 2;

                        static_tmp.x = lambert*info.n.x - l.x;
                        static_tmp.y = lambert*info.n.y - l.y;
                        static_tmp.z = lambert*info.n.z - l.z;
                        double spec = 
                            v.dotProduct(static_tmp);

                        if ( spec > 0 ) {
                            // OJO: Raro...
                            spec = ((specular.r + specular.g + specular.b)/3)*(
                                Math.pow(spec, material.getPhongExponent()));
                            result.r += spec*lightEmission.r;
                            result.g += spec*lightEmission.g;
                            result.b += spec*lightEmission.b;
                        }
                    }
                }
              } // else case of "if ( light.tipo_de_luz == Light.AMBIENT )" conditional
        } // for ( i = 0; i< lights.size(); i++ )

        // Compute illumination due to reflection
        double kr = material.getReflectionCoefficient();
        if ( kr > 0 ) {
            double t = v.dotProduct(info.n);
            if ( t > 0 ) {
                t *= 2;
                Vector3D reflect = new Vector3D(t*info.n.x - v.x, 
                                                t*info.n.y - v.y, 
                                                t*info.n.z - v.z);
                Vector3D poffset = new Vector3D(info.p.x + VSDK.EPSILON*reflect.x, 
                                                info.p.y + VSDK.EPSILON*reflect.y, 
                                                info.p.z + VSDK.EPSILON*reflect.z);
                Ray reflected_ray = new Ray(poffset, reflect);
                nearestObject = 
                    selectNearestThingInRayDirection(reflected_ray, objects);
                if ( nearestObject != null ) {
                    Vector3D rv = new Vector3D();
                    GeometryIntersectionInformation subInfo = 
                        new GeometryIntersectionInformation();

                    //--------------------------------------------------------
                    nearestObject.doExtraInformation(
                        reflected_ray, reflected_ray.t, subInfo);

                    //-----
                    if ( !inQualitySelection.isTextureSet() ) {
                        subInfo.texture = null;
                    }
                    else {
                        if ( subInfo.texture == null ) {
                            subInfo.texture = nearestObject.getTexture();
                        }
                    }

                    //-----
                    if ( !inQualitySelection.isBumpMapSet() ) {
                        subInfo.normalMap = nearestObject.getNormalMap();
                    }

                    //--------------------------------------------------------

                    rv.x = -reflected_ray.direction.x;
                    rv.y = -reflected_ray.direction.y;
                    rv.z = -reflected_ray.direction.z;                    
                    ColorRgb rcolor =
                        evaluateIlluminationModel(subInfo, rv, lights, objects, 
                                                  background, material, 
                                                  inQualitySelection);

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
    selectNearestThingInRayDirection(Ray inOut_Ray, ArrayList <SimpleBody> inSimpleBodyArray) {
        int i;
        SimpleBody gi;
        SimpleBody nearestObject;
        double nearestDistance;

        nearestDistance = Double.MAX_VALUE;
        nearestObject = null;
        for ( i = 0; i < inSimpleBodyArray.size(); i++ ) {
            inOut_Ray.t = Double.MAX_VALUE;
            gi = inSimpleBodyArray.get(i);
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

    Note that this method can return null, that means a transparent pixel
    should be used.
    */
    private ColorRgb followRayPath(Ray inRay,
                                  ArrayList <SimpleBody> in_objetos, 
                                  ArrayList <Light> in_luces,
                                  Background in_background,
                                  RendererConfiguration inQualitySelection)
    {
        ColorRgb c;
        Vector3D v = new Vector3D();
        SimpleBody nearestObject;
        Ray myRay;
        GeometryIntersectionInformation info = 
            new GeometryIntersectionInformation();

        nearestObject = selectNearestThingInRayDirection(inRay, in_objetos);
        if ( nearestObject != null ) {
            //------------------------------------------------------------
            nearestObject.doExtraInformation(inRay, inRay.t, info);
            //-----
            if ( !inQualitySelection.isTextureSet() ) {
                info.texture = null;
            }
            else {
                if ( info.texture == null ) {
                    info.texture = nearestObject.getTexture();
                }
            }

            //-----
            if ( !inQualitySelection.isBumpMapSet() ) {
                info.normalMap = nearestObject.getNormalMap();
            }

            //------------------------------------------------------------
            v.x = -inRay.direction.x;
            v.y = -inRay.direction.y;
            v.z = -inRay.direction.z;

            Material material;
            if ( info.material != null ) {
                material = info.material;
            }
            else {
                material = nearestObject.getMaterial();
            }

            c = evaluateIlluminationModel(
                info, v, in_luces, in_objetos, in_background, material,
                inQualitySelection);
          }
          else {
            c = in_background.colorInDireccion(inRay.direction);
        }
        return c;
    }

    public void execute(RGBImage inoutViewport,
                        RendererConfiguration inQualitySelection,
                        ArrayList <SimpleBody> inSimpleBodyArray,
                        ArrayList <Light> in_arr_luces,
                        Background in_background,
                        Camera inCamera,
                        ProgressMonitor report)
    {
        execute(inoutViewport, inQualitySelection,
                inSimpleBodyArray, in_arr_luces,
                in_background, inCamera, report, null, 0, 0,
                inoutViewport.getXSize(), inoutViewport.getYSize());
    }

    public void execute(RGBImage inoutViewport, 
                        RendererConfiguration inQualitySelection,
                        ArrayList <SimpleBody> inSimpleBodyArray,
                        ArrayList <Light> in_arr_luces,
                        Background in_background,
                        Camera inCamera,
                        ProgressMonitor report,
                        ZBuffer depthmap)
    {
        execute(inoutViewport, inQualitySelection, inSimpleBodyArray, 
                in_arr_luces,
                in_background, inCamera, report, depthmap, 0, 0,
                inoutViewport.getXSize(), inoutViewport.getYSize());
    }

    /**
    Macroalgoritmo de control para raytracing. Este m&eacute;todo recibe
    el modelo de una escena 3D previamente construida en memoria y una
    imagen, y modifica la imagen de tal forma que contiene una visualizacion
    de la escena, result de aplicar la t&eacute;cnica de raytracing.

    PARAMETERS
    - `inout_viewport`: imagen RGB en donde el algoritmo calcular&aacute; su
       result.
    - `in_objetos`: arreglo din&aacute;mico de SimpleBodys que constituyen los
       objetos visibles de la escena.
    - `in_luces`: arreglo din&aacute;mico de Light'es (luces puntuales)
    - `in_background`: especificaci&oacute;n de un color de fondo para la escena
      (i.e. el color que se ve si no se ve ning&uacute;n objeto!)
    - `inCamera`: especificaci&oacute;n de la transformaci&oacute;n de
      proyecci&oacute;n 3D a 2D que se lleva a cabo en el proceso de 
      visualizaci&oacute;n.
    - `depthmap`: can be null or a reference to a ZBuffer. If it is null,
      nothing is done with this parameter. If it is not null, the associated
      ZBuffer is filled with depth values corresponding to distances 
      calculated in world space coordinates from ray intersections.
      Note that depth values are not scaled neither clamped to any specific
      range, so post-processing should be done if wanting to combine that
      with other depth maps, as those generated from OpenGL's ZBuffer.
    - `liveReport` can be null. In that case no report is updated.


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
       virtual `inCamera`.

    NOTA: Este algoritmo se inici&oacute; como una modificaci&oacute;n del 
          raytracer del curso 6.837 (computaci&oacute;n gr&aacute;fica) de MIT,
          original de Leonard McMillan y Tomas Lozano Perez, pero puede 
          considerarse que es una re-escritura y re-estructuraci&oacute;n 
          completa de Oscar Chavarro.
    */
    public void execute(RGBImage inoutViewport,
                        RendererConfiguration inQualitySelection,
                        ArrayList <SimpleBody> inSimpleBodyArray,
                        ArrayList <Light> inLightsArray,
                        Background inBackground,
                        Camera inCamera,
                        ProgressMonitor liveReport,
                        ZBuffer outDepthmap,
                        int limx1, int limy1,
                        int limx2, int limy2)
    {
        int x, y;
        int relativeX;
        int relativeY;
        Ray rayo;
        ColorRgb color;

        inCamera.updateVectors();

        if ( liveReport != null ) {
            liveReport.begin();
        }
        for ( y = limy1, relativeY = 0; y < limy2; y++, relativeY++ ) {
            if ( liveReport != null ) {
                liveReport.update(0, inoutViewport.getYSize(), y);
            }
            for ( x = limx1, relativeX = 0; x < limx2; x++, relativeX++ ) {
                //- Trazado individual de un rayo --------------------------
                // Es importante que la operacion generateRay sea inline
                // (i.e. "final")
                rayo = inCamera.generateRay(x, y);
                color = followRayPath(rayo, inSimpleBodyArray,
                                      inLightsArray, inBackground, 
                                      inQualitySelection);
                if ( outDepthmap != null ) {
                    outDepthmap.setZ(x, y, (float)rayo.t);
                }
                //- Exporto el result de color del pixel ----------------
                if ( color != null ) {
                    inoutViewport.putPixel(relativeX, relativeY,
                                              (byte)(255 * color.r),
                                              (byte)(255 * color.g),
                                              (byte)(255 * color.b));
                }
            }
        }
        if ( liveReport != null ) {
            liveReport.end();
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

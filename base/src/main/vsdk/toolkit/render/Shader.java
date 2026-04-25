//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package vsdk.toolkit.render;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RaytraceStatistics;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.LightType;

final class Shader {
    private Shader()
    {
    }

    /**
    Performs local illumination evaluation over the current hit point:
    ambient + diffuse + specular + bump perturbation + shadow checks.

    Returns the final perturbed normal used in the local shading stage so
    callers can reuse it in recursive reflection/refraction steps.
    */
    static Vector3D shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        Material material,
        RenderContext renderContext,
        TraceWorkspace workspace,
        ColorRgb outColor)
    {
        Vector3D surfaceNormal = info.n;

        //- Normal perturbation / bump mapping ----------------------------
        // This code follows the variable name convention used on equation
        // [FOLE1992].16.23, section [FOLE1992].16.3.3.
        //-----------------------------------------------------------------
        if ( info.normalMap != null ) {
            // Information inherent to current geometry
            Vector3D N;                      // Normal vector on surface
            Vector3D Pu;                     // Local tangent basis (u direction)
            Vector3D Pv;                     // Local tangent basis (v direction)
            // Information extracted from precomputed bump-derived field
            Vector3D normalVariation;        // Sampled vector at (u, v)
            double Fu;                       // dF/du term for bumpmap F
            double Fv;                       // dF/dv term for bumpmap F
            // Auxiliary variables
            Vector3D normalPerturbation;
            Vector3D NxPt;
            Vector3D NxPs;

            normalVariation = info.normalMap.getNormal(info.u, 1-info.v);
            if ( normalVariation != null ) {
                // [BLIN1978b] Section 2:
                // N' = N + D, where D = (Fu (N x Pv) - Fv (N x Pu)) / |N|
                // Here Fu/Fv are reconstructed from the sampled bump-derived
                // vector field produced by NormalMap.importBumpMap.
                // `Pu`/`Pv` are built from the available tangent direction in
                // RayHit (orthonormal local frame approximation).
                N = surfaceNormal.normalized();
                Pu = info.t.normalized();
                Pv = N.crossProduct(Pu).normalized();
                NxPt = N.crossProduct(Pv);
                NxPs = N.crossProduct(Pu);

                Vector3D bumpScale = info.normalMap.getBumpMapScale();
                double nz = normalVariation.z();
                if ( Math.abs(nz) <= VSDK.EPSILON ) {
                    nz = (nz < 0) ? -VSDK.EPSILON : VSDK.EPSILON;
                }
                // In NormalMap.importBumpMap we build vectors from:
                // cross((2,0,du),(0,2,dv)) = (-2du, -2dv, 4), then apply
                // anisotropic scale and normalize. Ratios against z recover
                // derivative-like terms up to known scaling factors.
                Fu = -2.0 * (bumpScale.z() / bumpScale.x()) * (normalVariation.x() / nz);
                Fv = -2.0 * (bumpScale.z() / bumpScale.y()) * (normalVariation.y() / nz);
                normalPerturbation =
                    NxPt.multiply(Fu).subtract(NxPs.multiply(Fv));
                surfaceNormal = surfaceNormal.add(normalPerturbation).normalized();
            }
        }
        double surfaceNormalX = surfaceNormal.x();
        double surfaceNormalY = surfaceNormal.y();
        double surfaceNormalZ = surfaceNormal.z();

        int i;
        for ( i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            ColorRgb lightEmission = light.getSpecularReference();

            if ( light.tipo_de_luz == LightType.AMBIENT ) {
                ColorRgb ambient = material.getAmbientReference();
                outColor.r += ambient.r*lightEmission.r;
                outColor.g += ambient.g*lightEmission.g;
                outColor.b += ambient.b*lightEmission.b;
            }
            else {
                if ( !renderContext.localLightingEnabled ) {
                    continue;
                }
                double lx;
                double ly;
                double lz;
                double maxShadowDistance = Double.MAX_VALUE;
                if ( light.tipo_de_luz == LightType.POINT ) {
                    lx = light.lvec.x() - info.p.x();
                    ly = light.lvec.y() - info.p.y();
                    lz = light.lvec.z() - info.p.z();
                    double lightDistanceSquared = lx*lx + ly*ly + lz*lz;
                    if ( lightDistanceSquared <= VSDK.EPSILON ) {
                        continue;
                    }
                    double lightDistance = Math.sqrt(lightDistanceSquared);
                    double invLightDistance = 1.0 / lightDistance;
                    lx *= invLightDistance;
                    ly *= invLightDistance;
                    lz *= invLightDistance;
                    maxShadowDistance = lightDistance - VSDK.EPSILON;
                    if ( maxShadowDistance <= VSDK.EPSILON ) {
                        continue;
                    }
                }
                else {
                    lx = -light.lvec.x();
                    ly = -light.lvec.y();
                    lz = -light.lvec.z();
                }

                // Check if the surface point is in shadow
                Vector3D shadowOffset = new Vector3D(
                    info.p.x() + VSDK.EPSILON*lx,
                    info.p.y() + VSDK.EPSILON*ly,
                    info.p.z() + VSDK.EPSILON*lz);
                RaytraceStatistics.recordShadowRay();
                Ray shadowRay = new Ray(shadowOffset, new Vector3D(lx, ly, lz));
                if ( anyThingInRayDirection(
                         shadowRay,
                         objects,
                         maxShadowDistance,
                         workspace.shadowCandidateHit) ) {
                    continue;
                }

                double lambert =
                    surfaceNormalX*lx + surfaceNormalY*ly + surfaceNormalZ*lz;
                if ( lambert > 0 ) {
                    ColorRgb diffuse = material.getDiffuseReference();
                    double diffuseR = diffuse.r;
                    double diffuseG = diffuse.g;
                    double diffuseB = diffuse.b;
                    if ( info.texture != null ) {
                        ColorRgb textureColor =
                            info.texture.getColorRgbBiLinear(info.u, 1-info.v);
                        diffuseR *= textureColor.r;
                        diffuseG *= textureColor.g;
                        diffuseB *= textureColor.b;
                    }
                    if ( (diffuseR + diffuseG + diffuseB) > 0 ) {
                        outColor.r += lambert*diffuseR*lightEmission.r;
                        outColor.g += lambert*diffuseG*lightEmission.g;
                        outColor.b += lambert*diffuseB*lightEmission.b;
                    }
                    ColorRgb specular = material.getSpecularReference();

                    if ( (specular.r + specular.g + specular.b) > 0 ) {
                        double twoLambert = 2*lambert;
                        double reflectedViewX = twoLambert*surfaceNormalX - lx;
                        double reflectedViewY = twoLambert*surfaceNormalY - ly;
                        double reflectedViewZ = twoLambert*surfaceNormalZ - lz;
                        double spec =
                            viewX*reflectedViewX +
                            viewY*reflectedViewY +
                            viewZ*reflectedViewZ;

                        if ( spec > 0 ) {
                            spec = ((specular.r + specular.g + specular.b)/3)*(
                                Math.pow(spec, material.getPhongExponent()));
                            outColor.r += spec*lightEmission.r;
                            outColor.g += spec*lightEmission.g;
                            outColor.b += spec*lightEmission.b;
                        }
                    }
                }
            }
        }

        return surfaceNormal;
    }

    private static boolean anyThingInRayDirection(
        Ray inRay,
        List<SimpleBody> inSimpleBodiesArray,
        double maxDistance,
        RayHit candidateHit)
    {
        int i;

        candidateHit.setStoreRay(false);
        RaytraceStatistics.recordSceneTraversal();
        for ( i = 0; i < inSimpleBodiesArray.size(); i++ ) {
            SimpleBody gi = inSimpleBodiesArray.get(i);
            candidateHit.resetForDistanceOnly();
            RaytraceStatistics.recordObjectIntersectionTest();
            if ( gi.doIntersection(inRay, candidateHit) ) {
                double hitDistance = candidateHit.hitDistance();
                if ( hitDistance > VSDK.EPSILON && hitDistance < maxDistance ) {
                    return true;
                }
            }
        }
        return false;
    }
}

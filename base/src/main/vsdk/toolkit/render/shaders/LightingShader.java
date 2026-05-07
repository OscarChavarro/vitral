//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

public abstract class LightingShader extends Shader {
    private final boolean specularEnabled;
    private final boolean textureEnabled;
    private final boolean bumpMapEnabled;

    protected LightingShader(
        boolean specularEnabled,
        boolean textureEnabled,
        boolean bumpMapEnabled)
    {
        this.specularEnabled = specularEnabled;
        this.textureEnabled = textureEnabled;
        this.bumpMapEnabled = bumpMapEnabled;
    }

    @Override
    public final LocalShadingResult shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        SimpleMaterial material,
        TraceWorkspace workspace)
    {
        Vector3D surfaceNormal = info.n;

        if ( bumpMapEnabled ) {
            surfaceNormal = computeBlinnPerturbedNormal(info, surfaceNormal);
        }

        double normalX = surfaceNormal.x();
        double normalY = surfaceNormal.y();
        double normalZ = surfaceNormal.z();
        double outR = 0.0;
        double outG = 0.0;
        double outB = 0.0;

        for ( int i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            ColorRgb lightEmission = light.getSpecularReference();

            if ( light.tipo_de_luz == LightType.AMBIENT ) {
                ColorRgb ambient = material.getAmbientReference();
                outR += ambient.r()*lightEmission.r();
                outG += ambient.g()*lightEmission.g();
                outB += ambient.b()*lightEmission.b();
                continue;
            }

            double lx;
            double ly;
            double lz;
            double maxShadowDistance = Double.POSITIVE_INFINITY;
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

            if ( isShadowed(
                info,
                lx,
                ly,
                lz,
                maxShadowDistance,
                objects,
                workspace) ) {
                continue;
            }

            double lambert = normalX*lx + normalY*ly + normalZ*lz;
            if ( lambert <= 0 ) {
                continue;
            }

            ColorRgb diffuse = material.getDiffuseReference();
            double diffuseR = diffuse.r();
            double diffuseG = diffuse.g();
            double diffuseB = diffuse.b();

            if ( textureEnabled && info.texture != null ) {
                ColorRgb textureColor =
                    CpuTextureSamplingConfig.sample(info.texture, info.u, 1-info.v);
                diffuseR *= textureColor.r();
                diffuseG *= textureColor.g();
                diffuseB *= textureColor.b();
            }

            if ( (diffuseR + diffuseG + diffuseB) > 0 ) {
                outR += lambert*diffuseR*lightEmission.r();
                outG += lambert*diffuseG*lightEmission.g();
                outB += lambert*diffuseB*lightEmission.b();
            }
            if ( !specularEnabled ) {
                continue;
            }

            ColorRgb specular = material.getSpecularReference();
            if ( (specular.r() + specular.g() + specular.b()) <= 0 ) {
                continue;
            }

            double twoLambert = 2*lambert;
            double reflectedViewX = twoLambert*normalX - lx;
            double reflectedViewY = twoLambert*normalY - ly;
            double reflectedViewZ = twoLambert*normalZ - lz;
            double spec =
                viewX*reflectedViewX +
                viewY*reflectedViewY +
                viewZ*reflectedViewZ;
            if ( spec > 0 ) {
                spec = ((specular.r() + specular.g() + specular.b())/3)*(
                    Math.pow(spec, material.getPhongExponent()));
                outR += spec*lightEmission.r();
                outG += spec*lightEmission.g();
                outB += spec*lightEmission.b();
            }
        }

        return new LocalShadingResult(surfaceNormal, new ColorRgb(outR, outG, outB));
    }

    private static boolean isShadowed(
        RayHit info,
        double lightDirX,
        double lightDirY,
        double lightDirZ,
        double maxShadowDistance,
        List<SimpleBody> objects,
        TraceWorkspace workspace)
    {
        Vector3D shadowOrigin = new Vector3D(
            info.p.x() + VSDK.EPSILON * lightDirX,
            info.p.y() + VSDK.EPSILON * lightDirY,
            info.p.z() + VSDK.EPSILON * lightDirZ);
        Vector3D shadowDirection = new Vector3D(lightDirX, lightDirY, lightDirZ);
        Ray shadowRay = new Ray(shadowOrigin, shadowDirection);
        RayHit shadowCandidateHit = workspace.shadowCandidateHit();
        shadowCandidateHit.setStoreRay(false);

        for ( int i = 0; i < objects.size(); i++ ) {
            SimpleBody candidateObject = objects.get(i);
            shadowCandidateHit.resetForDistanceOnly();
            if ( candidateObject.doIntersection(shadowRay, shadowCandidateHit) ) {
                double hitDistance = shadowCandidateHit.hitDistance();
                if ( hitDistance > VSDK.EPSILON && hitDistance < maxShadowDistance ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Vector3D computeBlinnPerturbedNormal(
        RayHit info,
        Vector3D surfaceNormal)
    {
        if ( info.normalMap == null ) {
            return surfaceNormal;
        }
        Vector3D normalVariation =
            CpuTextureSamplingConfig.sampleNormal(info.normalMap, info.u, 1-info.v);
        if ( normalVariation == null ) {
            return surfaceNormal;
        }

        Vector3D baseNormal = surfaceNormal.normalized();
        Vector3D surfaceTangentU = info.t.normalized();
        Vector3D surfaceTangentV = baseNormal.crossProduct(surfaceTangentU).normalized();
        Vector3D nCrossPv = baseNormal.crossProduct(surfaceTangentV);
        Vector3D nCrossPu = baseNormal.crossProduct(surfaceTangentU);

        Vector3D bumpScale = info.normalMap.getBumpMapScale();
        double nz = normalVariation.z();
        if ( Math.abs(nz) <= VSDK.EPSILON ) {
            nz = (nz < 0) ? -VSDK.EPSILON : VSDK.EPSILON;
        }

        double derivativeFu =
            -2.0 * (bumpScale.z() / bumpScale.x()) * (normalVariation.x() / nz);
        double derivativeFv =
            -2.0 * (bumpScale.z() / bumpScale.y()) * (normalVariation.y() / nz);
        Vector3D normalPerturbation =
            nCrossPv.multiply(derivativeFu).subtract(nCrossPu.multiply(derivativeFv));
        return surfaceNormal.add(normalPerturbation).normalized();
    }

}

/**
Bit masks naming the components of a scattering or emission model, as the
integrated quantities of PhongMaterial select them.

A single lobe (reflection, transmission or emission) has three components:
DIFFUSE, GLOSSY and SPECULAR. A scattering request covers both lobes in one
mask: the reflection components in the three low bits and the transmission
components in the next three, so that a request for the diffuse reflection
and the diffuse transmission is REFLECTION_DIFFUSE | TRANSMISSION_DIFFUSE.
The values match RenderPark's BRDF/BTDF component flags bit for bit.
*/
export class ScatteringComponent {
    public static readonly NONE = 0;
    public static readonly DIFFUSE = 1;
    public static readonly GLOSSY = 2;
    public static readonly SPECULAR = 4;
    public static readonly ALL = 1 | 2 | 4;
    public static readonly COMPONENTS_PER_LOBE = 3;

    public static readonly REFLECTION_DIFFUSE = 1;
    public static readonly REFLECTION_GLOSSY = 2;
    public static readonly REFLECTION_SPECULAR = 4;
    public static readonly TRANSMISSION_DIFFUSE = 1 << 3;
    public static readonly TRANSMISSION_GLOSSY = 2 << 3;
    public static readonly TRANSMISSION_SPECULAR = 4 << 3;

    public static reflectionComponents(scatteringComponents: number): number {
        return scatteringComponents & ScatteringComponent.ALL;
    }

    public static transmissionComponents(scatteringComponents: number): number {
        return (scatteringComponents >> ScatteringComponent.COMPONENTS_PER_LOBE) & ScatteringComponent.ALL;
    }
}

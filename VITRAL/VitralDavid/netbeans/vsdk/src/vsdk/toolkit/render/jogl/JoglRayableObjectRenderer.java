package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Mesh;
import vsdk.toolkit.environment.geometry.MeshGroup;
import vsdk.toolkit.environment.geometry.RayableObject;
import vsdk.toolkit.environment.geometry.Sphere;

public class JoglRayableObjectRenderer
{
    public static void draw(GL gl, RayableObject ro, QualitySelection qs)
    {
        gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
        {
            JoglMaterialRenderer.activate(gl, ro.getMaterial());
            gl.glPushMatrix();
            {
                gl.glTranslated(ro.getPosition().x, ro.getPosition().y, ro.getPosition().z);
                //IOU rotations & scale
                
                Geometry o=ro.getGeometry();
                
                if(o instanceof MeshGroup)
                {
                    JoglMeshGroupRenderer.draw(gl, (MeshGroup)o, qs);
                }
                else if(o instanceof Mesh)
                {
                    JoglMeshRenderer.draw(gl, (Mesh)o, qs, false);
                }
                else if(o instanceof Arrow)
                {
                    JoglArrowRenderer.draw(gl, (Arrow)o, null, qs);
                }
                else if(o instanceof Box)
                {
                    JoglBoxRenderer.draw(gl, (Box)o, null, qs);
                }
                else if(o instanceof Cone)
                {
                    JoglConeRenderer.draw(gl, (Cone)o, null, qs);
                }
                else if(o instanceof Sphere)
                {
                    JoglSphereRenderer.draw(gl, (Sphere)o, null, qs);
                }
                
            }
            gl.glPopMatrix();
        }
        gl.glPopAttrib();
    }
}

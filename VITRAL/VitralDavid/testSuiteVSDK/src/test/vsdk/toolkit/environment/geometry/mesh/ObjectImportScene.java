package test.vsdk.toolkit.environment.geometry.mesh;

import vsdk.framework.presentation.util.SceneController;
import vsdk.framework.render.Renderizable;
import vsdk.framework.render.util.TrivialRenderizable;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.common.Vector3D;

import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.RayableObject;;

public class ObjectImportScene
{
    private TrivialRenderizable objects=new TrivialRenderizable();
    private Light light;
    private ObjectImportSceneController oisc;
    
    private QualitySelection quality=new QualitySelection();
    
    /** Creates a new instance of ObjectImportScene */
    public ObjectImportScene()
    {
        light=new Light(Light.POINT, new Vector3D(0, -1, 1), new ColorRgb(1, 1, 1));
        light.setAmbient(new ColorRgb(1,1,1));
        light.setDiffuse(new ColorRgb(1,1,1));
        light.setSpecular(new ColorRgb(1,1,1));
        light.setAtenuation(0,0,0);

        oisc=new ObjectImportSceneController();
        oisc.setLight(light);
        oisc.setTrivialRenderizable(objects);
        oisc.setQualitySelection(quality);
        
        QualitySelection lightQuality=new QualitySelection();
        lightQuality.setLit(false);
        lightQuality.setWires(true);
        lightQuality.setBumpMap(false);
        lightQuality.setSurfaces(false);
        lightQuality.setTexture(false);
        lightQuality.setShadingType(quality.SHADING_TYPE_FLAT);
        
        Sphere sLight=new Sphere(0.05);
        RayableObject roLight=new RayableObject();
        roLight.setGeometry(sLight);
        roLight.setPosition(light.getPosition());
        
        objects.addObject(light);
        objects.addObject(lightQuality);
        objects.addObject(roLight);
        
        quality.setLit(true);
        objects.addObject(quality);
    }
    
    public Renderizable getData()
    {
        return objects;
    }
    
    public SceneController getSceneController()
    {
        return oisc;
    }
}

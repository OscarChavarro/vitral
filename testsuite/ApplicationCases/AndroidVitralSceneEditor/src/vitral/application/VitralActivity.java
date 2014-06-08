//===========================================================================
package vitral.application;

// Android packages
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

// Android packages: GUI
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;

//@TargetApi(Build.VERSION_CODES.CUPCAKE) 
public class VitralActivity extends ActionBarActivity {
    private BasicGLSurfaceView canvas;

    private void createGUI()
    {
        canvas = new BasicGLSurfaceView(getApplication());
        setContentView(canvas);
    }
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Profiler: create .trace file to be used with traceview ADT tool.
        //Debug.startMethodTracing("VITRAL");
        
        createGUI();
    }
    
    @Override
    protected void onDestroy()
    {
        // Profiler: close .trace file to be used with traceview ADT tool.
        //Debug.stopMethodTracing();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        canvas.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        canvas.onResume();
    }

    /**
    Creates an options menu.
    
    Pending to check when device has old-style hardware menu buttons and enable
    that part of code only on that cases.
    @return android specific.
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Activate menu for new devices without menu button, on Android
        // versions 4.0 and up: put it on the action bar
        SubMenu popup;
        popup = menu.addSubMenu(1001, 1002, 1003, "Popup");
        popup.setIcon(R.drawable.abc_ic_menu_moreoverflow_normal_holo_light);
        popup.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        fillPopupWithItems(popup);

        // Activate menu for old-fashioned devices with menu button, on Android
        // versions previous to 4.0 (this code should not be executed on newer
        // devices, as it appears repeated).
        fillPopupWithItems(menu);
        menu.getItem(1).setEnabled(true);
        menu.getItem(1).setShowAsAction(
            MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return true; 
    }

    private void fillPopupWithItems(Menu popupMenu) {
        // Populate popup menu options
        SubMenu q = popupMenu.addSubMenu(0, 100, 0, "Rendering configuration");
        q.add(0, 0, 0, "Constant shading");
        q.add(0, 1, 0, "Flat shading");
        q.add(0, 2, 0, "Gouraud shading");
        q.add(0, 3, 0, "Phong shading");
        q.add(0, 4, 0, "Toggle texture mapping");
        q.add(0, 5, 0, "Toggle bump mapping");
        q.add(0, 6, 0, "Toggle points");
        q.add(0, 7, 0, "Toggle wires");
        q.add(0, 8, 0, "Toggle surfaces");
        q.add(0, 9, 0, "Toggle bounding boxes");
        q.add(0, 10, 0, "Toggle selection corners");
        q.add(0, 11, 0, "Toggle normals");
        q.add(0, 26, 0, "Toggle vertexColors");

        SubMenu o = popupMenu.addSubMenu(1, 101, 1, "Object selection");
        o.add(0, 12, 0, "Sphere (low res)");
        o.add(0, 13, 0, "Sphere (high res)");
        o.add(0, 14, 0, "Box");
        o.add(0, 15, 0, "Cone");
        o.add(0, 16, 0, "Mesh Mug");
        o.add(0, 28, 0, "Mesh Cow");
        o.add(0, 17, 0, "Toggle reference square");
        
        SubMenu a = popupMenu.addSubMenu(2, 102, 2, "Animation options");
        a.add(0, 18, 0, "Toggle object rotation");
        a.add(0, 19, 0, "Toggle first light rotation");

        SubMenu x = popupMenu.addSubMenu(3, 103, 3, "Scene");
        x.add(0, 20, 0, "Add light");
        x.add(0, 21, 0, "Add sphere");
        x.add(0, 22, 0, "Add 10 spheres");
        x.add(0, 29, 0, "Clear scene");

        SubMenu i = popupMenu.addSubMenu(4, 104, 4, "Interaction");
        i.add(0, 23, 0, "Select objects");
        i.add(0, 24, 0, "Insert spheres");

        SubMenu r = popupMenu.addSubMenu(5, 105, 5, "Rendering");
        r.add(0, 25, 0, "Raytrace");
        r.add(0, 27, 0, "Toggle performance report");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return canvas.getGlExecutor().executeMenuCommand(item.getItemId());
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

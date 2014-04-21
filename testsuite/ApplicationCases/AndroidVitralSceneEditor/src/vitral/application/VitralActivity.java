//===========================================================================

package vitral.application;

// Android packages
import android.annotation.TargetApi;
import android.support.v7.app.ActionBarActivity;
import android.os.Build;
import android.os.Bundle;

// Android packages: GUI
//import android.view.View;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
//import android.widget.Toast;
//import android.widget.TextView;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.Gallery.LayoutParams;
//import android.view.Gravity;
//import android.widget.ImageView.ScaleType;
//import android.widget.Button;
import android.view.View.OnClickListener;

// Android packages: GPS API
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;
//import android.location.GpsStatus.Listener;
import android.content.Context;
import android.os.Debug;
import android.view.Window;

// Vsdk classes
//import vsdk.toolkit.common.linealAlgebra.Vector3D;
//import vsdk.toolkit.common.ColorRgb;
//import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBody;

@TargetApi(Build.VERSION_CODES.CUPCAKE) 
public class VitralActivity 
extends ActionBarActivity 
implements LocationListener, GpsStatus.Listener /*, OnClickListener*/ {
    private BasicGLSurfaceView canvas;

    private LocationManager locationManager;
    private String provider;
    private int numberOfLocations;
    private int numberOfLights;

    //= Basic Activity methods =================================================
    private void createGUI()
    {
/*
        TextView label = new TextView(this);
        label.setText("VITRAL Android Application Template");
        label.setTextSize(10);
        label.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView pic = new ImageView(this);
        pic.setImageResource(R.raw.render);  
        pic.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));  
        pic.setAdjustViewBounds(true);  
        pic.setScaleType(ScaleType.FIT_XY);  
        pic.setMaxHeight(250);  
        pic.setMaxWidth(250);  
*/

        canvas = new BasicGLSurfaceView(getApplication());
        
        //canvas.setLayoutParams(new LayoutParams(
        //    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

/*
        Button b1 = new Button(this);
        b1.setId(1);
        b1.setText("Click me");
        b1.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        b1.setOnClickListener(this);

        Button b2 = new Button(this);
        b2.setId(2);
        b2.setText("Tickle me");
        b2.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        b2.setOnClickListener(this);

        LinearLayout buttonsPanel = new LinearLayout(this);  
        buttonsPanel.setOrientation(LinearLayout.HORIZONTAL);  
        buttonsPanel.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));  
        //buttonsPanel.setGravity(Gravity.CENTER);  
        buttonsPanel.addView(b1);
        buttonsPanel.addView(b2);

        // Organize elements
        LinearLayout mainPanel = new LinearLayout(this);  
        mainPanel.setOrientation(LinearLayout.VERTICAL);  
        mainPanel.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));  
        mainPanel.setGravity(Gravity.CENTER);  
        mainPanel.addView(label);  
        //mainPanel.addView(pic);
        mainPanel.addView(buttonsPanel);
        mainPanel.addView(canvas);  
        setContentView(mainPanel);
*/
        setContentView(canvas);
        //requestWindowFeature(Window.FEATURE_ACTION_BAR);
        //getActionBar().show();
        //getSupportActionBar().show();
    }

/*
    @Override
    public void onClick(View v)
    {
        System.out.println("Huy! " + v.getId());
        Toast.makeText(this, "Huy!", Toast.LENGTH_SHORT).show();  
    }
*/
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Profiler: create .trace file to be used with traceview ADT tool.
        //Debug.startMethodTracing("VITRAL");
        
        createGUI();

        // Scene related
        numberOfLights = 1;

        // GPS related
        numberOfLocations = 1;
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getProvider(LocationManager.GPS_PROVIDER).getName();
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if ( location != null ) {
            System.out.println("OnCreate: provider " + provider + " has been selected.");
            onLocationChanged(location);
          }
          else {
            System.out.println("Location not available");
        }
    }
    
    @Override
    protected void onDestroy()
    {
        //Debug.stopMethodTracing();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        canvas.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        canvas.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    //= Extended methods for GPS ===============================================
    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        System.out.println("[" + numberOfLocations + "] LAT: " + lat + " LON: " + lon);
        numberOfLocations++;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        
    }

    @Override
    public void onProviderEnabled(String provider) {
        System.out.println("Enabled new location provider: " + provider);
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if ( location != null ) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
          }
          else {
            System.out.println("(Again) Location not available");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        System.out.println("Enabled new location provider: " + provider);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch ( event ) {
          case GpsStatus.GPS_EVENT_STARTED:
            System.out.println("GPS_EVENT_STARTED");
            break;
          case GpsStatus.GPS_EVENT_STOPPED:
            System.out.println("GPS_EVENT_STOPPED");
            break;
          case GpsStatus.GPS_EVENT_FIRST_FIX:
            System.out.println("GPS_EVENT_FIRST_FIX");
            break;
          case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            System.out.println("GPS_EVENT_SATELLITE_STATUS");
            break;
          default:
            System.out.println("Unknown GPS event");
            break;
        }
    }

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
        // versions previous to 4.0
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
        RendererConfiguration q;
        q = canvas.getGlExecutor().getRendererConfiguration();
        Scene s = canvas.getGlExecutor().getScene();
        SimpleBody b;

        canvas.getGlExecutor().resetTimers();

        switch ( item.getItemId() ) {
          case 0:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            break;
          case 1:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);
            break;
          case 2:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);
            break;
          case 3:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
            break;
          case 4:
            q.changeTexture();
            break;
          case 5:
            q.changeBumpMap();
            break;
          case 6:
            q.changePoints();
            break;
          case 7:
            q.changeWires();
            break;
          case 8:
            q.changeSurfaces();
            break;
          case 9:
            q.changeBoundingVolume();
            break;
          case 10:
            q.changeSelectionCorners();
            break;
          case 11:
            q.changeNormals();
            break;
          case 12:
            canvas.getGlExecutor().selectObject(1);
            break;
          case 13:
            canvas.getGlExecutor().selectObject(2);
            break;
          case 14:
            canvas.getGlExecutor().selectObject(3);
            break;
          case 15:
            canvas.getGlExecutor().selectObject(4);
            break;
          case 16:
            canvas.getGlExecutor().selectObject(5);
            break;
          case 28:
            canvas.getGlExecutor().selectObject(6);
            break;
          case 17:
            canvas.getGlExecutor().toggleReferenceSquare();
            break;
          case 18:
            canvas.getGlExecutor().toggleObjectRotation();
            break;
          case 19:
            canvas.getGlExecutor().toggleLightRotation();
            break;
          case 20:
            numberOfLights++;
            canvas.getGlExecutor().prepareLights(numberOfLights);
            break;
          case 21:
            canvas.getGlExecutor().getScene().addRandomSphere();
            break;
          case 22:
            for ( int i = 0; i < 10; i++ ) {
                canvas.getGlExecutor().getScene().addRandomSphere();
            }
            break;
          case 23:
            canvas.getGlExecutor().setInteraction(1);
            break;
          case 24:
            canvas.getGlExecutor().setInteraction(2);
            break;
          case 25:
            canvas.getGlExecutor().requestRaytracer();
            break;
          case 26:
            q.setUseVertexColors(!q.getUseVertexColors());
            break;
          case 27:
            canvas.getGlExecutor().toggleHudReport();
            break;
          case 29:
            canvas.getGlExecutor().clearSceneFromObjects();
            break;
        }
        return true;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================

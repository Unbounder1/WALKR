package com.WALKR.core.geospatial;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.GuardedBy;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.ar.core.Anchor;
import com.google.ar.core.Anchor.RooftopAnchorState;
import com.google.ar.core.Anchor.TerrainAnchorState;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Earth;
import com.google.ar.core.Frame;
import com.google.ar.core.GeospatialPose;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.ResolveAnchorOnRooftopFuture;
import com.google.ar.core.ResolveAnchorOnTerrainFuture;
import com.google.ar.core.Session;
import com.google.ar.core.StreetscapeGeometry;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.VpsAvailability;
import com.google.ar.core.VpsAvailabilityFuture;
import com.WALKR.core.common.helpers.CameraPermissionHelper;
import com.WALKR.core.common.helpers.DisplayRotationHelper;
import com.WALKR.core.common.helpers.FullScreenHelper;
import com.WALKR.core.common.helpers.LocationPermissionHelper;
import com.WALKR.core.common.helpers.SnackbarHelper;
import com.WALKR.core.common.helpers.TrackingStateHelper;
import com.WALKR.core.common.helpers.AudioPermissionHelper;
import com.WALKR.core.common.samplerender.Framebuffer;
import com.WALKR.core.common.samplerender.IndexBuffer;
import com.WALKR.core.common.samplerender.Mesh;
import com.WALKR.core.common.samplerender.SampleRender;
import com.WALKR.core.common.samplerender.Shader;
import com.WALKR.core.common.samplerender.Shader.BlendFactor;
import com.WALKR.core.common.samplerender.Texture;
import com.WALKR.core.common.samplerender.VertexBuffer;
import com.WALKR.core.common.samplerender.arcore.BackgroundRenderer;
import com.WALKR.core.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.FineLocationPermissionNotGrantedException;
import com.google.ar.core.exceptions.GooglePlayServicesLocationLibraryNotLinkedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.core.exceptions.UnsupportedConfigurationException;
import okhttp3.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.CameraUpdateFactory;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import android.text.Html;
import android.os.Build;
import android.widget.TextView;

// the good ones *_^


/**
 * Main activity for the Geospatial API example.
 *
 * <p>This example shows how to use the Geospatial APIs. Once the device is localized, anchors can
 * be created at the device's geospatial location. Anchor locations are persisted across sessions
 * and will be recreated once localized.
 */
public class GeospatialActivity extends AppCompatActivity
        implements SampleRender.Renderer,
        VpsAvailabilityNoticeDialogFragment.NoticeDialogListener,
        PrivacyNoticeDialogFragment.NoticeDialogListener,
        OnMapReadyCallback,
        SpeechRecognizerHelper.SpeechRecognitionListener {

  private static final String TAG = GeospatialActivity.class.getSimpleName();

  private static final String SHARED_PREFERENCES_SAVED_ANCHORS = "SHARED_PREFERENCES_SAVED_ANCHORS";
  private static final String ALLOW_GEOSPATIAL_ACCESS_KEY = "ALLOW_GEOSPATIAL_ACCESS";
  private static final String ANCHOR_MODE = "ANCHOR_MODE";

  private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
  private static final String DIRECTIONS_API_KEY = BuildConfig.API_KEY;
  private static final int ANCHOR_INTERVAL_METERS = 1; // Adjust as needed
  private static final int MIN_ANCHOR_INTERVAL_METERS = 1;
  private static final double PROXIMITY_THRESHOLD_METERS = 2.0; // Threshold for proximity to an anchor
  private static final double GEOFENCING_THRESHOLD_METERS = 100.0;
  private static final double MAP_CIRCLE_RADIUS = 10.0;
  private static final float MAP_ZOOM = 18;
  private static final double BUS_STOP_THRESHOLD = 500.0;
  private boolean isWheelchairAccessible = false;
  private boolean isRoutingActive = false;

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 1000f;

  // Localization thresholds
  private boolean isRecalculatingRoute = false;
  private Location lastRouteLocation = null;
  private boolean busStopAnchorsPlaced = false;
  private static final float ROUTE_RECALCULATION_DISTANCE_METERS = 20.0f; // Adjust as needed
  private static final double LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS = 100;
  private static final double LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES = 150;
  private static final double LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS = 100;
  private static final double LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES = 100;

  private static final int LOCALIZING_TIMEOUT_SECONDS = 180;
  private static final int MAX_SEGMENT_DISTANCE_METERS = 50;

  // Replace static final variables with instance variables
  private double FINAL_DESTINATION_LATITUDE = 42.72992506225727; // Example latitude
  private double FINAL_DESTINATION_LONGITUDE = -73.67667982209569; // Example longitude
  private double FINAL_DESTINATION_ALTITUDE = 0; // Example altitude in meters

  private SpeechRecognizerHelper speechRecognizerHelper;
  private PlaceFinderHelper placeFinderHelper;

  // Rendering
  private GLSurfaceView surfaceView;
  private final Set<Anchor> busStopAnchors = new HashSet<>();
  private MapView mapView;
  private GoogleMap googleMap;

  private boolean installRequested;
  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

  /**
   * Timer to keep track of how much time has passed since localizing has started.
   */
  private long localizingStartTimestamp;
  /**
   * Deadline for showing resolving terrain anchors no result yet message.
   */
  private long deadlineForMessageMillis;
  private Marker currentLocationMarker;
  private Circle userLocationCircle;


  enum State {
    UNINITIALIZED,
    UNSUPPORTED,
    EARTH_STATE_ERROR,
    PRETRACKING,
    LOCALIZING,
    LOCALIZING_FAILED,
    LOCALIZED
  }

  private State state = State.UNINITIALIZED;

  enum AnchorType {
    GEOSPATIAL,
    TERRAIN,
    ROOFTOP,
    TERRAIN_ROUTE // New type for route-specific Terrain Anchors
  }

  private static class TargetLocation {
    double latitude;
    double longitude;
    double altitude;
    boolean isRendered; // To prevent multiple renders

    TargetLocation(double latitude, double longitude, double altitude) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.altitude = altitude;
      this.isRendered = false;
    }
  }

  // Initialize your target locations
  private List<TargetLocation> targetLocations = new ArrayList<>();


  private AnchorType anchorType = AnchorType.GEOSPATIAL;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private SampleRender render;
  private SharedPreferences sharedPreferences;

  private String lastStatusText;
  private Button clearAnchorsButton;
  private Button replaceRouteAnchorsButton;
  private SwitchCompat streetscapeGeometrySwitch;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;
  private boolean isRenderStreetscapeGeometry = false;

  private Mesh virtualObjectMesh;
  private Shader geospatialAnchorVirtualObjectShader;
  private Shader terrainAnchorVirtualObjectShader;
  private Texture terrainAnchorTextureDefault;
  private Texture terrainAnchorTextureAccessible;

  private final Object anchorsLock = new Object();

  @GuardedBy("anchorsLock")
  private final List<Anchor> anchors = new ArrayList<>();
  private List<LatLng> previousRoutePoints = new ArrayList<>();
  private final Set<Anchor> terrainAnchors = new HashSet<>();
  private final Set<Anchor> rooftopAnchors = new HashSet<>();

  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16];
  private final float[] modelViewProjectionMatrix = new float[16];

  private final float[] identityQuaternion = {0, 0, 0, 1};

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  private long lastPointCloudTimestamp = 0;

  private Mesh targetObjectMesh;
  private Shader targetObjectShader;
  private Texture targetObjectTexture;
  // Provides device location.
  private FusedLocationProviderClient fusedLocationClient;

  // MediaPlayer for success sound
  private MediaPlayer mediaPlayer;

  // Location updates
  private LocationCallback locationCallback;
  private long lastRouteComputeTime = 0;

  // Streetscape geometry.
  private final ArrayList<float[]> wallsColor = new ArrayList<>();
  private Shader streetscapeGeometryTerrainShader;
  private Shader streetscapeGeometryBuildingShader;
  private final Map<StreetscapeGeometry, Mesh> streetscapeGeometryToMeshes = new HashMap<>();
  @GuardedBy("routeAnchorsLock")
  private final Set<Anchor> routeAnchors = new HashSet<>();

  private final Object routeAnchorsLock = new Object();

  private List<DirectionStep> directionSteps = new ArrayList<>();
  private int currentStepIndex = 0;
  private static final double STEP_COMPLETION_THRESHOLD_METERS = 10.0;
  private static class DirectionStep {
    String instruction;
    LatLng startLocation;
    LatLng endLocation;

    DirectionStep(String instruction, LatLng startLocation, LatLng endLocation) {
      this.instruction = instruction;
      this.startLocation = startLocation;
      this.endLocation = endLocation;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initialize PlaceFinderHelper
    placeFinderHelper = new PlaceFinderHelper(DIRECTIONS_API_KEY);

    // Initialize SpeechRecognizerHelper
    List<String> customPhrases = Arrays.asList("hibachi station", "report pothole", "hibachi station accessible");
    speechRecognizerHelper = new SpeechRecognizerHelper(this, this, DIRECTIONS_API_KEY);
    speechRecognizerHelper.initializeRecognizer(customPhrases);

    targetLocations.add(new TargetLocation(42.73005514542626, -73.68164119587962, 40));

    sharedPreferences = getPreferences(Context.MODE_PRIVATE);

    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    clearAnchorsButton = findViewById(R.id.clear_anchors_button);
    replaceRouteAnchorsButton = findViewById(R.id.replace_route_anchors_button); // Initialize new button

    // Set up Replace Route Anchors Button
    replaceRouteAnchorsButton.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                handleReplaceRouteAnchorsButton();
              }
            });
    clearAnchorsButton.setOnClickListener(view -> handleClearAnchorsButton());

    // Initialize DisplayRotationHelper and Renderer
    displayRotationHelper = new DisplayRotationHelper(/* activity= */ this);
    render = new SampleRender(surfaceView, this, getAssets());
    installRequested = false;

    // Initialize MapView
    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this); // This will trigger onMapReady when the map is ready

    // Initialize FusedLocationProviderClient for location updates
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(/* context= */ this);

    // Initialize MediaPlayer for success sound
    mediaPlayer = MediaPlayer.create(this, R.raw.success_sound); // Place your sound file in res/raw/

    // Set up location callback
    locationCallback =
            locationCallback = new LocationCallback() {
              @Override
              public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                  return;
                }
                for (Location location : locationResult.getLocations()) {
                  if (isRoutingActive) {
                    // Check proximity to anchors
                    checkProximityToAnchors(location);

                    // Recompute route based on significant movement
                    if (lastRouteLocation == null || location.distanceTo(lastRouteLocation) > ROUTE_RECALCULATION_DISTANCE_METERS) {
                      lastRouteLocation = location;
                      computeRoute(location);
                    }

                    // Update MapView's marker and camera
                    updateMapLocation(location);
                    checkIfReachedStepEnd(location);
                  } else {
                    // Optionally, you can still update the user's location on the map
                    updateMapLocation(location);
                  }
                  break; // Use the first location in the list
                }
              }
            };
    // Start location updates
    requestNewLocationData();
  }

  @Override
  public void onRecognizedText(String text) {
    if (text.equalsIgnoreCase("hibachi station") || text.equalsIgnoreCase("hibachi station accessible")) {
      handleHibachiStationRequest(text);
    } else if (text.equalsIgnoreCase("report pothole")) {
      handlePotholeReportRequest();
    } else {
      handleUnrecognizedPhrase();
    }
  }

  private void handleHibachiStationRequest(String text) {
    // Set isWheelchairAccessible based on the presence of "wheelchair" in the recognized text
    boolean isWheelchairAccessible = text.toLowerCase().contains("accessible");

    runOnUiThread(() ->
            Toast.makeText(this, "Fetching coordinates for Hibachi Station...", Toast.LENGTH_SHORT).show()
    );

    placeFinderHelper.getPlaceAddress("hibachi station", new PlaceFinderHelper.PlaceFinderCallback() {
      @Override
      public void onResult(String address, Map<String, Double> coordinates) {
        if (coordinates != null) {
          double lat = coordinates.get("lat");
          double lng = coordinates.get("lng");

          FINAL_DESTINATION_LATITUDE = lat;
          FINAL_DESTINATION_LONGITUDE = lng;

          runOnUiThread(() -> {
            String accessibility = isWheelchairAccessible ? " (Wheelchair accessible)" : "";
            Toast.makeText(
                    GeospatialActivity.this,
                    "Destination updated to Hibachi Station" + accessibility,
                    Toast.LENGTH_SHORT
            ).show();
            isRoutingActive = true;
            fetchCurrentLocationAndComputeRoute();
          });
        } else {
          runOnUiThread(() ->
                  Toast.makeText(GeospatialActivity.this, "Failed to fetch coordinates for Hibachi Station", Toast.LENGTH_SHORT).show()
          );
        }
      }
    });
  }

  private void handlePotholeReportRequest() {
    runOnUiThread(() ->
            Toast.makeText(this, "Pothole report functionality not implemented yet.", Toast.LENGTH_SHORT).show()
    );
  }

  private void handleUnrecognizedPhrase() {
    runOnUiThread(() ->
            Toast.makeText(this, "Unrecognized phrase", Toast.LENGTH_SHORT).show()
    );
  }

  @Override
  public void onRecognitionError(String errorMessage) {
    runOnUiThread(() -> {
      Toast.makeText(this, "Recognition Error: " + errorMessage, Toast.LENGTH_SHORT).show();
    });
  }

  @Override
  protected void onDestroy() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      session.close();
      session = null;
    }


    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (sharedPreferences.getBoolean(ALLOW_GEOSPATIAL_ACCESS_KEY, /* defValue= */ false)) {
      createSession();
    } else {
      showPrivacyNoticeDialog();
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();

    // Start location updates
    requestNewLocationData();
  }

  private void showPrivacyNoticeDialog() {
    DialogFragment dialog = PrivacyNoticeDialogFragment.createDialog();
    dialog.show(getSupportFragmentManager(), PrivacyNoticeDialogFragment.class.getName());
  }

  private void createSession() {
    Exception exception = null;
    String message = null;
    if (session == null) {

      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // Check camera permissions
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }
        // Check location permissions
        if (!LocationPermissionHelper.hasFineLocationPermission(this)) {
          LocationPermissionHelper.requestFineLocationPermission(this);
          return;
        }
        // Check audio input permission
        if (AudioPermissionHelper.hasAudioPermission(this)) {
          AudioPermissionHelper.requestAudioPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
               | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }
    // Check VPS availability before configure and resume session.
    if (session != null) {
      getLastLocation();
    }

    try {
      configureSession();
      session.resume();
    } catch (CameraNotAvailableException e) {
      message = "Camera not available. Try restarting the app.";
      exception = e;
    } catch (GooglePlayServicesLocationLibraryNotLinkedException e) {
      message = "Google Play Services location library not linked or obfuscated with Proguard.";
      exception = e;
    } catch (FineLocationPermissionNotGrantedException e) {
      message = "The Android permission ACCESS_FINE_LOCATION was not granted.";
      exception = e;
    } catch (UnsupportedConfigurationException e) {
      message = "This device does not support GeospatialMode.ENABLED.";
      exception = e;
    } catch (SecurityException e) {
      message = "Camera failure or the internet permission has not been granted.";
      exception = e;
    }

    if (message != null) {
      session = null;
      messageSnackbarHelper.showError(this, message);
      Log.e(TAG, "Exception configuring and resuming the session", exception);
      return;
    }
  }

  private void getLastLocation() {
    try {
      fusedLocationClient
              .getLastLocation()
              .addOnSuccessListener(
                      new com.google.android.gms.tasks.OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                          double latitude = 0;
                          double longitude = 0;
                          if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                          } else {
                            Log.e(TAG, "Error location is null");
                          }
                          checkVpsAvailability(latitude, longitude);
                        }
                      });
    } catch (SecurityException e) {
      Log.e(TAG, "No location permissions granted by User!");
    }
  }

  private void checkVpsAvailability(double latitude, double longitude) {
    final VpsAvailabilityFuture future =
            session.checkVpsAvailabilityAsync(
                    latitude,
                    longitude,
                    availability -> {
                      if (availability != VpsAvailability.AVAILABLE) {
                        showVpsNotAvailabilityNoticeDialog();
                      }
                    });
  }

  private void showVpsNotAvailabilityNoticeDialog() {
    DialogFragment dialog = VpsAvailabilityNoticeDialogFragment.createDialog();
    dialog.show(getSupportFragmentManager(), VpsAvailabilityNoticeDialogFragment.class.getName());
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
    // Remove location updates
    fusedLocationClient.removeLocationUpdates(locationCallback);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
    // Check if this result pertains to the location permission.
    if (LocationPermissionHelper.hasFineLocationPermissionsResponseInResult(permissions)
            && !LocationPermissionHelper.hasFineLocationPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(
                      this,
                      "Precise location permission is needed to run this application",
                      Toast.LENGTH_LONG)
              .show();
      if (!LocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        LocationPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
    // Check if this result pertains to the audio permission.
    if (AudioPermissionHelper.hasAudioPermissionsResponseInResult(permissions)
            && AudioPermissionHelper.hasAudioPermission(this)) {
      Toast.makeText(
                      this,
                      "Audio input permission is needed to run this application",
                      Toast.LENGTH_LONG)
              .show();
      if (!AudioPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        AudioPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  /**
   * Updates the terrain anchor shader's texture based on the wheelchair accessibility flag.
   */
  private void updateTerrainAnchorTexture() {
    if (terrainAnchorVirtualObjectShader == null) {
      Log.e(TAG, "Terrain Anchor Shader is not initialized.");
      return;
    }

    if (isWheelchairAccessible) {
      terrainAnchorVirtualObjectShader.setTexture("u_Texture", terrainAnchorTextureAccessible);
      Log.d(TAG, "Switched to Wheelchair Accessible Texture.");
    } else {
      terrainAnchorVirtualObjectShader.setTexture("u_Texture", terrainAnchorTextureDefault);
      Log.d(TAG, "Switched to Default Texture.");
    }
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

      // Bus stop manual
      targetObjectTexture = Texture.createFromAsset(
              render,
              "models/blue_color.png",
              Texture.WrapMode.REPEAT,
              Texture.ColorFormat.SRGB
      );
      targetObjectMesh = Mesh.createFromAsset(render, "models/bus_stop.obj");
      targetObjectShader = Shader.createFromAssets(
              render,
              "shaders/ar_unlit_object.vert",
              "shaders/ar_unlit_object.frag",
              null
      ).setTexture("u_Texture", targetObjectTexture);

      // Virtual object to render (ARCore geospatial)
      Texture virtualObjectTexture =
              Texture.createFromAsset(
                      render,
                      "models/blue_color.png",
                      Texture.WrapMode.REPEAT,
                      Texture.ColorFormat.SRGB);

      virtualObjectMesh = Mesh.createFromAsset(render, "models/dashed_line.obj");
      geospatialAnchorVirtualObjectShader =
              Shader.createFromAssets(
                              render,
                              "shaders/ar_unlit_object.vert",
                              "shaders/ar_unlit_object.frag",
                              /* defines= */ null)
                      .setTexture("u_Texture", virtualObjectTexture);

      // **Set Light Position for Shadows**
      float[] lightPos = {3.0f, 10.0f, 5.0f};
      geospatialAnchorVirtualObjectShader.setVec3("u_LightPos", lightPos);

      // **Initialize Camera Position Uniform (will be updated each frame)**
      // Initially set to origin; will be updated in onDrawFrame
      float[] initialCameraPos = {0.0f, 0.0f, 0.0f};
      geospatialAnchorVirtualObjectShader.setVec3("u_CameraPos", initialCameraPos);

      // Load both textures upfront
      terrainAnchorTextureDefault =
              Texture.createFromAsset(
                      render,
                      "models/green_color.png", // Default texture
                      Texture.WrapMode.REPEAT,
                      Texture.ColorFormat.SRGB);
      terrainAnchorTextureAccessible =
              Texture.createFromAsset(
                      render,
                      "models/blue_color.png", // Texture for wheelchair-accessible mode
                      Texture.WrapMode.REPEAT,
                      Texture.ColorFormat.SRGB);

      // Initialize the shader with the default texture
      terrainAnchorVirtualObjectShader =
              Shader.createFromAssets(
                              render,
                              "shaders/ar_unlit_object.vert",
                              "shaders/ar_unlit_object.frag",
                              /* defines= */ null)
                      .setTexture("u_Texture", terrainAnchorTextureDefault);

      backgroundRenderer.setUseDepthVisualization(render, false);
      backgroundRenderer.setUseOcclusion(render, false);

      // Point cloud
      pointCloudShader =
              Shader.createFromAssets(
                              render,
                              "shaders/point_cloud.vert",
                              "shaders/point_cloud.frag",
                              /* defines= */ null)
                      .setVec4(
                              "u_Color",
                              new float[]{31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
                      .setFloat("u_PointSize", 5.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
              new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
              new Mesh(
                      render,
                      Mesh.PrimitiveMode.POINTS,
                      /* indexBuffer= */ null,
                      pointCloudVertexBuffers);

      streetscapeGeometryBuildingShader =
              Shader.createFromAssets(
                              render,
                              "shaders/streetscape_geometry.vert",
                              "shaders/streetscape_geometry.frag",
                              /* defines= */ null)
                      .setBlend(
                              BlendFactor.DST_ALPHA, // RGB (src)
                              BlendFactor.ONE); // ALPHA (dest)

      streetscapeGeometryTerrainShader =
              Shader.createFromAssets(
                              render,
                              "shaders/streetscape_geometry.vert",
                              "shaders/streetscape_geometry.frag",
                              /* defines= */ null)
                      .setBlend(
                              BlendFactor.DST_ALPHA, // RGB (src)
                              BlendFactor.ONE); // ALPHA (dest)
      wallsColor.add(new float[]{0.5f, 0.0f, 0.5f, 0.3f});
      wallsColor.add(new float[]{0.5f, 0.5f, 0.0f, 0.3f});
      wallsColor.add(new float[]{0.0f, 0.5f, 0.5f, 0.3f});
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  @Override
  public void onDrawFrame(SampleRender render) {
    if (session == null) {
      return;
    }

    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
              new int[]{backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    displayRotationHelper.updateSessionIfNeeded(session);
    updateStreetscapeGeometries(session.getAllTrackables(StreetscapeGeometry.class));

    Frame frame;
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }
    Camera camera = frame.getCamera();

    backgroundRenderer.updateDisplayGeometry(frame);
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    Earth earth = session.getEarth();
    if (earth != null) {
      updateGeospatialState(earth);
    }

    String message = null;
    switch (state) {
      case UNINITIALIZED:
        break;
      case UNSUPPORTED:
        message = getResources().getString(R.string.status_unsupported);
        break;
      case PRETRACKING:
        message = getResources().getString(R.string.status_pretracking);
        break;
      case EARTH_STATE_ERROR:
        message = getResources().getString(R.string.status_earth_state_error);
        break;
      case LOCALIZING:
        message = getResources().getString(R.string.status_localize_hint);
        break;
      case LOCALIZING_FAILED:
        message = getResources().getString(R.string.status_localize_timeout);
        break;
    }


    // -- Draw background

    if (frame.getTimestamp() != 0) {
      backgroundRenderer.drawBackground(render);
    }

    // If not tracking, don't draw 3D objects.
    if (camera.getTrackingState() != TrackingState.TRACKING || state != State.LOCALIZED) {
      return;
    }

    // -- Draw virtual objects

    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);
    camera.getViewMatrix(viewMatrix, 0);

    // **Update Camera Position in Shader**
    Pose cameraPose = camera.getPose();
    float[] cameraPos = {cameraPose.tx(), cameraPose.ty(), cameraPose.tz()};
    geospatialAnchorVirtualObjectShader.setVec3("u_CameraPos", cameraPos);

    // Visualize tracked points. - DEBUG
//    try (PointCloud pointCloud = frame.acquirePointCloud()) {
//      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
//        pointCloudVertexBuffer.set(pointCloud.getPoints());
//        lastPointCloudTimestamp = pointCloud.getTimestamp();
//      }
//      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
//      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
//      render.draw(pointCloudMesh, pointCloudShader);
//    }

    // Visualize planes. - DEBUG
//    planeRenderer.drawPlanes(
//            render,
//            session.getAllTrackables(Plane.class),
//            camera.getDisplayOrientedPose(),
//            projectionMatrix);


    // Visualize anchors created by touch.
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);

    // -- Draw Streetscape Geometries.
    if (isRenderStreetscapeGeometry) {
      int index = 0;
      for (Map.Entry<StreetscapeGeometry, Mesh> set : streetscapeGeometryToMeshes.entrySet()) {
        StreetscapeGeometry streetscapeGeometry = set.getKey();
        if (streetscapeGeometry.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }
        Mesh mesh = set.getValue();
        Pose pose = streetscapeGeometry.getMeshPose();
        pose.toMatrix(modelMatrix, 0);

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        if (streetscapeGeometry.getType() == StreetscapeGeometry.Type.BUILDING) {
          float[] color = wallsColor.get(index % wallsColor.size());
          index += 1;
          streetscapeGeometryBuildingShader
                  .setVec4(
                          "u_Color",
                          new float[]{/* r= */ color[0], /* g= */ color[1], /* b= */ color[2], color[3]})
                  .setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
          render.draw(mesh, streetscapeGeometryBuildingShader);
        } else if (streetscapeGeometry.getType() == StreetscapeGeometry.Type.TERRAIN) {
          streetscapeGeometryTerrainShader.setVec4(
                  "u_Color", new float[]{/* r= */ 0f, /* g= */ .5f, /* b= */ 0f, 0.3f});
          streetscapeGeometryTerrainShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
          render.draw(mesh, streetscapeGeometryTerrainShader);
        }
      }
    }
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
    synchronized (anchorsLock) {
      for (Anchor anchor : anchors) {
        if (anchor.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }
        anchor.getPose().toMatrix(modelMatrix, 0);
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        float scale = getScale(anchor.getPose(), camera.getDisplayOrientedPose());
        scaleMatrix[0] = scale;
        scaleMatrix[5] = scale;
        scaleMatrix[10] = scale;
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        // Determine which shader to use based on anchor type
        if (terrainAnchors.contains(anchor) || rooftopAnchors.contains(anchor)) {
          terrainAnchorVirtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
          render.draw(virtualObjectMesh, terrainAnchorVirtualObjectShader, virtualSceneFramebuffer);
        } else {
          if (isTargetLocationAnchor(anchor)) {
            targetObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(targetObjectMesh, targetObjectShader, virtualSceneFramebuffer);
          } else {
            geospatialAnchorVirtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(virtualObjectMesh, geospatialAnchorVirtualObjectShader, virtualSceneFramebuffer);
          }
        }
      }
    }

    // Compose the virtual scene with the background.
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
  }

  /**
   * Updates all the StreetscapeGeometries. Existing StreetscapeGeometries will have pose updated,
   * and non-existing StreetscapeGeometries will be removed from the scene.
   */
  private void updateStreetscapeGeometries(Collection<StreetscapeGeometry> streetscapeGeometries) {
    for (StreetscapeGeometry streetscapeGeometry : streetscapeGeometries) {
      if (streetscapeGeometryToMeshes.containsKey(streetscapeGeometry)) {
        // Existing StreetscapeGeometry, update if needed
      } else {
        // Create new StreetscapeGeometry mesh
        Mesh mesh = getSampleRenderMesh(streetscapeGeometry);
        streetscapeGeometryToMeshes.put(streetscapeGeometry, mesh);
      }
    }
  }

  private Mesh getSampleRenderMesh(StreetscapeGeometry streetscapeGeometry) {
    FloatBuffer streetscapeGeometryBuffer = streetscapeGeometry.getMesh().getVertexList();
    streetscapeGeometryBuffer.rewind();
    VertexBuffer meshVertexBuffer =
            new VertexBuffer(
                    render, /* numberOfEntriesPerVertex= */ 3, /* entries= */ streetscapeGeometryBuffer);
    IndexBuffer meshIndexBuffer =
            new IndexBuffer(render, streetscapeGeometry.getMesh().getIndexList());
    final VertexBuffer[] meshVertexBuffers = {meshVertexBuffer};
    return new Mesh(
            render,
            Mesh.PrimitiveMode.TRIANGLES,
            /* indexBuffer= */ meshIndexBuffer,
            meshVertexBuffers);
  }

  /**
   * Configures the session with feature settings.
   */
  private void configureSession() {
    if (!session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
      state = State.UNSUPPORTED;
      return;
    }

    Config config = session.getConfig();
    config =
            config
                    .setGeospatialMode(Config.GeospatialMode.ENABLED)
                    .setStreetscapeGeometryMode(Config.StreetscapeGeometryMode.ENABLED)
                    .setDepthMode(Config.DepthMode.AUTOMATIC);
    session.configure(config);
    state = State.PRETRACKING;
    localizingStartTimestamp = System.currentTimeMillis();
  }

  /**
   * Update the state based on Earth tracking
   */
  private void updateGeospatialState(Earth earth) {
    if (earth.getEarthState() != Earth.EarthState.ENABLED) {
      state = State.EARTH_STATE_ERROR;
      return;
    }
    if (earth.getTrackingState() != TrackingState.TRACKING) {
      state = State.PRETRACKING;
      return;
    }
    if (state == State.PRETRACKING) {
      updatePretrackingState(earth);
    } else if (state == State.LOCALIZING) {
      updateLocalizingState(earth);
    } else if (state == State.LOCALIZED) {
      updateLocalizedState(earth);
    }
  }

  /**
   * Handles the updating for State.PRETRACKING
   */
  private void updatePretrackingState(Earth earth) {
    if (earth.getTrackingState() == TrackingState.TRACKING) {
      state = State.LOCALIZING;
      return;
    }
  }

  /**
   * Handles the updating for State.LOCALIZING
   */
  private void updateLocalizingState(Earth earth) {
    GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
    if (geospatialPose.getHorizontalAccuracy() <= LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
            && geospatialPose.getOrientationYawAccuracy()
            <= LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES) {
      state = State.LOCALIZED;
      return;
    }

    if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - localizingStartTimestamp)
            > LOCALIZING_TIMEOUT_SECONDS) {
      state = State.LOCALIZING_FAILED;
      return;
    }

  }

  /**
   * Handles the updating for State.LOCALIZED
   */
  private void updateLocalizedState(Earth earth) {
    GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
    if (geospatialPose.getHorizontalAccuracy()
            > LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
            + LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS
            || geospatialPose.getOrientationYawAccuracy()
            > LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES
            + LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES) {
      state = State.LOCALIZING;
      localizingStartTimestamp = System.currentTimeMillis();
      runOnUiThread(() -> {
        clearAnchorsButton.setVisibility(View.INVISIBLE);
      });
      return;
    }

    // **New Code: Place Bus Stop Anchors Once Localized**
    if (!busStopAnchorsPlaced) {
      placeBusStopAnchors();
      busStopAnchorsPlaced = true;
    }
  }

  /**
   * Calculate scale based on distance between camera and anchor
   */
  private float getScale(Pose anchorPose, Pose cameraPose) {
    double distance =
            Math.sqrt(
                    Math.pow(anchorPose.tx() - cameraPose.tx(), 2.0)
                            + Math.pow(anchorPose.ty() - cameraPose.ty(), 2.0)
                            + Math.pow(anchorPose.tz() - cameraPose.tz(), 2.0));
    double mapDistance = Math.min(Math.max(2, distance), 20);
    return (float) (mapDistance - 2) / (20 - 2) + 1;
  }

  /**
   * Menu button to choose anchor type.
   */
  protected boolean settingsMenuClick(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.anchor_reset) {
      return true;
    }
    item.setChecked(!item.isChecked());
    sharedPreferences.edit().putInt(ANCHOR_MODE, itemId).commit();
    if (itemId == R.id.geospatial) {
      anchorType = AnchorType.GEOSPATIAL;
      return true;
    } else if (itemId == R.id.terrain) {
      anchorType = AnchorType.TERRAIN;
      return true;
    } else if (itemId == R.id.rooftop) {
      anchorType = AnchorType.ROOFTOP;
      return true;
    }
    return false;
  }

  private void handleClearAnchorsButton() {
    synchronized (anchorsLock) {
      Iterator<Anchor> iterator = anchors.iterator();
      while (iterator.hasNext()) {
        Anchor anchor = iterator.next();
        if (!busStopAnchors.contains(anchor)) {
          anchor.detach();
          iterator.remove();
        }
      }
    }

    // Only hide the clear button if no non-bus stop anchors remain
    if (anchors.size() == busStopAnchors.size()) {
      clearAnchorsButton.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * Create an anchor at a specific geodetic location using a EUS quaternion.
   */
  private void createAnchor(
          Earth earth, double latitude, double longitude, double altitude, float[] quaternion) {
    Anchor anchor =
            earth.createAnchor(
                    latitude,
                    longitude,
                    altitude,
                    quaternion[0],
                    quaternion[1],
                    quaternion[2],
                    quaternion[3]);
    synchronized (anchorsLock) {
      anchors.add(anchor);
    }
  }

  /**
   * Creates a Terrain Anchor at a specific geodetic location using a EUS quaternion.
   * This method ensures that Terrain Anchors are correctly stored and managed.
   */
  private void createTerrainAnchor(
          Earth earth, double latitude, double longitude, float[] quaternion) {
    final ResolveAnchorOnTerrainFuture future =
            earth.resolveAnchorOnTerrainAsync(
                    latitude,
                    longitude,
                    /* altitudeAboveTerrain= */ 0.0f,
                    quaternion[0],
                    quaternion[1],
                    quaternion[2],
                    quaternion[3],
                    (anchor, state) -> {
                      if (state == TerrainAnchorState.SUCCESS) {
                        synchronized (anchorsLock) {
                          anchors.add(anchor);
                          terrainAnchors.add(anchor);
                        }
                      } else {
                      }
                    });
  }

  @Override
  public void onDialogPositiveClick(DialogFragment dialog) {
    if (!sharedPreferences.edit().putBoolean(ALLOW_GEOSPATIAL_ACCESS_KEY, true).commit()) {
      throw new AssertionError("Could not save the user preference to SharedPreferences!");
    }
    createSession();
  }

  @Override
  public void onDialogContinueClick(DialogFragment dialog) {
    dialog.dismiss();
  }

  private void onRenderStreetscapeGeometryChanged(CompoundButton button, boolean isChecked) {
    if (session == null) {
      return;
    }
    isRenderStreetscapeGeometry = isChecked;
  }

  /**
   * Places Terrain Anchors along the interpolated route with quaternions facing the direction of the next anchor.
   *
   * @param interpolatedRoutePoints List of LatLng points representing the interpolated route.
   */
  private void placeAnchorsAlongRoute(List<LatLng> interpolatedRoutePoints) {
    Log.d(TAG, "Placing Terrain Anchors along the route. Number of interpolated points: " + interpolatedRoutePoints.size());
    if (interpolatedRoutePoints.isEmpty()) {
      Toast.makeText(this, "No points to place anchors.", Toast.LENGTH_SHORT).show();
      Log.e(TAG, "Interpolated route points are empty. Cannot place anchors.");
      return;
    }

    Earth earth = session.getEarth();
    if (earth == null || earth.getTrackingState() != TrackingState.TRACKING) {
      Toast.makeText(this, "Earth is not tracking yet.", Toast.LENGTH_SHORT).show();
      Log.e(TAG, "Earth is not in TRACKING state.");
      return;
    }

    for (int i = 0; i < interpolatedRoutePoints.size(); i++) {
      LatLng currentPoint = interpolatedRoutePoints.get(i);

      // Determine the bearing towards the next point, if not the last point
      float bearing = 0f;
      if (i < interpolatedRoutePoints.size() - 1) {
        LatLng nextPoint = interpolatedRoutePoints.get(i + 1);
        bearing = calculateBearing(currentPoint, nextPoint);
      }

      // Convert bearing to a quaternion that faces the direction of the next anchor
      float[] quaternion = calculateQuaternionFromBearing(bearing);

      // Create a Terrain Anchor at the specified geospatial location with the direction-facing quaternion
      createTerrainAnchor(earth, currentPoint.latitude, currentPoint.longitude, quaternion);
    }

    runOnUiThread(() -> {
      Toast.makeText(this, "Anchors placed along the route.", Toast.LENGTH_SHORT).show();
    });
  }

  /**
   * Calculates the bearing from the current point to the next point.
   *
   * @param start The starting LatLng point.
   * @param end   The destination LatLng point.
   * @return The bearing in degrees.
   */
  private float calculateBearing(LatLng start, LatLng end) {
    double startLat = Math.toRadians(start.latitude);
    double startLng = Math.toRadians(start.longitude);
    double endLat = Math.toRadians(end.latitude);
    double endLng = Math.toRadians(end.longitude);

    double dLng = endLng - startLng;
    double y = Math.sin(dLng) * Math.cos(endLat);
    double x = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng);
    return (float) Math.toDegrees(Math.atan2(y, x));
  }

  /**
   * Converts a bearing angle to a quaternion facing that direction.
   *
   * @param bearing The bearing angle in degrees.
   * @return A quaternion array representing the orientation.
   */
  private float[] calculateQuaternionFromBearing(float bearing) {
    // If your arrow model points along the negative Z-axis by default, add 180 degrees
    float adjustedBearing = bearing + 180.0f;
    // Convert bearing to radians
    float theta = (float) Math.toRadians(adjustedBearing);
    float halfTheta = theta / 2.0f;
    float sinHalfTheta = (float) Math.sin(halfTheta);
    float cosHalfTheta = (float) Math.cos(halfTheta);
    return new float[]{0f, sinHalfTheta, 0f, cosHalfTheta}; // [qx, qy, qz, qw]
  }

  /**
   * Calculates the distance between two geographic coordinates in meters using the Haversine formula.
   *
   * @param lat1 Latitude of the first point.
   * @param lon1 Longitude of the first point.
   * @param lat2 Latitude of the second point.
   * @param lon2 Longitude of the second point.
   * @return Distance in meters.
   */
  private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371000; // Radius of the Earth in meters
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = R * c;
    return distance;
  }

  private static class LatLng {
    double latitude;
    double longitude;

    LatLng(double lat, double lng) {
      this.latitude = lat;
      this.longitude = lng;
    }
  }

  /**
   * Recompute the route from the current location to the final destination.
   *
   * @param currentLocation The user's current location.
   */
  private void computeRoute(Location currentLocation) {
    if (isRecalculatingRoute) {
      Log.d(TAG, "Route recalculation already in progress. Skipping this request.");
      return;
    }

    isRecalculatingRoute = true;
    Log.d(TAG, "Computing route from current location.");

    getRoute(
            currentLocation.getLatitude(),
            currentLocation.getLongitude(),
            FINAL_DESTINATION_LATITUDE,
            FINAL_DESTINATION_LONGITUDE,
            new DirectionsCallback() {
              @Override
              public void onSuccess(List<LatLng> newRoutePoints, List<DirectionStep> newSteps) {
                Log.d(TAG, "Route computation successful. Number of points: " + newRoutePoints.size());

                // Define interpolation parameters
                int intervalMeters = ANCHOR_INTERVAL_METERS; // e.g., 10 meters
                int maxSegmentDistanceMeters = MAX_SEGMENT_DISTANCE_METERS; // e.g., 1000 meters

                // Interpolate the route points
                List<LatLng> interpolatedPoints = interpolateRoutePoints(newRoutePoints, intervalMeters, maxSegmentDistanceMeters);
                Log.d(TAG, "Number of interpolated points: " + interpolatedPoints.size());

                // Filter interpolated points based on proximity to current location
                List<LatLng> proximatePoints = filterPointsByProximity(interpolatedPoints, currentLocation, GEOFENCING_THRESHOLD_METERS);
                Log.d(TAG, "Number of proximate points within " + PROXIMITY_THRESHOLD_METERS + " meters: " + proximatePoints.size());

                // Place anchors along the proximate route points
                placeAnchorsAlongRoute(proximatePoints);

                // Update previousRoutePoints for future comparisons
                previousRoutePoints = new ArrayList<>(newRoutePoints);

                // Store the steps
                directionSteps = newSteps;
                currentStepIndex = 0;

                // Update UI with the first two steps
                runOnUiThread(() -> updateDirectionUI());

                isRecalculatingRoute = false;
              }

              @Override
              public void onFailure(String error) {
                Log.e(TAG, "Route computation failed: " + error);
                runOnUiThread(() -> {
                  Toast.makeText(GeospatialActivity.this, "Route Error: " + error, Toast.LENGTH_LONG).show();
                  isRecalculatingRoute = false;
                });
              }
            }
    );
  }

  private List<LatLng> filterPointsByProximity(List<LatLng> routePoints, Location currentLocation, double thresholdMeters) {
    List<LatLng> proximatePoints = new ArrayList<>();

    for (LatLng point : routePoints) {
      double distance = distanceBetween(
              currentLocation.getLatitude(),
              currentLocation.getLongitude(),
              point.latitude,
              point.longitude
      );

      if (distance <= thresholdMeters) {
        proximatePoints.add(point);
        Log.d(TAG, "Point (" + point.latitude + ", " + point.longitude + ") is within " + thresholdMeters + " meters.");
      } else {
        Log.d(TAG, "Point (" + point.latitude + ", " + point.longitude + ") is outside " + thresholdMeters + " meters. Skipping.");
      }
    }

    return proximatePoints;
  }

  /**
   * Fetches the route from origin to destination using Google Directions API.
   *
   * @param originLat Latitude of the origin.
   * @param originLng Longitude of the origin.
   * @param destLat   Latitude of the destination.
   * @param destLng   Longitude of the destination.
   * @param callback  Callback to handle the API response.
   */
  private void getRoute(double originLat, double originLng, double destLat, double destLng, DirectionsCallback callback) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(DIRECTIONS_API_URL).newBuilder()
            .addQueryParameter("origin", originLat + "," + originLng)
            .addQueryParameter("destination", destLat + "," + destLng)
            .addQueryParameter("key", DIRECTIONS_API_KEY);

    if (isWheelchairAccessible) {
      // Use transit mode with preference for less walking
      urlBuilder.addQueryParameter("mode", "transit")
              .addQueryParameter("transit_routing_preference", "less_walking");
    } else {
      // Default to walking mode
      urlBuilder.addQueryParameter("mode", "walking");
    }

    HttpUrl url = urlBuilder.build();
    Request request = new Request.Builder().url(url).build();
    OkHttpClient client = new OkHttpClient();

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        callback.onFailure("Network Error: " + e.getMessage());
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (response.isSuccessful()) {
          String resp = response.body().string();
          parseDirections(resp, callback);
        } else {
          callback.onFailure("API Error: " + response.message());
        }
      }
    });
  }

  /**
   * Parses the JSON response from Directions API and decodes the polyline.
   *
   * @param json     The JSON response string.
   * @param callback The callback to handle the parsed route points.
   */
  private void parseDirections(String json, DirectionsCallback callback) {
    try {
      JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
      JsonArray routes = jsonObject.getAsJsonArray("routes");
      if (routes.size() == 0) {
        callback.onFailure("No routes found");
        return;
      }

      JsonObject route = routes.get(0).getAsJsonObject();
      JsonObject overviewPolyline = route.getAsJsonObject("overview_polyline");
      String encodedPoints = overviewPolyline.get("points").getAsString();
      List<LatLng> routePoints = decodePolyline(encodedPoints);

      // Extract steps
      List<DirectionStep> steps = new ArrayList<>();
      JsonArray legs = route.getAsJsonArray("legs");
      if (legs.size() > 0) {
        JsonObject leg = legs.get(0).getAsJsonObject();
        JsonArray jsonSteps = leg.getAsJsonArray("steps");
        for (JsonElement stepElement : jsonSteps) {
          JsonObject step = stepElement.getAsJsonObject();
          String htmlInstructions = step.get("html_instructions").getAsString();

          // Remove HTML tags from instructions
          String plainInstructions;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            plainInstructions = Html.fromHtml(htmlInstructions, Html.FROM_HTML_MODE_LEGACY).toString();
          } else {
            plainInstructions = Html.fromHtml(htmlInstructions).toString();
          }

          // Get start location
          JsonObject startLocation = step.getAsJsonObject("start_location");
          double startLat = startLocation.get("lat").getAsDouble();
          double startLng = startLocation.get("lng").getAsDouble();

          // Get end location
          JsonObject endLocation = step.getAsJsonObject("end_location");
          double endLat = endLocation.get("lat").getAsDouble();
          double endLng = endLocation.get("lng").getAsDouble();

          DirectionStep directionStep = new DirectionStep(
                  plainInstructions,
                  new LatLng(startLat, startLng),
                  new LatLng(endLat, endLng)
          );
          steps.add(directionStep);
        }
      }

      callback.onSuccess(routePoints, steps);
    } catch (Exception e) {
      callback.onFailure("Parsing Error: " + e.getMessage());
    }
  }

  /**
   * Decodes a polyline string into a list of LatLng points using Google's algorithm.
   *
   * @param encoded The encoded polyline string.
   * @return A list of LatLng points.
   */
  private List<LatLng> decodePolyline(String encoded) {
    List<LatLng> poly = new ArrayList<>();
    int index = 0, len = encoded.length();
    int lat = 0, lng = 0;

    while (index < len) {
      int b, shift = 0, result = 0;
      do {
        b = encoded.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        b = encoded.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
      lng += dlng;

      LatLng point = new LatLng(lat / 1E5, lng / 1E5);
      poly.add(point);
    }
    return poly;
  }

  private interface DirectionsCallback {
    void onSuccess(List<LatLng> routePoints, List<DirectionStep> steps);
    void onFailure(String error);
  }

  private void requestNewLocationData() {
    LocationRequest locationRequest = LocationRequest.create();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(1000); // 1 second
    locationRequest.setFastestInterval(500); // 0.5 seconds

    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
  }

  /**
   * Interpolates route points to ensure anchors are placed at exact intervals,
   * but only for segments within a specified maximum distance.
   *
   * @param routePoints        List of LatLng points representing the route.
   * @param intervalMeters     Desired interval between anchors in meters.
   * @param maxSegmentDistance Maximum allowable distance between two consecutive points for interpolation.
   * @return List of LatLng points spaced at the specified interval within the segment distance constraint.
   */
  private List<LatLng> interpolateRoutePoints(List<LatLng> routePoints, int intervalMeters, int maxSegmentDistance) {
    List<LatLng> interpolatedPoints = new ArrayList<>();
    if (routePoints.isEmpty()) return interpolatedPoints;

    LatLng previousPoint = routePoints.get(0);
    interpolatedPoints.add(previousPoint);
    Log.d(TAG, "Initial route point added: (" + previousPoint.latitude + ", " + previousPoint.longitude + ")");
    double accumulatedDistance = 0.0;

    for (int i = 1; i < routePoints.size(); i++) {
      LatLng currentPoint = routePoints.get(i);
      double segmentDistance = distanceBetween(
              previousPoint.latitude, previousPoint.longitude,
              currentPoint.latitude, currentPoint.longitude);

      // Check if the segment distance is within the maximum allowed distance
      if (segmentDistance <= maxSegmentDistance) {
        accumulatedDistance += segmentDistance;

        while (accumulatedDistance >= intervalMeters) {
          double ratio = (intervalMeters - (accumulatedDistance - segmentDistance)) / segmentDistance;
          double newLat = previousPoint.latitude + (currentPoint.latitude - previousPoint.latitude) * ratio;
          double newLng = previousPoint.longitude + (currentPoint.longitude - previousPoint.longitude) * ratio;
          LatLng newPoint = new LatLng(newLat, newLng);

          // Deduplication check
          if (interpolatedPoints.isEmpty() || distanceBetween(
                  interpolatedPoints.get(interpolatedPoints.size() - 1).latitude,
                  interpolatedPoints.get(interpolatedPoints.size() - 1).longitude,
                  newPoint.latitude,
                  newPoint.longitude) >= intervalMeters) {
            interpolatedPoints.add(newPoint);
            Log.d(TAG, "Interpolated route point added at " + intervalMeters + " meters: (" + newPoint.latitude + ", " + newPoint.longitude + ")");
          }

          previousPoint = newPoint;
          accumulatedDistance -= intervalMeters;
        }

        previousPoint = currentPoint;
      } else {
        // Segment exceeds maximum distance; skip interpolation for this segment
        interpolatedPoints.add(currentPoint);
        Log.d(TAG, "Segment distance (" + segmentDistance + "m) exceeds max (" + maxSegmentDistance + "m). Skipping interpolation.");
        accumulatedDistance = 0.0;
        previousPoint = currentPoint;
      }
    }

    Log.d(TAG, "Total interpolated route points: " + interpolatedPoints.size());
    return interpolatedPoints;
  }

  /**
   * Creates a Terrain Anchor specifically for the route and adds it to routeAnchors set.
   *
   * @param earth      The Earth object from ARCore.
   * @param latitude   The latitude of the anchor location.
   * @param longitude  The longitude of the anchor location.
   * @param quaternion The orientation quaternion.
   */
  private void createRouteTerrainAnchor(
          Earth earth, double latitude, double longitude, float[] quaternion) {
    Log.d(TAG, "Resolving Route Terrain Anchor asynchronously for (" + latitude + ", " + longitude + ")");
    earth.resolveAnchorOnTerrainAsync(
            latitude,
            longitude,
            /* altitudeAboveTerrain= */ 0.0f,
            quaternion[0],
            quaternion[1],
            quaternion[2],
            quaternion[3],
            (anchor, state) -> {
              if (state == TerrainAnchorState.SUCCESS) {
                synchronized (routeAnchorsLock) {
                  routeAnchors.add(anchor);
                }
                synchronized (anchorsLock) {
                  anchors.add(anchor);
                  terrainAnchors.add(anchor);
                }
                // Store Route Terrain Anchor parameters for persistence
                Log.d(TAG, "Successfully placed Route Terrain Anchor at (" + latitude + ", " + longitude + ")");
              } else {
                String errorMessage;
                errorMessage = "Failed to place Route Terrain Anchor.";
                Log.e(TAG, "Error placing Route Terrain Anchor at (" + latitude + ", " + longitude + "): " + errorMessage);
                runOnUiThread(() -> {
                  Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                });
              }
            });
  }

  private void checkProximityToAnchors(Location currentLocation) {
    synchronized (anchorsLock) {
      Iterator<Anchor> iterator = anchors.iterator();
      while (iterator.hasNext()) {
        Anchor anchor = iterator.next();
        if (anchor.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }

        // Obtain the GeospatialPose from ARCore Earth API
        GeospatialPose geospatialPose = session.getEarth().getGeospatialPose(anchor.getPose());

        if (geospatialPose == null) {
          Log.e(TAG, "GeospatialPose is null for anchor: " + anchor);
          continue;
        }

        double anchorLat = geospatialPose.getLatitude();
        double anchorLng = geospatialPose.getLongitude();

        double distance = distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                anchorLat,
                anchorLng
        );

        Log.d(TAG, "Distance to anchor (" + anchorLat + ", " + anchorLng + "): " + distance + " meters.");

        if (distance < PROXIMITY_THRESHOLD_METERS) {
          // User is close to the anchor
          if (!busStopAnchors.contains(anchor)) {
            playSuccessSound();
            // Remove the anchor
            anchor.detach();
            iterator.remove();

            synchronized (routeAnchorsLock) {
              routeAnchors.remove(anchor);
            }
            synchronized (terrainAnchors) {
              terrainAnchors.remove(anchor);
            }

            Log.d(TAG, "Anchor removed due to proximity: (" + anchorLat + ", " + anchorLng + ")");
          }
        }
      }
    }
  }

  /**
   * Plays a success sound when the user walks past an anchor.
   */
  private void playSuccessSound() {
    if (mediaPlayer != null) {
      mediaPlayer.start();
    }
  }

  /**
   * Clears all existing route-specific Terrain Anchors from ARCore and internal storage.
   */
  /**
   * Clears all existing route-specific Terrain Anchors from ARCore and internal storage.
   */
  private void clearRouteAnchors() {
    synchronized (routeAnchorsLock) {
      Iterator<Anchor> iterator = routeAnchors.iterator();
      while (iterator.hasNext()) {
        Anchor anchor = iterator.next();
        if (!busStopAnchors.contains(anchor)) {
          anchor.detach();
          iterator.remove();
          synchronized (anchorsLock) {
            anchors.remove(anchor);
            terrainAnchors.remove(anchor);
          }
        }
      }
      Log.d(TAG, "Cleared route Terrain Anchors.");
    }

    // Reset previous route points to trigger a fresh route computation next time
    previousRoutePoints.clear();
  }

  /**
   * Handles the Replace Route Anchors button click.
   * Clears existing route anchors and places new anchors along the computed route.
   */
  private void handleReplaceRouteAnchorsButton() {
    Log.d(TAG, "Replace Route Anchors button clicked.");

    // Step 1: Clear existing route anchors
    clearRouteAnchors();
    isRoutingActive = true;

    // Step 2: Recompute the route and place new anchors
    fetchCurrentLocationAndComputeRoute();
  }

  /**
   * Fetches the current location and computes a new route to the final destination.
   */
  private void fetchCurrentLocationAndComputeRoute() {
    Log.d(TAG, "Fetching current location for route recomputation.");
    fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
              if (location != null) {
                Log.d(TAG, "Current location obtained: (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                computeRoute(location);
              } else {
                Log.e(TAG, "Current location is null. Requesting new location data.");
                requestNewLocationData();
              }
            })
            .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to get current location: " + e.getMessage());
              Toast.makeText(this, "Failed to get current location.", Toast.LENGTH_LONG).show();
            });
  }

  /**
   * Compares two lists of LatLng points to determine if they are different.
   *
   * @param oldRoute The existing route points.
   * @param newRoute The newly computed route points.
   * @return True if the routes are different; false otherwise.
   */
  private boolean isRouteDifferent(List<LatLng> oldRoute, List<LatLng> newRoute) {
    if (oldRoute == null || oldRoute.isEmpty()) {
      return newRoute != null && !newRoute.isEmpty();
    }
    if (newRoute == null || newRoute.isEmpty()) {
      return oldRoute != null && !oldRoute.isEmpty();
    }
    if (oldRoute.size() != newRoute.size()) {
      return true;
    }
    for (int i = 0; i < oldRoute.size(); i++) {
      LatLng oldPoint = oldRoute.get(i);
      LatLng newPoint = newRoute.get(i);
      double distance = distanceBetween(
              oldPoint.latitude, oldPoint.longitude,
              newPoint.latitude, newPoint.longitude);
      if (distance > PROXIMITY_THRESHOLD_METERS) { // Threshold in meters
        return true;
      }
    }
    return false;
  }

  private boolean isTargetLocationAnchor(Anchor anchor) {
    Earth earth = session.getEarth();
    if (earth == null) return false;

    // Obtain the geospatial pose of the anchor
    Pose anchorPose = anchor.getPose();
    GeospatialPose anchorGeospatialPose = earth.getGeospatialPose(anchorPose);

    for (TargetLocation target : targetLocations) {
      // Calculate distance between anchor's geospatial position and target location
      double distance = distanceBetween(
              anchorGeospatialPose.getLatitude(),
              anchorGeospatialPose.getLongitude(),
              target.latitude,
              target.longitude
      );

      // If the anchor is within the threshold distance of the target location, return true
      if (distance < BUS_STOP_THRESHOLD) { // BUS_STOP_THRESHOLD is 500 meters
        return true;
      }
    }
    return false; // No matching target location found
  }

  private void placeBusStopAnchors() {
    Earth earth = session.getEarth();
    if (earth == null || earth.getTrackingState() != TrackingState.TRACKING) {
      Toast.makeText(this, "Earth is not tracking yet.", Toast.LENGTH_SHORT).show();
      Log.e(TAG, "Earth is not in TRACKING state.");
      return;
    }

    for (TargetLocation busStop : targetLocations) {
      createAnchorAtBusStop(earth, busStop);
    }

    runOnUiThread(() -> {
      Toast.makeText(this, "Bus stops placed in AR scene.", Toast.LENGTH_SHORT).show();
    });
  }

  private void createAnchorAtBusStop(Earth earth, TargetLocation busStop) {
    double latitude = busStop.latitude;
    double longitude = busStop.longitude;
    double altitude = busStop.altitude;

    // Calculate bearing towards the next bus stop
    float bearing = calculateBearingToNextBusStop(busStop);
    float[] quaternion = calculateQuaternionFromBearing(bearing);

    // Create a Geospatial Anchor
    Anchor anchor = earth.createAnchor(
            latitude,
            longitude,
            altitude,
            quaternion[0],
            quaternion[1],
            quaternion[2],
            quaternion[3]
    );

    synchronized (anchorsLock) {
      anchors.add(anchor);
    }

    // Add to busStopAnchors set
    busStopAnchors.add(anchor);

    Log.d(TAG, "Bus Stop Anchor created at (" + latitude + ", " + longitude + ") with bearing " + bearing);
  }

  private float calculateBearingToNextBusStop(TargetLocation currentBusStop) {
    int currentIndex = targetLocations.indexOf(currentBusStop);
    if (currentIndex == -1 || currentIndex >= targetLocations.size() - 1) {
      return 0f; // Default bearing
    }
    TargetLocation nextBusStop = targetLocations.get(currentIndex + 1);
    return calculateBearing(
            new LatLng(currentBusStop.latitude, currentBusStop.longitude),
            new LatLng(nextBusStop.latitude, nextBusStop.longitude)
    );
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    this.googleMap = googleMap;

    // Enable My Location layer if permissions are granted
    try {
      googleMap.setMyLocationEnabled(true);
    } catch (SecurityException e) {
      Log.e(TAG, "Location permissions not granted for My Location layer.");
    }

    // Disable default UI controls as needed
    googleMap.getUiSettings().setMyLocationButtonEnabled(false); // Hide location button
    googleMap.getUiSettings().setZoomControlsEnabled(false); // Hide zoom controls
    googleMap.getUiSettings().setAllGesturesEnabled(false); // Disable all gestures to prevent user interaction

    // Initially center the map on the user's last known location
    fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
              if (location != null) {
                updateMapLocation(location);
              }
            });
  }
  /**
   * Updates the map's marker and camera position based on the user's current location.
   *
   * @param location The user's current location.
   */
  private void updateMapLocation(Location location) {
    if (googleMap == null) {
      return;
    }

    com.google.android.gms.maps.model.LatLng userLatLng = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
    if (currentLocationMarker == null) {
      // Create a new marker if it doesn't exist
      MarkerOptions markerOptions = new MarkerOptions()
              .position(userLatLng)
              .title("You are here")
              .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)); // Customize marker color if desired
      currentLocationMarker = googleMap.addMarker(markerOptions);

      // Add a circle with 100-meter radius
      CircleOptions circleOptions = new CircleOptions()
              .center(userLatLng)
              .radius(MAP_CIRCLE_RADIUS) // Radius in meters
              .strokeColor(0x5500FF00) // Semi-transparent green
              .fillColor(0x2200FF00)   // More transparent green
              .strokeWidth(2f);
      userLocationCircle = googleMap.addCircle(circleOptions);

      // Move camera to user's location
      googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, MAP_ZOOM));
    } else {
      // Update marker position
      currentLocationMarker.setPosition(userLatLng);

      // Update circle position
      if (userLocationCircle != null) {
        userLocationCircle.setCenter(userLatLng);
      }

      // Optionally, animate camera movement
      googleMap.animateCamera(CameraUpdateFactory.newLatLng(userLatLng));
    }
  }
  private void updateDirectionUI() {
    TextView currentDirectionView = findViewById(R.id.current_direction);
    TextView nextDirectionView = findViewById(R.id.next_direction);

    if (currentStepIndex < directionSteps.size()) {
      DirectionStep currentStep = directionSteps.get(currentStepIndex);
      currentDirectionView.setText(currentStep.instruction != null && !currentStep.instruction.isEmpty()
              ? currentStep.instruction
              : "Proceed to your destination");
    } else {
      currentDirectionView.setText("You have arrived at your destination");
    }

    if (currentStepIndex + 1 < directionSteps.size()) {
      DirectionStep nextStep = directionSteps.get(currentStepIndex + 1);
      nextDirectionView.setText(nextStep.instruction != null && !nextStep.instruction.isEmpty()
              ? nextStep.instruction
              : "Continue to your destination");
      nextDirectionView.setVisibility(View.VISIBLE);
    } else {
      nextDirectionView.setVisibility(View.GONE);
    }
  }

  private void checkIfReachedStepEnd(Location location) {
    if (currentStepIndex >= directionSteps.size()) {
      return;
    }

    DirectionStep currentStep = directionSteps.get(currentStepIndex);
    double distanceToEnd = distanceBetween(
            location.getLatitude(),
            location.getLongitude(),
            currentStep.endLocation.latitude,
            currentStep.endLocation.longitude
    );

    if (distanceToEnd < STEP_COMPLETION_THRESHOLD_METERS) {
      // Move to next step
      currentStepIndex++;
      runOnUiThread(() -> updateDirectionUI());
    }
  }


}


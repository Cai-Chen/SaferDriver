/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package unimelb.comp90018.saferdriver;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import unimelb.comp90018.saferdriver.ui.camera.CameraSourcePreview;
import unimelb.comp90018.saferdriver.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for Googly Eyes, an app that uses the camera to track faces and superimpose Googly Eyes
 * animated graphics over the eyes.  The app also detects whether the eyes are open or closed,
 * drawing the eyes in the correct state.<p>
 *
 * This app supports both a front facing mode and a rear facing mode, which demonstrate different
 * API functionality trade-offs:<p>
 *
 * Front facing mode uses the device's front facing camera to track one user, in a "selfie" fashion.
 * The settings for the face detector and its associated processing pipeline are set to optimize for
 * the single face case, where the face is relatively large.  These factors allow the face detector
 * to be faster and more responsive to quick motion.<p>
 *
 * Rear facing mode uses the device's rear facing camera to track any number of faces.  The settings
 * for the face detector and its associated processing pipeline support finding multiple faces, and
 * attempt to find smaller faces in comparison to the front facing mode.  But since this requires
 * more scanning at finer levels of detail, rear facing mode may not be as responsive as front
 * facing mode.<p>
 */
public final class GooglyEyesActivity extends AppCompatActivity {
    private static final String TAG = "GooglyEyes";

    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private boolean mIsFrontFacing = true;

    private static AppCompatActivity instance;

    //==============================================================================================
    // Location update variables
    //==============================================================================================

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    private static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * The formatted location address.
     */
    private String mAddressOutput;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     */
    //private boolean mAddressRequested;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    // UI Widgets.
    private Button mStopUpdatesButton;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        instance = this;

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mStopUpdatesButton = (Button) findViewById(R.id.stopButton);

        final Button button = (Button) findViewById(R.id.flipButton);
        button.setOnClickListener(mFlipButtonListener);

        //=========================================================================================
        mLastUpdateTime = "";
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        mRequestingLocationUpdates = true;
        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        if (savedInstanceState != null) {
            mIsFrontFacing = savedInstanceState.getBoolean("IsFrontFacing");
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        if (checkPermissions()) {
            createCameraSource();
            startLocationUpdates();
            setButtonsEnabledState();
        } else {
            requestPermissions();
        }

        //==========================================================================================

    }


    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();

        //==========================================================================================
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        updateUI();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        stopLocationUpdates();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method is invoked for every call on
     * {@link #requestPermissions(String[], int)}.<p>
     *
     * <strong>Note:</strong> It is possible that the permissions request interaction with the user
     * is interrupted. In this case you will receive empty permissions and results arrays which
     * should be treated as a cancellation.<p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode != REQUEST_PERMISSIONS_REQUEST_CODE) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length > 0 ) {
            boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            boolean locationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission && locationPermission){
                Log.d(TAG, "permission granted - initialize the camera source and location");
                // we have permission, so create the camerasource
                createCameraSource();
                startLocationUpdates();
                return;
            }else {
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }

        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // UI
    //==============================================================================================

    /**
     * Saves the camera facing mode, so that it can be restored after the device is rotated.
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("IsFrontFacing", mIsFrontFacing);
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
    }

    /**
     * Toggles between front-facing and rear-facing modes.
     */
    private View.OnClickListener mFlipButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            mIsFrontFacing = !mIsFrontFacing;

            if (mCameraSource != null) {
                mCameraSource.release();
                mCameraSource = null;
            }

            createCameraSource();
            startCameraSource();
        }
    };

    //==============================================================================================
    // Detector
    //==============================================================================================

    /**
     * Creates the face detector and associated processing pipeline to support either front facing
     * mode or rear facing mode.  Checks if the detector is ready to use, and displays a low storage
     * warning if it was not possible to download the face library.
     */
    @NonNull
    private FaceDetector createFaceDetector(Context context) {
        // For both front facing and rear facing modes, the detector is initialized to do landmark
        // detection (to find the eyes), classification (to determine if the eyes are open), and
        // tracking.
        //
        // Use of "fast mode" enables faster detection for frontward faces, at the expense of not
        // attempting to detect faces at more varied angles (e.g., faces in profile).  Therefore,
        // faces that are turned too far won't be detected under fast mode.
        //
        // For front facing mode only, the detector will use the "prominent face only" setting,
        // which is optimized for tracking a single relatively large face.  This setting allows the
        // detector to take some shortcuts to make tracking faster, at the expense of not being able
        // to track multiple faces.
        //
        // Setting the minimum face size not only controls how large faces must be in order to be
        // detected, it also affects performance.  Since it takes longer to scan for smaller faces,
        // we increase the minimum face size for the rear facing mode a little bit in order to make
        // tracking faster (at the expense of missing smaller faces).  But this optimization is less
        // important for the front facing case, because when "prominent face only" is enabled, the
        // detector stops scanning for faces after it has found the first (large) face.
        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(mIsFrontFacing)
                .setMinFaceSize(mIsFrontFacing ? 0.35f : 0.15f)
                .build();

        Detector.Processor<Face> processor;
        if (mIsFrontFacing) {
            // For front facing mode, a single tracker instance is used with an associated focusing
            // processor.  This configuration allows the face detector to take some shortcuts to
            // speed up detection, in that it can quit after finding a single face and can assume
            // that the nextIrisPosition face position is usually relatively close to the last seen
            // face position.
            Tracker<Face> tracker = new GooglyFaceTracker(mGraphicOverlay);
            processor = new LargestFaceFocusingProcessor.Builder(detector, tracker).build();
        } else {
            // For rear facing mode, a factory is used to create per-face tracker instances.  A
            // tracker is created for each face and is maintained as long as the same face is
            // visible, enabling per-face state to be maintained over time.  This is used to store
            // the iris position and velocity for each face independently, simulating the motion of
            // the eyes of any number of faces over time.
            //
            // Both the front facing mode and the rear facing mode use the same tracker
            // implementation, avoiding the need for any additional code.  The only difference
            // between these cases is the choice of Processor: one that is specialized for tracking
            // a single face or one that can handle multiple faces.  Here, we use MultiProcessor,
            // which is a standard component of the mobile vision API for managing multiple items.
            MultiProcessor.Factory<Face> factory = new MultiProcessor.Factory<Face>() {
                @Override
                public Tracker<Face> create(Face face) {
                    return new GooglyFaceTracker(mGraphicOverlay);
                }
            };
            processor = new MultiProcessor.Builder<>(factory).build();
        }

        detector.setProcessor(processor);

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }
        return detector;
    }

    //==============================================================================================
    // Camera Source
    //==============================================================================================

    /**
     * Creates the face detector and the camera.
     */
    private void createCameraSource() {
        Context context = getApplicationContext();
        FaceDetector detector = createFaceDetector(context);

        int facing = CameraSource.CAMERA_FACING_FRONT;
        if (!mIsFrontFacing) {
            facing = CameraSource.CAMERA_FACING_BACK;
        }

        // The camera source is initialized to use either the front or rear facing camera.  We use a
        // relatively low resolution for the camera preview, since this is sufficient for this app
        // and the face detector will run faster at lower camera resolutions.
        //
        // However, note that there is a speed/accuracy trade-off with respect to choosing the
        // camera resolution.  The face detector will run faster with lower camera resolutions,
        // but may miss smaller faces, landmarks, or may not correctly detect eyes open/closed in
        // comparison to using higher camera resolutions.  If you have any of these issues, you may
        // want to increase the resolution.
        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(facing)
                .setRequestedPreviewSize(320 * 100, 240 * 100)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    public static Context getContext()
    {
        return instance.getApplicationContext();
    }



    //==============================================================================================
    // Location Methods
    //==============================================================================================


    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                getAddress(mCurrentLocation);
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
//                startIntentService(mCurrentLocation);
            }
        };
    }

//    private void startIntentService(Location lastLocation) {
//        // Create an intent for passing to the intent service responsible for fetching the address.
//        Intent intent = new Intent(this, LocationUpdateIntentService.class);
//
//        // Pass the result receiver as an extra to the service.
//        intent.putExtra(Constants.RECEIVER, mResultReceiver);
//
//        // Pass the location data as an extra to the service.
//        intent.putExtra(Constants.LOCATION_DATA_EXTRA, lastLocation);
//
//        // Start the service. If the service isn't already running, it is instantiated and started
//        // (creating a process for it if needed); if it is running then it remains running. The
//        // service kills itself automatically once all intents are processed.
//        startService(intent);
//    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
        }
    }
    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(GooglyEyesActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(GooglyEyesActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI();
                    }
                });
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            System.out.println(mAddressOutput+mLastUpdateTime);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionStateLocation = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionStateCamera = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionStateCamera+permissionStateLocation == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean locationShouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        boolean camereShouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (locationShouldProvideRationale||camereShouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(GooglyEyesActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.CAMERA},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(GooglyEyesActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates.
     */
    public void stopUpdatesButtonHandler(View view) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        stopLocationUpdates();
    }

    public void getAddress(Location location){
        if (location == null) {
            Log.w(TAG, "onSuccess:null");
            return;
        }

        mCurrentLocation = location;

        // Determine whether a Geocoder is available.
        if (!Geocoder.isPresent()) {
            showSnackbar(getString(R.string.no_geocoder_available));
            return;
        }

        // If the user pressed the fetch address button before we had the location,
        // this will be set to true indicating that we should kick off the intent
        // service after fetching the location.
        if (mRequestingLocationUpdates) {
            mAddressOutput = findAddress(location);
        }
    }

    public String findAddress(Location location){
        String errorMessage = "";

        // Make sure that the location data was really sent over through an extra. If it wasn't,
        // send an error error message and return.
        if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            Log.wtf(TAG, errorMessage);
            return errorMessage;
        }

        // Errors could still arise from using the Geocoder (for example, if there is no
        // connectivity, or if the Geocoder is given illegal location data). Or, the Geocoder may
        // simply not have an address for a location. In all these cases, we communicate with the
        // receiver using a resultCode indicating failure. If an address is found, we use a
        // resultCode indicating success.

        // The Geocoder used in this sample. The Geocoder's responses are localized for the given
        // Locale, which represents a specific geographical or linguistic region. Locales are used
        // to alter the presentation of information such as numbers or dates to suit the conventions
        // in the region they describe.
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Address found using the Geocoder.
        List<Address> addresses = null;

        try {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, we get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            return errorMessage;
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using {@code getAddressLine},
            // join them, and send them to the thread. The {@link android.location.address}
            // class provides other options for fetching address details that you may prefer
            // to use. Here are some examples:
            // getLocality() ("Mountain View", for example)
            // getAdminArea() ("CA", for example)
            // getPostalCode() ("94043", for example)
            // getCountryCode() ("US", for example)
            // getCountryName() ("United States", for example)
            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, getString(R.string.address_found));
            return TextUtils.join(System.getProperty("line.separator"), addressFragments);
        }
    }

}

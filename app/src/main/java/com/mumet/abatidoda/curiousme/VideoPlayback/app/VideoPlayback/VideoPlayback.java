/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.mumet.abatidoda.curiousme.VideoPlayback.app.VideoPlayback;

import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.DataSet;
import com.qualcomm.vuforia.ObjectTracker;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.TargetFinder;
import com.qualcomm.vuforia.TargetSearchResult;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vuforia;
import com.mumet.abatidoda.curiousme.SampleApplication.SampleApplicationControl;
import com.mumet.abatidoda.curiousme.SampleApplication.SampleApplicationException;
import com.mumet.abatidoda.curiousme.SampleApplication.SampleApplicationSession;
import com.mumet.abatidoda.curiousme.SampleApplication.utils.LoadingDialogHandler;
import com.mumet.abatidoda.curiousme.SampleApplication.utils.SampleApplicationGLView;
import com.mumet.abatidoda.curiousme.SampleApplication.utils.Texture;
import com.mumet.abatidoda.curiousme.VideoPlayback.R;
import com.mumet.abatidoda.curiousme.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.mumet.abatidoda.curiousme.VideoPlayback.ui.ActivityList.AboutScreen;
import com.mumet.abatidoda.curiousme.VideoPlayback.ui.SampleAppMenu.SampleAppMenu;
import com.mumet.abatidoda.curiousme.VideoPlayback.ui.SampleAppMenu.SampleAppMenuGroup;
import com.mumet.abatidoda.curiousme.VideoPlayback.ui.SampleAppMenu.SampleAppMenuInterface;


// The AR activity for the VideoPlayback sample.
public class VideoPlayback extends Activity implements
    SampleApplicationControl, SampleAppMenuInterface
{
    private static final String LOGTAG = "VideoPlayback";
    
    SampleApplicationSession vuforiaAppSession;
    
    Activity mActivity;
    
    // Helpers to detect events such as double tapping:
    private GestureDetector mGestureDetector = null;
    private SimpleOnGestureListener mSimpleListener = null;
    
    // Movie for the Targets:
    public static final int NUM_TARGETS = 2;
    public static final int STONES = 0;
    public static final int CHIPS = 1;
    private VideoPlayerHelper mVideoPlayerHelper = null;
    private int mSeekPosition;
    private boolean mWasPlaying;
    private String mMovieName;
    
    // A boolean to indicate whether we come from full screen:
    private boolean mReturningFromFullScreen = false;
    
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    
    // Our renderer:
    private VideoPlaybackRenderer mRenderer;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    
    DataSet dataSetStonesAndChips = null;
    
    private RelativeLayout mUILayout;
    
    private boolean mPlayFullscreenVideo = false;
    
    private SampleAppMenu mSampleAppMenu;
    
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
        this);
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    
    boolean mIsDroidDevice = false;
    boolean mIsInitialized = false;

    //Pocci
    boolean mFinderStarted = false;
    static final int UPDATE_ERROR_NO_NETWORK_CONNECTION = -3;
    static final int UPDATE_ERROR_SERVICE_NOT_AVAILABLE = -4;
    private boolean mExtendedTracking = false;
    private static final String kAccessKey = "INSERT ACCESS KEY HERE";
    private static final String kSecretKey = "INSERT SECRET KEY HERE";
    private int mInitErrorCode = 0;

    // Called when the activity first starts or the user navigates back
    // to an activity.
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        vuforiaAppSession = new SampleApplicationSession(this);
        
        mActivity = this;
        
        startLoadingAnimation();
        
        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();
        
        // Create the gesture detector that will handle the single and
        // double taps:
        mSimpleListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getApplicationContext(),
            mSimpleListener);
        
        mVideoPlayerHelper = null;
        mSeekPosition = 0;
        mWasPlaying = false;
        mMovieName = "";
        
        // Create the video player helper that handles the playback of the movie
        // for the targets:

        mVideoPlayerHelper = new VideoPlayerHelper();
        mVideoPlayerHelper.init();
        mVideoPlayerHelper.setActivity(this);

        
        // Set the double tap listener:
        mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener()
        {
            public boolean onDoubleTap(MotionEvent e)
            {
               // We do not react to this event
               return false;
            }
            
            
            public boolean onDoubleTapEvent(MotionEvent e)
            {
                // We do not react to this event
                return false;
            }
            
            
            // Handle the single tap
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                boolean isSingleTapHandled = false;
                // Do not react if the StartupScreen is being displayed

                // Verify that the tap happened inside the target
                if (mRenderer!= null && mRenderer.isTapOnScreenInsideTarget(e.getX(),
                    e.getY()))
                {
                    // Check if it is playable on texture
                    if (mVideoPlayerHelper.isPlayableOnTexture())
                    {
                        // We can play only if the movie was paused, ready
                        // or stopped
                        if ((mVideoPlayerHelper.getStatus() == MEDIA_STATE.PAUSED)
                            || (mVideoPlayerHelper.getStatus() == MEDIA_STATE.READY)
                            || (mVideoPlayerHelper.getStatus() == MEDIA_STATE.STOPPED)
                            || (mVideoPlayerHelper.getStatus() == MEDIA_STATE.REACHED_END))
                        {
                            // Pause all other media
//                            pauseAll(i);

                            // If it has reached the end then rewind
                            if ((mVideoPlayerHelper.getStatus() == MEDIA_STATE.REACHED_END))
                                mSeekPosition = 0;

                            mVideoPlayerHelper.play(mPlayFullscreenVideo,
                                mSeekPosition);
                            mSeekPosition = VideoPlayerHelper.CURRENT_POSITION;
                        } else if (mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING)
                        {
                            // If it is playing then we pause it
                            mVideoPlayerHelper.pause();
                        }
                    } else if (mVideoPlayerHelper.isPlayableFullscreen())
                    {
                        // If it isn't playable on texture
                        // Either because it wasn't requested or because it
                        // isn't supported then request playback fullscreen.
                        mVideoPlayerHelper.play(true,
                            VideoPlayerHelper.CURRENT_POSITION);
                    }

                    isSingleTapHandled = true;
                }
                
                return isSingleTapHandled;
            }
        });
    }
    
    
    // We want to load specific textures from the APK, which we will later
    // use for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk(
                "VideoPlayback/VuforiaSizzleReel_1.png", getAssets()));
        //mTextures.add(Texture.loadTextureFromApk(
                //"VideoPlayback/VuforiaSizzleReel_2.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/play.png",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/busy.png",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("VideoPlayback/error.png",
            getAssets()));
    }
    
    
    // Called when the activity will start interacting with the user.
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();
        
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try
        {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Resume the GL view
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
        
        // Reload all the movies
        if (mRenderer != null)
        {

            if (!mReturningFromFullScreen)
            {
                mRenderer.requestLoad(mMovieName, mSeekPosition,
                    false);
            } else
            {
                mRenderer.requestLoad(mMovieName, mSeekPosition,
                    mWasPlaying);
            }

        }
        
        mReturningFromFullScreen = false;
    }
    
    // Called when returning from the full screen player
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1)
        {
            
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            
            if (resultCode == RESULT_OK)
            {
                // The following values are used to indicate the position in
                // which the video was being played and whether it was being
                // played or not:
                String movieBeingPlayed = data.getStringExtra("movieName");
                mReturningFromFullScreen = true;
                
                // Find the movie that was being played full screen

                if (movieBeingPlayed.compareTo(mMovieName) == 0)
                {
                    mSeekPosition = data.getIntExtra(
                        "currentSeekPosition", 0);
                    mWasPlaying = data.getBooleanExtra("playing", false);
                }

            }
        }
    }
    
    
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
    }
    
    
    // Called when the system is about to start resuming a previous activity.
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        // Store the playback state of the movies and unload them:

        // If the activity is paused we need to store the position in which
        // this was currently playing:
        if (mVideoPlayerHelper.isPlayableOnTexture())
        {
            mSeekPosition = mVideoPlayerHelper.getCurrentPosition();
            mWasPlaying = mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING ? true
                : false;
        }

        // We also need to release the resources used by the helper, though
        // we don't need to destroy it:
        if (mVideoPlayerHelper != null)
            mVideoPlayerHelper.unload();

        
        mReturningFromFullScreen = false;
        
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }
    
    
    // The final call you receive before your activity is destroyed.
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
     // If the activity is destroyed we need to release all resources:
        if (mVideoPlayerHelper != null)
            mVideoPlayerHelper.deinit();
        mVideoPlayerHelper = null;

        
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    
    
    // Pause all movies except one
    // if the value of 'except' is -1 then
    // do a blanket pause
//    private void pauseAll(int except)
//    {
//        // And pause all the playing videos:
//        for (int i = 0; i < NUM_TARGETS; i++)
//        {
//            // We can make one exception to the pause all calls:
//            if (i != except)
//            {
//                // Check if the video is playable on texture
//                if (mVideoPlayerHelper[i].isPlayableOnTexture())
//                {
//                    // If it is playing then we pause it
//                    mVideoPlayerHelper[i].pause();
//                }
//            }
//        }
//    }
    
    
    // Do not exit immediately and instead show the startup screen
    public void onBackPressed()
    {
//        pauseAll(-1);
        super.onBackPressed();
    }
    
    
    private void startLoadingAnimation()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
            null, false);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);
        
        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        // Shows the loading indicator at start
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }
    
    
    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new VideoPlaybackRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        
        // The renderer comes has the OpenGL context, thus, loading to texture
        // must happen when the surface has been created. This means that we
        // can't load the movie from this thread (GUI) but instead we must
        // tell the GL thread to load it once the surface has been created.

        mRenderer.setVideoPlayerHelper(mVideoPlayerHelper);
        //mRenderer.requestLoad(mMovieName, 0, false);

        
        mGlView.setRenderer(mRenderer);


        float[] temp = { 0f, 0f, 0f };
        mRenderer.targetPositiveDimensions.setData(temp);
        mRenderer.videoPlaybackTextureID[0] = -1;

        
    }
    
    
    // We do not handle the touch event here, we just forward it to the
    // gesture detector
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean result = false;
        if ( mSampleAppMenu != null )
            result = mSampleAppMenu.processEvent(event);
        
        // Process the Gestures
        if (!result)
            mGestureDetector.onTouchEvent(event);
        
        return result;
    }

    public void startFinderIfStopped()
    {
        if(!mFinderStarted)
        {
            mFinderStarted = true;

            // Get the object tracker:
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager
                    .getTracker(ObjectTracker.getClassType());

            // Initialize target finder:
            TargetFinder targetFinder = objectTracker.getTargetFinder();

            targetFinder.clearTrackables();
            targetFinder.startRecognition();
        }
    }


    public void stopFinderIfStarted()
    {
        if(mFinderStarted)
        {
            mFinderStarted = false;

            // Get the object tracker:
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager
                    .getTracker(ObjectTracker.getClassType());

            // Initialize target finder:
            TargetFinder targetFinder = objectTracker.getTargetFinder();

            targetFinder.stop();
        }
    }


    @Override
    public boolean doInitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Indicate if the trackers were initialized correctly
        boolean result = true;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        return result;
    }


    @Override
    public boolean doLoadTrackersData()
    {
        Log.d(LOGTAG, "initCloudReco");

        // Get the object tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Initialize target finder:
        TargetFinder targetFinder = objectTracker.getTargetFinder();

        // Start initialization:
        if (targetFinder.startInit(kAccessKey, kSecretKey))
        {
            targetFinder.waitUntilInitFinished();
        }

        int resultCode = targetFinder.getInitState();
        if (resultCode != TargetFinder.INIT_SUCCESS)
        {
            if(resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION)
            {
                mInitErrorCode = UPDATE_ERROR_NO_NETWORK_CONNECTION;
            }
            else
            {
                mInitErrorCode = UPDATE_ERROR_SERVICE_NOT_AVAILABLE;
            }

            Log.e(LOGTAG, "Failed to initialize target finder.");
            return false;
        }

        // Use the following calls if you would like to customize the color of
        // the UI
        // targetFinder->setUIScanlineColor(1.0, 0.0, 0.0);
        // targetFinder->setUIPointColor(0.0, 0.0, 1.0);

        return true;
    }


    @Override
    public boolean doDeinitTrackers()
    {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        // Start the tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        objectTracker.start();

        // Start cloud based recognition if we are in scanning mode:
        TargetFinder targetFinder = objectTracker.getTargetFinder();
        targetFinder.startRecognition();
        mFinderStarted = true;

        return result;
    }


    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if(objectTracker != null)
        {
            objectTracker.stop();

            // Stop cloud based recognition:
            TargetFinder targetFinder = objectTracker.getTargetFinder();
            targetFinder.stop();
            mFinderStarted = false;

            // Clears the trackables
            targetFinder.clearTrackables();
        }
        else
        {
            result = false;
        }

        return result;
    }

    @Override
    public boolean doUnloadTrackersData()
    {
        return true;
    }

    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
            
            mRenderer.mIsActive = true;
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            
            // Hides the Loading Dialog
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            
            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            
            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }
            
            boolean result = CameraDevice.getInstance().setFocusMode(
                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            
            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");
            
            mSampleAppMenu = new SampleAppMenu(this, this, "CuriousMe",
                mGlView, mUILayout, null);
            setSampleAppMenuSettings();
            
            mIsInitialized = true;
            
        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
        
    }
    
    
    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    VideoPlayback.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    @Override
    public void onQCARUpdate(State state)
    {
        // Get the tracker manager:
        TrackerManager trackerManager = TrackerManager.getInstance();

        // Get the object tracker:
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Get the target finder:
        TargetFinder finder = objectTracker.getTargetFinder();

        // Check if there are new results available:
        final int statusCode = finder.updateSearchResults();

        // Show a message if we encountered an error:
        if (statusCode < 0)
        {

            boolean closeAppAfterError = (
                    statusCode == UPDATE_ERROR_NO_NETWORK_CONNECTION ||
                            statusCode == UPDATE_ERROR_SERVICE_NOT_AVAILABLE);
            Log.e(LOGTAG,"ci sono problemi!");
            //showErrorMessage(statusCode, state.getFrame().getTimeStamp(), closeAppAfterError);

        } else if (statusCode == TargetFinder.UPDATE_RESULTS_AVAILABLE)
        {
            // Process new search results
            if (finder.getResultCount() > 0)
            {
                TargetSearchResult result = finder.getResult(0);

                // Check if this target is suitable for tracking:
                if (result.getTrackingRating() > 0)
                {
                    Trackable trackable = finder.enableTracking(result);

                    if (mExtendedTracking)
                        trackable.startExtendedTracking();
                }
            }
        }
    }
    
    final private static int CMD_BACK = -1;
    final private static int CMD_FULLSCREEN_VIDEO = 1;
    
    
    // This method sets the menu's settings
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;
        
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);
        
        group = mSampleAppMenu.addGroup("", true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            group.addSelectionItem(getString(R.string.menu_playFullscreenVideo),
                CMD_FULLSCREEN_VIDEO, mPlayFullscreenVideo);
        }
        
        mSampleAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        
        boolean result = true;
        
        switch (command)
        {
            case CMD_BACK:
                Intent intent = new Intent(VideoPlayback.this,AboutScreen.class);
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.VideoPlayback.VideoPlayback");
                intent.putExtra("ABOUT_TEXT_TITLE", "CuriousMe");
                intent.putExtra("ABOUT_TEXT", "VideoPlayback/VP_about.html");
                startActivity(intent);
                //finish();
                break;
            
            case CMD_FULLSCREEN_VIDEO:
                mPlayFullscreenVideo = !mPlayFullscreenVideo;
                
                if (mVideoPlayerHelper.getStatus() == MEDIA_STATE.PLAYING)
                    {
                        // If it is playing then we pause it
                        mVideoPlayerHelper.pause();

                        mVideoPlayerHelper.play(true,
                            mSeekPosition);
                    }

                break;
            
        }
        
        return result;
    }

    public void setUrl(String url){
        if(mMovieName!=url) {
            mMovieName = url;
//            mRenderer.requestLoad(mMovieName, mSeekPosition,
//                    false);
        }
    }
}

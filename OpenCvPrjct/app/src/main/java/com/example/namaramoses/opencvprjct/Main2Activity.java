package com.example.namaramoses.opencvprjct;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import org.opencv.core.Mat;

import org.opencv.core.Size;

import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.cvtColor;



public class Main2Activity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_GRAY     = 1;
    private static final int       VIEW_MODE_CANNY    = 2;
    private static final int       VIEW_MODE_FEATURES = 5;
    private static final int       VIEW_MODE_FEATURESJAVA = 7;
    private static final int       VIEW_MODE_ORB = 9;



    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;
    private Mat                    mInitial;

    private Size                    screenSize;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;
    private MenuItem               mItemPreviewFeatures2;
    private MenuItem               mItemPreviewOrb;


    private int frameCount;

    PositionTracker positionTracker;
    Context context;




    //Doesn't seem neccessary
//    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.i(TAG, "OpenCV loaded successfully");
//
//                    // Load native library after(!) OpenCV initialization
//                    //System.loadLibrary("features");
//
//                    mOpenCvCameraView.enableView();
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "CREATING");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main2);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.enableView();

        System.loadLibrary("opencv_java3");

        //System.loadLibrary("features"); Wasn't neccessary
//      Log.d(TAG, "OpenCV library found inside package. Using it!");
        //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

    }

    public void onPause()
    {
        Log.i(TAG, "PAUSING");

        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        Log.i(TAG, "RESUMING");
        super.onResume();

//      System.loadLibrary("opencv_java3");
//      Log.d(TAG, "OpenCV library found inside package. Using it!");

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();
        }
        //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        Log.i(TAG, "DESTROYING");

        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        //mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("FFNative");
        mItemPreviewFeatures2 = menu.add("FFJava");
        mItemPreviewOrb = menu.add("ORB");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        frameCount=0; // 3
        //templateMatcher = new TemplateMatcher(15,5);


        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        }
        else if (item == mItemPreviewFeatures2){
            mViewMode = VIEW_MODE_FEATURESJAVA;
        }
        else if (item == mItemPreviewOrb){
            mViewMode = VIEW_MODE_ORB;
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "func: onCameraViewStarted");
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "func: onCameraViewStopped");
        //mRgba.release();
        //mGray.release();
        //mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final int viewMode = VIEW_MODE_ORB;
        switch (viewMode) {
            case VIEW_MODE_ORB:
                mGray = inputFrame.gray();
                if (frameCount>50){
                    mRgba = inputFrame.rgba();
                    positionTracker.update(mGray,mRgba);
                }
                else if (frameCount==50){
                    context = getApplicationContext();

                    //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                    //mOpenCvCameraView.enableView();

                    positionTracker = new PositionTracker(50,3,200,mGray,context);
                    //positionTracker = new PositionTracker(20,15,70,mGray,context);


                }

                frameCount++;
                break;
            case VIEW_MODE_GRAY:
                // input frame has gray scale format
                cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case VIEW_MODE_RGBA:
                // input frame has RBGA format
                mRgba = inputFrame.rgba();
                break;
            case VIEW_MODE_CANNY:
                // input frame has gray scale format
                // mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
                // Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                mRgba = mIntermediateMat;
                break;
            case VIEW_MODE_FEATURES:
                // input frame has RGBA format
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                //FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
                break;
            case VIEW_MODE_FEATURESJAVA:
                // input frame has RGBA format
//                mRgba = inputFrame.rgba();
//                mGray = inputFrame.gray();
//
//                javaFeatureDetector.detect(mGray, keypoints1);
//                List<KeyPoint> listOfPoints = keypoints1.toList();
//                for (KeyPoint kp : listOfPoints) {
//                    circle(mRgba, kp.pt, 10, new Scalar(255, 0, 0, 255), 1);
//                }

                break;

       }

        return mRgba;
    }

    //public native void FindFeatures(long matAddrGr, long matAddrRgba);

}

package com.example.namaramoses.opencvprjct;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.resize;


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
    private FeatureDetector javaFeatureDetector;
    private MatOfKeyPoint keypoints1;
    private MatOfKeyPoint keypoints2;
    private MatOfKeyPoint keyPointsUse;
    private Mat descriptors1;
    private Mat descriptors2;
    private FeatureDetector orbFeatureDetector;
    private DescriptorExtractor descriptor;
    private DescriptorMatcher matcher;
    Mat output;
    Mat output2;
    private MatOfDMatch matches;
    private MatOfDMatch matchesUse;
    TemplateMatcher templateMatcher;
    PositionTracker positionTracker;
    double startTime, endTime;


    boolean readyForDisplay;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("features");

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        System.loadLibrary("opencv_java3");
        Log.d(TAG, "OpenCV library found inside package. Using it!");
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
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

        readyForDisplay = false;

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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        javaFeatureDetector = FeatureDetector.create(1);

        keypoints1 = new MatOfKeyPoint();
        keypoints2 = new MatOfKeyPoint();
        descriptors1 = new Mat();
        descriptors2 = new Mat();
        orbFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
        //orbFeatureDetector = FeatureDetector.create(1);

        screenSize = new Size(width,height);

        descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
            case VIEW_MODE_ORB:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                if (frameCount==0){
                    positionTracker = new PositionTracker(16,5,64,mRgba);
                    frameCount++;
                }
                else{
                    positionTracker.update(mRgba);
                    frameCount++;
                }
//                if(firstFrame==0) {
//                    firstFrame=1;
//                    mInitial = inputFrame.gray().clone();
//                    orbFeatureDetector.detect(mInitial, keypoints1);
//                    descriptor.compute(mInitial, keypoints1, descriptors1);
//                    mRgba = mInitial;
//                    output = new Mat();
//                    output2 = new Mat();
//                    keypoints2 = new MatOfKeyPoint();
//                    keyPointsUse = new MatOfKeyPoint();
//                    matches = new MatOfDMatch();
//                    matchesUse = new MatOfDMatch();
//                    startTime = System.nanoTime();
//                }
//                else{
//                    MatOfByte mask = new MatOfByte();
//
//                    Scalar RED = new Scalar(255,0,0);
//                    Scalar GREEN = new Scalar(0,255,0);
                // if(frameCount==3) {
                // orbFeatureDetector.detect(mGray, keypoints2);
                // descriptor.compute(mGray, keypoints2, descriptors2);
                //matcher.match(descriptors1, descriptors2, matches);
                //   frameCount=0;
                //   }
//                        new Thread(new Runnable() {
//                            public void run() {
//                                Mat mGray2 = mGray.clone();
//                                orbFeatureDetector.detect(mGray2, keypoints2);
//                                descriptor.compute(mGray2, keypoints2, descriptors2);
//                                matcher.match(descriptors1, descriptors2, matches);
//
//                                synchronized (matchesUse) {
//                                    matchesUse = matches;
//                                    keyPointsUse = keypoints2;
//                                }
//
//                            }
//                        }).start();
//                        frameCount=0;
                // frameCount++;
                //Flan Matching
                //  synchronized (matchesUse) {

                //   Features2d.drawMatches(mInitial, keypoints1, mGray, keypoints2, matches, output,
                //           GREEN, RED, mask, Features2d.NOT_DRAW_SINGLE_POINTS);
                //  }
                // resize(output, output2, screenSize);

//                    endTime = System.nanoTime();
//                    double total = (endTime - startTime)/1000000000.0;
                //                  Log.i(TAG, "Time: " + total);
//                    startTime = endTime;

//                    Log.i(TAG, "mInitaialDims: " + mInitial.width() + "," + mInitial.height());
//                    Log.i(TAG, "mGrayDims: " + mGray.width() +","+ mGray.height());
//                    Log.i(TAG,"outputDims: " + output.width() +","+ output.height());
                //mRgba = output2;
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
                FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
                break;
            case VIEW_MODE_FEATURESJAVA:
                // input frame has RGBA format
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                javaFeatureDetector.detect(mGray, keypoints1);
                List<KeyPoint> listOfPoints = keypoints1.toList();
                for (KeyPoint kp : listOfPoints) {
                    circle(mRgba, kp.pt, 10, new Scalar(255, 0, 0, 255), 1);
                }

                break;

       }

        return mRgba;
    }

    public native void FindFeatures(long matAddrGr, long matAddrRgba);

}

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
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.HOUGH_GRADIENT;
import static org.opencv.imgproc.Imgproc.HoughCircles;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.resize;


public class Main2Activity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_GRAY     = 1;
    private static final int       VIEW_MODE_CANNY    = 2;
    private static final int       VIEW_MODE_FEATURES = 5;
    private static final int       VIEW_MODE_FEATURES2 = 7;
    private static final int       VIEW_MODE_HOG = 8;
    private static final int       VIEW_MODE_HOUGHLINES = 9;
    private static final int       VIEW_MODE_HOUGHCIRCLES = 12;
    private static final int       VIEW_MODE_CONTOUR = 10;
    private static final int       VIEW_MODE_OPTICALFIELD = 11;
    private static final int       VIEW_MODE_ORB = 13;
    private static final int       VIEW_MODE_FUNDAMENTALMATRIX = 15;
    private static final int       VIEW_MODE_ESSENTIALMATRIX = 16;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mSharpened;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;
    private Mat                    mInitial;
    private Mat                    mPrevGray;
    private Mat                    mUFlow;
    private Mat                    lines;
    private Mat                    circles;
    Mat output;
    Mat output2;
    private Size                    screenSize;
    private int firstFrame;
    private int frameCount;
    private FeatureDetector javaFeatureDetector;
    private MatOfKeyPoint keypoints1;
    private MatOfKeyPoint keypoints2;
    private Mat                    descriptors1;
    private Mat                    descriptors2;
    private DescriptorExtractor    descriptor;
    private DescriptorMatcher      matcher;
    private MatOfDMatch            matches;


    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;
    private MenuItem               mItemPreviewFeatures2;
    private MenuItem               mItemPreviewHog;
    private MenuItem               mItemPreviewHoughLines;
    private MenuItem               mItemPreviewHoughCircles;
    private MenuItem               mItemPreviewContour;
    private MenuItem               mItemPreviewOpticalField;
    private MenuItem               mItemPreviewOrb;
    private MenuItem               mItemPreviewFundamentalMatrix;
    private MenuItem               mItemPreviewEssentialMatrix;

    private FeatureDetector orbFeatureDetector;
    private MatOfKeyPoint keypoints;
    private HOGDescriptor mHogDescriptor;
    private MatOfRect mRect;
    private MatOfPoint mFound;
    private MatOfDouble mWeights;

    private Mat canny_output;
    private List<MatOfPoint> contours;
    private List<Mat> pyramids;
    private Mat hierarchy;
    private int houghVoteThreshold;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    // Log.i(TAG, "OpenCV loaded successfully");

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
        // Log.d(TAG, "OpenCV library found inside package. Using it!");
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
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features Native");
        mItemPreviewFeatures2 = menu.add("Find features Java");
        mItemPreviewHog = menu.add("Hog Descriptor");
        mItemPreviewHoughLines = menu.add("Hough Lines");
        mItemPreviewHoughCircles = menu.add("Hough Circles");
        mItemPreviewContour = menu.add("Contour");
        mItemPreviewOpticalField = menu.add("Optical Field");
        mItemPreviewEssentialMatrix = menu.add("Essential Matrix");
        mItemPreviewFundamentalMatrix = menu.add("Fundamental Matrix");
        mItemPreviewOrb = menu.add("ORB");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        firstFrame=0;
        frameCount=3;
        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        } else if (item == mItemPreviewFeatures2){
            mViewMode = VIEW_MODE_FEATURES2;
        } else if (item == mItemPreviewHog){
            mViewMode = VIEW_MODE_HOG;
        } else if (item == mItemPreviewHoughLines) {
            mViewMode = VIEW_MODE_HOUGHLINES;
        } else if (item == mItemPreviewContour) {
            mViewMode = VIEW_MODE_CONTOUR;
        } else if (item == mItemPreviewHoughCircles) {
            mViewMode = VIEW_MODE_HOUGHCIRCLES;
        } else if (item == mItemPreviewOpticalField) {
            mViewMode = VIEW_MODE_OPTICALFIELD;
        } else if (item == mItemPreviewOrb){
            mViewMode = VIEW_MODE_ORB;
        } else if (item == mItemPreviewEssentialMatrix){
            mViewMode = VIEW_MODE_ESSENTIALMATRIX;
        } else if (item == mItemPreviewFundamentalMatrix){
            mViewMode = VIEW_MODE_FUNDAMENTALMATRIX;
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        orbFeatureDetector = FeatureDetector.create(1);
        keypoints = new MatOfKeyPoint();
        mHogDescriptor = new HOGDescriptor();
        mRect = new MatOfRect();
        mWeights = new MatOfDouble();

        canny_output = new Mat();
        contours = new ArrayList<MatOfPoint>();
        hierarchy = new Mat();
        lines = new Mat();
        circles = new Mat();
        mSharpened = new Mat();
        pyramids = new ArrayList<Mat>();

        keypoints1 = new MatOfKeyPoint();
        keypoints2 = new MatOfKeyPoint();
        descriptors1 = new Mat();
        descriptors2 = new Mat();

        orbFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
        screenSize = new Size(width,height);
        descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        houghVoteThreshold = 140;
        //mHogDescriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
        mRect.release();
        mWeights.release();
        keypoints.release();
        canny_output.release();
        contours.clear();
        hierarchy.release();
        lines.release();
        circles.release();
        mSharpened.release();
        pyramids.clear();
        keypoints1.release();
        keypoints2.release();
        descriptors1.release();
        descriptors2.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
            case VIEW_MODE_GRAY:
                // input frame has gray scale format
                Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case VIEW_MODE_RGBA:
                // input frame has RBGA format
                mRgba = inputFrame.rgba();
                break;
            case VIEW_MODE_CANNY:
                // input frame has gray scale format
                //mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mRgba, 520, 650);
                //Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case VIEW_MODE_FEATURES:
                // input frame has RGBA format
                //mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                if (false) {
//                    Log.w(TAG, "mRgba size = "+mRgba.size().toString());
//                    Log.w(TAG, "mRgba submat size = "+mRgba.submat(0,2,0,2).size().toString());
//                    Log.w(TAG, "mRgba submat = "+mRgba.submat(0,2,0,2).dump().toString());
                }
                FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
                break;
            case VIEW_MODE_FEATURES2:
                // input frame has RGBA format
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                //Not actually orb yet.
                String fileName = "C:\\\\abcfolder\\\\textfile.txt";
                File file = new File(fileName);
                PrintWriter writer = new PrintWriter(file);
                writer.print("scaleFactor: 1.1\\nnLevels: 3\\nfirstLevel: 0\\nedgeThreshold: 31\\npatchSize: 31\\n");
                writer.close();
                orbFeatureDetector.read(fileName);
                List<KeyPoint> listOfPoints = keypoints.toList();
//commit
                if (false) {
                    Log.w(TAG, "Number of circles found: " + listOfPoints.size());
                }
                int ipt = 0;
                for (KeyPoint kp : listOfPoints) {
                    ipt++;
                    circle(mRgba, kp.pt, 10, new Scalar(kp.pt.x / 640 * 255, kp.pt.y / 800 * 255, 0, 255), 1);
                    // circle(mRgba, kp.pt, 10, new Scalar(255, 0, 0, 255), 1);
                    if (false && ipt < 3) {
                        Log.w(TAG, "kp = " + kp.toString());
                    }
                }
                break;
            case VIEW_MODE_ESSENTIALMATRIX:
                break;
            case VIEW_MODE_FUNDAMENTALMATRIX:
                break;
            case VIEW_MODE_HOG:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                mHogDescriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
                mHogDescriptor.detectMultiScale(mGray, mRect, mWeights, (double) 0.0,
                        new Size(8, 8), new Size(0, 0), (double) 1.05, (double) 2, false);
//                mHogDescriptor.detect(mGray, mFound, mWeights);
                if (false) Log.w(TAG, "mRect size: " + mRect.size().toString());
                for (Rect rec : mRect.toArray()) {
                    rectangle(mRgba, rec.tl(), rec.br(), new Scalar(255, 0, 0, 255));
                    Log.w(TAG, "rec top left: " + rec.tl() + ", rect bottom right: " + rec.br());
//                    circle(mRgba, rec.tl(), 10, new Scalar(255, 0, 0, 255), 1);
//                    System.out.println(rec);
                }
                break;
            case VIEW_MODE_HOUGHLINES:
                // Ref
                // http://homepages.inf.ed.ac.uk/rbf/HIPR2/hough.htm

                // input frame has gray scale format
                mRgba = inputFrame.rgba();
                mIntermediateMat = inputFrame.gray();
                Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(5, 5), 0, 0);
                Imgproc.Canny(mIntermediateMat, mIntermediateMat, 80, 100); //, 3, true);
                // houghVoteThreshold = 60;
                // Solve for threshold to create 8 lines
                int lowThreshold = 1, highTreshold = 1000;

                int goldenSearchThreshold = 60;
                // int goldenSearchThreshold = (int) goldenSectionSearch(5, 90, 200, .05);

                Imgproc.HoughLines(mIntermediateMat, lines, 1, Math.PI / 180, goldenSearchThreshold);
                final double nHoughLines = lines.size().height;
//                final int initialHoughVoteThreshold = 120, deltaVoteThreshold = 10;
//                if (houghVoteThreshold < 1) { // we lost all lines. reset
//                    houghVoteThreshold = initialHoughVoteThreshold;
//                    Log.w(TAG, "Reset houghVoteThreshold to " + houghVoteThreshold);
//                } else if (nHoughLines < 4) {
//                    houghVoteThreshold -= deltaVoteThreshold;
//                    Log.w(TAG, "Decreased houghVoteThreshold to " + houghVoteThreshold);
//                } else if (nHoughLines > 40) {
//                    houghVoteThreshold += deltaVoteThreshold;
//                    Log.w(TAG, "Increased houghVoteThreshold to " + houghVoteThreshold);
//                }
                if(nHoughLines != 0) {
                    final int nmRGBARows = mRgba.width();
                    boolean isFirst = true;
                    final int colorRange=255, colorMin=0;
                    final boolean listLines = false;
                    // if (listLines) {
                        Log.w(TAG, "number of lines is " + nHoughLines);
                    // }
                    for (int linesIndx = 0; linesIndx < nHoughLines; linesIndx++) {
                        final double[] data = lines.get(linesIndx,0);
                        final double rho = data[0], theta = data[1];
//                        final boolean isNotHVLine = theta > 0.09 && theta < 1.48
//                                || theta < 3.05 && theta > 1.66;
//                        if (isNotHVLine) {
                        final double tCos = Math.cos(theta), tSin = Math.sin(theta);
                        final boolean isGoodLine = Math.abs(tCos) > 1e-4;
                        if (isGoodLine) {
                            // filter to remove   vertical and horizontal lines
                            // point of intersection of the line with first row
                            Point pt1 = new Point(rho / tCos, 0);
                            // point of intersection of the line with last row
                            Point pt2 = new Point((rho - nmRGBARows * tSin) / tCos,
                                    nmRGBARows);
                            // draw a line
                            final double colorGuide = Math.abs(tCos) * Math.abs(tCos);
                            final int red=(int)(colorGuide * colorRange) + colorMin;
                            final int green=(int)((1-colorGuide) * colorRange) + colorMin;
                            final int blue=0;
                            Scalar color = new Scalar( red, green, blue );
                            line(mRgba, pt1, pt2, color, 4);

                            if (listLines) {
                                if (isFirst) {
                                    Log.w(TAG, "Index    Radius   THETA   P1      P2");
                                    isFirst = false;
                                }
                                Log.w(TAG, linesIndx+1 + "/" + nHoughLines + ":  " +
                                        rho + "  " + theta + "  " + pt1.toString() + pt2.toString());
                            }
                        }
                    }
                }


                break;
            case VIEW_MODE_HOUGHCIRCLES:
                mRgba = inputFrame.gray();
                mIntermediateMat = inputFrame.gray();
                Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(5, 5), 0, 0);
                Imgproc.Canny(mIntermediateMat, mIntermediateMat, 50, 100, 3, true); //, 3, true);
                HoughCircles(mIntermediateMat, circles, HOUGH_GRADIENT, 1, 10, 120, 10, 70, 80);
                for(int i = 0; i < circles.cols(); i++){
                    double[] circlePts = circles.get(0, i);
                    Point center = new Point(Math.round(circlePts[0]), Math.round(circlePts[1]));
                    circle(mRgba, center, (int) Math.round(circlePts[2]), new Scalar(255, 0, 0, 255), 3);
                    circle(mRgba, center, 3, new Scalar(0, 255, 0), -1, 8, 0);
                    Log.w(TAG, "Number circles:" + circles.cols());
                }
                break;
            case VIEW_MODE_CONTOUR:
                /// Detect edges using canny
                mRgba = inputFrame.rgba();
                int thresh = 50;
                Imgproc.Canny(inputFrame.gray(), canny_output, thresh, thresh * 2, 3, true);
                /// Find contours
                Imgproc.findContours(canny_output, contours, hierarchy, Imgproc.RETR_TREE,
                        Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

                /// Draw contours
                // mRgba = Mat.zeros(canny_output.size(), CvType.CV_8UC3);
                mIntermediateMat = inputFrame.rgba();
                final int thickness=2, lineType = 8;
                final int maxLevel=0;
                final int colorRange=255, colorMin=0;
                Log.w(TAG, "There are "+contours.size()+" contours");
                for( int contourIdx = 0; contourIdx< contours.size(); contourIdx++ )
                {
                    final int red=(int)(Math.random() * colorRange) + colorMin;;
                    final int green=(int)(Math.random() * colorRange) + colorMin;;
                    final int blue=(int)(Math.random() * colorRange) + colorMin;;
                    Scalar color = new Scalar( red, green, blue );
                    // drawContours(drawing, contours, contourIdx, color, 2, 8, hierarchy, 0, Point());
                    if (contourIdx % 100 == 1) {
                        Log.w(TAG, "Drawing contour "+contourIdx+" of "+contours.size());
                    }
                    Imgproc.drawContours(mIntermediateMat, contours, contourIdx, color, thickness,
                            lineType, hierarchy, maxLevel, new Point(0, 0));

//                    for(int i=0; i<contours.size(); i++){
//                        Imgproc.drawContours(drawing, hull, i, new Scalar(255.0, 255.0, 255.0), 5);
//                    }
                }


                /// Show in a window
//                namedWindow( "Contours", CV_WINDOW_AUTOSIZE );
//                imshow( "Contours", drawing );
                break;
            case VIEW_MODE_ORB:
                long startTime = System.currentTimeMillis();
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                if(firstFrame==0) {
                    firstFrame=1;
                    mInitial = inputFrame.gray().clone();
                    orbFeatureDetector.detect(mInitial, keypoints1);
                    descriptor.compute(mInitial, keypoints1, descriptors1);
                    mRgba = mInitial;
                    output = new Mat();
                    output2 = new Mat();
                    keypoints2 = new MatOfKeyPoint();
                    matches = new MatOfDMatch();
                }
                else {
                    MatOfByte mask = new MatOfByte();

                    Scalar RED = new Scalar(255, 0, 0);
                    Scalar GREEN = new Scalar(0, 255, 0);

                    if (frameCount == 3) {
                        orbFeatureDetector.detect(mGray, keypoints2);
                        descriptor.compute(mGray, keypoints2, descriptors2);
                        matcher.match(descriptors1, descriptors2, matches);
                        frameCount = 0;
                    }
                    frameCount++;
                    //Flan Matching

                    Features2d.drawMatches(mInitial, keypoints1, mGray, keypoints2, matches, output,
                            GREEN, RED, mask, Features2d.NOT_DRAW_SINGLE_POINTS);
                    resize(output, output2, screenSize);

                    Log.i(TAG, "mInitaialDims: " + mInitial.width() + "," + mInitial.height());
                    Log.i(TAG, "mGrayDims: " + mGray.width() + "," + mGray.height());
                    Log.i(TAG, "outputDims: " + output.width() + "," + output.height());
                    mRgba = output2;
                }
                long stopTime = System.currentTimeMillis();
                Log.w(TAG, "Elapsed time was " + (stopTime - startTime) + " miliseconds.");
        }
        return mRgba;
    }

    private double f(double x) {
        final int nVotes = (int) Math.floor(x);
        Imgproc.HoughLines(mIntermediateMat, lines, 1, Math.PI / 180, nVotes);
        final double nHoughLines = lines.size().height;
        Log.w(TAG, "threshold "+nVotes+" produces " + nHoughLines+" lines");
        return nHoughLines-20;
    }

    /*static void drawOptFlowMap(final Mat flow, Mat cflowmap, int step,
                               double threshold, final Scalar color)
    {
        for(int y = 0; y < cflowmap.rows(); y += step)
            for(int x = 0; x < cflowmap.cols(); x += step)
            {
                final Mat fxy = flow.submat(y, y, x, x);
                line(cflowmap, Point(x,y), Point(Math.round(x+fxy.), Math.round(y+fxy.y)),
                        color);
                circle(cflowmap, Point(x,y), 2, color, -1);
            }
    }*/

    final double phi = (1 + Math.sqrt(5)) / 2;
    final double resphi = 2 - phi;
    private double goldenSectionSearch(double a, double b, double c, double tau) {
        Log.w(TAG, "  golden section a: "+a+", b " + b+",  c "+c+", tau ");
// a and c are the current bounds; the minimum is between them.
// b is a center point
// f(x) is some mathematical function elsewhere defined
// a corresponds to x1; b corresponds to x2; c corresponds to x3
// x corresponds to x4
// tau is a tolerance parameter; see above
        double x;
        if (c - b > b - a)
            x = b + resphi * (c - b);
        else
            x = b - resphi * (b - a);
        final double currntTau = Math.abs(c - a) / (Math.abs(b) + Math.abs(x));
        // final boolean stopCondition = Math.abs(c - a) < tau * (Math.abs(b) + Math.abs(x));
        final boolean stopCondition = currntTau < tau;
        Log.w(TAG, "  golden section x: "+x+", stop condition " + stopCondition);
        if (stopCondition)
            return (c + a) / 2;
        final double fx = f(x), fc=f(c), fb=f(b), fa=f(a);
        Log.w(TAG, "  golden section fa: "+fa+", fb " + fb+",  fc "+fc+", fx "+fx);
        assert(fx != fc);
        if (fx < fb) {
            if (c - b > b - a) return goldenSectionSearch(b, x, c, tau);
            else return goldenSectionSearch(a, x, b, tau);
        }
        else {
            if (c - b > b - a) return goldenSectionSearch(a, b, x, tau);
            else return goldenSectionSearch(x, b, c, tau);
        }
    }

    public native void FindFeatures(long matAddrGr, long matAddrRgba);

}

package com.example.namaramoses.opencvprjct;

import android.util.Log;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.Random;

import static org.opencv.imgproc.Imgproc.rectangle;

/**
 * Created by ahmadsalem on 8/6/15.
 */
public class TemplateMatcher {
    private static final String    TAG = "OCVSample::Activity";
    private int maxWidth;
    private int maxPointsPerFrame;
    private Mat prevFrame;
    private Mat currFrame;
    private MatOfKeyPoint currKeyPts;
    private ArrayList<KeyPoint> CurrKeyPtsArr;
    private MatOfKeyPoint trimmedCurrKeyPts;
    private ArrayList<KeyPoint> trimmedCurrKeyPtsList;
    private KeyPoint[] trimmedKeyPtsArray;

    private Mat currDescriptors;
    private Mat prevDescriptors;

    private FeatureDetector fastFeatureDetector; // Fast
    private DescriptorExtractor descriptorExtractor;
    private DescriptorMatcher matcher;


    public TemplateMatcher(int maxWidth, int maxPointsPerFrame){ // 17
        this.maxWidth = maxWidth;
        this.maxPointsPerFrame = maxPointsPerFrame;
        this.currKeyPts = new MatOfKeyPoint();
        trimmedCurrKeyPts = new MatOfKeyPoint();
        this.trimmedCurrKeyPtsList = new ArrayList<KeyPoint>();
        trimmedKeyPtsArray = new KeyPoint[maxPointsPerFrame];
        this.CurrKeyPtsArr = new ArrayList<KeyPoint>();
        this.currDescriptors = new Mat();
        this.prevDescriptors = new Mat();
        this.currFrame = null;
        this.prevFrame = null;
        fastFeatureDetector = FeatureDetector.create(1);
        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

    }

    public void initialize(Mat frame){
        this.currFrame = frame;
        fastFeatureDetector.detect(currFrame,currKeyPts);
        Random random = new Random();
        CurrKeyPtsArr =  new ArrayList<KeyPoint>(currKeyPts.toList());
        for(int i = 0;i<maxPointsPerFrame;i++){
            KeyPoint randPoint = CurrKeyPtsArr.get(random.nextInt(CurrKeyPtsArr.size())); // Get a random pt
            trimmedCurrKeyPtsList.add(randPoint); // Add to trimmed array;
            trimmedKeyPtsArray[i] = randPoint;

        }

        trimmedCurrKeyPts.fromList(trimmedCurrKeyPtsList); // Convert random key points to a matrix(ofKeypoints)
        descriptorExtractor.compute(currFrame, trimmedCurrKeyPts, currDescriptors); // Generate the descriptors from the matrix
    }

    public void setCurrentFrameAndUpdate(Mat frame){
        this.prevFrame = currFrame;
        this.currFrame = frame;
        updateFoundPoints();
    }

    private void updateFoundPoints(){
        int foundThisUpdate = 0; // ( Number of Points / maxPointsPerFrame )
        Log.i(TAG, "Frame");
        if(prevFrame!=null){
            fastFeatureDetector.detect(currFrame,currKeyPts);
            CurrKeyPtsArr =  new ArrayList<KeyPoint>(currKeyPts.toList());

            for(KeyPoint keypt : trimmedCurrKeyPtsList){ // This should probably be descriptors instead
                //Crop frame matrix around keypoint.pt+maxWidth, find features and descriptors in this area
                //Match with this keypt. If found, then foundThisUpdate++, if not, then find a new random point


                //region cropMatrix
                // Used for cropping the matrix around the specific point with a pixel distance of maxWidth
                Point interestPt = keypt.pt;
                int minX=0,maxX=0,minY=0,maxY=0;
                Log.i(TAG,"Interest Point.x" + interestPt.x );
                Log.i(TAG,"Interest Point.y" + interestPt.y );
                Log.i(TAG,"width" + currFrame.width() );
                Log.i(TAG,"cols" + currFrame.cols() );

                if(interestPt.x-maxWidth>=0){
                    minX = (int)interestPt.x - maxWidth;
                }
                else{
                    minX=0;
                }

                if(interestPt.x+maxWidth<=currFrame.cols()){

                    maxX = (int)interestPt.x + maxWidth;
                }
                else{
                    maxX=currFrame.cols()-1;
                }

                if(interestPt.y-maxWidth>=0){
                    minY = (int)interestPt.y - maxWidth;
                }
                else{
                    minY=0;
                }

                if(interestPt.y+maxWidth<=currFrame.rows()){
                    maxY = (int)interestPt.y + maxWidth;
                }
                else{
                    maxY=currFrame.rows()-1;
                }
                //Debug
                rectangle(currFrame,new Point(minX,minY),new Point(maxX,maxY),new Scalar(255, 51, 204),3);

               // Mat interestMat = currFrame.submat(new Rect(new Point(minX,minY),new Point(maxX,maxY)));

                //endregion

                // Check if keypt is in interestMat

                //Find all keypoints in region
                MatOfKeyPoint interestPts = new MatOfKeyPoint();
                Mat trainDes = new Mat();

//                Mat interestDescriptors = new Mat();
//                MatOfDMatch matches = new MatOfDMatch();
//                fastFeatureDetector.detect(interestMat, interestPts);
//                descriptorExtractor.compute(interestMat, interestPts, interestDescriptors);
//                matcher.match(interestDescriptors, currDescriptors, matches); // WRONG: Checks for matches between in interest region and ALL of currDescriptorsmInstead of just checking for match between key
//
//                Log.i(TAG, matches.toList().size() + "");
//                interestMat.copyTo(currFrame);



            }

        } else { // First Frame
            fastFeatureDetector.detect(currFrame,currKeyPts);
            Random random = new Random();
            CurrKeyPtsArr =  new ArrayList<KeyPoint>(currKeyPts.toList());
            for(int i = 0;i<maxPointsPerFrame;i++){
                KeyPoint randPoint = CurrKeyPtsArr.get(random.nextInt(CurrKeyPtsArr.size())); // Get a random pt
                if(randPoint!=null) {
                    trimmedCurrKeyPtsList.add(randPoint); // Add to trimmed array
                }
            }
            trimmedCurrKeyPts.fromList(trimmedCurrKeyPtsList); // Convert random key points to a matrix(ofKeypoints)
            descriptorExtractor.compute(currFrame, trimmedCurrKeyPts, currDescriptors); // Generate the descriptors from the matrix

        }




    }





}

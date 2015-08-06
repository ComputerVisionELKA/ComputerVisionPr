package com.example.namaramoses.opencvprjct;

import android.util.Log;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;

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
    private ArrayList<KeyPoint> trimmedCurrKeyPtsArr;

    private Mat currDescriptors;
    private Mat prevDescriptors;

    private FeatureDetector fastFeatureDetector; // Fast
    private DescriptorExtractor descriptorExtractor;

    public TemplateMatcher(int maxWidth, int maxPointsPerFrame){ // 17
        this.maxWidth = maxWidth;
        this.maxPointsPerFrame = maxPointsPerFrame;
        this.currKeyPts = new MatOfKeyPoint();
        trimmedCurrKeyPts = new MatOfKeyPoint();
        this.trimmedCurrKeyPtsArr = new ArrayList<KeyPoint>();
        this.CurrKeyPtsArr = new ArrayList<KeyPoint>();
        this.currDescriptors = new Mat();
        this.prevDescriptors = new Mat();
        this.currFrame = null;
        this.prevFrame = null;
        fastFeatureDetector = FeatureDetector.create(1);
        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

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

        }else{ // First Frame
            fastFeatureDetector.detect(currFrame,currKeyPts);
            CurrKeyPtsArr =  new ArrayList<KeyPoint>(currKeyPts.toList());
            for(int i = 0;i<maxPointsPerFrame;i++){
                KeyPoint randPoint = CurrKeyPtsArr.get(i * 3); // Get a semi random pt
                if(randPoint!=null) {
                    trimmedCurrKeyPtsArr.add(randPoint); // Add to trimmed array
                }
            }
            trimmedCurrKeyPts.fromList(trimmedCurrKeyPtsArr);
            descriptorExtractor.compute(currFrame,trimmedCurrKeyPts,currDescriptors);


        }




    }





}

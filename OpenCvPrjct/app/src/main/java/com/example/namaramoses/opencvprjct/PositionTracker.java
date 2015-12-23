package com.example.namaramoses.opencvprjct;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by ahmadsalem on 10/26/15.
 */

public class PositionTracker {
    /// Options
    // Size of area to save
    private int corner_size;
    // Number of points to track
    private int num_track;
    // Size of search region
    private int sweep_size;

    /// Data structures
    // Stores the foi's (feature of interest) location in 2d space
    private Point[] pts_xy;
    // Stores the foi's actual matrix around it's point in 2d space with dims corner_size
    private Mat[] pts_img;
    // Stores the most recent frames's Matrix
    private Mat currentFrame;


    /// Auxilliary
    private FeatureDetector orbFeatureDetector; // Orb 5
    private Random random;


    public PositionTracker(int corner_size, int num_track, int sweep_size, Mat firstImg){
        // Set options
        this.corner_size = corner_size;
        this.num_track = num_track;
        this.sweep_size = sweep_size;

        // init arrays
        pts_xy = new Point[num_track];
        pts_img = new Mat[num_track];

        //Aux
        random = new Random();

        init(firstImg);

    }


    // Initializes for the first frame
    private void init(Mat firstImg){
        MatOfKeyPoint detected = null;
        KeyPoint[] detectedKeyPtsArr;

        orbFeatureDetector = FeatureDetector.create(5);
        orbFeatureDetector.detect(firstImg,detected);
        detectedKeyPtsArr = detected.toArray();


        for(int i =0;i<num_track;i++ ){
            KeyPoint foiToStore = getRandomPoint(detectedKeyPtsArr);
            pts_xy[i] = foiToStore.pt;
            pts_img[i] = getImgAroundPt(firstImg,foiToStore.pt,corner_size);
        }
    }

    //TODO:IMPLEMENT
    // Returns a (size dims) Mat around cordinates pt of Mat img
    private Mat getImgAroundPt(Mat img, Point pt, int dims ){

        return null;
    }

    private KeyPoint getRandomPoint(KeyPoint[] detected){
        int randIdx = random.nextInt(detected.length - 0 + 1) + 0;
        return detected[randIdx];
    }

    // Takes the new frame and does all logic
   public void update(Mat frame){
       this.currentFrame = frame;

       // Main loop
       for(int i = 0; i<num_track;i++){
           // Extract data from previous frame
           Point foi_xy = pts_xy[i];
           Mat foi_img = pts_img[i];

       }

   }



}

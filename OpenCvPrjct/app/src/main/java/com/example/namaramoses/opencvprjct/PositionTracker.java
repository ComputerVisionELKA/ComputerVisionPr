package com.example.namaramoses.opencvprjct;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.Random;

import static org.opencv.imgproc.Imgproc.rectangle;

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
    public Mat firstImg;


    /// Auxilliary
    private FeatureDetector orbFeatureDetector; // Orb 5
    private Random random;


    public PositionTracker(int corner_size, int num_track, int sweep_size, Mat firstImg){
        // Set options
        this.corner_size = corner_size;
        this.num_track = num_track;
        this.sweep_size = sweep_size;
        this.firstImg = firstImg;

        // init arrays
        pts_xy = new Point[num_track];
        pts_img = new Mat[num_track];

        //Aux
        random = new Random();

        init(firstImg);

    }


    // Initializes for the first frame
    private void init(Mat firstImg){
        MatOfKeyPoint detected = new MatOfKeyPoint();
        KeyPoint[] detectedKeyPtsArr;
        Point[] drawmin = new Point[num_track];
        Point[] drawmax = new Point[num_track];

        orbFeatureDetector = FeatureDetector.create(5);
        orbFeatureDetector.detect(firstImg,detected);
        detectedKeyPtsArr = detected.toArray();


        for(int i =0;i<num_track;i++ ){
            KeyPoint foiToStore = getRandomPoint(detectedKeyPtsArr);
            while(!inBounds(foiToStore.pt, firstImg)){
                foiToStore = getRandomPoint(detectedKeyPtsArr);
            }

            pts_xy[i] = foiToStore.pt;
            // Gets the mat of the point (Squares only and no bounds check)
            pts_img[i] = firstImg.submat(new Rect(new Point(foiToStore.pt.x - corner_size / 2, foiToStore.pt.y - corner_size / 2),
                    new Point(foiToStore.pt.x + corner_size / 2, foiToStore.pt.y + corner_size / 2)));

            // DBG draws intial point's rectangles
           drawmin[i] = new Point(foiToStore.pt.x - corner_size / 2, foiToStore.pt.y - corner_size / 2);
            drawmax[i] = new Point(foiToStore.pt.x + corner_size / 2, foiToStore.pt.y + corner_size / 2);

        }
        // DBG draws intial point's rectangles
        for(int i = 0;i<num_track;i++) {
            rectangle(firstImg, drawmin[i], drawmax[i], new Scalar(255, 51, 204), 3);
        }
    }

    // Determines if the image of the point is completely inside of the parent (full) matrix
    private boolean inBounds(Point point, Mat img){
        if (point.x - (corner_size/2)<0 || point.x + (corner_size/2) > img.cols() || point.y - (corner_size/2) < 0 ||point.x+(corner_size/2)>img.rows() ) {
            return false;
        }
        else {
            return true;
        }
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

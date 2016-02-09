package com.example.namaramoses.opencvprjct;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

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
    private static final String TAG = "OCVSample::Activity";
   // private int match_method = Imgproc.TM_CCOEFF_NORMED;
   private int match_method;


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
            pts_img[i] = firstImg.submat(new Rect(new Point(foiToStore.pt.x - (corner_size / 2), foiToStore.pt.y - (corner_size / 2)),
                    new Point(foiToStore.pt.x + (corner_size / 2), foiToStore.pt.y + (corner_size / 2))));

            // DBG draws intial point's rectangles
           drawmin[i] = new Point(foiToStore.pt.x - (corner_size / 2), foiToStore.pt.y - (corner_size / 2));
            drawmax[i] = new Point(foiToStore.pt.x + (corner_size / 2), foiToStore.pt.y + (corner_size / 2));

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
        int randIdx = random.nextInt(detected.length - 1 + 1) + 0;
//        Log.i(TAG, "randIdx " + randIdx);
//        Log.i(TAG, "detected arr len " + detected.length);
        return detected[randIdx];
    }

    // Takes the new frame and does all logic
   public void update(Mat frame,Mat coloredFrame){
       this.currentFrame = frame;
       Point[] drawmin = new Point[num_track];
       Point[] drawmax = new Point[num_track];
       Point[] matchLocs = new Point[num_track];
       Double[] matchScores = new Double[num_track];
       Boolean[] keepPoint = new Boolean[num_track]; // Synonomous with checking if point >.75
       Double[] distances = new Double[num_track];


       // Main loop
       for(int i = 0; i<num_track;i++){
           // Extract data from previous frame
           Point foi_xy = pts_xy[i];
           Mat foi_img = pts_img[i]; //(templ)


           drawmin[i] = getSearchRgnRectMin(foi_xy, frame);
           drawmax[i] = getSearchRgnRectMax(foi_xy, frame);

           Rect searchRgnRect = new Rect(drawmin[i],drawmax[i]);
           Mat searchRgn = frame.submat(searchRgnRect); // This is the search region matrix (img)
           // Now we need to search for foi_img in searchRgn using template matcher
           int result_cols = searchRgn.cols() - foi_img.cols() + 1;
           int result_rows = searchRgn.rows() - foi_img.rows() + 1;
           Mat result = new Mat(result_rows,result_cols, CvType.CV_32FC1);

           match_method = Imgproc.TM_CCOEFF_NORMED;
           //match_method = Imgproc.TM_SQDIFF_NORMED;
           Imgproc.matchTemplate(searchRgn, foi_img, result, match_method);
          // Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
           Core.MinMaxLocResult mmr = Core.minMaxLoc(result);



           if (match_method == Imgproc.TM_SQDIFF
                   || match_method == Imgproc.TM_SQDIFF_NORMED) {
               matchLocs[i] = mmr.minLoc;
               matchScores[i] = mmr.minVal;
               Log.i(TAG, "minVal " + mmr.minVal);
           } else {

               matchLocs[i] = mmr.maxLoc;
               matchScores[i] = mmr.maxVal;
               Log.i(TAG, "maxVal [" +i+"] "+ mmr.maxVal);
               //Log.i(TAG, "minVal [" +i+"] "+ mmr.minVal);
           }

       }
       for (int i = 0;i<num_track;i++){
           Point matchedPoint = new Point(drawmin[i].x+(matchLocs[i].x+(corner_size/2.0)),drawmin[i].y+(matchLocs[i].y+(corner_size/2.0)));
           distances[i] = Math.sqrt((pts_xy[i].x-matchedPoint.x)*(pts_xy[i].x-matchedPoint.x) + (pts_xy[i].y-matchedPoint.y)*(pts_xy[i].y-matchedPoint.y));
           if (matchScores[i] > .97 && distances[i] < 15){
               Log.i(TAG, "distance "+ distances[i]);

               pts_xy[i] = matchedPoint;

               pts_img[i] = frame.submat(new Rect(new Point(drawmin[i].x+matchLocs[i].x, drawmin[i].y+matchLocs[i].y),
                       new Point(drawmin[i].x+matchLocs[i].x+corner_size, drawmin[i].y+matchLocs[i].y+corner_size)));

           }
           else{
               Log.i(TAG, "distancefail "+ distances[i]);
           }
       }

        //DBG
       for(int i = 0;i<num_track;i++) {
           rectangle(coloredFrame, drawmin[i], drawmax[i], new Scalar(237, 167, 55), 3);
           Scalar colorFound;
           if(matchScores[i] > .97 && distances[i] < 15){
               colorFound = new Scalar(0, 255, 0); //Green


           }
           else{
               colorFound = new Scalar(255,0,0); //Red
           }
           rectangle(coloredFrame, new Point(drawmin[i].x+matchLocs[i].x,drawmin[i].y+matchLocs[i].y),
                   new Point(drawmin[i].x+matchLocs[i].x+corner_size,drawmin[i].y+matchLocs[i].y+corner_size), colorFound, 3);
       }

   }
    // Gets the minPoint (dimensions) of which to submatrix. This is a scaled rectangle of max search-region dims
    private Point getSearchRgnRectMin(Point pt, Mat img){
        double minx=0,miny=0;

        if(pt.x - sweep_size >=0){
            minx = (int)pt.x-sweep_size;
        }
        else{
            minx = 0;
        }

        if(pt.y-sweep_size>=0){
            miny = (int)pt.y-sweep_size;
        }
        else{
            miny=0;
        }


        return new Point(minx,miny);
    }
    // Gets the maxPoint (dimensions) of which to submatrix. This is a scaled rectangle of max search-region dims
    private Point getSearchRgnRectMax(Point pt, Mat img){
        double minx=0,maxx=0,miny=0,maxy=0;

        if(pt.x + sweep_size <= img.cols()){
            maxx = (int)pt.x+sweep_size;
        }
        else{
            maxx = pt.x+img.cols()-1;
        }

        if(pt.y+sweep_size<=img.rows()){
            maxy=(int)pt.y+sweep_size;
        }
        else{
            maxy=img.rows()-1;
        }

        return new Point(maxx,maxy);
    }



}

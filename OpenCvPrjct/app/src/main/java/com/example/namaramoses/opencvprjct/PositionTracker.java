package com.example.namaramoses.opencvprjct;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import static org.opencv.imgproc.Imgproc.arrowedLine;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.putText;
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
    public Mat firstImg;
    // Store the location of the previous best match in the sweep region (null if first)
    Point[] prevLocalGuess;


    Context c;
    int frameNum;


    /// Auxilliary
    private FeatureDetector orbFeatureDetector; // Orb 5
    private Random random;
    private static final String TAG = "OCVSample::Activity";
   // private int match_method = Imgproc.TM_CCOEFF_NORMED;
   private int match_method;


    public PositionTracker(int corner_size, int num_track, int sweep_size, Mat firstImg, Context c){
        // Set options
        this.c = c;
        this.corner_size = corner_size;
        this.num_track = num_track;
        this.sweep_size = sweep_size;
        this.firstImg = firstImg;

        // init arrays
        pts_xy = new Point[num_track];
        pts_img = new Mat[num_track];
        prevLocalGuess = new Point[num_track];

        //Aux
        random = new Random();
        this.frameNum = 0;

        init(firstImg);

    }


    // Initializes for the first frame
    private void init(Mat firstImg){


        MatOfKeyPoint detected = new MatOfKeyPoint();
        KeyPoint[] detectedKeyPtsArr;
        //Point[] drawmin = new Point[num_track];
        //Point[] drawmax = new Point[num_track];

        // Scan initial Features
        orbFeatureDetector = FeatureDetector.create(5);
        orbFeatureDetector.detect(firstImg,detected);
        detectedKeyPtsArr = detected.toArray();

        //Select random Features to be used as points
        for(int i =0;i<num_track;i++ ){
            KeyPoint foiToStore = getRandomPoint(detectedKeyPtsArr);
            while(!inBounds(foiToStore.pt, firstImg)){
                foiToStore = getRandomPoint(detectedKeyPtsArr);
            }

            pts_xy[i] = foiToStore.pt;
            // Gets the mat of the point (Squares only and no bounds check)
            pts_img[i] = firstImg.submat(new Rect(new Point(foiToStore.pt.x - (corner_size / 2.0), foiToStore.pt.y - (corner_size / 2.0)),
                    new Point(foiToStore.pt.x + (corner_size / 2.0), foiToStore.pt.y + (corner_size / 2.0))));

            // DBG draws intial point's rectangles
            //drawmin[i] = new Point(foiToStore.pt.x - (corner_size / 2.0), foiToStore.pt.y - (corner_size / 2.0));
            //drawmax[i] = new Point(foiToStore.pt.x + (corner_size / 2.0), foiToStore.pt.y + (corner_size / 2.0));

        }
  //      saveImageToDisk(pts_img[0], "pts_img_0", "OpenCV_Inits", c, -1);
//        saveImageToDisk(pts_img[1], "pts_img_1", "OpenCV_Inits", c, -1);
        // DBG draws intial point's rectangles
//        for(int i = 0;i<num_track;i++) {
//            rectangle(firstImg, drawmin[i], drawmax[i], new Scalar(144, 0, 255), 3);
//            circle(firstImg,pts_xy[i], corner_size/2, new Scalar(144, 0, 255), 1);
//        }
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
   public void update(Mat frame, Mat coloredFrame){
       //this.currentFrame = frame;
       Point[] drawmin = new Point[num_track];
       Point[] drawmax = new Point[num_track];
       Point[] matchLocs = new Point[num_track];
       Double[] matchScores = new Double[num_track];
       //Boolean[] keepPoint = new Boolean[num_track]; // Synonomous with checking if point >.75
       Double[] distances = new Double[num_track];
       Mat testSearchRgn = null;

       //Used for calculating change in position (temporary method)
       double delta_x = 0;



       // Main loop
       for(int i = 0; i<num_track;i++){
           // Extract data from previous frame
           Point foi_xy = pts_xy[i];
           Mat foi_img = pts_img[i]; //(templ)


           drawmin[i] = getSearchRgnRectMin(foi_xy, frame);
           drawmax[i] = getSearchRgnRectMax(foi_xy, frame);

           Rect searchRgnRect = new Rect(drawmin[i],drawmax[i]);
           Mat searchRgn = frame.submat(searchRgnRect); // This is the search region matrix (img)
           if (frameNum==0 && i==0){
               saveImageToDisk(searchRgn, "searchRgn_0", "OpenCV_Result", c, -1);
           }



           // Now we need to search for foi_img in searchRgn using template matcher
           int result_cols = searchRgn.cols() - foi_img.cols() + 1;
           int result_rows = searchRgn.rows() - foi_img.rows() + 1;
           Mat result = new Mat(result_rows,result_cols, CvType.CV_32FC1);
           Mat resultout= new Mat(result_rows,result_cols,CvType.CV_32FC1);


          // testSearchRgn = result.clone();

           match_method = Imgproc.TM_CCOEFF_NORMED; // This one sometimes works with red squares perfectly tracking
           //match_method = Imgproc.TM_CCOEFF;
           //match_method = Imgproc.TM_SQDIFF_NORMED;
           Imgproc.matchTemplate(searchRgn, foi_img, result, match_method); // TODO: try using masks



           Core.normalize(result, resultout, 0, 1, Core.NORM_MINMAX, -1);
//           if (frameNum==0 && i==0){
//               saveImageToDisk(resultcpy, "result_afterNorm_0", "OpenCV_Result", c, -1);
//           }
           Core.MinMaxLocResult mmr = Core.minMaxLoc(resultout);
           //Core.MinMaxLocResult mmr = Core.minMaxLoc(result);



           if (match_method == Imgproc.TM_SQDIFF
                   || match_method == Imgproc.TM_SQDIFF_NORMED ) {
               matchLocs[i] = mmr.minLoc;
               matchScores[i] = mmr.minVal;
               //Log.i(TAG, "minVal " + mmr.minVal);
           } else {
               if(prevLocalGuess[i]!=null) { // Used to calculate delta_avg. Null means first frame for sweep region
                   delta_x += mmr.maxLoc.x - prevLocalGuess[i].x;
               }
               prevLocalGuess[i] = mmr.maxLoc;
               matchLocs[i] = mmr.maxLoc;
               matchScores[i] = mmr.maxVal;
               //Log.i(TAG, "maxVal [" +i+"] "+ mmr.maxVal);
               //Log.i(TAG, "minVal [" +i+"] "+ mmr.minVal);
           }

       }

       // Goes through every 'match' to see if reference points and mats should be updated TEMPORARILY DEBUGGING REMOVAL
//       for (int i = 0;i<num_track;i++){
//           Point matchedPoint = new Point(drawmin[i].x+(matchLocs[i].x+(corner_size/2.0)),drawmin[i].y+(matchLocs[i].y+(corner_size/2.0)));
//           distances[i] = Math.sqrt((pts_xy[i].x-matchedPoint.x)*(pts_xy[i].x-matchedPoint.x) + (pts_xy[i].y-matchedPoint.y)*(pts_xy[i].y-matchedPoint.y));
//           if (matchScores[i] > .97 && distances[i] < 15){
//               Log.i(TAG, "distance "+ distances[i]);
//
//               pts_xy[i] = matchedPoint;
//
//               pts_img[i] = frame.submat(new Rect(new Point(drawmin[i].x+matchLocs[i].x, drawmin[i].y+matchLocs[i].y),
//                       new Point(drawmin[i].x+matchLocs[i].x+corner_size, drawmin[i].y+matchLocs[i].y+corner_size)));
//
//           }
//           else{
//               Log.i(TAG, "distancefail "+ distances[i]);
//           }
//       }


        //Drawing Directonal text
       double delta_x_avg =  delta_x / (double) num_track;
       DecimalFormat avgdf =  new DecimalFormat("#.##"); // Round errors
       if(delta_x_avg<0) {
          // putText(coloredFrame, "Dir: " + avgdf.format(delta_x_avg), new Point(20, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255,0,0));
           rectangle(coloredFrame, new Point(coloredFrame.cols()-60, 150), new Point(coloredFrame.cols()-20, 180), new Scalar(255, 0, 0),5);

       }
       else
       {
           //putText(coloredFrame, "Dir: " + avgdf.format(delta_x_avg), new Point(20, 50), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(0, 255, 0));
           rectangle(coloredFrame,new Point(20,150),new Point(60,180),new Scalar(255,0,0),5);

       }
       //Drawing speed intensity
       rectangle(coloredFrame,new Point(coloredFrame.cols()/2-30,coloredFrame.rows()-100),
               new Point(coloredFrame.cols()/2+30,coloredFrame.rows()-40),new Scalar(255,0,0),20);

        //Drawing Rects
       for(int i = 0;i<num_track;i++) {
           rectangle(coloredFrame, drawmin[i], drawmax[i], new Scalar(237, 167, 55), 1);
           Scalar colorFound;
           if(matchScores[i] > .97){ //&& distances[i] < 15){
               colorFound = new Scalar(0, 255, 0); //Green
           }
           else{
               colorFound = new Scalar(255,0,0); //Red
           }
           rectangle(coloredFrame, new Point(drawmin[i].x+matchLocs[i].x,drawmin[i].y+matchLocs[i].y),
                   new Point(drawmin[i].x+matchLocs[i].x+corner_size,drawmin[i].y+matchLocs[i].y+corner_size), colorFound, 1);
       }

       //testSearchRgn.copyTo(coloredFrame.colRange(0, testSearchRgn.cols()).rowRange(0,testSearchRgn.rows()));



       frameNum++;
   }
    // Gets the minPoint (dimensions) of which to submatrix. This is a scaled rectangle of max search-region dims
    private Point getSearchRgnRectMin(Point pt, Mat img){
        double minx=0,miny=0;

        if(pt.x - (sweep_size/2.0) >=0){
            minx = (double)pt.x-(sweep_size/2.0);
        }
        else{
            minx = 0;
        }

        if(pt.y-(sweep_size/2.0)>=0){
            miny = (double)pt.y-(sweep_size/2.0);
        }
        else{
            miny=0;
        }


        return new Point(minx,miny);
    }
    // Gets the maxPoint (dimensions) of which to submatrix. This is a scaled rectangle of max search-region dims
    private Point getSearchRgnRectMax(Point pt, Mat img){
        double minx=0,maxx=0,miny=0,maxy=0;

        if(pt.x + (sweep_size/2.0) <= img.cols()){
            maxx = (double)pt.x+(sweep_size/2.0);
        }
        else{
            maxx = pt.x+img.cols()-1;
        }

        if(pt.y+(sweep_size/2.0)<=img.rows()){
            maxy=(double)pt.y+(sweep_size/2.0);
        }
        else{
            maxy=img.rows()-1;
        }

        return new Point(maxx,maxy);
    }

    /**
     * Saves a Mat to the SD card application folder as a jpg.
     *
     * @param source The image to save.
     * @param filename The name of the file to be saved.
     * @param directoryName The directory where the
     * @param ctx The activity context.
     * @param colorConversion The openCV color conversion to apply to the image. -1 will use no color conversion.
     */
    public void saveImageToDisk(Mat source, String filename, String directoryName, Context ctx, int colorConversion){

        Mat mat = source.clone();
        if(colorConversion != -1)
            Imgproc.cvtColor(mat, mat, colorConversion, 4);

        Bitmap bmpOut = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmpOut);
        if (bmpOut != null){

            mat.release();
            OutputStream fout = null;
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            String dir = root + "/" + ctx.getResources().getString(R.string.app_name) + "/" + directoryName;
            String fileName = filename + ".jpg";
            File file = new File(dir);
            file.mkdirs();
            file = new File(dir, fileName);

            try {
                fout = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fout);
                bmpOut.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
                bmpOut.recycle();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }
        bmpOut.recycle();
    }

}

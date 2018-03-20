package com.toonit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.photo.Photo;
import org.opencv.imgproc.*;
import org.opencv.core.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class CVActivity extends Activity {

	private CameraBridgeViewBase mOpenCvCameraView;
	
	private Camera mCamera;
    private CameraPreview mPreview;
    
    private Mat intermediate;
    private Mat rgba;
    private Mat imageMat;
    private Mat imageMatG;
    
    private double mThresh1 = 80;
    private double mThresh2 = 90;
    
    private ImageView mPhotoView;
    private Switch mSwitch;

    private ProgressBar mProgressBar;
    private Bitmap resultBitmap;
    
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    
    public final static String APP_PATH_SD_CARD = "/ToonIt/";
    public final static String APP_THUMBNAIL_PATH_SD_CARD = "thumbnails";
    
	static String TAG = "CVActivity";
	
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
        Log.i("cam", mCamera == null ? "null" : "not null");
    }

    private void showCameraView(boolean show) {
    	if (show) {
    		findViewById(R.id.camera_preview).setVisibility(View.VISIBLE);
    		findViewById(R.id.CaptureButton).setVisibility(View.VISIBLE);
    	}
    	else {
    		findViewById(R.id.camera_preview).setVisibility(View.GONE);
    		findViewById(R.id.CaptureButton).setVisibility(View.GONE);
    	}
    }
    
    private void showPhotoView(boolean show) {
    	if (show) {
    		mPhotoView.setVisibility(View.VISIBLE);
    		findViewById(R.id.ReturnButton).setVisibility(View.VISIBLE);
    		findViewById(R.id.filter_choice).setVisibility(View.VISIBLE);
    		if (mSwitch.isChecked()) {
    			findViewById(R.id.seek_bar1).setVisibility(View.VISIBLE);
    			findViewById(R.id.seek_bar2).setVisibility(View.VISIBLE);
    		}
    	}
    	else {
    		mPhotoView.setVisibility(View.GONE);
    		findViewById(R.id.ReturnButton).setVisibility(View.GONE);
    		findViewById(R.id.filter_choice).setVisibility(View.GONE);
    		mPhotoView.setImageBitmap(null);
    		if (mSwitch.isChecked()) {
    			findViewById(R.id.seek_bar1).setVisibility(View.GONE);
    			findViewById(R.id.seek_bar2).setVisibility(View.GONE);
    		}
    	}
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera);
        //mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraView);
       // mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //mOpenCvCameraView.setCvCameraViewListener(this);
		mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        showCameraView(true);
        
        mPhotoView = (ImageView) findViewById(R.id.PhotoView);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        Button captureButton = (Button) findViewById(R.id.CaptureButton);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	mCamera.takePicture(null, null, mPicture);
                	showCameraView(false);
                	showPhotoView(true);
                }
            }
        );
        
        ((Button) findViewById(R.id.ReturnButton)).setOnClickListener(
        		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mCamera.startPreview();
						showPhotoView(false);
						showCameraView(true);
						
					}
				}
        	);
        
        SeekBar seekBar1 =(SeekBar) findViewById(R.id.seek_bar1);
        seekBar1.setOnSeekBarChangeListener(
        		new SeekBar.OnSeekBarChangeListener() {
        			@Override
        			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        				mThresh1 = progress;
        				
        			}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
        				new CannyEdgeTask().execute(imageMatG);
					}
        		}
        	);
        
        SeekBar seekBar2 =(SeekBar) findViewById(R.id.seek_bar2);
        seekBar2.setOnSeekBarChangeListener(
        		new SeekBar.OnSeekBarChangeListener() {
        			@Override
        			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        				mThresh2 = progress;

        				
        			}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
        				new CannyEdgeTask().execute(imageMatG);
						
					}
        		}
        	);
        
        mSwitch = (Switch) findViewById(R.id.filter_choice);
        mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            	if (isChecked) {
                	new CannyEdgeTask().execute(imageMatG);
            		findViewById(R.id.seek_bar1).setVisibility(View.VISIBLE);
            		findViewById(R.id.seek_bar2).setVisibility(View.VISIBLE);
                } else {
                	new GaussianTask().execute(imageMat);
                	findViewById(R.id.seek_bar1).setVisibility(View.GONE);
            		findViewById(R.id.seek_bar2).setVisibility(View.GONE);
                }
            }
        });

    }
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	        Log.i(TAG, "Camera open");
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    return c; // returns null if camera is unavailable
	}
	
	public void takePicture() {
		int numCamera = Camera.getNumberOfCameras();
		Log.i(TAG, "" + numCamera);
		Camera camera = getCameraInstance();
		if (camera == null) {
			//TODO: do something
		}
		Parameters params = camera.getParameters();
	}
	
	private Bitmap resetPicture() {
		
		Mat i = new Mat(imageMat.size(), imageMat.type());
		Imgproc.cvtColor(imageMat, i, Imgproc.COLOR_RGBA2GRAY);
		Mat edges = i.clone();
		
        Imgproc.Canny(imageMat, edges, mThresh1, 150);
        
        //Mat invertcolormatrix= new Mat(edges.rows(),edges.cols(), edges.type(), new Scalar(255,255,255));
        //Core.subtract(invertcolormatrix, edges, edges);
        
        //Imgproc.medianBlur(edges, edges, 3);
        Imgproc.bilateralFilter(imageMat, i, 5, 220, 220);
        
		i.setTo(new Scalar(0, 0, 0, 255), edges);
        resultBitmap = Bitmap.createBitmap(i.cols(),  i.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(i, resultBitmap);
        i.release();
        edges.release();
        
        return resultBitmap;
	}
	
	private PictureCallback mPicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {
	    		
	    		Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
	    		imageMat = new Mat (picture.getHeight(), picture.getWidth(), CvType.CV_8UC3);
	    		imageMatG = new Mat (picture.getHeight(), picture.getWidth(), CvType.CV_8UC3);
	            Utils.bitmapToMat(picture, imageMat);
	         
	            Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGBA2RGB);
	            
	            /*** Canny Edge with sliders
	             * 
	             */
//	            Imgproc.GaussianBlur(imageMat, imageMat, new Size(17, 17), 2);
//	            Imgproc.equalizeHist(i, i);
//	            
//	            Scalar m = Core.mean(i);
//	            Log.i(TAG, m.val.length + "");
//	            Log.i(TAG, m.val[0] + " " + m.val[1] + " " + m.val[2] + " " + m.val[3]);
//	            
//	            mThresh1 = m.val[0]*0.66;
//	            mThresh2 = m.val[0]*1.33;
//	            
//	            ((SeekBar) findViewById(R.id.seek_bar1)).setProgress((int) mThresh1);
//	            ((SeekBar) findViewById(R.id.seek_bar2)).setProgress((int) mThresh2);
//	            //((SeekBar) findViewById(R.id.seek_bar1)).setMax((int) mThresh2);
//	            
//	    		new CannyEdgeTask().execute(imageMat);
	           
	            
//	            /*** 
//	             * Canny Edge without sliders
//	             */
//	            Imgproc.GaussianBlur(imageMat, imageMat, new Size(17, 17), 2);
//	            Imgproc.equalizeHist(i, i);
//	            

	            Imgproc.GaussianBlur(imageMat, imageMatG, new Size(17, 17), 2);
	            
	            if (mSwitch.isChecked()) {
	            	new CannyEdgeTask().execute(imageMatG);
	            } else {
	            	new GaussianTask().execute(imageMat);
	            }

//	            new SobelEdgeTask().execute(imageMat);
	            
//	            new ScharrEdgeTask().execute(imageMat);

				//Imgproc.Canny(ImageMat, i, 60, 100);
				//ImageMat.setTo(new Scalar(0, 0, 0, 255), i);
	    		
	    		//toonImage(mat);
	    }
	};
	  
	private void toonImage(Mat raw) {
		float h = 3;
		float hColor=3;
		int templateWindowSize=7;
		int searchWindowSize=21;
		Mat denoised = null;
		Mat toon = null;
		Photo.fastNlMeansDenoisingColored(raw, denoised, h, hColor, templateWindowSize, searchWindowSize);
		double threshold1 = 60;
		double threshold2 = 100;
		Mat edges = null;
		Imgproc.Canny(denoised, edges, threshold1, threshold2);
		int d = 5;						// d = 5 recommended for real time application
		double sigmaColor = 150; 
		double sigmaSpace = 150; 		// recommended to have sigma values > 150 to make images look cartoonish
		Mat filteredIm = null;
		Imgproc.bilateralFilter(denoised, filteredIm, d, sigmaColor, sigmaSpace);
		Core.add(filteredIm, edges, toon);		// could also make 0 = black and 1 = white and multiple that edge image by the filtered image to 0 out edges
			
	}


	public void onCameraViewStarted(int width, int height) {
		intermediate = new Mat();
	}


	public void onCameraViewStopped() {
		if (intermediate != null) {
			intermediate.release();
		}
		
		intermediate = null;

	}

	 public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
	     rgba = inputFrame.rgba();
	     	//float h = 3;
			//float hColor=3;
			//int templateWindowSize=7;
			//int searchWindowSize=21;
			//Mat denoised = rgba.clone();
			//Imgproc.cvtColor(rgba, intermediate, Imgproc.COLOR_RGBA2RGB);
			//Mat toon = new Mat();
			//Photo.fastNlMeansDenoisingColored(intermediate, rgba);
			//double threshold1 = 60;
			//double threshold2 = 100;
			
			//Imgproc.Canny(rgba, intermediate, 60, 100);
			//rgba.setTo(new Scalar(0, 0, 0, 255), intermediate);
			//int d = 5;						// d = 5 recommended for real time application
			//double sigmaColor = 150; 
			//double sigmaSpace = 150; 		// recommended to have sigma values > 150 to make images look cartoonish
			//Mat filteredIm = null;
			//Imgproc.bilateralFilter(denoised, filteredIm, d, sigmaColor, sigmaSpace);
			//Core.add(filteredIm, edges, toon);		// could also make 0 = black and 1 = white and multiple that edge image by the filtered image to 0 out edges
			
			return rgba;
	 }
	
	 @Override
	    protected void onPause() {
	        super.onPause();
	        releaseCamera();              // release the camera immediately on pause event
	        
	    }


	    private void releaseCamera(){
	    	mPreview.setCamera(null);
	        if (mCamera != null){
	            mCamera.release();        // release the camera for other applications
	            mCamera = null;
	        }
	    }

	    /***
	     * Saves bitmap images in JPEG format to external SD card in "ToonIt" folder
	     * @param image, context
	     * @return
	     */
	    public boolean saveImageToExternalStorage(Bitmap image, Context context) {
	    String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD + APP_THUMBNAIL_PATH_SD_CARD;

	    try {
	    File dir = new File(fullPath);
	    if (!dir.exists()) {
	    dir.mkdirs();
	    }

	    OutputStream fOut = null;
	    File file = new File(fullPath, "toonImage.jpg");
	    file.createNewFile();
	    fOut = new FileOutputStream(file);

	    // 100 means no compression, the lower you go, the stronger the compression
	    image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
	    fOut.flush();
	    fOut.close();
	    
	    MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

	    return true;

	    } catch (Exception e) {
	    Log.e("saveToExternalStorage()", e.getMessage());
	    return false;
	    }
	    }
	    
	    private class GaussianTask extends AsyncTask<Mat, Void, Bitmap> {
	        protected void onPostExecute(Bitmap result) {
	            mPhotoView.setImageBitmap(result);
	            mProgressBar.setVisibility(View.GONE);
	            Log.i("FILTER", "g");
	            //Context context = getApplicationContext();
	           // boolean savedImage = saveImageToExternalStorage(result, context);
	           // Log.i("SaveToStorage", Boolean.valueOf(savedImage).toString());
	            
	        }
	        
	        protected void onPreExecute() {
	        	mProgressBar.setVisibility(View.VISIBLE);
	        }
	        
	        @Override
			protected Bitmap doInBackground(Mat... mat) {
	        	Mat im = mat[0];
	       
	        	Mat g1 = new Mat(im.size(), im.type());
	        	Mat g2 = g1.clone();
	        	Mat i= g1.clone();
	        	Mat edges = g1.clone();
	        	
	        	Imgproc.cvtColor(im, i, Imgproc.COLOR_RGB2GRAY);
	        	
//	        	Imgproc.equalizeHist(i, i);
	        	
	        	Imgproc.GaussianBlur(i, g1, new Size(21,21), 3);
	        	Imgproc.GaussianBlur(i, g2, new Size(21,21), 4.8);
	        	Core.subtract(g1, g2, edges);
	        	Log.i(TAG, "min: " + Core.minMaxLoc(i).minVal + " max: " + Core.minMaxLoc(i).maxVal);
	        	
//	        	Core.convertScaleAbs(edges, edges, 10, 0);
//	        	Imgproc.equalizeHist(edges, edges);
	        	Imgproc.medianBlur(edges, edges, 3);
	        	Imgproc.medianBlur(edges, edges, 3);
	        	
	        	// Close-Open
	        	Imgproc.erode(edges, edges, new Mat());
	        	Imgproc.dilate(edges, edges, new Mat());
	        	Imgproc.dilate(edges, edges, new Mat());
	        	Imgproc.erode(edges, edges, new Mat()); 
	        	
	        	Imgproc.dilate(edges, edges, new Mat()); 
	        	
	        	
	        	// Open-Close
//	        	Imgproc.dilate(edges, edges, new Mat());
//	        	Imgproc.erode(edges, edges, new Mat());
//	        	Imgproc.erode(edges, edges, new Mat());
//	        	Imgproc.dilate(edges, edges, new Mat());
	        	 
	        	
	        	Imgproc.bilateralFilter(im, i, 5, 250, 250);
//	        	Imgproc.GaussianBlur(im, i, new Size(21, 21), 3.0);
	        	
	        	i.setTo(new Scalar(0, 0, 0, 255), edges);
	        	
	        	
	        	Core.convertScaleAbs(i, i, 1./20, 0);
		        Core.convertScaleAbs(i, i, 20, 0);
		           
	            resultBitmap = Bitmap.createBitmap(i.cols(),  i.rows(),Bitmap.Config.ARGB_8888);;
	            Utils.matToBitmap(i, resultBitmap);
//	            Utils.matToBitmap(edges, resultBitmap);
	            
	            return resultBitmap;
	        }
	    }
	    
	    private class CannyEdgeTask extends AsyncTask<Mat, Void, Bitmap> {

	        /** The system calls this to perform work in the UI thread and delivers
	          * the result from doInBackground() */
	        protected void onPostExecute(Bitmap result) {
	            mPhotoView.setImageBitmap(result);
	            mProgressBar.setVisibility(View.GONE);
	            Log.i("FILTER", "c");
//	            ContextWrapper cw = new ContextWrapper(getApplicationContext());
//	            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
//	            File mypath=new File(directory,"photo.jpg");
//
//	            FileOutputStream fos;
//				try {
//					fos =new FileOutputStream(mypath);
//					result.compress(Bitmap.CompressFormat.PNG, 100, fos);
//		            fos.close();
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
	        }
	        
	        protected void onPreExecute() {
	        	mProgressBar.setVisibility(View.VISIBLE);
	        }

			@Override
			protected Bitmap doInBackground(Mat... mat) {
	            Mat im = mat[0];
	            
	            Mat i = new Mat(im.size(), im.type());

	    		Imgproc.cvtColor(im, i, Imgproc.COLOR_RGB2GRAY);
	    		Imgproc.equalizeHist(i, i);
	    		
	           // mThresh2 = Imgproc.threshold(i, i, 0, 255, Imgproc.THRESH_OTSU);
	            
	            //mThresh1 = mThresh2/2; 
	    		
	            
	    		Mat edges = i.clone();
	    		
//	            Imgproc.Canny(im, edges, mThresh1, mThresh2);
	    		Imgproc.Canny(im, edges, mThresh1, mThresh2);
	    		
	    		((SeekBar) findViewById(R.id.seek_bar1)).setProgress((int) mThresh1);
	            ((SeekBar) findViewById(R.id.seek_bar2)).setProgress((int) mThresh2);
	    		
	            Imgproc.dilate(edges, edges, new Mat());
	            //Mat invertcolormatrix= new Mat(edges.rows(),edges.cols(), edges.type(), new Scalar(255,255,255));
	            //Core.subtract(invertcolormatrix, edges, edges);
	            
	            //Imgproc.medianBlur(edges, edges, 3);
//	            Imgproc.bilateralFilter(im, i, 5, 250, 250);

	            Imgproc.GaussianBlur(im, i, new Size(21, 21), 3.0);
//	            Imgproc.adaptiveBilateralFilter(im, i, new Size(5,5), 220, 20, new Point(-1, -1), Imgproc.BORDER_DEFAULT);
	            
	    		i.setTo(new Scalar(0, 0, 0, 255), edges);
	    		
	            Core.convertScaleAbs(i, i, 1./20, 0);
	            Core.convertScaleAbs(i, i, 20, 0);
	            
	            resultBitmap = Bitmap.createBitmap(i.cols(),  i.rows(),Bitmap.Config.ARGB_8888);;
	            Utils.matToBitmap(i, resultBitmap);
//	            Utils.matToBitmap(edges, resultBitmap);
	            i.release();
	            edges.release();
	            
	            return resultBitmap;
			}
	    }
	    
	    private class SobelEdgeTask extends AsyncTask<Mat, Void, Bitmap> {

	        /** The system calls this to perform work in the UI thread and delivers
	          * the result from doInBackground() */
	        protected void onPostExecute(Bitmap result) {
	            mPhotoView.setImageBitmap(result);
	        }

			@Override
			protected Bitmap doInBackground(Mat... mat) {
				
	            Mat im = mat[0];
	            
	            Mat i = new Mat(im.size(), im.type());


	    		Imgproc.cvtColor(im, i, Imgproc.COLOR_RGB2GRAY);
//	    		Imgproc.equalizeHist(i, i); 
	    		Mat edges = i.clone();
	    		
	    		Imgproc.Sobel(i, edges, CvType.CV_8U, 1, 1);
	    		Core.convertScaleAbs(edges, edges, 10, 0);
	    		
//	            Imgproc.dilate(edges, edges, new Mat());
	            //Mat invertcolormatrix= new Mat(edges.rows(),edges.cols(), edges.type(), new Scalar(255,255,255));
	            //Core.subtract(invertcolormatrix, edges, edges);
	            
	            Imgproc.medianBlur(edges, edges, 3);
//	            Imgproc.bilateralFilter(im, i, 5, 250, 250);

//	            Imgproc.GaussianBlur(im, i, new Size(21, 21), 3.0);
//	            Imgproc.adaptiveBilateralFilter(im, i, new Size(5,5), 220, 20, new Point(-1, -1), Imgproc.BORDER_DEFAULT);
	            
	    		i.setTo(new Scalar(0, 0, 0, 255), edges);
	    		
	            Core.convertScaleAbs(i, i, 1./20, 0);
	            Core.convertScaleAbs(i, i, 20, 0);
	            
	            resultBitmap = Bitmap.createBitmap(i.cols(),  i.rows(),Bitmap.Config.ARGB_8888);;
//	            Utils.matToBitmap(i, resultBitmap);
	            Utils.matToBitmap(edges, resultBitmap);
	            i.release();
	            edges.release();
	            
	            return resultBitmap;
			}
	    }
	    
	    private class ScharrEdgeTask extends AsyncTask<Mat, Void, Bitmap> {

	        /** The system calls this to perform work in the UI thread and delivers
	          * the result from doInBackground() */
	        protected void onPostExecute(Bitmap result) {
	            mPhotoView.setImageBitmap(result);
	        }

			@Override
			protected Bitmap doInBackground(Mat... mat) {
				
	            Mat im = mat[0];
	            
	            Mat i = new Mat(im.size(), im.type());


	    		Imgproc.cvtColor(im, i, Imgproc.COLOR_RGB2GRAY);
//	    		Imgproc.equalizeHist(i, i); 
	    		Mat edges = i.clone();
	    		
	    		Imgproc.Scharr(i, edges, CvType.CV_8U, 1, 0);
//	    		Core.convertScaleAbs(edges, edges, 10, 0);
	    		
//	            Imgproc.dilate(edges, edges, new Mat());
	            //Mat invertcolormatrix= new Mat(edges.rows(),edges.cols(), edges.type(), new Scalar(255,255,255));
	            //Core.subtract(invertcolormatrix, edges, edges);
	            
	            Imgproc.medianBlur(edges, edges, 3);
//	            Imgproc.bilateralFilter(im, i, 5, 250, 250);

//	            Imgproc.GaussianBlur(im, i, new Size(21, 21), 3.0);
//	            Imgproc.adaptiveBilateralFilter(im, i, new Size(5,5), 220, 20, new Point(-1, -1), Imgproc.BORDER_DEFAULT);
	            
	    		i.setTo(new Scalar(0, 0, 0, 255), edges);
	    		
	            Core.convertScaleAbs(i, i, 1./20, 0);
	            Core.convertScaleAbs(i, i, 20, 0);
	            
	            resultBitmap = Bitmap.createBitmap(i.cols(),  i.rows(),Bitmap.Config.ARGB_8888);;
//	            Utils.matToBitmap(i, resultBitmap);
	            Utils.matToBitmap(edges, resultBitmap);
	            i.release();
	            edges.release();
	            
	            return resultBitmap;
			}
	    }
	  
}

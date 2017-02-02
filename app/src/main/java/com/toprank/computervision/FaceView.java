package com.toprank.computervision;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.view.View;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import static com.toprank.computervision.FacePreview.d;
import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvTranspose;
import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

/**
 * Created by john on 8/16/15.
 */
class FaceView extends View implements Camera.PreviewCallback {
    public static final int SUBSAMPLING_FACTOR = 4;

    protected opencv_core.IplImage grayImage = new opencv_core.IplImage();
    protected opencv_core.IplImage img;
    protected opencv_objdetect.CvHaarClassifierCascade classifier;
    protected opencv_core.CvMemStorage storage;
    protected opencv_core.CvSeq faces;

    protected int myRotation = 0;

    protected CameraDirection cameraDirection = CameraDirection.FRONT_FACING;

    protected Activity activity;

    protected Util util;

    public FaceView(Activity activity) throws IOException {
        super(activity);
        this.activity = activity;
        util = new Util(activity);
        d("Faceview constructor");
        d("creating input stream");
        // Load the classifier file from Java resources.
        InputStream raw = new FileInputStream(new File(new Util(activity).canonize("haarcascade_frontalface_alt.xml")));
        d("extracting resouce ");
        File classifierFile = Util.extractResource(raw, activity.getCacheDir(), "classifier", ".xml");
        if (classifierFile == null || classifierFile.length() <= 0) {
            throw new IOException("Could not extract the classifier file from Java resource.");
        }

        d("Classifier file: " + classifierFile.getAbsolutePath());

        d("Loading module");
        // Preload the opencv_objdetect module to work around a known bug.
        Loader.load(opencv_objdetect.class);
        d("Creating classifier");
        classifier = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
        d("Deleting classifer file");
        classifierFile.delete();

        if (classifier.isNull()) {
            throw new IOException("Could not load the classifier file.");
        }
        d("Creating CVMemstorage");
        storage = opencv_core.CvMemStorage.create();
    }



    public void onPreviewFrame(final byte[] data, final Camera camera) {
        try {
            Camera.Size size = camera.getParameters().getPreviewSize();
            processImage(data, size.width, size.height);
            camera.addCallbackBuffer(data);
        } catch (RuntimeException e) {
            // The camera has probably just been released, ignore.
            e.printStackTrace();
        }
    }

    protected void rotate_90n(opencv_core.IplImage src, opencv_core.IplImage dst, int angle)
    {

        if(angle == 270 || angle == -90){
            // Rotate clockwise 270 degrees
            cvTranspose(src, dst);
            cvFlip(dst, dst, 0);
        }else if(angle == 180 || angle == -180){
            // Rotate clockwise 180 degrees
            cvFlip(src, dst, -1);
        }else if(angle == 90 || angle == -270){
            // Rotate clockwise 90 degrees
            cvTranspose(src, dst);
            cvFlip(dst, dst, 1);
        }else if(angle == 360 || angle == 0){
            cvCopy(src, dst);
        }
    }

    protected synchronized void processImage(byte[] data, int width, int height) {
        // First, downsample our image and convert it into a grayscale IplImage
        int f = SUBSAMPLING_FACTOR;

        if (img == null || img.width() != width/f || img.height() != height/f) {
            img = opencv_core.IplImage.create(width / f, height / f, IPL_DEPTH_8U, 1);
            grayImage = opencv_core.IplImage.create(img.height(), img.width(), img.depth(), img.nChannels());
        }
        int imageWidth  = img.width();
        int imageHeight = img.height();
        int dataStride = f*width;
        int imageStride = img.widthStep();
        ByteBuffer imageBuffer = img.getByteBuffer();
        for (int y = 0; y < imageHeight; y++) {
            int dataLine = y*dataStride;
            int imageLine = y*imageStride;
            for (int x = 0; x < imageWidth; x++) {
                imageBuffer.put(imageLine + x, data[dataLine + f*x]);
            }
        }

        if(cameraDirection == CameraDirection.FRONT_FACING){
            int r = myRotation - 90;
            rotate_90n(img, grayImage, r);
            cvFlip(grayImage, grayImage, 1);//Mirror
        }else{
            rotate_90n(img, grayImage, myRotation + 90);
        }


        cvClearMemStorage(storage);
        faces = cvHaarDetectObjects(grayImage, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(20);

        String s = "FacePreview - This side up.";
        float textWidth = paint.measureText(s);
        canvas.drawText(s, (getWidth() - textWidth) / 2, 20, paint);

        if (faces != null) {
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            float scaleX = (float)getWidth()/grayImage.width();
            float scaleY = (float)getHeight()/grayImage.height();
            int total = faces.total();
            for (int i = 0; i < total; i++) {
                opencv_core.CvRect r = new opencv_core.CvRect(cvGetSeqElem(faces, i));
                int x = r.x(), y = r.y(), w = r.width(), h = r.height();
                canvas.drawRect(x*scaleX, y*scaleY, (x+w)*scaleX, (y+h)*scaleY, paint);
            }
        }
    }

    public synchronized void takeSnapShot(){
        if(grayImage == null){
            return;
        }
        String id = UUID.randomUUID().toString();
        util.addImage(id, grayImage);
    }

    public void setMyRotation(int myRotation) {
        this.myRotation = myRotation;
    }

    public void setCameraDirection(CameraDirection cameraDirection) {
        this.cameraDirection = cameraDirection;
    }
}

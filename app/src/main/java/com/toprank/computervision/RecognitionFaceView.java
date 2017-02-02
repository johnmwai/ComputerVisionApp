package com.toprank.computervision;

import android.app.Activity;

import org.bytedeco.javacpp.opencv_core;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

/**
 * Created by john on 8/23/15.
 */
public class RecognitionFaceView extends FaceView {
    RecognitionResult recognitionResult;
    RecognizeActivity recognizeActivity;
    public RecognitionFaceView(Activity activity) throws IOException {
        super(activity);
        recognizeActivity = (RecognizeActivity) activity;
        util.prepareForRecog();
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

        recognitionResult = util.recognize(grayImage);

        recognizeActivity.showRecognitionInfo(recognitionResult);

        postInvalidate();
    }
}

package com.toprank.computervision;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import com.toprank.computervision.database.IplImageController;
import com.toprank.computervision.database.MatController;
import com.toprank.computervision.database.PersonController;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.opencv_core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.helper.opencv_legacy.cvCalcEigenObjects;
import static org.bytedeco.javacpp.helper.opencv_legacy.cvEigenDecomposite;
import static org.bytedeco.javacpp.opencv_contrib.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.CV_L1;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvConvertScale;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMat;
import static org.bytedeco.javacpp.opencv_core.cvMinMaxLoc;
import static org.bytedeco.javacpp.opencv_core.cvNormalize;
import static org.bytedeco.javacpp.opencv_core.cvRect;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvTermCriteria;
import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;
import static org.bytedeco.javacpp.opencv_legacy.CV_EIGOBJ_NO_CALLBACK;


/**
 * Created by john on 8/16/15.
 */
public class Util {

    private final Context context;

    /** the logger */
    private static final Logger LOGGER = Logger.getLogger(FaceRecognition.class.getName());
    /** the number of training faces */
    private int nTrainFaces = 0;
    /** the training face image array */
    opencv_core.IplImage[] trainingFaceImgArr;
    /** the test face image array */
    opencv_core.IplImage[] testFaceImgArr;
    /** the person number array */
    opencv_core.CvMat personNumTruthMat;
    /** the number of persons */
    int nPersons;
    /** the person names */
    final List<String> personNames = new ArrayList<String>();
    /** the number of eigenvalues */
    int nEigens = 0;
    /** eigenvectors */
    opencv_core.IplImage[] eigenVectArr;
    /** eigenvalues */
    opencv_core.CvMat eigenValMat;
    /** the average image */
    opencv_core.IplImage pAvgTrainImg;
    /** the projected training faces */
    opencv_core.CvMat projectedTrainFaceMat;

    opencv_core.CvMat trainPersonNumMat;  // the person numbers during training

    List<Person> peopleInDb;

    private static Person person;

    public void addPersonToDb(String name){
        PersonController personController = new PersonController();
        person = personController.getPerson(context, name);
        if(person == null){
            Person p = new Person();
            p.setName(name);
            LinkedList<Person> list  = new LinkedList<>();
            list.add(p);
            personController.insertPeople(context, list);
            person = personController.getPerson(context, name);
        }


    }

    public void addImage(Person person, String filename, opencv_core.IplImage image){
        IplImageController c = new IplImageController();
        HashMap<String, opencv_core.IplImage> list = new HashMap<>();
        list.put(filename, image);
        c.insertImages(context, person, list);
    }

    public void addImage(String filename, opencv_core.IplImage image){
        addImage(person, filename, image);
    }

    public Util(Context context) {
        this.context = context;
    }

    public void copyAssets(final ResultListener listener){
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object[] objects) {
                File cascades = new File(new Util(context).canonize("haarcascade_frontalface_alt.xml"));
                if(!cascades.isFile()){
                    copyFileOrDir("");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                listener.successHook(null);
            }
        }.execute();
    }

    private void copyFileOrDir(String path){
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list(path);
            if(files.length == 0){
                copyFile(path);
            }else{
                for(String filename : files) {
                    File f = new File(path, filename);
                    copyFileOrDir(f.getPath());
                }
            }
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
    }

    private void copyFile(String filename){
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            File outFile = new File(context.getExternalFilesDir(null), filename);
            File dir = new File(outFile.getParent());
            if(!dir.isDirectory()){
                dir.mkdirs();
            }
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e("tag", "Failed to copy asset file: " + filename, e);
            System.exit(1);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public String canonize(String name){
        File f = new File(context.getExternalFilesDir(null), name);
        return f.getAbsolutePath();
    }

    public static File extractResource(InputStream is, File directory,
                                       String prefix, String suffix) throws IOException {
        if (is == null) {
            return null;
        }
        File file = null;
        boolean fileExisted = false;
        try {
            file = File.createTempFile(prefix, suffix, directory);
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            if (file != null && !fileExisted) {
                file.delete();
            }
            throw e;
        }
        return file;
    }

    public void cleanUp(){
        MatController matController = new MatController();
        matController.deleteMats(context);
        IplImageController imageController = new IplImageController();
        imageController.deleteImages(context);
        PersonController personController = new PersonController();
        personController.deletePeople(context);
    }

    public void learn() {
        int i;

        // load training data
        LOGGER.info("===========================================");
        LOGGER.info("Loading the training images");
        trainingFaceImgArr = loadFaceImgArray();
        nTrainFaces = trainingFaceImgArr.length;
        LOGGER.info("Got " + nTrainFaces + " training images");
        if (nTrainFaces < 3) {
            LOGGER.severe("Need 3 or more training faces\n"
                    + "Input file contains only " + nTrainFaces);
            return;
        }

        // do Principal Component Analysis on the training faces
        doPCA();

        LOGGER.info("projecting the training images onto the PCA subspace");
        // project the training images onto the PCA subspace
        projectedTrainFaceMat = cvCreateMat(
                nTrainFaces, // rows
                nEigens, // cols
                CV_32FC1); // type, 32-bit float, 1 channel

        // initialize the training face matrix - for ease of debugging
        for (int i1 = 0; i1 < nTrainFaces; i1++) {
            for (int j1 = 0; j1 < nEigens; j1++) {
                projectedTrainFaceMat.put(i1, j1, 0.0);
            }
        }

        LOGGER.info("created projectedTrainFaceMat with " + nTrainFaces + " (nTrainFaces) rows and " + nEigens + " (nEigens) columns");
        if (nTrainFaces < 5) {
            LOGGER.info("projectedTrainFaceMat contents:\n" + oneChannelCvMatToString(projectedTrainFaceMat));
        }

        final FloatPointer floatPointer = new FloatPointer(nEigens);
        for (i = 0; i < nTrainFaces; i++) {
            cvEigenDecomposite(
                    trainingFaceImgArr[i], // obj
                    nEigens, // nEigObjs
                    eigenVectArr, // eigInput (Pointer)
                    0, // ioFlags
                    null, // userData (Pointer)
                    pAvgTrainImg, // avg
                    floatPointer); // coeffs (FloatPointer)

            if (nTrainFaces < 5) {
                LOGGER.info("floatPointer: " + floatPointerToString(floatPointer));
            }
            for (int j1 = 0; j1 < nEigens; j1++) {
                projectedTrainFaceMat.put(i, j1, floatPointer.get(j1));
            }
        }
        if (nTrainFaces < 5) {
            LOGGER.info("projectedTrainFaceMat after cvEigenDecomposite:\n" + projectedTrainFaceMat);
        }

        // store the recognition data as an xml file
        storeTrainingData();

        // Save all the eigenvectors as images, so that they can be checked.
        storeEigenfaceImages();
    }

    public synchronized void prepareForRecog(){
        // load the saved training data
        trainPersonNumMat = loadTrainingData();
        PersonController personController = new PersonController();
        peopleInDb = personController.getPeople(context, 0, Integer.MAX_VALUE);
    }

    /** Opens the training data from the file 'facedata.xml'.
     *
     * @param pTrainPersonNumMat
     * @return the person numbers during training, or null if not successful
     */
    private opencv_core.CvMat loadTrainingData() {
        LOGGER.info("loading training data");
        opencv_core.CvMat pTrainPersonNumMat = null; // the person numbers during training

        int i;

        PersonController personController = new PersonController();
        IplImageController imageController = new IplImageController();
        MatController matController = new MatController();


        // Load the person names.
        personNames.clear();        // Make sure it starts as empty.
        nPersons = personController.countPeople(context);
        if (nPersons == 0) {
            LOGGER.severe("No people found in the training database 'facedata.xml'.");
            return null;
        } else {
            LOGGER.info(nPersons + " persons read from the training database");
        }

        List<Person> people = personController.getPeople(context, 0, Integer.MAX_VALUE);

        // Load each person's name.
        for (Person p: people) {
            personNames.add(p.getName());
        }
        LOGGER.info("person names: " + personNames);

        // Load the data
        nTrainFaces = imageController.countFaceImages(context);
        nEigens = nTrainFaces - 1;

        Map<String, opencv_core.CvMat> mats = matController.getMats(context, 0, Integer.MAX_VALUE);

        pTrainPersonNumMat = mats.get("trainPersonNumMat");
        eigenValMat = mats.get("eigenValMat");
        projectedTrainFaceMat = mats.get("projectedTrainFaceMat");


        pAvgTrainImg = imageController.getImage(context, "avgTrainImg");

        eigenVectArr = new opencv_core.IplImage[nTrainFaces];
        for (i = 0; i <= nEigens; i++) {
            String varname = "eigenVect_" + i;
            opencv_core.IplImage img = imageController.getImage(context, varname);
            if(img == null && i > 0){
                //Prev =
                opencv_core.IplImage prev = eigenVectArr[i - 1];
                img = cvCreateImage(prev.cvSize(), prev.depth(), prev.nChannels());
            }
            eigenVectArr[i] = img;
        }
        LOGGER.info("Training data loaded (" + nTrainFaces + " training images of " + nPersons + " people)");
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("People: ");
        if (nPersons > 0) {
            stringBuilder.append("<").append(personNames.get(0)).append(">");
        }
        for (i = 1; i < nPersons; i++) {
            stringBuilder.append(", <").append(personNames.get(i)).append(">");
        }
        LOGGER.info(stringBuilder.toString());

        return pTrainPersonNumMat;
    }

    public synchronized RecognitionResult recognize(opencv_core.IplImage img){
        float[] projectedTestFace;
        projectedTestFace = new float[nEigens];
        float confidence = 0.0f;
        if (trainPersonNumMat == null) {
            return null;
        }

        int iNearest;
        int nearest;

        // project the test image onto the PCA subspace
        cvEigenDecomposite(
                img, // obj
                nEigens, // nEigObjs
                eigenVectArr, // eigInput (Pointer)
                0, // ioFlags
                null, // userData
                pAvgTrainImg, // avg
                projectedTestFace);  // coeffs

        //LOGGER.info("projectedTestFace\n" + floatArrayToString(projectedTestFace));

        final FloatPointer pConfidence = new FloatPointer(confidence);
        iNearest = findNearestNeighbor(projectedTestFace, new FloatPointer(pConfidence));
        confidence = pConfidence.get();
        nearest = trainPersonNumMat.data_i().get(iNearest);
        Person p = peopleInDb.get(nearest);
        RecognitionResult result = new RecognitionResult();
        result.p = p;
        result.confidence = confidence;
        return result;
    }

    private int findNearestNeighbor(float projectedTestFace[], FloatPointer pConfidencePointer) {
        double leastDistSq = Double.MAX_VALUE;
        int i = 0;
        int iTrain = 0;
        int iNearest = 0;

        LOGGER.info("................");
        LOGGER.info("find nearest neighbor from " + nTrainFaces + " training faces");
        for (iTrain = 0; iTrain < nTrainFaces; iTrain++) {
            //LOGGER.info("considering training face " + (iTrain + 1));
            double distSq = 0;

            for (i = 0; i < nEigens; i++) {
                //LOGGER.debug("  projected test face distance from eigenface " + (i + 1) + " is " + projectedTestFace[i]);

                float projectedTrainFaceDistance = (float) projectedTrainFaceMat.get(iTrain, i);
                float d_i = projectedTestFace[i] - projectedTrainFaceDistance;
                distSq += d_i * d_i; // / eigenValMat.data_fl().get(i);  // Mahalanobis distance (might give better results than Eucalidean distance)
//          if (iTrain < 5) {
//            LOGGER.info("    ** projected training face " + (iTrain + 1) + " distance from eigenface " + (i + 1) + " is " + projectedTrainFaceDistance);
//            LOGGER.info("    distance between them " + d_i);
//            LOGGER.info("    distance squared " + distSq);
//          }
            }

            if (distSq < leastDistSq) {
                leastDistSq = distSq;
                iNearest = iTrain;
                LOGGER.info("  training face " + (iTrain + 1) + " is the new best match, least squared distance: " + leastDistSq);
            }
        }

        // Return the confidence level based on the Euclidean distance,
        // so that similar images should give a confidence between 0.5 to 1.0,
        // and very different images should give a confidence between 0.0 to 0.5.
        float pConfidence = (float) (1.0f - Math.sqrt(leastDistSq / (float) (nTrainFaces * nEigens)) / 255.0f);
        pConfidencePointer.put(pConfidence);

        LOGGER.info("training face " + (iNearest + 1) + " is the final best match, confidence " + pConfidence);
        return iNearest;
    }

    /** Saves all the eigenvectors as images, so that they can be checked. */
    private void storeEigenfaceImages() {
        // Store the average image to a file
        LOGGER.info("Saving the image of the average face as 'out_averageImage.bmp'");

        IplImageController imageController = new IplImageController();
        imageController.insertImage(context, "out_averageImage.bmp", pAvgTrainImg);

        // Create a large image made of many eigenface images.
        // Must also convert each eigenface image to a normal 8-bit UCHAR image instead of a 32-bit float image.
        LOGGER.info("Saving the " + nEigens + " eigenvector images as 'out_eigenfaces.bmp'");

        if (nEigens > 0) {
            // Put all the eigenfaces next to each other.
            int COLUMNS = 8;        // Put upto 8 images on a row.
            int nCols = Math.min(nEigens, COLUMNS);
            int nRows = 1 + (nEigens / COLUMNS);        // Put the rest on new rows.
            int w = eigenVectArr[0].width();
            int h = eigenVectArr[0].height();
            opencv_core.CvSize size = cvSize(nCols * w, nRows * h);
            final opencv_core.IplImage bigImg = cvCreateImage(
                    size,
                    IPL_DEPTH_8U, // depth, 8-bit Greyscale UCHAR image
                    1);        // channels
            for (int i = 0; i < nEigens; i++) {
                // Get the eigenface image.
                opencv_core.IplImage byteImg = convertFloatImageToUcharImage(eigenVectArr[i]);
                // Paste it into the correct position.
                int x = w * (i % COLUMNS);
                int y = h * (i / COLUMNS);
                opencv_core.CvRect ROI = cvRect(x, y, w, h);
                cvSetImageROI(
                        bigImg, // image
                        ROI); // rect
                cvCopy(
                        byteImg, // src
                        bigImg, // dst
                        null); // mask
                cvResetImageROI(bigImg);
                cvReleaseImage(byteImg);
            }
            imageController.insertImage(context, "out_eigenfaces.bmp", bigImg);
            cvReleaseImage(bigImg);
        }
    }

    /** Converts the given float image to an unsigned character image.
     *
     * @param srcImg the given float image
     * @return the unsigned character image
     */
    private opencv_core.IplImage convertFloatImageToUcharImage(opencv_core.IplImage srcImg) {
        opencv_core.IplImage dstImg;
        if ((srcImg != null) && (srcImg.width() > 0 && srcImg.height() > 0)) {
            // Spread the 32bit floating point pixels to fit within 8bit pixel range.
            double[] minVal = new double[1];
            double[] maxVal = new double[1];
            cvMinMaxLoc(srcImg, minVal, maxVal);
            // Deal with NaN and extreme values, since the DFT seems to give some NaN results.
            if (minVal[0] < -1e30) {
                minVal[0] = -1e30;
            }
            if (maxVal[0] > 1e30) {
                maxVal[0] = 1e30;
            }
            if (maxVal[0] - minVal[0] == 0.0f) {
                maxVal[0] = minVal[0] + 0.001;  // remove potential divide by zero errors.
            }                        // Convert the format
            dstImg = cvCreateImage(cvSize(srcImg.width(), srcImg.height()), 8, 1);
            cvConvertScale(srcImg, dstImg, 255.0 / (maxVal[0] - minVal[0]), -minVal[0] * 255.0 / (maxVal[0] - minVal[0]));
            return dstImg;
        }
        return null;
    }

    /** Stores the training data to the file 'facedata.xml'. */
    private void storeTrainingData() {
        int i;

        LOGGER.info("Storing training data in database");

        MatController matController = new MatController();
        matController.insertMat(context, "trainPersonNumMat", personNumTruthMat);
        matController.insertMat(context, "eigenValMat", eigenValMat);
        matController.insertMat(context, "projectedTrainFaceMat", projectedTrainFaceMat);

        IplImageController imageController = new IplImageController();
        imageController.insertImage(context, "avgTrainImg", pAvgTrainImg);
        for (i = 0; i < nEigens; i++) {
            String varname = "eigenVect_" + i;
            imageController.insertImage(context, varname, eigenVectArr[i]);
        }
    }

    /** Returns a string representation of the given float pointer.
     *
     * @param floatPointer the given float pointer
     * @return a string representation of the given float pointer
     */
    private String floatPointerToString(final FloatPointer floatPointer) {
        final StringBuilder stringBuilder = new StringBuilder();
        boolean isFirst = true;
        stringBuilder.append('[');
        for (int i = 0; i < floatPointer.capacity(); i++) {
            if (isFirst) {
                isFirst = false;
            } else {
                stringBuilder.append(", ");
            }
            stringBuilder.append(floatPointer.get(i));
        }
        stringBuilder.append(']');

        return stringBuilder.toString();
    }

    /** Does the Principal Component Analysis, finding the average image and the eigenfaces that represent any image in the given dataset. */
    private void doPCA() {
        int i;
        opencv_core.CvTermCriteria calcLimit;
        opencv_core.CvSize faceImgSize = new opencv_core.CvSize();

        // set the number of eigenvalues to use
        nEigens = nTrainFaces - 1;

        LOGGER.info("allocating images for principal component analysis, using " + nEigens + (nEigens == 1 ? " eigenvalue" : " eigenvalues"));

        // allocate the eigenvector images
        faceImgSize.width(trainingFaceImgArr[0].width());
        faceImgSize.height(trainingFaceImgArr[0].height());
        eigenVectArr = new opencv_core.IplImage[nEigens];
        for (i = 0; i < nEigens; i++) {
            eigenVectArr[i] = cvCreateImage(
                    faceImgSize, // size
                    IPL_DEPTH_32F, // depth
                    1); // channels
        }

        // allocate the eigenvalue array
        eigenValMat = cvCreateMat(
                1, // rows
                nEigens, // cols
                CV_32FC1); // type, 32-bit float, 1 channel

        // allocate the averaged image
        pAvgTrainImg = cvCreateImage(
                faceImgSize, // size
                IPL_DEPTH_32F, // depth
                1); // channels

        // set the PCA termination criterion
        calcLimit = cvTermCriteria(
                CV_TERMCRIT_ITER, // type
                nEigens, // max_iter
                1); // epsilon

        LOGGER.info("computing average image, eigenvalues and eigenvectors");
        // compute average image, eigenvalues, and eigenvectors
        cvCalcEigenObjects(
                nTrainFaces, // nObjects
                trainingFaceImgArr, // input
                eigenVectArr, // output
                CV_EIGOBJ_NO_CALLBACK, // ioFlags
                0, // ioBufSize
                null, // userData
                calcLimit,
                pAvgTrainImg, // avg
                eigenValMat.data_fl()); // eigVals

        LOGGER.info("normalizing the eigenvectors");
        cvNormalize(
                eigenValMat, // src (CvArr)
                eigenValMat, // dst (CvArr)
                1, // a
                0, // b
                CV_L1, // norm_type
                null); // mask
    }

    /** Returns a string representation of the given one-channel CvMat object.
     *
     * @param cvMat the given CvMat object
     * @return a string representation of the given CvMat object
     */
    public String oneChannelCvMatToString(final opencv_core.CvMat cvMat) {
        //Preconditions
        if (cvMat.channels() != 1) {
            throw new RuntimeException("illegal argument - CvMat must have one channel");
        }

        final int type = cvMat.type();
        StringBuilder s = new StringBuilder("[ ");
        for (int i = 0; i < cvMat.rows(); i++) {
            for (int j = 0; j < cvMat.cols(); j++) {
                if (type == CV_32FC1 || type == CV_32SC1) {
                    s.append(cvMat.get(i, j));
                } else {
                    throw new RuntimeException("illegal argument - CvMat must have one channel and type of float or signed integer");
                }
                if (j < cvMat.cols() - 1) {
                    s.append(", ");
                }
            }
            if (i < cvMat.rows() - 1) {
                s.append("\n  ");
            }
        }
        s.append(" ]");
        return s.toString();
    }


    /** Reads the names & image filenames of people from a text file, and loads all those images listed.
     *
     * @param filename the training file name
     * @return the face image array
     */
    private opencv_core.IplImage[] loadFaceImgArray() {
        //Intel's Image Processing Library image array
        opencv_core.IplImage[] faceImgArr;

        BufferedReader imgListFile;
        String imgFilename;
        int iFace = 0;
        IplImageController imageController = new IplImageController();
        PersonController personController = new PersonController();
        List<Person> people = personController.getPeople(context, 0, Integer.MAX_VALUE);
//        Map<Person, List<opencv_core.IplImage>> map = new HashMap<>();

        personNames.clear();        // Make sure it starts as empty.
        nPersons = people.size();
        int nFaces = imageController.countFaceImages(context);
        faceImgArr = new opencv_core.IplImage[nFaces];
        // allocate the face-image array and person number matrix
        personNumTruthMat = cvCreateMat(
                1, // rows
                nFaces, // cols
                CV_32SC1); // type, 32-bit unsigned, one channel

        // initialize the person number matrix - for ease of debugging
        for (int j1 = 0; j1 < nFaces; j1++) {
            personNumTruthMat.put(0, j1, 0);
        }
        int iIndex = 0;
        for(Person p: people){
            int pIndex = people.indexOf(p);
            personNames.add(p.getName());
            List<opencv_core.IplImage> images = imageController.getImages(context, p, 0, Integer.MAX_VALUE);
            for(opencv_core.IplImage img: images){
                personNumTruthMat.put(
                        0, // i
                        iIndex, // j
                        pIndex); // v

                faceImgArr[iIndex] = img; // isColor
                iIndex ++;
            }
        }

        LOGGER.info("nFaces: " + nFaces);
        int i;


        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("People: ");
        if (nPersons > 0) {
            stringBuilder.append("<").append(personNames.get(0)).append(">");
        }
        for (i = 1; i < nPersons && i < personNames.size(); i++) {
            stringBuilder.append(", <").append(personNames.get(i)).append(">");
        }
        LOGGER.info(stringBuilder.toString());

        return faceImgArr;
    }

    public abstract static class ResultListener {
        public void successHook(Object o){
            successHook();
        }

        public void successHook(){

        }

        public void failureHook(Object o){
            failureHook();
        }

        public void failureHook(){

        }

        public void always(Object o){
            always();
        }

        public void always(){

        }
    }
}

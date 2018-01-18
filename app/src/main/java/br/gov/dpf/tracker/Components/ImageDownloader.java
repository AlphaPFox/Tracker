package br.gov.dpf.tracker.Components;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import br.gov.dpf.tracker.Entities.Model;

public class ImageDownloader extends AsyncTask<Void, Void, Bitmap> {

    private ImageView bmImage;
    private File imgFile;
    private String modelID, modelURL;

    public ImageDownloader(ImageView bmImage, String modelID) {
        this.bmImage = bmImage;
        this.modelID = modelID;
    }

    public ImageDownloader(ImageView bmImage, String modelID, String modelURL) {
        this.bmImage = bmImage;
        this.modelID = modelID;
        this.modelURL = modelURL;
    }

    protected Bitmap doInBackground(Void... urls) {

        //File path to image
        imgFile = new File(bmImage.getContext().getFilesDir(), modelID);

        //If image was already downloaded
        if(imgFile.exists())
        {
            //Return image disk path
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        else
        {
            if(modelURL == null)
            {
                try
                {
                    //Try to get model data from firestore DB (wait for operation to finish)
                    QuerySnapshot result = Tasks.await(FirebaseFirestore.getInstance().collection("Model").whereEqualTo("name", modelID).get());

                    //Check if query is successful
                    if(!result.isEmpty())
                    {
                        //Parse result to a model object
                        Model trackerModel = result.getDocuments().get(0).toObject(Model.class);

                        //Execute download from model image
                        modelURL = trackerModel.getImagePath();
                    }
                }
                catch (Exception ex)
                {
                    //Log data
                    Log.e("ImageDownloader", "Error getting model image", ex);
                }
            }
            //Create new image file
            Bitmap mIcon11 = null;

            try
            {
                //Perform download
                InputStream in = new java.net.URL(modelURL).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }

            return mIcon11;
        }
    }

    protected void onPostExecute(Bitmap result)
    {
        try
        {
            //Set image view using downloaded  image
            bmImage.setImageBitmap(result);

            //If image was already downloaded
            if(!imgFile.exists())
            {
                //Create file
                FileOutputStream out = new FileOutputStream(imgFile, imgFile.createNewFile());

                //Store compressed png version
                result.compress(Bitmap.CompressFormat.PNG, 90, out);
            }
        }
        catch (Exception e)
        {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
    }
}

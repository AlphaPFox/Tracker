package br.gov.dpf.tracker.Components;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

    private ImageView bmImage;
    private String modelID;

    public ImageDownloader(ImageView bmImage, String modelID) {
        this.bmImage = bmImage;
        this.modelID = modelID;
    }

    protected Bitmap doInBackground(String... urls) {

        //File path to image
        File imgFile = new File(bmImage.getContext().getFilesDir(), modelID);

        //If image was already downloaded
        if(imgFile.exists())
        {
            //Return image disk path
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        else
        {
            //Create new image file
            Bitmap mIcon11 = null;

            try
            {
                //Perform download
                InputStream in = new java.net.URL(urls[0]).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }

            return mIcon11;
        }
    }

    protected void onPostExecute(Bitmap result) {
        try
        {
            //Set image view using downloaded  image
            bmImage.setImageBitmap(result);

            //File path to store new image
            File imgFile = new File(bmImage.getContext().getFilesDir(), modelID);

            //Create file
            FileOutputStream out = new FileOutputStream(imgFile, imgFile.createNewFile());

            //Store compressed png version
            result.compress(Bitmap.CompressFormat.PNG, 90, out);
        }
        catch (Exception e)
        {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
    }
}

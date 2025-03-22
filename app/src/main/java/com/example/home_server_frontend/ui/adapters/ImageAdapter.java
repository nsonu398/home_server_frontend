package com.example.home_server_frontend.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.ui.BottomReached;
import com.example.home_server_frontend.ui.MainActivity;
import com.example.home_server_frontend.utils.PicassoAuth;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private final BottomReached bottomReached;
    private Context mContext;
    private List<ImageEntity> mImageList;
    private PreferenceManager preferenceManager;
    private Picasso picassoAuth;

    public ImageAdapter(Context context, List<ImageEntity> imageList, BottomReached bottomReached) {
        mContext = context;
        mImageList = imageList;
        this.bottomReached = bottomReached;
        preferenceManager = new PreferenceManager(context);
        picassoAuth = PicassoAuth.getPicassoInstance(context, preferenceManager.getAuthToken());
    }

    @Override
    public int getCount() {
        return mImageList.size();
    }

    @Override
    public Object getItem(int position) {
        return mImageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            // If it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    400));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        // Set the image for the ImageView
        Picasso.get()
                .load(mImageList.get(position).getLocalUrl().isEmpty()?preferenceManager.getBaseUrl()+mImageList.get(position).getRemoteUrl():mImageList.get(position).getLocalUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .resize(400, 400)
                .centerCrop()
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        // Load an alternative URL when the first one fails
                        Log.d("TAG", "onBitmapFailed: "+e);
                        picassoAuth.get()
                                .load(preferenceManager.getBaseUrl()+mImageList.get(position).getRemoteUrl())
                                .resize(400, 400)
                                .centerCrop()
                                .into(imageView);
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        // You can set a placeholder here if needed
                    }
                });

        if(position>=mImageList.size()-1){
            bottomReached.onBottomReached();
        }
        return imageView;
    }

    // Method to update the image list
    public void updateImages(List<ImageEntity> newImages) {
        mImageList = newImages;
        notifyDataSetChanged();
    }
    public void addImages(List<ImageEntity> newImages) {
        mImageList.addAll(newImages);
        notifyDataSetChanged();
    }
}
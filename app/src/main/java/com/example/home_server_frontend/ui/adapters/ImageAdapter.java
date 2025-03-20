package com.example.home_server_frontend.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.home_server_frontend.ui.BottomReached;
import com.example.home_server_frontend.ui.MainActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private final BottomReached bottomReached;
    private Context mContext;
    private List<MainActivity.ImageData> mImageList;

    public ImageAdapter(Context context, List<MainActivity.ImageData> imageList, BottomReached bottomReached) {
        mContext = context;
        mImageList = imageList;
        this.bottomReached = bottomReached;
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
        Picasso.get().load("file://"+mImageList.get(position).getPath()).resize(400, 400).centerCrop().into(imageView);
        if(position>=mImageList.size()-1){
            bottomReached.onBottomReached();
        }
        return imageView;
    }

    // Method to update the image list
    public void updateImages(List<MainActivity.ImageData> newImages) {
        mImageList = newImages;
        notifyDataSetChanged();
    }
    public void addImages(List<MainActivity.ImageData> newImages) {
        mImageList.addAll(newImages);
        notifyDataSetChanged();
    }
}
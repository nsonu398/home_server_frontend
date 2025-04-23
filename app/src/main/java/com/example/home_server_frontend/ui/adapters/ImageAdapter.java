package com.example.home_server_frontend.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.ui.BottomReached;
import com.example.home_server_frontend.ui.ImageDetailsActivity;
import com.example.home_server_frontend.utils.PicassoAuth;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageAdapter extends BaseAdapter {
    private final BottomReached bottomReached;
    private Context mContext;
    private List<ImageEntity> mImageList;
    private PreferenceManager preferenceManager;
    private Picasso picassoAuth;
    private LayoutInflater inflater;

    public ImageAdapter(Context context, List<ImageEntity> imageList, BottomReached bottomReached) {
        mContext = context;
        mImageList = imageList;
        this.bottomReached = bottomReached;
        preferenceManager = new PreferenceManager(context);
        picassoAuth = PicassoAuth.getPicassoInstance(context, preferenceManager.getAuthToken());
        inflater = LayoutInflater.from(context);
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

    @SuppressLint("CheckResult")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // Create a new view using our custom layout
            convertView = inflater.inflate(R.layout.grid_item_image, parent, false);

            // Set the layout parameters to match the GridView cell size
            convertView.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    400));

            // Create and store the ViewHolder
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.image_view);
            holder.statusIcon = convertView.findViewById(R.id.status_icon);

            convertView.setTag(holder);
        } else {
            // Reuse existing view
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the current image entity
        ImageEntity imageEntity = mImageList.get(position);

        // Set the status icon based on image status
        updateStatusIcon(holder.statusIcon, imageEntity.getStatus());

        // Load the image
        if (imageEntity.getLocalUrl().isEmpty()) {
            // This is a remote image
            picassoAuth.get()
                    .load(preferenceManager.getBaseUrl() + imageEntity.getRemoteUrl())
                    .resize(400, 400)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            // This is a local image, load thumbnail
            Single.fromCallable(() -> MediaStore.Images.Thumbnails.getThumbnail(
                            mContext.getContentResolver(),
                            Long.parseLong(imageEntity.getImageId()),
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                    )).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                        holder.imageView.setImageBitmap(bitmap);
                    }, error -> {
                        Log.d("ImageAdapter", "Error loading thumbnail: " + error);
                    });
        }

        // Trigger bottom reached callback if necessary
        if (position >= mImageList.size() - 1) {
            bottomReached.onBottomReached();
        }

        // Set click listener for the image item
        convertView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, ImageDetailsActivity.class);
            intent.putExtra("selectedImage", new Gson().toJson(imageEntity));
            mContext.startActivity(intent);
        });

        return convertView;
    }

    /**
     * Update the status icon based on the image status
     */
    private void updateStatusIcon(ImageView statusIcon, String status) {
        statusIcon.setVisibility(View.VISIBLE);

        switch (status) {
            case "UPLOADED":
                // Image is synced with server
                statusIcon.setImageResource(R.drawable.ic_sync_complete);
                statusIcon.setContentDescription("Image synced");
                break;
            case "UPLOADING":
                // Image is currently uploading
                statusIcon.setImageResource(R.drawable.ic_sync_progress);
                statusIcon.setContentDescription("Upload in progress");
                break;
            case "PENDING":
                // Image is waiting to be uploaded
                statusIcon.setImageResource(R.drawable.ic_sync_pending);
                statusIcon.setContentDescription("Upload pending");
                break;
            case "FAILED":
                // Upload failed
                statusIcon.setImageResource(R.drawable.ic_sync_failed);
                statusIcon.setContentDescription("Upload failed");
                break;
            default:
                // Local image not scheduled for sync or other status
                statusIcon.setVisibility(View.GONE);
                break;
        }
    }

    // ViewHolder pattern class
    private static class ViewHolder {
        ImageView imageView;
        ImageView statusIcon;
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
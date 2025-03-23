package com.example.home_server_frontend.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.home_server_frontend.R;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.ui.BottomReached;
import com.example.home_server_frontend.ui.ImageDetailsActivity;
import com.example.home_server_frontend.ui.MainActivity;
import com.example.home_server_frontend.utils.PicassoAuth;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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

    @SuppressLint("CheckResult")
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
        if (mImageList.get(position).getLocalUrl().isEmpty()) {
            picassoAuth.get()
                    .load(preferenceManager.getBaseUrl() + mImageList.get(position).getRemoteUrl())
                    .resize(400, 400)
                    .centerCrop()
                    .into(imageView);
        } else {
            Single.fromCallable(() -> MediaStore.Images.Thumbnails.getThumbnail(
                            mContext.getContentResolver(),
                            Long.parseLong(mImageList.get(position).getImageId()),
                            MediaStore.Images.Thumbnails.MINI_KIND, // Use MINI_KIND (512x384) for larger thumbnails
                            null
                    )).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                        imageView.setImageBitmap(bitmap);
                    }, error -> {
                        Log.d("TAG", "getView: " + error);
                    });
        }
        if (position >= mImageList.size() - 1) {
            bottomReached.onBottomReached();
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ImageDetailsActivity.class);
                intent.putExtra("selectedImage", new Gson().toJson(mImageList.get(position)));
                mContext.startActivity(intent);
            }
        });
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
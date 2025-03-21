package com.example.home_server_frontend.repository;

import android.content.Context;
import android.os.Build;

import com.example.home_server_frontend.database.AppDatabase;
import com.example.home_server_frontend.database.ImageDao;
import com.example.home_server_frontend.database.ImageEntity;

import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageRepository {
    private final ImageDao imageDao;

    public ImageRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        imageDao = db.imageDao();
    }

    public Single<Long> insertImage(ImageEntity image) {
        return imageDao.insertImage(image)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<List<Long>> insertImages(List<ImageEntity> images){
        return imageDao.insertAllImage(images)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable updateImage(ImageEntity image) {
        return imageDao.updateImage(image)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable deleteImage(ImageEntity image) {
        return imageDao.deleteImage(image)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Flowable<List<ImageEntity>> getAllImages() {
        return imageDao.getAllImages()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Flowable<List<ImageEntity>> getImagesByStatus(String status) {
        return imageDao.getImagesByStatus(status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Get the oldest pending upload, wrapped in an Optional
     * @return Flowable that will emit when a pending upload becomes available
     */
    public Flowable<Optional<ImageEntity>> getOldestPendingUpload() {
        return imageDao.getOldestPendingUpload()
                .map(list -> {
                    if (list.isEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            return Optional.<ImageEntity>empty();
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        return Optional.of(list.get(0));
                    }
                    return null;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<ImageEntity> getImageById(long id) {
        return imageDao.getImageById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<ImageEntity> getImageByLocalUrl(String localUrl) {
        return imageDao.getImageByLocalUrl(localUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable updateImageStatus(long id, String status) {
        return imageDao.updateImageStatus(id, status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable setImageUploaded(long id, String remoteUrl) {
        return imageDao.setImageUploaded(id, remoteUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
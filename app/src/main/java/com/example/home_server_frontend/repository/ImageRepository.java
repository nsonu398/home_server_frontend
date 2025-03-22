package com.example.home_server_frontend.repository;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.home_server_frontend.api.ApiClient;
import com.example.home_server_frontend.api.ApiService;
import com.example.home_server_frontend.api.models.ImageListResponse;
import com.example.home_server_frontend.api.models.ServerImage;
import com.example.home_server_frontend.crypto.CryptoUtils;
import com.example.home_server_frontend.crypto.KeyManager;
import com.example.home_server_frontend.database.AppDatabase;
import com.example.home_server_frontend.database.ImageDao;
import com.example.home_server_frontend.database.ImageEntity;
import com.example.home_server_frontend.utils.ImageUtils;
import com.example.home_server_frontend.utils.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageRepository {
    private final ImageDao imageDao;
    private final PreferenceManager preferenceManager;
    private final ApiService apiService;
    private final KeyManager keyManager;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final String TAG = "ImageRepo";

    public ImageRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        imageDao = db.imageDao();
        preferenceManager = new PreferenceManager(context);
        apiService = ApiClient.getApiService(preferenceManager.getBaseUrl());
        keyManager = new KeyManager(context);

    }

    public Single<Long> insertImage(ImageEntity image) {
        return imageDao.insertImage(image)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /*public Single<List<Long>> insertImages(List<ImageEntity> images) {
        return imageDao.insertAllImage(images)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }*/

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
     *
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

    public Single<Long> getMostRecentImageTimestamp() {
        return imageDao.getMostRecentImageTimestamp()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void startSync() {

        // Get auth token
        String authToken = preferenceManager.getAuthToken();
        if (authToken == null) {
            return;
        }

        // Call the API to get server images
        apiService.getServerImages("Bearer " + authToken).enqueue(new Callback<ImageListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ImageListResponse> call, @NonNull Response<ImageListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        processServerImageList(response.body());
                    } catch (Exception e) {
                        Log.e("", "Error processing server images", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ImageListResponse> call, @NonNull Throwable t) {
                Log.e("TAG", "Network error retrieving server images", t);
            }
        });
    }

    private void processServerImageList(ImageListResponse response) {
        try {
            // Decrypt the server's response
            String decryptedJson = CryptoUtils.decryptHybridPackage(
                    response.getEncryptedResponse(),
                    keyManager.getPrivateKey()
            );

            if (decryptedJson == null) {
                Log.e("TAG", "Failed to decrypt server response");
                return;
            }

            // Parse the response
            JSONObject jsonResponse = new JSONObject(decryptedJson);
            boolean success = jsonResponse.getBoolean("success");

            if (!success) {
                return;
            }

            JSONArray imagesArray = jsonResponse.getJSONArray("images");

            // Convert JSON array to list of ServerImage objects
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ServerImage>>() {
            }.getType();
            List<ServerImage> serverImages = gson.fromJson(imagesArray.toString(), listType);
            List<ImageEntity> entities = getImageEntities(serverImages);
            saveImagesToLocalDB(entities);
        } catch (Exception e) {
            Log.e("TAG", "Error processing server image list", e);
        }
    }

    private List<ImageEntity> getImageEntities(List<ServerImage> serverImages) {
        List<ImageEntity> entities = new ArrayList<>();
        for (ServerImage image : serverImages) {
            ImageEntity entity = new ImageEntity("", "UPLOADED", image.getSize(), image.getResolution(), image.getOriginalFilename(), image.getImageId(), image.getUpdatedTime());
            entity.setRemoteUrl("api/images/" + image.getId());
            entities.add(entity);
        }
        return entities;
    }

    private void saveImagesToLocalDB(List<ImageEntity> entities) {
        compositeDisposable.add(insertImages(entities).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                result ->{
                    Log.d(TAG, "saveImagesToLocalDB: "+result.size());
                    preferenceManager.setAllServerImagesFetched();
                },
                error -> {
                    Log.d(TAG, "saveImagesToLocalDB: "+error);
                }
        ));
    }

    // Then in ImageRepository
    public Single<List<Long>> insertImages(List<ImageEntity> newImages) {
        return imageDao.getAllImageFileNames()
                .flatMap(existingFileNames -> {
                    Set<String> fileNameSet = new HashSet<>(existingFileNames);
                    List<ImageEntity> uniqueImages = new ArrayList<>();

                    // Filter out images that already exist
                    for (ImageEntity image : newImages) {
                        if (!fileNameSet.contains(image.getFileName())) {
                            uniqueImages.add(image);
                        }
                    }

                    if (uniqueImages.isEmpty()) {
                        return Single.just(new ArrayList<Long>());
                    } else {
                        return imageDao.insertAllImage(uniqueImages);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
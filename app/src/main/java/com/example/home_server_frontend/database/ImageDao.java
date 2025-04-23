package com.example.home_server_frontend.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Single<Long> insertImage(ImageEntity image);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Single<List<Long>> insertAllImage(List<ImageEntity> images);

    @Query("UPDATE images SET status = :toStatus WHERE status=:fromStatus")
    Completable updateAllRowsToStatus(String fromStatus, String toStatus);

    @Transaction
    @Query("SELECT fileName FROM images")
    Single<List<String>> getAllImageFileNames();

    @Update
    Completable updateImage(ImageEntity image);

    @Delete
    Completable deleteImage(ImageEntity image);

    @Query("SELECT * FROM images ORDER BY updatedTime DESC")
    Flowable<List<ImageEntity>> getAllImages();

    @Query("SELECT * FROM images WHERE status = :status ORDER BY timestamp DESC")
    Flowable<List<ImageEntity>> getImagesByStatus(String status);

    /**
     * Get the oldest pending upload (by timestamp ascending)
     * @return Flowable emitting a list with one item (the oldest) or empty list if none found
     */
    @Query("SELECT * FROM images WHERE status in ('PENDING', 'FAILED') ORDER BY timestamp ASC LIMIT 1")
    Flowable<List<ImageEntity>> getOldestPendingUpload();

    @Query("SELECT * FROM images WHERE id = :id")
    Single<ImageEntity> getImageById(long id);

    @Query("SELECT * FROM images WHERE localUrl = :localUrl")
    Single<ImageEntity> getImageByLocalUrl(String localUrl);

    @Query("UPDATE images SET status = :status WHERE id = :id")
    Completable updateImageStatus(long id, String status);

    @Query("UPDATE images SET remoteUrl = :remoteUrl, status = 'UPLOADED' WHERE id = :id")
    Completable setImageUploaded(long id, String remoteUrl);

    @Query("SELECT MAX(updatedTime) FROM images")
    Single<Long> getMostRecentImageTimestamp();

    @Query("SELECT MIN(updatedTime) FROM images")
    Single<Long> getOldestTimestamp();
}
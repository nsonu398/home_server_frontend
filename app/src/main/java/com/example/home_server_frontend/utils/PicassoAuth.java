package com.example.home_server_frontend.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class PicassoAuth {

    public static Picasso getPicassoInstance(Context context, final String authToken) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + authToken)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                })
                .build();

        Picasso picasso = new Picasso.Builder(context)
                .downloader(new OkHttp3Downloader(client))
                .build();

        Picasso.setSingletonInstance(picasso);  // <== Ensure it's used globally

        return picasso;
    }

}

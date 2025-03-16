package com.example.home_server_frontend.api;


import com.example.home_server_frontend.utils.Constants;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Retrofit client for API requests
 */
public class ApiClient {
    private static Retrofit retrofit = null;

    /**
     * Get Retrofit client instance
     * @return Retrofit instance
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Add logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Build OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();

            // Build Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    /**
     * Get API service interface
     * @return ApiService interface
     */
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
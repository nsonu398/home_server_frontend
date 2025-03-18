package com.example.home_server_frontend.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client for API requests
 */
public class ApiClient {
    private static Retrofit retrofit = null;
    private static String baseUrl = null;

    /**
     * Get Retrofit client instance
     * @param baseUrl The base URL for API calls
     * @return Retrofit instance
     */
    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null || !ApiClient.baseUrl.equals(baseUrl)) {
            ApiClient.baseUrl = baseUrl;

            // Add logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Build OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();

            // Build Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    /**
     * Get API service interface
     * @param baseUrl The base URL for API calls
     * @return ApiService interface
     */
    public static ApiService getApiService(String baseUrl) {
        return getClient(baseUrl).create(ApiService.class);
    }
}
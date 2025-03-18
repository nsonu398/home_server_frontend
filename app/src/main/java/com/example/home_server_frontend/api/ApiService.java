package com.example.home_server_frontend.api;

import com.example.home_server_frontend.api.models.ImageUploadResponse;
import com.example.home_server_frontend.api.models.KeyExchangeResponse;
import com.example.home_server_frontend.api.models.LoginRequest;
import com.example.home_server_frontend.api.models.LoginResponse;
import com.example.home_server_frontend.api.models.RegisterClientKeyRequest;
import com.example.home_server_frontend.api.models.RegisterClientKeyResponse;
import com.example.home_server_frontend.api.models.RegistrationRequest;
import com.example.home_server_frontend.api.models.RegistrationResponse;
import com.example.home_server_frontend.api.models.VerificationRequest;
import com.example.home_server_frontend.api.models.VerificationResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit interface for API endpoints
 */
public interface ApiService {

    /**
     * Get server's public key
     */
    @GET("/api/server-public-key")
    Call<KeyExchangeResponse> getServerPublicKey();

    /**
     * Register client's public key
     */
    @POST("/api/register-client-key")
    Call<RegisterClientKeyResponse> registerClientKey(@Body RegisterClientKeyRequest request);

    /**
     * Register a new user (Step 1)
     */
    @POST("/api/register")
    Call<RegistrationResponse> register(@Body RegistrationRequest request);

    /**
     * Verify registration with code (Step 2)
     */
    @POST("/api/verify_registration")
    Call<VerificationResponse> verifyRegistration(@Body VerificationRequest request);

    /**
     * User login
     */
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /**
     * Upload image to server
     * @param auth Authentication token in the format "Bearer <token>"
     * @param metadata Encrypted image metadata
     * @param image The image file part
     * @return Response containing the encrypted server response
     */
    @Multipart
    @POST("/api/upload")
    Call<ImageUploadResponse> uploadImage(
            @Part("auth") RequestBody auth,
            @Part("metadata") RequestBody metadata,
            @Part MultipartBody.Part image
    );
}
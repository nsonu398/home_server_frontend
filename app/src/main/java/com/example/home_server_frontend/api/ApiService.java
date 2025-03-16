package com.example.home_server_frontend.api;

import com.example.home_server_frontend.api.models.KeyExchangeResponse;
import com.example.home_server_frontend.api.models.LoginRequest;
import com.example.home_server_frontend.api.models.LoginResponse;
import com.example.home_server_frontend.api.models.RegisterClientKeyRequest;
import com.example.home_server_frontend.api.models.RegisterClientKeyResponse;
import com.example.home_server_frontend.api.models.RegistrationRequest;
import com.example.home_server_frontend.api.models.RegistrationResponse;
import com.example.home_server_frontend.api.models.VerificationRequest;
import com.example.home_server_frontend.api.models.VerificationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

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
}
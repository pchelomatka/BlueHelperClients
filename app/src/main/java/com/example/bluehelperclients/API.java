package com.example.bluehelperclients;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface API {

    static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36";

    @Headers({"User-Agent: " + userAgent})
    @GET("api/map")
    Call<Example> map(@Query("building_id") String buildingId);
}

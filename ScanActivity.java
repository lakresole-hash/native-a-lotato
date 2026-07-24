package com.smartxplorer.bestsystemlottery.server;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

/**
 * Created by Ernst Jean Charles on 02/23/2026.
 */

public class RetrofitClient {

    private static Retrofit mInstance;
    private static Retrofit mGoogleInstance;

    public static synchronized Retrofit getClient(String baseUrl) {

        if (null == mInstance) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            if (ApiHelper.SHOW_LOG)
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.readTimeout(1, TimeUnit.MINUTES);
            httpClient.connectTimeout(1, TimeUnit.MINUTES);
            httpClient.addInterceptor(logging);
            mInstance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(httpClient.build())
                    .build();
        }
        return mInstance;
    }
}

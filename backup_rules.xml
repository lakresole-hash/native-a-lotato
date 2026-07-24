package com.smartxplorer.bestsystemlottery.server;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Ernst Jean Charles on 08/21/2022..
 * Service Class
 * Define methods for executing HTTP requests such as @GET.
 */


public interface APIService {
    @GET("/api/agentTicket")
    Call<ResponseBody> Tickets(@Query("ML-API-KEY") String mlAPIKey,
                               @Query("ML-API-SECRET") String APISecret,
                               @Query("username") String username);

    @GET("/api/printTicket")
    Call<ResponseBody> PrintTicket(@Query("ML-API-KEY") String mlAPIKey,
                                   @Query("ML-API-SECRET") String APISecret,
                                   @Query("serial_number") String serial_number,
                                   @Query("date_time") String date_time);

    @GET("/api/rapportVentes")
    Call<ResponseBody> SalesReport(@Query("ML-API-KEY") String mlAPIKey,
                                   @Query("ML-API-SECRET") String APISecret,
                                   @Query("username") String username,
                                   @Query("date_time") String date);

    @GET("/api/historicVentes")
    Call<ResponseBody> historicalSales(@Query("ML-API-KEY") String mlAPIKey,
                                       @Query("ML-API-SECRET") String APISecret,
                                       @Query("username") String username,
                                       @Query("date_time_start") String date_time_start,
                                       @Query("date_time_end") String date_time_end);

    @GET("/api/ticketsGagnants")
    Call<ResponseBody> winningTicket(@Query("ML-API-KEY") String mlAPIKey,
                                     @Query("ML-API-SECRET") String APISecret,
                                     @Query("username") String username,
                                     @Query("date_time_start") String date_time_start,
                                     @Query("date_time_end") String date_time_end);

}
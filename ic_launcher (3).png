package com.smartxplorer.bestsystemlottery.servicesManage;

import static com.smartxplorer.bestsystemlottery.MainActivity.API_KEY;
import static com.smartxplorer.bestsystemlottery.MainActivity.API_SECRET;

import android.content.Context;

import com.smartxplorer.bestsystemlottery.apiCall.ApiCallback;
import com.smartxplorer.bestsystemlottery.server.ApiHelper;
import com.smartxplorer.bestsystemlottery.server.Request;

import okhttp3.ResponseBody;

/**
 * Created by Ernst Jean Charles on 23/02/2026.
 */


public class ServicesManager extends DataManager {

    private final Context mContext;

//    private String TAG = ServicesManager.class.getName();

    public ServicesManager(Context context) {
        mContext = context;
    }

    public void GetTickets(String username, Request.RequestListener<ResponseBody> json) {

        ApiHelper.getParamsAPI().Tickets(API_KEY,
                API_SECRET,
                username).enqueue(new ApiCallback<>(mContext, json));
    }

    public void GetByTicketNumber(String serial_number, String date,
                                  Request.RequestListener<ResponseBody> json) {

        ApiHelper.getParamsAPI().PrintTicket(API_KEY,
                API_SECRET,
                serial_number, date).enqueue(new ApiCallback<>(mContext, json));
    }

    public void GetSalesReport(String username, String date, Request.RequestListener<ResponseBody> json) {

        ApiHelper.getParamsAPI().SalesReport(API_KEY,
                API_SECRET,
                username, date).enqueue(new ApiCallback<>(mContext, json));
    }

    public void GethistoricalSales(String username, String date_time_start,
                                   String date_time_end, Request.RequestListener<ResponseBody> json) {

        ApiHelper.getParamsAPI().historicalSales(API_KEY,
                API_SECRET,
                username, date_time_start, date_time_end).enqueue(new ApiCallback<>(mContext, json));
    }

    public void GetwinningTicket(String username, String date_time_start,
                                   String date_time_end, Request.RequestListener<ResponseBody> json) {

        ApiHelper.getParamsAPI().winningTicket(API_KEY,
                API_SECRET,
                username, date_time_start, date_time_end).enqueue(new ApiCallback<>(mContext, json));
    }

}

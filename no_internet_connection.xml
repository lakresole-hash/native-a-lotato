package com.smartxplorer.bestsystemlottery.apiCall;

import android.content.Context;

import com.smartxplorer.bestsystemlottery.server.Request;

import org.json.JSONException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiCallback<T> implements Callback<T> {

    protected Request.RequestListener<T> listener;

    protected Context context;

    public ApiCallback(Context context, Request.RequestListener<T> listener) {
        this.listener = listener;
        this.context = context;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {

        listener.onResponse();
        try {
            listener.onSuccess(response.body());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        listener.onResponse();
        listener.onError();
    }
}
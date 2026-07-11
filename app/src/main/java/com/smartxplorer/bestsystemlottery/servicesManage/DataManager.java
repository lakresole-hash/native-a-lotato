package com.smartxplorer.bestsystemlottery.servicesManage;


import android.content.Context;

/**
 * Created by Ernst Jean Charles on 02/23/2026.
 */

public class DataManager {

    private static DataManager mInstance;
    private Context mContext;

    private ServicesManager mServicesManager;

    protected static String header = "";

    public DataManager() {
    }

    private DataManager(Context mContext) {
        this.mContext = mContext;
        mServicesManager = new ServicesManager(this.mContext);
    }

    public static synchronized DataManager getInstance(Context mContext) {
//        header = AppUtil.getHeader(mContext);
        if (null == mInstance) {
            mInstance = new DataManager(mContext);
        }
        return mInstance;
    }

    public static String getHeader() {
        return header;
    }

    public void reset() {
        mInstance = null;
    }

    public ServicesManager getServicesManager() {
        return mServicesManager;
    }
}

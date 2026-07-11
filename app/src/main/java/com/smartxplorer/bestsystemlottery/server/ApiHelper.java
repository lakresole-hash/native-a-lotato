package com.smartxplorer.bestsystemlottery.server;

import static com.smartxplorer.bestsystemlottery.MainActivity.BASE_URL;


/**
 * Created by Ernst Jean Charles on 02/23/2026.
 */

public class ApiHelper {

    public static boolean SHOW_LOG = true;

    public static APIService getParamsAPI() {
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

}

package com.keyfixer.customer.Common;

import com.keyfixer.customer.Remote.FCMClient;
import com.keyfixer.customer.Remote.IFCMService;
import com.keyfixer.customer.Remote.IGoogleAPI;
import com.keyfixer.customer.Remote.RetrofitClient;

public class Common {
    public static final String fixer_tbl = "Fixers";
    public static final String fixer_inf_tbl = "Users";
    public static final String customer_tbl = "Customers";
    public static final String fix_request_tbl = "FixRequest";
    public static final String token_tbl = "Tokens";
    public static final String fcmUrl = "https://fcm.googleapis.com/";

    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmUrl).create(IFCMService.class);
    }
}

package com.keyfixer.customer.Common;

import com.keyfixer.customer.Model.User;
import com.keyfixer.customer.Remote.FCMClient;
import com.keyfixer.customer.Remote.IFCMService;
import com.keyfixer.customer.Remote.IGoogleAPI;
import com.keyfixer.customer.Remote.RetrofitClient;

public class Common {
    public static boolean isFixerFound = false;
    public static String fixerid = "";
    public static boolean isFixDone = false;
    public static final String user_field = "customer_username";
    public static final String pwd_field = "customer_password";
    public static User currentUser;
    public static final String fixer_tbl = "Fixers";
    public static final String fixer_inf_tbl = "Users";
    public static final String customer_tbl = "Customers";
    public static final String fix_request_tbl = "FixRequest";
    public static final String token_tbl = "Tokens";
    public static final String rate_detail_tbl = "RateDetails";
    public static final String fcmUrl = "https://fcm.googleapis.com/";

    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmUrl).create(IFCMService.class);
    }
}

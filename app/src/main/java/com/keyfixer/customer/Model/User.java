package com.keyfixer.customer.Model;

public class User {
    private String strEmail, strPassword, strPhone, strName;

    @Override
    public String toString() {
        return "User{" +
                "strEmail='" + strEmail + '\'' +
                ", strPassword='" + strPassword + '\'' +
                ", strPhone='" + strPhone + '\'' +
                ", strName='" + strName + '\'' +
                '}';
    }

    public User() {
    }

    public User(String strEmail, String strPassword, String strName, String strPhone) {
        this.strEmail = strEmail;
        this.strPassword = strPassword;
        this.strPhone = strPhone;
        this.strName = strName;
    }

    public String getStrEmail() {
        return strEmail;
    }

    public void setStrEmail(String strEmail) {
        this.strEmail = strEmail;
    }

    public String getStrPassword() {
        return strPassword;
    }

    public void setStrPassword(String strPassword) {
        this.strPassword = strPassword;
    }

    public String getStrPhone() {
        return strPhone;
    }

    public void setStrPhone(String strPhone) {
        this.strPhone = strPhone;
    }

    public String getStrName() {
        return strName;
    }

    public void setStrName(String strName) {
        this.strName = strName;
    }
}

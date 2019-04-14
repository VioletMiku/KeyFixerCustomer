package com.keyfixer.customer.Model;

import java.io.Serializable;

public class Service implements Serializable {
    private int image;
    private String title;
    private double price;

    public Service() {
    }

    public Service(int image , String title , double price) {
        this.image = image;
        this.title = title;
        this.price = price;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Service{" +
                "image=" + image +
                ", title='" + title + '\'' +
                ", price=" + price +
                '}';
    }
}

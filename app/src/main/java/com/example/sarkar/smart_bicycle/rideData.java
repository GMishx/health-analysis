package com.example.sarkar.smart_bicycle;

import android.text.format.DateFormat;

/**
 * Created by Sarkar on 04-12-2016.
 */

class rideData {
    private int _id;
    private int BPM;
    private float speed;
    private String dateTime;
    rideData(){}
    rideData(int id, int BPM, float speed, String dateTime){
        this._id=id;
        this.BPM=BPM;
        this.speed=speed;
        this.dateTime=new String(dateTime);
    }
    rideData(int BPM, float speed){
        this.BPM=BPM;
        this.speed=speed;
        this.dateTime = (DateFormat.format("dd-MM-yyyy kk:mm:ss", new java.util.Date()).toString());
    }
    int getID(){
        return this._id;
    }
    void setID(int id){
        this._id=id;
    }
    int getBPM(){
        return this.BPM;
    }
    void setBPM(int BPM){
        this.BPM=BPM;
    }
    float getSpeed(){
        return this.speed;
    }
    void setSpeed(float speed){
        this.speed=speed;
    }
    String getDateTime(){
        return dateTime;
    }
    void setDateTime(String dateTime){
        this.dateTime=new String(dateTime);
    }
    public String toString(){
        return "ID: " + this._id + ", BPM: " + this.BPM + ", Speed: " + this.speed + ", DT: " + this.dateTime;
    }
}
package com.example.sarkar.smart_bicycle;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

class userData {
    private int _id;
    private long dob;
    private float dist;
    private String name;
    userData(){
        this.dob = (new Date()).getTime();
        this.dist = 0;
    }
    userData(int id, String name, long dob, float dist){
        this._id=id;
        this.dob=dob;
        this.dist=dist;
        this.name=new String(name);
    }
    public userData(int dob, String name){
        this.dob=dob;
        this.name=new String(name);
        this.dist=0;
    }
    int getID(){
        return this._id;
    }
    public void setID(int id){
        this._id=id;
    }
    public String getName(){
        return this.name;
    }
    public void setName(String name){
        this.name=new String(name);
    }
    float getDist(){
        return this.dist;
    }
    void setDist(float dist){
        this.dist=dist;
    }
    long getDOB(){
        return this.dob;
    }
    void setDOB(long dob){
        this.dob=dob;
    }
    int getAge(){
        Calendar birth = new GregorianCalendar();
        birth.setTime(new Date(dob));
        Calendar today = new GregorianCalendar();
        today.setTime(new Date());
        return (today.get(Calendar.YEAR) - birth.get(Calendar.YEAR));
    }
}

package rogatkin.mobile.app.homesafe;

import rogatkin.mobile.data.pertusin.StoreA;

public class NotifTask extends ID {
    @StoreA()
    public long home;
    @StoreA()
    public  String name;
    @StoreA()
    public String address;
    @StoreA()
    public long whenCreated;
    @StoreA()
    public int kind;
    @StoreA()
    public String message;
    @StoreA()
    public long lastFailed;
    @StoreA()
    public String failMessage;
}

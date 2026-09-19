package com.lauriel.mobile.data.api;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
public final class AuthEvents {
    private final MutableLiveData<Boolean> expired=new MutableLiveData<>(false);
    public LiveData<Boolean> expired(){return expired;}
    public void notifyExpired(){expired.postValue(true);}
    public void reset(){expired.setValue(false);}
}

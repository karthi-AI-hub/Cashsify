package com.cashsify.app.ui.ads;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AdsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AdsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Coming Soon...\nWe working on it");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
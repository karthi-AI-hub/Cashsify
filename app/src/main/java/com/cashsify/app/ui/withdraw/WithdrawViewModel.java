package com.cashsify.app.ui.withdraw;



import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WithdrawViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public WithdrawViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Coming Soon...\nWe working on it");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
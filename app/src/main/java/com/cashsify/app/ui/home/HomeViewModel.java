package com.cashsify.app.ui.home;



import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cashsify.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        if (user != null) {
            mText.setValue("This is home fragment" + "\n" + user.getEmail());
        }else{
            mText.setValue("This is home fragment");
        }


    }

    public LiveData<String> getText() {
        return mText;
    }
}
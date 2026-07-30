package com.example.nzreceiptapp.presentation.base;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * 基礎 ViewModel 類別
 */
public abstract class BaseViewModel extends ViewModel {
    protected final MutableLiveData<String> errorMessages = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public LiveData<String> getErrorMessages() {
        return errorMessages;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}

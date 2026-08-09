package com.example.nzreceiptapp;

import android.app.Application;

import com.example.nzreceiptapp.di.AppContainer;

/**
 * Application-level owner for dependencies that should be shared across screens.
 */
public class NzReceiptApplication extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}

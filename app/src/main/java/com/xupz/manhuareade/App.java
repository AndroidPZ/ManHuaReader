package com.xupz.manhuareade;

import android.app.Application;
import android.content.Context;


/**
 * Created by YuZhicong on 2017/5/19.
 */

public class App extends Application {

    public String startTheme;
    public static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

}

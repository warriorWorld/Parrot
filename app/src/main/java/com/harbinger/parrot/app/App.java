package com.harbinger.parrot.app;

import android.app.Application;

import com.harbinger.parrot.config.RooboServiceConfig;

import java.io.File;

/**
 * Created by acorn on 2023/3/10.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        RooboServiceConfig.INSTANCE.setPersistedPcmPath(
//                this.getExternalFilesDir(null).getAbsolutePath()
//                        + File.separator + "Bat"+File.separator);
    }
}

package com.example.swachhtasarthi.config;

import android.app.Application;
import com.imagekit.android.ImageKit;
import com.imagekit.android.entity.TransformationPosition;
import com.imagekit.android.entity.UploadPolicy;

public class ImageKitConfig extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ImageKit
        ImageKit.Companion.init(
                this,
                "public_gO/cfqlTIl+FEMU/cT93KbVza1E=",
                "https://ik.imagekit.io/cx6lz4agm",
                TransformationPosition.PATH,
                new UploadPolicy.Builder()
                        .requireNetworkType(UploadPolicy.NetworkType.ANY)
                        .maxRetries(3)
                        .build()
        );
    }
}

package com.ads.admob;

import androidx.lifecycle.MutableLiveData;

public final class AppUtil {
    public static volatile boolean VARIANT_DEV = false;
    public static volatile double currentTotalRevenue001Ad = 0.0;
    public static final MutableLiveData<String> messageInit = new MutableLiveData<>();

    private AppUtil() {
    }
}

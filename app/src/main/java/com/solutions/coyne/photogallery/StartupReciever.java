package com.solutions.coyne.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.solutions.coyne.photogallery.Service.PollService;

/**
 * Created by Patrick Coyne on 8/16/2017.
 */

public class StartupReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isOn = QueryPreferences.IsAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);

    }
}

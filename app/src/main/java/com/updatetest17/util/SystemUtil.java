package com.updatetest17.util;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.updatetest17.myapplication.R;


public class SystemUtil  {
    //private static Activity mActivity; // 문제가 될수 있다 ...
    private final String TAG = getClass().getName();
    private String mAppLastVersion = null;
    private static SystemUtil mInstance;

    public static synchronized SystemUtil getInstance() {
        if(mInstance == null) {
            mInstance = new SystemUtil();
        }
        return mInstance;
    }

    //나의 어플 최신버전 가져오기
    public String getMyAppVersion(Activity mActivity) {
        String mMyAppVersion = null;
        try {
            PackageInfo mPackageInfo = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
            mMyAppVersion = mPackageInfo.versionName;
        } catch(PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        return mMyAppVersion;
    }

    //마켓의 최신버전 셋팅하기
    public void setAppLastVersion(String mFirebaseVersionKey, AppUpdateManager appUpdateManager, Activity mActivity) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.appupdate);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(mActivity, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Toast.makeText(mActivity, "최선버전에 가져오는데 성공하였습니다.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mActivity, "최선버전에 가져오는데 실패하였습니다.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        String mWelcomeTextView = mFirebaseRemoteConfig.getString("update_message");
                        Boolean st = mFirebaseRemoteConfig.getBoolean("update_message_caps");
                        updateProcess(appUpdateManager, mActivity);//업데이트 실행
                    }
                });

    }

    //현재 버전과 안드로이드 버전 비교하기
    public int compareVersion(Activity mActivity) {
        int mUpdateCheck = -1;
        int IMMEDIATE = 0;
        int FLEXIBLE = 1;

        String mMyAppVersion = getMyAppVersion(mActivity);
        if(mAppLastVersion == null) {
            Toast.makeText(mActivity, "최신버전을 먼저 셋팅해주세요.", Toast.LENGTH_SHORT).show();
            return mUpdateCheck;
        }
        if(!mAppLastVersion.equals(mMyAppVersion)) {
            String [] mLatestVersionArr = mAppLastVersion.split("\\.");
            String [] mMyAppVersionArr = mMyAppVersion.split("\\.");
            if(mLatestVersionArr.length > 0 && !mLatestVersionArr[0].equals(mMyAppVersionArr[0])) {
                //즉시
                mUpdateCheck = IMMEDIATE;
            } else {
                //유연
                mUpdateCheck = FLEXIBLE;
            }
        }
        return mUpdateCheck;
    }

    public void updateProcess(AppUpdateManager appUpdateManager, Activity mActivity) {
        int mUpdateCheck =  compareVersion(mActivity);
        // 업데이트 실행
        if(mUpdateCheck != -1) {
            com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(mUpdateCheck)) {
                    requestUpdate(mActivity, appUpdateInfo, appUpdateManager, mUpdateCheck);
                }
            });

            // 유연업데이트 일때작동
            if(mUpdateCheck == 1) {
                InstallStateUpdatedListener installStateUpdatedListener = installState -> {
                    if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate(appUpdateManager, mActivity);
                    }
                };
                appUpdateManager.registerListener(installStateUpdatedListener);
                appUpdateManager.unregisterListener(installStateUpdatedListener);
            }
        }
    }

    // 업데이트 요청
    private void requestUpdate(Activity mActivity, AppUpdateInfo appUpdateInfo, AppUpdateManager appUpdateManager, int mUpdateCheck) {
        int REQUEST_CODE = 366;
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    mUpdateCheck,
                    mActivity,
                    REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void popupSnackbarForCompleteUpdate(AppUpdateManager appUpdateManager, Activity mActivity) {
        Snackbar snackbar = Snackbar.make(mActivity.findViewById(R.id.main_lay), "An appupdate has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setActionTextColor(mActivity.getResources().getColor(R.color.red));
        snackbar.show();
    }
}

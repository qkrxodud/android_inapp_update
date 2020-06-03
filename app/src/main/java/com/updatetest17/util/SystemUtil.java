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
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.updatetest17.myapplication.BuildConfig;
import com.updatetest17.myapplication.R;

import java.util.concurrent.Executor;


public class SystemUtil {

    private static Activity mActivity;
    private final String TAG = getClass().getName();
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final String LATEST_VERSION_KEY = "latest_version";
    private String mMyAppVersion = "";
    private String mLatestVersion = "";

    //유연 업데이트
    private AppUpdateManager appUpdateManager;
    private int REQUEST_CODE = 366;

    public SystemUtil(Activity activity) {
        this.mActivity = activity;
    }

    public void confirmVersion() {
        getMyAppVersion();
        getAppLastVersion();
    }

    private void getMyAppVersion() {
        mMyAppVersion = null;
        try {
            PackageInfo i = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
            mMyAppVersion = i.versionName;
        } catch(PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    private void getAppLastVersion() {
        //원활한 테스트 진행을 위해 디버그 모드 설정
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        fetchWelcome();
    }

    private void fetchWelcome() {
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(mActivity, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d(TAG, "Config params updated: " + updated);

                        }
                        displayWelcomeMessage();
                    }
                });
    }

    private void displayWelcomeMessage() {
        //받아온 데이터 중 "latest_version" 라는 이름의 매개변수 값을 가져온다.
        mLatestVersion = mFirebaseRemoteConfig.getString(LATEST_VERSION_KEY);
        if(!mLatestVersion.equals(mMyAppVersion)) {
            String [] mLatestVersionArr = mLatestVersion.split("\\.");
            String [] mMyAppVersionArr = mMyAppVersion.split("\\.");
           if(mLatestVersionArr.length > 0 && !mLatestVersionArr[0].equals(mMyAppVersionArr[0])) {
               //즉시

           } else {
               //유연
               flexibleUpdate();
           }
        }
    }

    // 유연업데이트 시작
    private void flexibleUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(mActivity);
        com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                requestUpdate(appUpdateInfo);
            }
        });

        InstallStateUpdatedListener installStateUpdatedListener = installState -> {
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            }
        };
        appUpdateManager.registerListener(installStateUpdatedListener);
        appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    // 업데이트 요청
    private void requestUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    mActivity,
                    REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar = Snackbar.make(mActivity.findViewById(R.id.main_lay), "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setActionTextColor(mActivity.getResources().getColor(R.color.red));
        snackbar.show();
    }


}

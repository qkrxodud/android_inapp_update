package com.updatetest17.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.updatetest17.myapplication.R;


public class SystemUtil  {
    private static Activity mActivity;
    private final String TAG = getClass().getName();
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final String LATEST_VERSION_KEY = "latest_version";
    private String mMyAppVersion = "";
    private String mLatestVersion = "";

    //업데이트
    public AppUpdateManager appUpdateManager;
    private int REQUEST_CODE = 366;
    public int updateGubun = 100;

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
        //업데이트 필요한 변수
        appUpdateManager = AppUpdateManagerFactory.create(mActivity);
        //받아온 데이터 중 "latest_version" 라는 이름의 매개변수 값을 가져온다.
        mLatestVersion = mFirebaseRemoteConfig.getString(LATEST_VERSION_KEY);
        if(!mLatestVersion.equals(mMyAppVersion)) {
            String [] mLatestVersionArr = mLatestVersion.split("\\.");
            String [] mMyAppVersionArr = mMyAppVersion.split("\\.");
           if(mLatestVersionArr.length > 0 && !mLatestVersionArr[0].equals(mMyAppVersionArr[0])) {
               //즉시
               updateGubun = 0;
           } else {
               //유연
               updateGubun = 1;
           }
            updateProcess();
        }
    }


    private void updateProcess() {
        // 업데이트 실행
        com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(updateGubun)) {
                requestUpdate(appUpdateInfo);
            }
        });

        //유연업데이트 일때작동
        if(updateGubun == 1) {
            InstallStateUpdatedListener installStateUpdatedListener = installState -> {
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate();
                }
            };
            appUpdateManager.registerListener(installStateUpdatedListener);
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }

    // 업데이트 요청
    public void requestUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateGubun,
                    mActivity,
                    REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar = Snackbar.make(mActivity.findViewById(R.id.main_lay), "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setActionTextColor(mActivity.getResources().getColor(R.color.red));
        snackbar.show();
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            // 업데이트가 성공적으로 끝나지 않은 경우
            if (resultCode != RESULT_OK) {
                com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
                appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(updateGubun)) {
                        //실패할경우 지속적으로 뜨는경우....
                        requestUpdate(appUpdateInfo);
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        if(updateGubun == 0) {
                            requestUpdate(appUpdateInfo);
                        } else {
                            popupSnackbarForCompleteUpdate();
                        }
                    }
                });
    }*/
}

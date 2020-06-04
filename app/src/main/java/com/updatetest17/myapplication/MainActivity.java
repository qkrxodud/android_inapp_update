package com.updatetest17.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.updatetest17.util.SystemUtil;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private SystemUtil mSystemUtil ;
    private AppUpdateManager mAppUpdateManager;
    private int updateGubun = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        mSystemUtil = new SystemUtil(this);
        mSystemUtil.setAppLastVersion("latest_version", mAppUpdateManager);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int REQUEST_CODE = 366;
        if (requestCode == REQUEST_CODE) {
            // 업데이트가 성공적으로 끝나지 않은 경우
            if (resultCode != RESULT_OK) {
                Task<AppUpdateInfo> appUpdateInfoTask = mAppUpdateManager.getAppUpdateInfo();
                appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                    updateGubun = mSystemUtil.compareVersion();
                    if(updateGubun != -1) {
                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                                && appUpdateInfo.isUpdateTypeAllowed(updateGubun)) {
                            //실패할경우 지속적으로 뜨는경우
                            mSystemUtil.updateProcess(mAppUpdateManager);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        updateGubun = mSystemUtil.compareVersion();
                        if(updateGubun == 0) {
                            mSystemUtil.updateProcess(mAppUpdateManager);
                        } else if (updateGubun == 1){
                            mSystemUtil.popupSnackbarForCompleteUpdate(mAppUpdateManager);
                        }
                    }
                });
    }
}

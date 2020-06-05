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
    private AppUpdateManager mAppUpdateManager;
    private int updateGubun = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        SystemUtil.getInstance().setAppLastVersion("latest_version", mAppUpdateManager, this);
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
                    updateGubun =  SystemUtil.getInstance().compareVersion(this);
                    if(updateGubun != -1) {
                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                                && appUpdateInfo.isUpdateTypeAllowed(updateGubun)) {
                            //실패할경우 지속적으로 뜨는경우
                            SystemUtil.getInstance().updateProcess(mAppUpdateManager, this);
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
                        updateGubun = SystemUtil.getInstance().compareVersion(this);
                        if(updateGubun == 0) {
                            SystemUtil.getInstance().updateProcess(mAppUpdateManager, this);
                        } else if (updateGubun == 1){
                            SystemUtil.getInstance().popupSnackbarForCompleteUpdate(mAppUpdateManager, this);
                        }
                    }
                });
    }
}

package com.updatetest17.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.updatetest17.util.SystemUtil;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private Context mContext;
    private int REQUEST_CODE = 366;
    private AppUpdateManager appUpdateManager;
    private SystemUtil sm ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        sm = new SystemUtil(this);
        sm.confirmVersion();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            // 업데이트가 성공적으로 끝나지 않은 경우
            if (resultCode != RESULT_OK) {
                com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = sm.appUpdateManager.getAppUpdateInfo();
                appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(sm.updateGubun)) {
                        //실패할경우 지속적으로 뜨는경우....
                        sm.requestUpdate(appUpdateInfo);
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        if(sm.updateGubun == 0) {
                            sm.requestUpdate(appUpdateInfo);
                        } else {
                            sm.popupSnackbarForCompleteUpdate();
                        }
                    }
                });
    }
}

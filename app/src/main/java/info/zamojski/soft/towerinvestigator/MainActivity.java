/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package info.zamojski.soft.towerinvestigator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import info.zamojski.soft.towerinvestigator.utils.DeviceUtils;
import info.zamojski.soft.towerinvestigator.utils.PermissionUtils;
import info.zamojski.soft.towerinvestigator.utils.ReflectionUtils;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mInvestigationResultTextView;

    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private CellLocation mCellLocation;
    private List<NeighboringCellInfo> mNeighboringCellInfos;
    private SignalStrength mSignalStrength;
    private String mInvestigationResult = "";
    private boolean mLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mInvestigationResultTextView = (TextView) findViewById(R.id.tv_investigation_result);

        FloatingActionButton fabSend = (FloatingActionButton) findViewById(R.id.fab_send);
        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(mInvestigationResult)) {
                    Toast.makeText(MainActivity.this, R.string.loading_in_progress, Toast.LENGTH_SHORT).show();
                } else {
                    sendResults();
                    finish();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        MainActivityPermissionsDispatcher.setupPhoneStateListenerWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void setupPhoneStateListener() {
        mPhoneStateListener = new PhoneStateListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCellLocationChanged(CellLocation cellLocation) {
                if (mLoaded)
                    return;
                mCellLocation = cellLocation;
                mNeighboringCellInfos = mTelephonyManager.getNeighboringCellInfo();
                load();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                if (mLoaded)
                    return;
                mSignalStrength = signalStrength;
                load();
            }
        };

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    @OnShowRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
    void onStartExportShowRationale(PermissionRequest request) {
        onShowRationale(request, R.string.permission_rationale_message);
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION)
    void onStartExportPermissionDenied() {
        onPermissionDenied(R.string.permission_denied_message);
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_COARSE_LOCATION)
    void onStartExportNeverAskAgain() {
        onNeverAskAgain(R.string.permission_never_ask_again_message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void onShowRationale(final PermissionRequest request, @StringRes int messageResId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(messageResId)
                .setCancelable(true)
                .setPositiveButton(R.string.dialog_proceed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    private void onPermissionDenied(@StringRes int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
    }

    private void onNeverAskAgain(@StringRes int messageResId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_denied)
                .setMessage(messageResId)
                .setCancelable(true)
                .setPositiveButton(R.string.dialog_permission_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionUtils.openAppSettings(MainActivity.this);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void load() {
        if (mCellLocation == null || mSignalStrength == null)
            return;
        new ReflectionTask().execute();
    }

    private void sendResults() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildConfig.EMAIL_ADDRESS});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Tower Investigator report");
        intent.putExtra(Intent.EXTRA_TEXT, mInvestigationResult);
        startActivity(Intent.createChooser(intent, getString(R.string.send_results)));
    }

    private class ReflectionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void[] objects) {
            String deviceInfo = DeviceUtils.dumpDeviceInfo();
            String telephonyManagerInfo = ReflectionUtils.dumpClass(TelephonyManager.class, MainActivity.this.mTelephonyManager);
            String cellLocationInfo = ReflectionUtils.dumpClass(MainActivity.this.mCellLocation.getClass(), MainActivity.this.mCellLocation);
            String cellSignalStrengthInfo = ReflectionUtils.dumpClass(SignalStrength.class, MainActivity.this.mSignalStrength);
            String neighboringCellInfo = ReflectionUtils.dumpClasses(NeighboringCellInfo.class, MainActivity.this.mNeighboringCellInfos.toArray());

            return deviceInfo
                    + cellLocationInfo
                    + neighboringCellInfo
                    + cellSignalStrengthInfo
                    + telephonyManagerInfo;
        }

        @Override
        protected void onPostExecute(String result) {
            mLoaded = true;
            mInvestigationResult = result;
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mInvestigationResultTextView.setText(mInvestigationResult);
            Toast.makeText(MainActivity.this, R.string.loading_completed, Toast.LENGTH_SHORT).show();
        }
    }
}

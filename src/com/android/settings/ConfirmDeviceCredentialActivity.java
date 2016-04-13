
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

/**
 * Launch this when you want to confirm the user is present by asking them to enter their
 * PIN/password/pattern.
 */
public class ConfirmDeviceCredentialActivity extends Activity {
    public static final String TAG = ConfirmDeviceCredentialActivity.class.getSimpleName();

    public static class InternalActivity extends ConfirmDeviceCredentialActivity {
    }

    public static Intent createIntent(CharSequence title, CharSequence details) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                ConfirmDeviceCredentialActivity.class.getName());
        intent.putExtra(KeyguardManager.EXTRA_TITLE, title);
        intent.putExtra(KeyguardManager.EXTRA_DESCRIPTION, details);
        return intent;
    }

    public static Intent createIntent(CharSequence title, CharSequence details, long challenge) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                ConfirmDeviceCredentialActivity.class.getName());
        intent.putExtra(KeyguardManager.EXTRA_TITLE, title);
        intent.putExtra(KeyguardManager.EXTRA_DESCRIPTION, details);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String title = intent.getStringExtra(KeyguardManager.EXTRA_TITLE);
        String details = intent.getStringExtra(KeyguardManager.EXTRA_DESCRIPTION);
        int userId = Utils.getCredentialOwnerUserId(this);
        if (isInternalActivity()) {
            try {
                userId = Utils.getUserIdFromBundle(this, intent.getExtras());
            } catch (SecurityException se) {
                Log.e(TAG, "Invalid intent extra", se);
            }
        }
        // if the client app did not hand in a title and we are about to show the work challenge,
        // check whether there is a policy setting the organization name and use that as title
        if ((title == null) && Utils.isManagedProfile(UserManager.get(this), userId)) {
            title = getTitleFromOrganizationName(userId);
        }
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        if (!helper.launchConfirmationActivity(0 /* request code */, null /* title */, title,
                details, false /* returnCredentials */, true /* isExternal */, userId)) {
            Log.d(TAG, "No pattern, password or PIN set.");
            setResult(Activity.RESULT_OK);
        }
        finish();
    }

    private boolean isInternalActivity() {
        return this instanceof ConfirmDeviceCredentialActivity.InternalActivity;
    }

    private String getTitleFromOrganizationName(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        CharSequence organizationNameForUser = (dpm != null)
                ? dpm.getOrganizationNameForUser(userId) : null;
        return organizationNameForUser != null ? organizationNameForUser.toString() : null;
    }
}

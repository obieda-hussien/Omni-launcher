/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.gestures.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import app.lawnchair.lawnchairApp
import app.lawnchair.views.ComposeBottomSheet
import com.android.launcher3.R

class RecentsGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        val app = launcher.lawnchairApp
        if (!app.isAccessibilityServiceBound()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ComposeBottomSheet.show(launcher) {
                ServiceWarningDialog(
                    title = R.string.d2ts_recents_a11y_hint_title,
                    description = R.string.recents_a11y_hint,
                    settingsIntent = intent,
                ) { close(true) }
            }
            return
        }
        if (!app.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)) {
            // SystemUI owns this action on many OEM Android builds; do not loop/restart.
            Log.w("OmniRecents", "System rejected GLOBAL_ACTION_RECENTS; check SystemUI/Quickstep logs")
            Toast.makeText(launcher, R.string.omni_recents_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}

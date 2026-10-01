/*
 * Copyright 2026 Abdelrahman Hussein (عبدالرحمن حسين)
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

package app.lawnchair.omni

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import com.android.launcher3.R

@Composable
fun OmniIntegrationPreferences() {
    val prefs = preferenceManager()
    PreferenceGroup(heading = stringResource(R.string.omni_integration)) {
        SwitchPreference(
            adapter = prefs.omniSearchSuggestions.getAdapter(),
            label = stringResource(R.string.omni_suggestions),
            description = stringResource(R.string.omni_suggestions_description),
        )
        SwitchPreference(
            adapter = prefs.omniAlwaysSuggest.getAdapter(),
            label = stringResource(R.string.omni_always_suggest),
            enabled = prefs.omniSearchSuggestions.getAdapter().state.value,
        )
        SwitchPreference(
            adapter = prefs.omniRemoteControl.getAdapter(),
            label = stringResource(R.string.omni_remote_control),
            description = stringResource(R.string.omni_remote_control_description),
        )
    }
}

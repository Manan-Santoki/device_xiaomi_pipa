/*
 * Copyright (C) 2026 MufasaXz
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.pipacore;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.service.quicksettings.TileService;
import android.util.Log;
import androidx.preference.PreferenceManager;

public final class ThermalManager {

    private static final String TAG = "ThermalManager";

    public static final String PREF_KEY_ENABLED = "pipacore_thermal_enabled";
    public static final String PREF_KEY_PROFILE = "pipacore_thermal_profile";
    public static final String PROP_PIPACORE_PROFILE = "sys.pipacore.profile";

    // 3 Strict Modes
    public static final int PROFILE_BALANCED = 0;
    public static final int PROFILE_BATTERY = 1;
    public static final int PROFILE_PERFORMANCE = 10;

    private ThermalManager() {}

    /**
     * Check if thermal profile management is enabled (Default is OFF / false)
     */
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_KEY_ENABLED, false);
    }

    /**
     * Enable or disable custom thermal profile management
     */
    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // commit() instead of apply(): the QS tile may be refreshed immediately below and
        // must observe the new value synchronously.
        prefs.edit().putBoolean(PREF_KEY_ENABLED, enabled).commit();

        if (enabled) {
            applyProfile(getProfile(context));
        } else {
            // Request Balanced tuning; this is not a restore of prior hardware state.
            applyProfile(PROFILE_BALANCED);
        }

        refreshTile(context);
    }

    /**
     * Get current active profile from SharedPreferences with system property fallback
     */
    public static int getProfile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int defaultVal = PROFILE_BALANCED;
        try {
            String prop = SystemProperties.get(PROP_PIPACORE_PROFILE, "0");
            defaultVal = Integer.parseInt(prop.trim());
        } catch (Exception ignored) {}
        int profile;
        try {
            profile = prefs.getInt(PREF_KEY_PROFILE, defaultVal);
        } catch (ClassCastException e) {
            Log.w(TAG, "Invalid saved profile type; using Balanced", e);
            profile = PROFILE_BALANCED;
        }
        return isValidProfile(profile) ? profile : PROFILE_BALANCED;
    }

    private static boolean isValidProfile(int profile) {
        return profile == PROFILE_BALANCED || profile == PROFILE_BATTERY
                || profile == PROFILE_PERFORMANCE;
    }

    /**
     * Save the selected tuning profile and request it through the init bridge.
     */
    public static void setProfile(Context context, int profile) {
        if (!isValidProfile(profile)) {
            Log.w(TAG, "Ignoring unsupported profile " + profile);
            return;
        }
        // 1. Update shared SharedPreferences value
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREF_KEY_PROFILE, profile).commit();

        // 2. Apply backend hardware & system property changes if master toggle is enabled
        if (isEnabled(context)) {
            applyProfile(profile);
        }

        refreshTile(context);
    }

    /**
     * Request init tuning; PowerHAL may subsequently update CPU policy limits.
     * Vendor thermal scenarios remain owned by XiaomiParts and mi_thermald.
     */
    public static void applyProfile(int profile) {
        if (!isValidProfile(profile)) {
            Log.w(TAG, "Ignoring unsupported profile " + profile);
            return;
        }
        final String profileVal = String.valueOf(profile);

        // A property request is not confirmation that sysfs tuning succeeded.
        try {
            SystemProperties.set(PROP_PIPACORE_PROFILE, profileVal);
            Log.i(TAG, "Requested profile " + profileVal + " -> " + PROP_PIPACORE_PROFILE + "=" + profileVal);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set SystemProperties: " + PROP_PIPACORE_PROFILE + "=" + profileVal, e);
        }
    }

    /**
     * Ask the Quick Settings tile to re-read its state.
     *
     * A TileService is only re-rendered when the system calls onStartListening().
     * Changing the master toggle or the active profile from the app does not notify
     * the tile on its own, so without this the tile keeps showing its previous
     * (stale) state -- e.g. it stays highlighted after the toggle is turned off,
     * or shows the wrong profile icon after the profile is changed.
     */
    public static void refreshTile(Context context) {
        try {
            TileService.requestListeningState(
                    context, new ComponentName(context, ThermalProfileTileService.class));
        } catch (Exception e) {
            Log.e(TAG, "Failed to request QS tile refresh", e);
        }
    }

    /**
     * Restores saved profile on device boot
     */
    public static void restoreProfile(Context context) {
        if (isEnabled(context)) {
            int currentProfile = getProfile(context);
            applyProfile(currentProfile);
        } else {
            applyProfile(PROFILE_BALANCED);
        }
    }
}

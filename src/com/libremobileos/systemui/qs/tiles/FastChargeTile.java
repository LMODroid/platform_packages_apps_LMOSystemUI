/*
 * Copyright (C) 2020 The LineageOS Project
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

package com.libremobileos.systemui.qs.tiles;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.systemui.R;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.UserSettingObserver;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.settings.SystemSettings;

import com.libremobileos.health.HealthInterface;
import com.libremobileos.providers.LMOSettings;

import javax.inject.Inject;

import vendor.lineage.health.FastChargeMode;

public class FastChargeTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "fastcharge";

    @Nullable
    private Icon mIcon = null;

    private HealthInterface mHealthInterface;
    private final UserSettingObserver mSetting;

    @Inject
    public FastChargeTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            UserTracker userTracker,
            SystemSettings systemSettings
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mHealthInterface = HealthInterface.getInstance(mContext);
        mSetting = new UserSettingObserver(systemSettings, mHandler,
                LMOSettings.System.FAST_CHARGE_MODE, userTracker.getUserId(),
                mHealthInterface.getFastChargeMode()) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return mHealthInterface.isFastChargeSupported();
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    public void handleClick(@Nullable Expandable expandable) {
        boolean fastChargeEnabled = mSetting.getValue() != FastChargeMode.NONE;
        if (mHealthInterface.setFastChargeMode(
                fastChargeEnabled ? FastChargeMode.NONE : FastChargeMode.FAST_CHARGE
            )) {
            refreshState();
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_fastcharge_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (!isAvailable()) {
            return;
        }

        if (mIcon == null) {
            mIcon = maybeLoadResourceIcon(R.drawable.ic_qs_fastcharge);
        }
        state.icon = mIcon;
        final int value = arg instanceof Integer ? (Integer) arg : mSetting.getValue();
        state.value = value != FastChargeMode.NONE;
        state.label = mContext.getString(R.string.quick_settings_fastcharge_label);

        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;

    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LMODROID;
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mSetting.setListening(listening);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        mSetting.setUserId(newUserId);
        handleRefreshState(mSetting.getValue());
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mSetting.setListening(false);
    }
}

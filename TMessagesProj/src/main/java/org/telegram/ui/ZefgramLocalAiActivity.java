/*
 * This file is part of zefgram, a fork of Telegram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright webarmour, 2026.
 */

package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.asr.ZefgramAsrModelManager;
import org.telegram.messenger.asr.ZefgramAsrModels;
import org.telegram.messenger.asr.ZefgramAsrSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

/**
 * Settings → Local AI: the neural network features that run on the device, one section each (for now voice and
 * video message transcription), and the space their models take.
 */
public class ZefgramLocalAiActivity extends BaseFragment implements ZefgramAsrModelManager.Listener {

    private static final int TYPE_HEADER = 0, TYPE_FEATURE = 1, TYPE_INFO = 2, TYPE_SHADOW = 3, TYPE_SETTING = 4;

    private RecyclerListView listView;
    private ListAdapter adapter;

    private int rowCount;
    private int voiceHeaderRow, voiceRow, voiceInfoRow;
    private int storageHeaderRow, storageRow, deleteAllRow, storageInfoRow;

    private void updateRows() {
        rowCount = 0;
        voiceHeaderRow = rowCount++;
        voiceRow = rowCount++;
        voiceInfoRow = rowCount++;
        storageHeaderRow = rowCount++;
        storageRow = rowCount++;
        deleteAllRow = rowCount++;
        storageInfoRow = rowCount++;
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows();
        ZefgramAsrModelManager.addListener(this);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        ZefgramAsrModelManager.removeListener(this);
        super.onFragmentDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAsrModelChanged(String modelId) {
        ZefgramAsrModels.Model model = ZefgramAsrModels.byId(modelId);
        int state = model == null ? -1 : ZefgramAsrModelManager.getState(model);
        if (adapter != null && (state == ZefgramAsrModelManager.STATE_READY || state == ZefgramAsrModelManager.STATE_NONE)) {
            adapter.notifyItemChanged(voiceRow);
            adapter.notifyItemChanged(storageRow);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.ZefgramLocalAiTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setVerticalScrollBarEnabled(false);
        root.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == voiceRow) {
                presentFragment(new ZefgramAsrSettingsActivity());
            } else if (position == deleteAllRow) {
                if (ZefgramAsrModelManager.getUsedSpace() == 0) {
                    return;
                }
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(LocaleController.getString(R.string.ZefgramAsrDeleteAll))
                        .setMessage(LocaleController.getString(R.string.ZefgramAsrDeleteAllConfirm))
                        .setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
                            ZefgramAsrModelManager.deleteAll();
                            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                                if (UserConfig.getInstance(a).isClientActivated()) {
                                    NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                                }
                            }
                        })
                        .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                        .create();
                showDialog(dialog);
                dialog.redPositive();
            }
        });

        fragmentView = root;
        return root;
    }

    /** "Off", "No model downloaded" or the downloaded models in use, e.g. "GigaAM v3, Moonshine Base". */
    private static String voiceStatus() {
        if (!ZefgramAsrSettings.isEnabled()) {
            return LocaleController.getString(R.string.ZefgramLocalAiOff);
        }
        StringBuilder sb = new StringBuilder();
        for (String lang : new String[]{ZefgramAsrModels.LANG_RU, ZefgramAsrModels.LANG_EN}) {
            ZefgramAsrModels.Model m = ZefgramAsrModelManager.readyModelFor(lang);
            if (m != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(m.title);
            }
        }
        return sb.length() > 0 ? sb.toString() : LocaleController.getString(R.string.ZefgramLocalAiNoModels);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == voiceRow || position == deleteAllRow;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == voiceHeaderRow || position == storageHeaderRow) return TYPE_HEADER;
            if (position == voiceRow) return TYPE_FEATURE;
            if (position == storageRow || position == deleteAllRow) return TYPE_SETTING;
            if (position == voiceInfoRow || position == storageInfoRow) return TYPE_INFO;
            return TYPE_SHADOW;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_HEADER:
                    view = new HeaderCell(context);
                    break;
                case TYPE_FEATURE:
                    view = new TextCell(context);
                    break;
                case TYPE_SETTING:
                    view = new TextSettingsCell(context);
                    break;
                case TYPE_INFO:
                    view = new TextInfoPrivacyCell(context);
                    break;
                case TYPE_SHADOW:
                default:
                    view = new ShadowSectionCell(context);
                    break;
            }
            if (viewType != TYPE_INFO && viewType != TYPE_SHADOW) {
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER:
                    ((HeaderCell) holder.itemView).setText(LocaleController.getString(position == voiceHeaderRow
                            ? R.string.ZefgramLocalAiVoiceHeader : R.string.ZefgramLocalAiStorageHeader));
                    break;
                case TYPE_FEATURE: {
                    TextCell cell = (TextCell) holder.itemView;
                    cell.setTextAndValueAndIcon(LocaleController.getString(R.string.ZefgramAsrTitle), voiceStatus(), R.drawable.settings_asr, false);
                    cell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    break;
                }
                case TYPE_SETTING: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    if (position == storageRow) {
                        cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        cell.setTextAndValue(LocaleController.getString(R.string.ZefgramAsrStorage),
                                AndroidUtilities.formatFileSize(ZefgramAsrModelManager.getUsedSpace()), true);
                    } else {
                        cell.setText(LocaleController.getString(R.string.ZefgramAsrDeleteAll), false);
                        cell.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
                    }
                    break;
                }
                case TYPE_INFO:
                    ((TextInfoPrivacyCell) holder.itemView).setText(LocaleController.getString(position == voiceInfoRow
                            ? R.string.ZefgramLocalAiVoiceInfo : R.string.ZefgramLocalAiStorageInfo));
                    break;
            }
        }
    }
}

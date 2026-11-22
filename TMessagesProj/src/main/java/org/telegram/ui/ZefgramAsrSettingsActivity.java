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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.asr.ZefgramAsrModelManager;
import org.telegram.messenger.asr.ZefgramAsrModels;
import org.telegram.messenger.asr.ZefgramAsrSettings;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ZefgramAsrModelCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.List;

/**
 * Settings → Voice transcription: on-device speech recognition on/off, the downloadable models of each language
 * (choose, download, cancel, delete) and the language and performance options. Opened from Local AI.
 */
public class ZefgramAsrSettingsActivity extends BaseFragment implements ZefgramAsrModelManager.Listener {

    private static final int TYPE_HEADER = 0, TYPE_CHECK = 1, TYPE_INFO = 2, TYPE_SHADOW = 3, TYPE_MODEL = 4, TYPE_SETTING = 5;

    private RecyclerListView listView;
    private ListAdapter adapter;

    private final List<ZefgramAsrModels.Model> ruModels = ZefgramAsrModels.forLang(ZefgramAsrModels.LANG_RU);
    private final List<ZefgramAsrModels.Model> enModels = ZefgramAsrModels.forLang(ZefgramAsrModels.LANG_EN);
    private final List<ZefgramAsrModels.Model> lidModels = ZefgramAsrModels.forLang(ZefgramAsrModels.LANG_ID);

    private int rowCount;
    private int enableRow, enableInfoRow;
    private int ruHeaderRow, ruStartRow, ruShadowRow;
    private int enHeaderRow, enStartRow, enInfoRow;
    private int lidHeaderRow, lidStartRow, lidInfoRow;
    private int settingsHeaderRow, langRow, perfRow, hapticRow, wifiRow, langInfoRow;

    private void updateRows() {
        rowCount = 0;
        enableRow = rowCount++;
        enableInfoRow = rowCount++;
        ruHeaderRow = rowCount++;
        ruStartRow = rowCount;
        rowCount += ruModels.size();
        ruShadowRow = rowCount++;
        enHeaderRow = rowCount++;
        enStartRow = rowCount;
        rowCount += enModels.size();
        enInfoRow = rowCount++;
        lidHeaderRow = rowCount++;
        lidStartRow = rowCount;
        rowCount += lidModels.size();
        lidInfoRow = rowCount++;
        settingsHeaderRow = rowCount++;
        langRow = rowCount++;
        perfRow = rowCount++;
        hapticRow = rowCount++;
        wifiRow = rowCount++;
        langInfoRow = rowCount++;
    }

    private ZefgramAsrModels.Model modelAt(int position) {
        if (position >= ruStartRow && position < ruStartRow + ruModels.size()) {
            return ruModels.get(position - ruStartRow);
        }
        if (position >= enStartRow && position < enStartRow + enModels.size()) {
            return enModels.get(position - enStartRow);
        }
        if (position >= lidStartRow && position < lidStartRow + lidModels.size()) {
            return lidModels.get(position - lidStartRow);
        }
        return null;
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows();
        ZefgramAsrModelManager.addListener(this);
        ZefgramAsrModelManager.checkUpdates();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        ZefgramAsrModelManager.removeListener(this);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.ZefgramAsrTitle));
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
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(animator);
        root.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            ZefgramAsrModels.Model model = modelAt(position);
            if (model != null) {
                if (!ZefgramAsrModels.LANG_ID.equals(model.lang)) {
                    selectModel(model);
                }
                if (ZefgramAsrModelManager.getState(model) == ZefgramAsrModelManager.STATE_NONE) {
                    startDownload(model);
                }
            } else if (position == enableRow) {
                boolean next = !ZefgramAsrSettings.isEnabled();
                ZefgramAsrSettings.setEnabled(next);
                ((TextCheckCell) view).setChecked(next);
                notifyChats();
            } else if (position == hapticRow) {
                boolean next = !ZefgramAsrSettings.isRevealHaptic();
                ZefgramAsrSettings.setRevealHaptic(next);
                ((TextCheckCell) view).setChecked(next);
                if (next) {
                    view.performHapticFeedback(android.os.Build.VERSION.SDK_INT >= 30 ? android.view.HapticFeedbackConstants.CONFIRM
                            : android.view.HapticFeedbackConstants.KEYBOARD_TAP, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
            } else if (position == wifiRow) {
                boolean next = !ZefgramAsrSettings.isWifiOnly();
                ZefgramAsrSettings.setWifiOnly(next);
                ((TextCheckCell) view).setChecked(next);
            } else if (position == langRow) {
                CharSequence[] items = {
                        LocaleController.getString(R.string.ZefgramAsrLangAuto),
                        LocaleController.getString(R.string.ZefgramAsrRussian),
                        LocaleController.getString(R.string.ZefgramAsrEnglish)
                };
                showChooser(LocaleController.getString(R.string.ZefgramAsrLanguage), items, which -> {
                    ZefgramAsrSettings.setLangMode(which);
                    adapter.notifyItemChanged(langRow);
                });
            } else if (position == perfRow) {
                CharSequence[] items = {
                        LocaleController.getString(R.string.ZefgramAsrPerfGentle),
                        LocaleController.getString(R.string.ZefgramAsrPerfEconomy),
                        LocaleController.getString(R.string.ZefgramAsrPerfFast)
                };
                final int[] values = {ZefgramAsrSettings.PERF_GENTLE, ZefgramAsrSettings.PERF_BALANCED, ZefgramAsrSettings.PERF_FAST};
                showChooser(LocaleController.getString(R.string.ZefgramAsrPerformance), items, which -> {
                    ZefgramAsrSettings.setPerformance(values[which]);
                    adapter.notifyItemChanged(perfRow);
                });
            }
        });

        fragmentView = root;
        return root;
    }

    private void showChooser(CharSequence title, CharSequence[] items, Utilities.Callback<Integer> onSelect) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setItems(items, (dialog, which) -> onSelect.run(which));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void selectModel(ZefgramAsrModels.Model model) {
        ZefgramAsrSettings.setSelectedModelId(model.lang, model.id);
        List<ZefgramAsrModels.Model> list = model.lang.equals(ZefgramAsrModels.LANG_RU) ? ruModels : enModels;
        for (ZefgramAsrModels.Model m : list) {
            updateModelCell(m);
        }
    }

    private void onModelAction(ZefgramAsrModels.Model model, int state) {
        if (state == ZefgramAsrModelManager.STATE_NONE) {
            if (!ZefgramAsrModels.LANG_ID.equals(model.lang)) {
                selectModel(model);
            }
            startDownload(model);
        } else if (state == ZefgramAsrModelManager.STATE_UPDATE) {
            startDownload(model);
        } else if (state == ZefgramAsrModelManager.STATE_READY) {
            AlertDialog dialog = new AlertDialog.Builder(getParentActivity())
                    .setTitle(LocaleController.getString(R.string.Delete))
                    .setMessage(LocaleController.formatString(R.string.ZefgramAsrDeleteModel, model.title))
                    .setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
                        ZefgramAsrModelManager.delete(model);
                        notifyChats();
                    })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .create();
            showDialog(dialog);
            dialog.redPositive();
        } else {
            ZefgramAsrModelManager.cancel(model);
        }
    }

    private void startDownload(ZefgramAsrModels.Model model) {
        if (ZefgramAsrModelManager.download(model, false) == ZefgramAsrModelManager.ERROR_WIFI_REQUIRED) {
            AlertDialog dialog = new AlertDialog.Builder(getParentActivity())
                    .setTitle(LocaleController.getString(R.string.ZefgramAsrMeteredTitle))
                    .setMessage(LocaleController.formatString(R.string.ZefgramAsrMeteredText, AndroidUtilities.formatFileSize(ZefgramAsrModelManager.downloadSize(model))))
                    .setPositiveButton(LocaleController.getString(R.string.ZefgramAsrDownload), (d, w) -> ZefgramAsrModelManager.download(model, true))
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .create();
            showDialog(dialog);
        }
    }

    @Override
    public void onAsrModelChanged(String modelId) {
        ZefgramAsrModels.Model model = ZefgramAsrModels.byId(modelId);
        if (model == null || listView == null) {
            return;
        }
        int state = ZefgramAsrModelManager.getState(model);
        if (state == ZefgramAsrModelManager.STATE_READY || state == ZefgramAsrModelManager.STATE_NONE) {
            notifyChats(); // the transcribe button appears / disappears in chats
        }
        updateModelCell(model);
    }

    private void updateModelCell(ZefgramAsrModels.Model model) {
        if (listView == null) {
            return;
        }
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof ZefgramAsrModelCell && ((ZefgramAsrModelCell) child).getModel() == model) {
                int position = listView.getChildAdapterPosition(child);
                ((ZefgramAsrModelCell) child).set(model, hasDivider(position), true);
            }
        }
    }

    private boolean hasDivider(int position) {
        return position != ruStartRow + ruModels.size() - 1 && position != enStartRow + enModels.size() - 1
                && position != lidStartRow + lidModels.size() - 1;
    }

    private void notifyChats() {
        for (int a = 0; a < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (org.telegram.messenger.UserConfig.getInstance(a).isClientActivated()) {
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == TYPE_CHECK || type == TYPE_MODEL || type == TYPE_SETTING;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (modelAt(position) != null) return TYPE_MODEL;
            if (position == ruHeaderRow || position == enHeaderRow || position == lidHeaderRow || position == settingsHeaderRow) return TYPE_HEADER;
            if (position == enableRow || position == hapticRow || position == wifiRow) return TYPE_CHECK;
            if (position == langRow || position == perfRow) return TYPE_SETTING;
            if (position == enableInfoRow || position == enInfoRow || position == lidInfoRow || position == langInfoRow) return TYPE_INFO;
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
                case TYPE_CHECK:
                    view = new TextCheckCell(context);
                    break;
                case TYPE_INFO:
                    view = new TextInfoPrivacyCell(context);
                    break;
                case TYPE_MODEL: {
                    ZefgramAsrModelCell cell = new ZefgramAsrModelCell(context);
                    cell.setDelegate(ZefgramAsrSettingsActivity.this::onModelAction);
                    view = cell;
                    break;
                }
                case TYPE_SETTING:
                    view = new TextSettingsCell(context);
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
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == ruHeaderRow) {
                        cell.setText(LocaleController.getString(R.string.ZefgramAsrRussian));
                    } else if (position == enHeaderRow) {
                        cell.setText(LocaleController.getString(R.string.ZefgramAsrEnglish));
                    } else if (position == lidHeaderRow) {
                        cell.setText(LocaleController.getString(R.string.ZefgramAsrLidHeader));
                    } else {
                        cell.setText(LocaleController.getString(R.string.ZefgramAsrSettingsHeader));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == enableRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.ZefgramAsrEnable), ZefgramAsrSettings.isEnabled(), false);
                    } else if (position == hapticRow) {
                        cell.setTextAndValueAndCheck(LocaleController.getString(R.string.ZefgramAsrHaptic),
                                LocaleController.getString(R.string.ZefgramAsrHapticInfo), ZefgramAsrSettings.isRevealHaptic(), true, true);
                    } else if (position == wifiRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.ZefgramAsrWifiOnly), ZefgramAsrSettings.isWifiOnly(), false);
                    }
                    break;
                }
                case TYPE_MODEL:
                    ((ZefgramAsrModelCell) holder.itemView).set(modelAt(position), hasDivider(position), false);
                    break;
                case TYPE_SETTING: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == langRow) {
                        int mode = ZefgramAsrSettings.getLangMode();
                        cell.setTextAndValue(LocaleController.getString(R.string.ZefgramAsrLanguage), LocaleController.getString(
                                mode == ZefgramAsrSettings.LANG_RU ? R.string.ZefgramAsrRussian
                                        : mode == ZefgramAsrSettings.LANG_EN ? R.string.ZefgramAsrEnglish : R.string.ZefgramAsrLangAuto), true);
                    } else if (position == perfRow) {
                        cell.setTextAndValue(LocaleController.getString(R.string.ZefgramAsrPerformance), LocaleController.getString(
                                ZefgramAsrSettings.getPerformance() == ZefgramAsrSettings.PERF_FAST ? R.string.ZefgramAsrPerfFast
                                        : ZefgramAsrSettings.isGentle() ? R.string.ZefgramAsrPerfGentle : R.string.ZefgramAsrPerfEconomy), true);
                    }
                    break;
                }
                case TYPE_INFO: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    int res;
                    if (position == enableInfoRow) {
                        res = R.string.ZefgramAsrEnableInfo;
                    } else if (position == enInfoRow) {
                        res = R.string.ZefgramAsrModelsInfo;
                    } else if (position == lidInfoRow) {
                        res = R.string.ZefgramAsrLidInfo;
                    } else {
                        cell.setText(LocaleController.getString(R.string.ZefgramAsrLanguageInfo) + "\n\n" + LocaleController.getString(R.string.ZefgramAsrPerfInfo));
                        break;
                    }
                    cell.setText(LocaleController.getString(res));
                    break;
                }
            }
        }
    }
}

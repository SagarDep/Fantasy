package com.wosloveslife.fantasy.manager;

import android.content.Context;

import com.wosloveslife.fantasy.bean.BFolder;
import com.wosloveslife.fantasy.dao.DbHelper;
import com.wosloveslife.fantasy.helper.SPHelper;

import java.util.List;
import java.util.Set;

/**
 * Created by zhangh on 2017/2/7.
 */

public class CustomConfiguration {
    private static final String KEY_CUSTOM_COUNTDOWN_DELAY = "setting.KEY_CUSTOM_COUNTDOWN_DELAY";
    private static final String KEY_CLOSE_AFTER_PLAY_END = "setting.KEY_CLOSE_AFTER_PLAY_END";
    private static final String KEY_PLAY_CONTROLLER_AUTO_EXPAND = "setting.KEY_PLAY_CONTROLLER_AUTO_EXPAND";
    private static final String KEY_MIN_DURATION = "setting.KEY_MIN_DURATION";
    private static final String KEY_CHANGE_SHEET_WITH_PLAY_LIST = "setting.KEY_CHANGE_SHEET_WITH_PLAY_LIST";
    private static final String KEY_PLAY_ORDER = "setting.KEY_PLAY_ORDER";

    public static final int PLAY_ORDER_SUCCESSIVE = 0;
    public static final int PLAY_ORDER_REPEAT_ONE = 1;
    public static final int PLAY_ORDER_RANDOM = 2;

    private static int sCustomCountdown; // 定时关闭用户自定义的时间 单位 分钟
    private static boolean sIsCloseAfterPlayEnd; // 定时关闭是否在当前歌曲播放完后(或中途暂停)再执行
    private static boolean sIsPlayControllerAutoExpand; // 是否跟随滑动自动展开
    private static int sMinDuration; // 歌曲过滤最小时间 单位 秒
    private static boolean sChangeSheetWithPlayList; //
    private static int sPlayOrder; //

    private static Context sContext;

    public static void init(Context context) {
        sContext = context.getApplicationContext();

        sCustomCountdown = SPHelper.getInstance().get(KEY_CUSTOM_COUNTDOWN_DELAY, -1);
        sIsCloseAfterPlayEnd = SPHelper.getInstance().get(KEY_CLOSE_AFTER_PLAY_END, false);
        sIsPlayControllerAutoExpand = SPHelper.getInstance().get(KEY_PLAY_CONTROLLER_AUTO_EXPAND, false);
        sMinDuration = SPHelper.getInstance().get(KEY_MIN_DURATION, 30);
        sChangeSheetWithPlayList = SPHelper.getInstance().get(KEY_CHANGE_SHEET_WITH_PLAY_LIST, false);
        sPlayOrder = SPHelper.getInstance().get(KEY_PLAY_ORDER, PLAY_ORDER_SUCCESSIVE);
    }

    /**
     * @param customCountdown 单位: 分钟
     */
    public static void saveCustomCountdown(int customCountdown) {
        sCustomCountdown = customCountdown;
        SPHelper.getInstance().save(KEY_CUSTOM_COUNTDOWN_DELAY, customCountdown);
    }

    public static int getCustomCountdown() {
        return sCustomCountdown;
    }

    public static void saveCloseAfterPlayEnd(boolean closeAfterPlayEnd) {
        sIsCloseAfterPlayEnd = closeAfterPlayEnd;
        SPHelper.getInstance().save(KEY_CLOSE_AFTER_PLAY_END, closeAfterPlayEnd);
    }

    public static boolean isCloseAfterPlayEnd() {
        return sIsCloseAfterPlayEnd;
    }

    public static void savePlayControllerAutoExpand(boolean isAutoExpand) {
        sIsPlayControllerAutoExpand = isAutoExpand;
        SPHelper.getInstance().save(KEY_PLAY_CONTROLLER_AUTO_EXPAND, isAutoExpand);
    }

    public static boolean isPlayControllerAutoExpand() {
        return sIsPlayControllerAutoExpand;
    }

    /**
     * 单位 秒
     */
    public static void saveMinDuration(int minDuration) {
        sMinDuration = minDuration;
        SPHelper.getInstance().save(KEY_MIN_DURATION, minDuration);
    }

    /**
     * 单位 秒
     *
     * @return 获取歌曲过滤的最小时间
     */
    public static int getMinDuration() {
        return sMinDuration;
    }

    /**
     * 保存包含音乐文件的全部文件夹.
     */
    public static void saveFolders(List<BFolder> folders) {
        DbHelper.getFolderHelper().clear();
        DbHelper.getFolderHelper().insertOrReplace(folders);
    }

    /**
     * 获取包含音乐文件的全部文件夹. 文件夹有两个属性,路径及是否被过滤
     */
    public static List<BFolder> getFolders() {
        return DbHelper.getFolderHelper().loadEntities();
    }

    /**
     * 只获取被过滤的文件夹集合
     */
    public static Set<String> getFilteredFolders() {
        return DbHelper.getFolderHelper().getFilteredFolder();
    }

    public static void saveChangeSheetWithPlayList(boolean changeSheetWithPlayList) {
        sChangeSheetWithPlayList = changeSheetWithPlayList;
        SPHelper.getInstance().save(KEY_CHANGE_SHEET_WITH_PLAY_LIST, changeSheetWithPlayList);
    }

    public static boolean isChangeSheetWithPlayList() {
        return sChangeSheetWithPlayList;
    }

    public static void savePlayOrder(int playOrder) {
        sPlayOrder = playOrder;
        SPHelper.getInstance().save(KEY_PLAY_ORDER, playOrder);
    }

    public static int getPlayOrder() {
        return sPlayOrder;
    }
}

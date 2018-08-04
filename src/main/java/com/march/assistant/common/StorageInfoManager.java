package com.march.assistant.common;

import android.text.TextUtils;

import com.march.common.Common;
import com.march.common.utils.DateFormatUtils;
import com.march.common.utils.FileUtils;
import com.march.common.utils.RecycleUtils;
import com.march.common.utils.StreamUtils;
import com.march.assistant.DataSource;
import com.march.assistant.Assistant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CreateAt : 2018/6/14
 * Describe :
 *
 * @author chendong
 */
public class StorageInfoManager {

    public static final int TIME_LIMIT = 10_000;

    private ExecutorService mExecutorService;
    private StorageRunnable mStorageRunnable;
    private long            mLastStoreTime;

    public StorageInfoManager() {
        mExecutorService = Executors.newSingleThreadExecutor();
        mStorageRunnable = new StorageRunnable();
    }

    public void store() {
        long curTime = System.currentTimeMillis();
        if (curTime - mLastStoreTime > TIME_LIMIT) {
            mLastStoreTime = curTime;
            mExecutorService.execute(mStorageRunnable);
            Assistant.handleLog("storage", DateFormatUtils.format(DateFormatUtils.PATTERN_HHmmss, curTime) + "落盘一次");
        }
    }

    public void backUp() {
        mExecutorService.execute(new BackUpRunnable());
    }

    class BackUpRunnable implements Runnable {

        @Override
        public void run() {
            try {
                File debugFile = new File(Common.getInst().getContext().getCacheDir(), "debug.log");
                if (FileUtils.isNotExist(debugFile)) {
                    return;
                }
                String json = StreamUtils.saveStreamToString(new FileInputStream(debugFile));
                if (!TextUtils.isEmpty(json)) {
                    DataSource dataSource = Common.getInst().getJsonAdapter().toObj(json, DataSource.class);
                    Assistant.getInst().getDataSource().backUp(dataSource);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    class StorageRunnable implements Runnable {

        private ByteArrayInputStream mBais;

        @Override
        public void run() {
            try {
                DataSource ds = Assistant.getInst().getDataSource().copy();
                String json = Common.getInst().getJsonAdapter().toJson(ds);
                if (!TextUtils.isEmpty(json)) {
                    mBais = new ByteArrayInputStream(json.getBytes());
                    StreamUtils.saveStreamToFile(new File(Common.getInst().getContext().getCacheDir(), "debug.log"), mBais);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                RecycleUtils.recycle(mBais);
            }
        }
    }
}
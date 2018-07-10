
package com.downloader.sdk.dlplugin.util.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.downloader.sdk.dlplugin.DownloadTask;
import com.downloader.sdk.dlplugin.DownloadTaskStatus;
import com.downloader.sdk.dlplugin.util.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangchangwei
 *
 */
/**
 * @author yangchangwei
 */
public class DBUtil extends SQLiteOpenHelper {
    private static DBUtil mInstance = null;
    private SQLiteDatabase mDb;
    private Context mContext;
    private boolean mIsSaveOldData;
    private static String dataBaseName = "dlplugin_database";
    private static final String TAG = "DBUtil";
    private static final int DATABASE_VERSION = 59;
    private static final String DATABASE_TABLE_DOWNLOAD = "tb_download";
    private static final String DATABASE_TABLE_CONFIG = "tb_config";
    /**
     * Database creation sql statement
     */
    private static final String CREATE_DOWNLOAD_TABLE = "create table "
            + DATABASE_TABLE_DOWNLOAD + " (" + DownloadTask.KEY_ROWID
            + " integer primary key autoincrement, " + DownloadTask.KEY_NAME
            + " text not null, " + DownloadTask.KEY_CNNAME + " text ,"
            + DownloadTask.KEY_FILESIZE + " INTEGER NOT NULL DEFAULT 0,"
            + DownloadTask.KEY_CREATETIME + " INTEGER NOT NULL DEFAULT 0,"
            + DownloadTask.KEY_URL + " text not null, "
            + DownloadTask.KEY_PACKAGE_NAME + " text, "
            + DownloadTask.KEY_DOWNLOAD_STRATEGY + " text, "
            + DownloadTask.KEY_STATUS + " INTEGER NOT NULL DEFAULT 0,"
            + DownloadTask.KEY_INSTALLED + " INTEGER NOT NULL DEFAULT 0,"
            + DownloadTask.KEY_SAVEPATH + " text );";

    private static final String CREATE_CONFIG_TABLE = "create table "
            + DATABASE_TABLE_CONFIG
            + " (_id  integer primary key autoincrement, latestCleanTime INTEGER NOT NULL DEFAULT 0);";

    private static final String DROP_DOWNLOAD_TABLE = "DROP TABLE IF EXISTS "
            + DATABASE_TABLE_DOWNLOAD;
    private static final String DROP_CONFIG_TABLE = "DROP TABLE IF EXISTS "
            + DATABASE_TABLE_CONFIG;

    private DBUtil(Context context, String dataBaseName, boolean isSaveOldDataBase) {
        super(context, dataBaseName, null, DATABASE_VERSION);
        handleDb(context, dataBaseName, isSaveOldDataBase);
        this.dataBaseName = dataBaseName;
        this.mIsSaveOldData = isSaveOldDataBase;
        this.mContext = context;
        try {
            mDb = getWritableDatabase();
        } catch (Exception e) {
            mDb = getReadableDatabase();
        }

    }

    public static void handleDb(Context context, String dataBaseName, boolean isSaveOldData) {
        if (isSaveOldData) {
            // 老数据库存在，数据库存在 ,重命名老数据库,删除 新数据库
            if (DBUtil.dataBaseExits(context, "dlplugin_database")
                    && DBUtil.dataBaseExits(context, dataBaseName)) {
                DBUtil.delDataBase(context, dataBaseName);
                DBUtil.reNameDataBase(context, "dlplugin_database", dataBaseName);
                Logger.d(TAG, "老数据库存在，新数据库存在 ,删除新数据库,重命名老数据库");
                // 老数据库不存在，新数据库存在 ,打开新数据库 对象
            }
        } else {
            if (DBUtil.dataBaseExits(context, "dlplugin_database")
                    && DBUtil.dataBaseExits(context, dataBaseName)) {
                DBUtil.delDataBase(context, dataBaseName);
                DBUtil.delDataBase(context, "dlplugin_database");
            }
        }
    }

    /**
     * 单例
     */
    public static synchronized DBUtil getInstance(Context context, String dataBaseName,
            boolean isSaveOldData) {
        if (mInstance == null) {
            mInstance = new DBUtil(context, dataBaseName, isSaveOldData);
        }

        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating DataBase: " + CREATE_DOWNLOAD_TABLE);
        if (mIsSaveOldData) {
            if (oldDataBaseExits(mContext) && !"dlplugin_database".equals(dataBaseName)) {
                Logger.e(TAG, "old db extist!!");
                File oldDataBaseFile = mContext.getApplicationContext().getDatabasePath(
                        "dlplugin_database");
                File newDataBaseFile = new File(oldDataBaseFile.getParentFile().getAbsolutePath()
                        + File.separator + dataBaseName);

                if (newDataBaseFile.exists()) {
                    newDataBaseFile.delete();
                    Logger.e(TAG, "delete init null db ");
                }
                copyDataBase(mContext);
                // 重新指定 数据库连接
                // 得到数据库对象
                Logger.e(TAG, "open not null db ");

                if (mDb != null) {
                    mDb.close();
                }
                try {
                    mDb = getWritableDatabase();
                    Logger.e(TAG, "get getWritableDatabase");
                } catch (Exception e) {
                    mDb = getReadableDatabase();
                    Logger.e(TAG, "get getReadableDatabase");
                }

            } else {
                createTable(db);
            }
        } else {
            try {
                if (oldDataBaseExits(mContext)) {
                    File oldDataBaseFile = mContext.getApplicationContext().getDatabasePath(
                            "dlplugin_database");
                    if (oldDataBaseFile.exists()) {
                        oldDataBaseFile.delete();
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Logger.e(TAG, e.toString());
            }
            createTable(db);
        }

    }

    public void createTable(SQLiteDatabase db) {
        db.execSQL(CREATE_DOWNLOAD_TABLE);
        db.execSQL(CREATE_CONFIG_TABLE);
        db.execSQL("insert into " + DATABASE_TABLE_CONFIG
                + "(latestCleanTime) values(" + System.currentTimeMillis()
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_DOWNLOAD_TABLE);
        db.execSQL(CREATE_DOWNLOAD_TABLE);
        db.execSQL(DROP_CONFIG_TABLE);
        db.execSQL(CREATE_CONFIG_TABLE);
        db.execSQL("insert into " + DATABASE_TABLE_CONFIG
                + "(latestCleanTime) values(" + System.currentTimeMillis()
                + ")");
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion);
    }

    public void close() {
        this.close();
    }

    public void truncateDownloadTable() {
        mDb.execSQL("delete from " + DATABASE_TABLE_DOWNLOAD, new Object[] {});
        mDb.execSQL("update sqlite_sequence set seq=0 where name= '"
                + DATABASE_TABLE_DOWNLOAD + "'", new Object[] {});
    }

    public long insertDownload(String name, String cnname, String url,
            String packageName, String downloadStrategy) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(DownloadTask.KEY_NAME, name);
        initialValues.put(DownloadTask.KEY_CNNAME, cnname);
        // initialValues.put(DownloadEntity.KEY_FILESIZE, name);
        initialValues.put(DownloadTask.KEY_CREATETIME,
                System.currentTimeMillis());
        initialValues.put(DownloadTask.KEY_URL, url);
        if (packageName != null)
            initialValues.put(DownloadTask.KEY_PACKAGE_NAME, packageName);
        if (downloadStrategy != null)
            initialValues.put(DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    downloadStrategy);

        initialValues.put(DownloadTask.KEY_STATUS, 0);
        initialValues.put(DownloadTask.KEY_INSTALLED, 0);

        return mDb.insert(DATABASE_TABLE_DOWNLOAD, null, initialValues);
    }

    public long insertDownload(String name, String cnname, String url,
            String packageName, String downloadStrategy, int stauts) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(DownloadTask.KEY_NAME, name);
        initialValues.put(DownloadTask.KEY_CNNAME, cnname);
        // initialValues.put(DownloadEntity.KEY_FILESIZE, name);
        initialValues.put(DownloadTask.KEY_CREATETIME,
                System.currentTimeMillis());
        initialValues.put(DownloadTask.KEY_URL, url);
        if (packageName != null)
            initialValues.put(DownloadTask.KEY_PACKAGE_NAME, packageName);
        if (downloadStrategy != null)
            initialValues.put(DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    downloadStrategy);

        initialValues.put(DownloadTask.KEY_STATUS, stauts);
        initialValues.put(DownloadTask.KEY_INSTALLED, 0);

        return mDb.insert(DATABASE_TABLE_DOWNLOAD, null, initialValues);
    }

    public boolean deleteDownload(long rowId) {
        return mDb.delete(DATABASE_TABLE_DOWNLOAD, DownloadTask.KEY_ROWID + "="
                + rowId, null) > 0;
    }

    public Cursor fetchAllDownload() {
        return mDb.query(DATABASE_TABLE_DOWNLOAD, new String[] {
                DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                DownloadTask.KEY_CNNAME, DownloadTask.KEY_URL,
                DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED
        }, null,
                null, null, null, null);
    }

    public List<DownloadTask> findUnCompletedTask() {
        List<DownloadTask> result = new ArrayList<DownloadTask>();
        Cursor mCursor = null;
        try {
            mCursor = mDb
                    .query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                            DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                            DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                            DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                            DownloadTask.KEY_PACKAGE_NAME,
                            DownloadTask.KEY_DOWNLOAD_STRATEGY,
                            DownloadTask.KEY_STATUS,
                            DownloadTask.KEY_INSTALLED,
                            DownloadTask.KEY_SAVEPATH
                    },
                            DownloadTask.KEY_STATUS + "<"
                                    + DownloadTaskStatus.COMPLETED + " and "
                                    + DownloadTask.KEY_STATUS + ">="
                                    + DownloadTaskStatus.WAITING, null, null,
                            null, "_id", null);

            if (mCursor.getCount() > 0) {
                while (mCursor.moveToNext()) {
                    DownloadTask entity = new DownloadTask()
                            .setId(mCursor.getLong(0))
                            .setName(mCursor.getString(1))
                            .setCnname(mCursor.getString(2))
                            .setFilesize(mCursor.getLong(3))
                            .setCreatetime(mCursor.getLong(4))
                            .setUrl(mCursor.getString(5))
                            .setPackageName(mCursor.getString(6))
                            .setDownloadStrategy(mCursor.getString(7))
                            .setStatus(mCursor.getInt(8))
                            .setInstalled(mCursor.getInt(9))
                            .setSavepath(mCursor.getString(10))
                            .setFileTypeByUrl(
                                    mCursor.getString(10) == null ? mCursor
                                            .getString(5) : mCursor
                                            .getString(10));
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            return result;
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return result;
    }

    public List<DownloadTask> findDownLoadingTask() {
        List<DownloadTask> result = new ArrayList<DownloadTask>();
        Cursor mCursor = null;
        try {
            mCursor = mDb.query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                    DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                    DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                    DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                    DownloadTask.KEY_PACKAGE_NAME,
                    DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED,
                    DownloadTask.KEY_SAVEPATH
            }, DownloadTask.KEY_STATUS + "=="
                    + DownloadTaskStatus.DOWNLOADING, null, null, null, "_id",
                    null);

            if (mCursor.getCount() > 0) {
                while (mCursor.moveToNext()) {
                    DownloadTask entity = new DownloadTask()
                            .setId(mCursor.getLong(0))
                            .setName(mCursor.getString(1))
                            .setCnname(mCursor.getString(2))
                            .setFilesize(mCursor.getLong(3))
                            .setCreatetime(mCursor.getLong(4))
                            .setUrl(mCursor.getString(5))
                            .setPackageName(mCursor.getString(6))
                            .setDownloadStrategy(mCursor.getString(7))
                            .setStatus(mCursor.getInt(8))
                            .setInstalled(mCursor.getInt(9))
                            .setSavepath(mCursor.getString(10))
                            .setFileTypeByUrl(
                                    mCursor.getString(10) == null ? mCursor
                                            .getString(5) : mCursor
                                            .getString(10));
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            return result;
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return result;
    }

    public DownloadTask fetchOneDownload(long id) {
        DownloadTask entity = null;
        Cursor mCursor = null;

        try {
            mCursor = mDb.query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                    DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                    DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                    DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                    DownloadTask.KEY_PACKAGE_NAME,
                    DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED,
                    DownloadTask.KEY_SAVEPATH
            }, DownloadTask.KEY_ROWID + "="
                    + id, null, null, null, null, null);

            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                entity = new DownloadTask()
                        .setId(mCursor.getLong(0))
                        .setName(mCursor.getString(1))
                        .setCnname(mCursor.getString(2))
                        .setFilesize(mCursor.getLong(3))
                        .setCreatetime(mCursor.getLong(4))
                        .setUrl(mCursor.getString(5))
                        .setPackageName(mCursor.getString(6))
                        .setDownloadStrategy(mCursor.getString(7))
                        .setStatus(mCursor.getInt(8))
                        .setInstalled(mCursor.getInt(9))
                        .setSavepath(mCursor.getString(10))
                        .setFileTypeByUrl(
                                mCursor.getString(10) == null ? mCursor
                                        .getString(5) : mCursor.getString(10));
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return entity;
    }

    public DownloadTask findDownloadByUrl(String url) {
        DownloadTask entity = null;
        Cursor mCursor = null;
        try {
            mCursor = mDb.query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                    DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                    DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                    DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                    DownloadTask.KEY_PACKAGE_NAME,
                    DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED,
                    DownloadTask.KEY_SAVEPATH
            }, DownloadTask.KEY_URL + "='"
                    + url + "'", null, null, null, null, null);

            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                entity = new DownloadTask();
                entity = entity
                        .setId(mCursor.getLong(0))
                        .setName(mCursor.getString(1))
                        .setCnname(mCursor.getString(2))
                        .setFilesize(mCursor.getLong(3))
                        .setCreatetime(mCursor.getLong(4))
                        .setUrl(mCursor.getString(5))
                        .setPackageName(mCursor.getString(6))
                        .setDownloadStrategy(mCursor.getString(7))
                        .setStatus(mCursor.getInt(8))
                        .setInstalled(mCursor.getInt(9))
                        .setSavepath(mCursor.getString(10))
                        .setFileTypeByUrl(
                                mCursor.getString(10) == null ? mCursor
                                        .getString(5) : mCursor.getString(10));
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return entity;
    }

    public ArrayList<DownloadTask> findDownloadByPackageName(String packageName) {
        ArrayList<DownloadTask> mDownloadTasksList = new ArrayList<DownloadTask>();
        Cursor mCursor = null;
        try {
            mCursor = mDb.query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                    DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                    DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                    DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                    DownloadTask.KEY_PACKAGE_NAME,
                    DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED,
                    DownloadTask.KEY_SAVEPATH
            }, DownloadTask.KEY_PACKAGE_NAME
                    + "='" + packageName + "' and " + DownloadTask.KEY_STATUS
                    + "=" + DownloadTaskStatus.COMPLETED, null, null, null,
                    null, null);

            if (mCursor.getCount() > 0) {
                while (mCursor.moveToNext()) {
                    DownloadTask entity = new DownloadTask();
                    entity = entity
                            .setId(mCursor.getLong(0))
                            .setName(mCursor.getString(1))
                            .setCnname(mCursor.getString(2))
                            .setFilesize(mCursor.getLong(3))
                            .setCreatetime(mCursor.getLong(4))
                            .setUrl(mCursor.getString(5))
                            .setPackageName(mCursor.getString(6))
                            .setDownloadStrategy(mCursor.getString(7))
                            .setStatus(mCursor.getInt(8))
                            .setInstalled(mCursor.getInt(9))
                            .setSavepath(mCursor.getString(10))
                            .setFileTypeByUrl(
                                    mCursor.getString(10) == null ? mCursor
                                            .getString(5) : mCursor
                                            .getString(10));

                    mDownloadTasksList.add(entity);
                }

            }
        } catch (Exception e) {
            return null;
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return mDownloadTasksList;

    }

    public boolean updateDownload(long id, String name, String cnname,
            String url, int status, int installed, String savepath,
            String packageName) {
        ContentValues args = new ContentValues();
        if (name != null && !name.equals(""))
            args.put(DownloadTask.KEY_NAME, name);
        if (cnname != null && !cnname.equals(""))
            args.put(DownloadTask.KEY_CNNAME, cnname);
        if (url != null && !url.equals(""))
            args.put(DownloadTask.KEY_URL, url);
        if (status >= 0)
            args.put(DownloadTask.KEY_STATUS, status);
        if (installed == 0 || installed == 1)
            args.put(DownloadTask.KEY_INSTALLED, installed);
        if (savepath != null && !savepath.equals(""))
            args.put(DownloadTask.KEY_SAVEPATH, savepath);
        if (packageName != null && !packageName.equals(""))
            args.put(DownloadTask.KEY_PACKAGE_NAME, packageName);
        return mDb.update(DATABASE_TABLE_DOWNLOAD, args, DownloadTask.KEY_ROWID
                + "=" + id, null) > 0;
    }

    public boolean updateDownloadStrategyById(long id,
            String downloadStrategyStr) {
        ContentValues args = new ContentValues();
        if (!TextUtils.isEmpty(downloadStrategyStr)) {
            args.put(DownloadTask.KEY_DOWNLOAD_STRATEGY, downloadStrategyStr);
        }
        return mDb.update(DATABASE_TABLE_DOWNLOAD, args, DownloadTask.KEY_ROWID
                + "=" + id, null) > 0;
    }

    // 清除过期的下载记录
    public void removeCompletedDownloadTask() {
        if (mDb.delete(DATABASE_TABLE_DOWNLOAD, DownloadTask.KEY_STATUS + "="
                + DownloadTaskStatus.COMPLETED, null) > 0) {
            udpateLatestCleanTime(System.currentTimeMillis());
        }
    }

    public long getLatestCleanTime() {
        long returnVal = -1;
        Cursor mCursor = null;
        try {
            mCursor = mDb.query(true, DATABASE_TABLE_CONFIG, new String[] {
                    "_id", "latestCleanTime"
            }, "_id=1", null, null, null,
                    null, null);
            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                returnVal = mCursor.getLong(1);
                if (returnVal == 0)
                    returnVal = -1;
            }
        } catch (Exception e) {
            Log.i(TAG, "getLatestCleanTime: error!");
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnVal;
    }

    public boolean udpateLatestCleanTime(long updateTime) {
        ContentValues args = new ContentValues();
        if (updateTime > 0)
            args.put("latestCleanTime", updateTime);
        return mDb.update(DATABASE_TABLE_CONFIG, args, "_id=1", null) > 0;
    }

    public ArrayList<DownloadTask> findAllTask() {
        ArrayList<DownloadTask> result = new ArrayList<DownloadTask>();
        Cursor mCursor = null;
        try {
            mCursor = mDb.query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                    DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                    DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                    DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                    DownloadTask.KEY_PACKAGE_NAME,
                    DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED,
                    DownloadTask.KEY_SAVEPATH
            }, null, null, null, null, "_id",
                    null);

            if (mCursor.getCount() > 0) {
                while (mCursor.moveToNext()) {
                    DownloadTask entity = new DownloadTask()
                            .setId(mCursor.getLong(0))
                            .setName(mCursor.getString(1))
                            .setCnname(mCursor.getString(2))
                            .setFilesize(mCursor.getLong(3))
                            .setCreatetime(mCursor.getLong(4))
                            .setUrl(mCursor.getString(5))
                            .setPackageName(mCursor.getString(6))
                            .setDownloadStrategy(mCursor.getString(7))
                            .setStatus(mCursor.getInt(8))
                            .setInstalled(mCursor.getInt(9))
                            .setSavepath(mCursor.getString(10))
                            .setFileTypeByUrl(
                                    mCursor.getString(10) == null ? mCursor
                                            .getString(5) : mCursor
                                            .getString(10));
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            return result;
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        return result;
    }

    public ArrayList<DownloadTask> findAllTempTaskByStatus() {
        ArrayList<DownloadTask> downLoadTaskList = new ArrayList<DownloadTask>();
        Cursor mCursor = null;
        try {
            mCursor = mDb.query(true, DATABASE_TABLE_DOWNLOAD, new String[] {
                    DownloadTask.KEY_ROWID, DownloadTask.KEY_NAME,
                    DownloadTask.KEY_CNNAME, DownloadTask.KEY_FILESIZE,
                    DownloadTask.KEY_CREATETIME, DownloadTask.KEY_URL,
                    DownloadTask.KEY_PACKAGE_NAME,
                    DownloadTask.KEY_DOWNLOAD_STRATEGY,
                    DownloadTask.KEY_STATUS, DownloadTask.KEY_INSTALLED,
                    DownloadTask.KEY_SAVEPATH
            }, DownloadTask.KEY_STATUS + "="
                    + DownloadTaskStatus.TEMP_NODOWNLOAD, null, null, null,
                    "_id", null);

            if (mCursor.getCount() > 0) {
                while (mCursor.moveToNext()) {
                    DownloadTask entity = new DownloadTask()
                            .setId(mCursor.getLong(0))
                            .setName(mCursor.getString(1))
                            .setCnname(mCursor.getString(2))
                            .setFilesize(mCursor.getLong(3))
                            .setCreatetime(mCursor.getLong(4))
                            .setUrl(mCursor.getString(5))
                            .setPackageName(mCursor.getString(6))
                            .setDownloadStrategy(mCursor.getString(7))
                            .setStatus(mCursor.getInt(8))
                            .setInstalled(mCursor.getInt(9))
                            .setSavepath(mCursor.getString(10))
                            .setFileTypeByUrl(
                                    mCursor.getString(10) == null ? mCursor
                                            .getString(5) : mCursor
                                            .getString(10));
                    downLoadTaskList.add(entity);
                }
            }
        } catch (Exception e) {
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return downLoadTaskList;

    }

    public static String getDATABASE_NAME() {
        return dataBaseName;
    }

    public void setDATABASE_NAME(String dATABASE_NAME) {
        dataBaseName = dATABASE_NAME;
    }

    /**
     * 重命名数据库达到 复制老数据库 的效果 `
     * 
     * @param context
     */
    public void copyDataBase(Context context) {
        Logger.e(TAG, "prepare to copy olddb!");
        if (context != null) {
            File oldDataBaseFile = context.getApplicationContext().getDatabasePath(
                    "dlplugin_database");
            if (oldDataBaseFile.exists() && !"dlplugin_database".equals(dataBaseName)) {
                File newDataBaseFile = new File(oldDataBaseFile.getParentFile().getAbsolutePath()
                        + File.separator + dataBaseName);
                if (newDataBaseFile.exists()) {
                    Logger.e(TAG, "newDataBaseFile ---exists!");
                }
                FileInputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(oldDataBaseFile);
                    outputStream = new FileOutputStream(newDataBaseFile);
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    try {
                        while ((len = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.flush();
                        oldDataBaseFile.delete();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        Logger.e(TAG, e.toString());
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    Logger.e(TAG, e.toString());
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            Logger.e(TAG, e.toString());
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            Logger.e(TAG, e.toString());
                        }

                    }
                }

            }
        }
        Logger.e(TAG, " db copy already!");
    }

    /**
     * 判断老数据库是否 存在
     * 
     * @param context
     * @return
     */
    public boolean oldDataBaseExits(Context context) {
        boolean result = false;
        if (context != null) {
            File oldDataBaseFile = context.getApplicationContext().getDatabasePath(
                    "dlplugin_database");
            if (oldDataBaseFile.exists()) {
                result = true;
            }
        }
        return result;

    }

    /**
     * 判断老数据库是否 存在
     * 
     * @param context
     * @return
     */
    public static boolean dataBaseExits(Context context, String dbName) {
        boolean result = false;
        if (context != null) {
            File oldDataBaseFile = context.getApplicationContext().getDatabasePath(
                    dbName);
            if (oldDataBaseFile.exists()) {
                result = true;
            }
        }
        return result;

    }

    /**
     * 判断老数据库是否 存在
     * 
     * @param context
     * @return
     */
    public static boolean delDataBase(Context context, String dbName) {
        boolean result = false;
        if (context != null) {
            File dataBaseFile = context.getApplicationContext().getDatabasePath(
                    dbName);
            if (dataBaseFile.exists()) {
                result = dataBaseFile.delete();
            }
        }
        return result;
    }

    /**
     * 重命名数据库达到 复制老数据库 的效果 `
     * 
     * @param context
     */
    public static void reNameDataBase(Context context, String oldDataBaseName,
            String newDataBaseName) {
        Logger.e(TAG, "prepare to copy olddb!");
        if (context != null) {
            File oldDataBaseFile = context.getApplicationContext().getDatabasePath(
                    oldDataBaseName);
            if (oldDataBaseFile.exists() && !"dlplugin_database".equals(dataBaseName)) {
                File newDataBaseFile = new File(oldDataBaseFile.getParentFile().getAbsolutePath()
                        + File.separator + dataBaseName);
                if (!newDataBaseFile.exists()) {
                    Logger.e(TAG, "newDataBaseFile ---exists!");
                    oldDataBaseFile.renameTo(newDataBaseFile);
                }
            }
        }
        Logger.e(TAG, " db copy already!");
    }
}

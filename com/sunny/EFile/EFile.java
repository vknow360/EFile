package com.sunny.EFile;
import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Environment;
import java.io.*;
import android.os.Handler;
import android.content.Context;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.FileUtil;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.PermissionResultHandler;
@DesignerComponent(version = 4,
        description ="A customized version of File component<br>Developed by Sunny Gupta",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames ="android.permission.WRITE_EXTERNAL_STORAGE,android.permission.READ_EXTERNAL_STORAGE")
public final class EFile extends AndroidNonvisibleComponent{
    public Activity activity;
    private boolean isRepl = false;
    private boolean hasWriteAccess = false;
    private boolean hasReadAccess = false;
    private final Context context;
    private boolean legacy = true;
    public EFile(ComponentContainer container) {
        super(container.$form());
        if (form instanceof com.google.appinventor.components.runtime.ReplForm) {
            isRepl = true;
        }
        activity = container.$context();
        context = container.$context();
        hasWriteAccess = context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == 0;
        hasReadAccess = context.checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0;
    }
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
            defaultValue = "True")
    @SimpleProperty(description = "Allows app to write to Application Specific Directory instead of Private Directory")
    public void LegacyMode(boolean legacy) {
        this.legacy = legacy;
    }


    @SimpleFunction(description="Returns external storage path")
    public String GetExternalStoragePath(){
        return getExternalStoragePath();
    }
    @SimpleFunction(description="Returns app's private storage directory path")
    public String GetPrivateDirPath(){
        return activity.getFilesDir().getAbsolutePath();
    }
    @SimpleFunction(description = "Returns application specific directory path")
    public String GetApplicationSpecificDirPath(){
        return context.getExternalFilesDir(null).getAbsolutePath();
    }
    @SimpleFunction(description="Returns the absolute path to the application specific cache directory on the filesystem")
    public String GetCacheDirPath(){
        return activity.getCacheDir().getAbsolutePath();
    }
    @SimpleFunction(description="Saves text to a file. If the filename " +
            "begins with a slash (/) the file is written to the sdcard. For example writing to " +
            "/myFile.txt will write the file to /sdcard/myFile.txt. If the filename does not start " +
            "with a slash, it will be written in the programs private data directory where it will " +
            "not be accessible to other programs on the phone. There is a special exception for the " +
            "AI/Kodular Companion where these files are written to /sdcard/AppInventor/data (and /sdcard/Makeroid/data for Kodular) to facilitate " +
            "debugging. Note that this block will overwrite a file if it already exists." +
            "\n\nIf you want to add content to a file use the append block.")
    public void SaveFile(String text, String fileName){
        Write(fileName,text,false);
    }
    public String getAbsoluteFilePath(String fileName) {
        if(fileName.isEmpty()){
            return fileName;
        }else{
            String sd = getExternalStoragePath();
            String completeFileName = fileName;
            if (fileName.startsWith("file:///")) {
                completeFileName = fileName.substring(7);
            } else if (fileName.startsWith("//")) {
                fileName = fileName.substring(2);
                if (isRepl) {
                    completeFileName = getReplFilePath() + fileName;
                }
            } else if (fileName.startsWith("/")) {
                if (!fileName.startsWith(sd)){
                    completeFileName = sd + fileName;
                }
            } else {
                if (legacy) {
                    completeFileName = GetApplicationSpecificDirPath() + File.separator + fileName;
                }else{
                    completeFileName = GetPrivateDirPath() + File.separator + fileName;
                }
            }
            return completeFileName;
        }
    }
    public void Write(final String filename, final String text, final boolean append){
        if(!context.getExternalFilesDir(null).exists()){
            context.getExternalFilesDir(null).mkdirs();
        }
        if(filename.startsWith("/")) {
                if (filename.contains(GetApplicationSpecificDirPath()) || filename.contains(GetPrivateDirPath()) || filename.contains(GetCacheDirPath())) {
                AsynchUtil.runAsynchronously(new Runnable() {
                    public void run() {
                        save(filename,text,append);
                    }
                });
            }else{
                if(!hasWriteAccess){
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            form.askPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    new PermissionResultHandler() {
                                        @Override
                                        public void HandlePermissionResponse(String permission, boolean granted) {
                                            hasWriteAccess = granted;
                                        }
                                    });
                        }
                    });
                }
                if (hasWriteAccess) {
                    AsynchUtil.runAsynchronously(new Runnable() {
                        public void run() {
                            save(getAbsoluteFilePath(filename),text,append);
                        }
                    });
                }
            }
        }else{
            AsynchUtil.runAsynchronously(new Runnable() {
                public void run() {
                    save(getAbsoluteFilePath(filename),text,append);
                }
            });
        }
    }
    public void save(final String filename,final String text,final boolean append){
        final File file = new File(filename);
        if(!file.exists()){
            try{
                file.createNewFile();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream fileWriter = new FileOutputStream(file,append);
            OutputStreamWriter out = new OutputStreamWriter(fileWriter);
            out.write(text);
            out.flush();
            out.close();
            fileWriter.close();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AfterFileSaved(filename);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SimpleFunction(description="Appends text to the end of a file storage, creating the file if it does not exist. " +
            "See the help text under SaveFile for information about where files are written.")
    public void AppendToFile(String text, String fileName) {
        Write(fileName, text, true);
    }
    @SimpleFunction(description="Reads text from a file in storage. " +
            "Prefix the filename with / to read from a specific file on the SD card. " +
            "for instance /myFile.txt will read the file /sdcard/myFile.txt. To read " +
            "assets packaged with an application (also works for the Companion) start " +
            "the filename with // (two slashes). If a filename does not start with a " +
            "slash, it will be read from the applications private storage (for packaged " +
            "apps) and from /sdcard/AppInventor/data for the AI Companion and /sdcard/Makeroid/data for Kodular Companion.")
    public void ReadFrom(final String fileName){
        try{
            InputStream inputStream;
            if(fileName.startsWith("//")){
                inputStream = form.openAsset(fileName.substring(2));
                final InputStream asyncInputStream = inputStream;
                AsynchUtil.runAsynchronously(new Runnable() {
                    @Override
                    public void run() {
                        AsyncRead(asyncInputStream,fileName);
                    }
                });
            }else if(fileName.startsWith("/")){
                if (fileName.contains(GetApplicationSpecificDirPath()) || fileName.contains(GetPrivateDirPath()) || fileName.contains(GetCacheDirPath())) {
                    inputStream = new FileInputStream(fileName);
                    final InputStream asyncInputStream = inputStream;
                    AsynchUtil.runAsynchronously(new Runnable() {
                        @Override
                        public void run() {
                            AsyncRead(asyncInputStream, fileName);
                        }
                    });
                }else{
                    if (!hasReadAccess) {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                form.askPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                                        new PermissionResultHandler() {
                                            @Override
                                            public void HandlePermissionResponse(String permission, boolean granted) {
                                                hasReadAccess = granted;
                                            }
                                        });
                            }
                        });
                    }
                    if (hasReadAccess) {
                        final InputStream asyncInputStream = FileUtil.openFile(getAbsoluteFilePath(fileName));
                        AsynchUtil.runAsynchronously(new Runnable() {
                            @Override
                            public void run() {
                                AsyncRead(asyncInputStream, fileName);
                            }
                        });
                    }
                }
            }else{
                final String filename = getAbsoluteFilePath(fileName);
                inputStream = new FileInputStream(filename);
                final InputStream asyncInputStream = inputStream;
                AsynchUtil.runAsynchronously(new Runnable() {
                    @Override
                    public void run() {
                        AsyncRead(asyncInputStream,filename);
                    }
                });
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void AsyncRead(InputStream fileInput, final String fileName) {
        InputStreamReader input = null;
        try {
            input = new InputStreamReader(fileInput);
            StringWriter output = new StringWriter();
            int BUFFER_LENGTH = 4096;
            char [] buffer = new char[BUFFER_LENGTH];
            int offset = 0;
            int length = 0;
            while ((length = input.read(buffer, offset, BUFFER_LENGTH)) > 0) {
                output.write(buffer, 0, length);
            }
            final String text = normalizeNewLines(output.toString());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GotText(text);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private String normalizeNewLines(String s) {
        return s.replaceAll("\r\n", "\n");
    }
    @SimpleEvent(description="Event indicating that the contents from the file have been read.")
    public void GotText(String text) {
        EventDispatcher.dispatchEvent(this, "GotText", text);
    }
    @SimpleEvent(description="Event indicating that the contents of the file have been written.")
    public void AfterFileSaved(String fileName) {
        EventDispatcher.dispatchEvent(this,"AfterFileSaved",fileName);
    }
    @SuppressWarnings("deprecation")
    public String getExternalStoragePath(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            return context.getExternalFilesDir(null).getAbsolutePath();
        }else{
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }
    private String getReplFilePath(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getExternalStoragePath() + "/data/";
        } else {
            return getExternalStoragePath() + "/AppInventor/data/";
        }
    }
}

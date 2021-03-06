package kr.go.egovframework.hyb.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

public class EgovZip extends CordovaPlugin {

	
    private static final int COMPRESSION_LEVEL = 8;
    private static final int BUFFER_SIZE = 1024 * 2;
	
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        
    	Log.d(this.getClass().getSimpleName(),">>>>> action = "+ action);
    	if ("unzip".equals(action)) {
    		
			String zipFile = Uri.parse(data.getString(0)).getPath(); //data.getString(0);			
			String targetDir = Uri.parse(data.getString(1)).getPath(); //data.getString(1);
			
			Log.d(this.getClass().getSimpleName(),">>>>> zipFile = "+ zipFile);
			Log.d(this.getClass().getSimpleName(),">>>>> targetDir = "+ targetDir);
            
			unzip(zipFile, targetDir, callbackContext);
			
            return true;
        }else if ("zip".equals(action)) {
    		
			String zipFile = Uri.parse(data.getString(0)).getPath(); //data.getString(0);			
			String targetDir = Uri.parse(data.getString(1)).getPath(); //data.getString(1);
			
			Log.d(this.getClass().getSimpleName(),">>>>> zipFile = "+ zipFile);
			Log.d(this.getClass().getSimpleName(),">>>>> targetDir = "+ targetDir);
            
			zip(zipFile, targetDir, callbackContext);
			
            return true;
        }
        return false;
    }

    
    private void unzip(String zipFile, String targetDir, CallbackContext callbackContext){
    	
    	try {
			unzip(zipFile, targetDir, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject()));
    	
    }
    
    private void zip(String zipFile, String targetDir, CallbackContext callbackContext){
    	
    	try {
			zip(zipFile, targetDir);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject()));
    	
    }
    
    /**
     * ????????? ????????? Zip ????????? ????????????.
     * @param sourcePath - ?????? ?????? ????????????
     * @param output - ?????? zip ?????? ??????
     * @throws Exception
     */
    public static void zip(String sourcePath, String output) throws Exception {

        // ?????? ??????(sourcePath)??? ??????????????? ????????? ????????? ????????????.
        File sourceFile = new File(sourcePath);
        if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
            throw new Exception("?????? ????????? ????????? ?????? ?????? ????????????.");
        }

        // output ??? ???????????? zip??? ????????? ????????????.
        //if (!(StringUtils.substringAfterLast(output, ".")).equalsIgnoreCase("zip")) {
        //    throw new Exception("?????? ??? ?????? ???????????? ???????????? ???????????????");
        //}

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ZipOutputStream zos = null;

        try {
            fos = new FileOutputStream(output); // FileOutputStream
            bos = new BufferedOutputStream(fos); // BufferedStream
            zos = new ZipOutputStream(bos); // ZipOutputStream
            zos.setLevel(COMPRESSION_LEVEL); // ?????? ?????? - ?????? ???????????? 9, ????????? 8
            
            zipEntry(sourceFile, sourcePath, zos); // Zip ?????? ??????
            zos.finish(); // ZipOutputStream finish
        } finally {
            if (zos != null) {
                zos.close();
            }
            if (bos != null) {
                bos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * ??????
     * @param sourceFile
     * @param sourcePath
     * @param zos
     * @throws Exception
     */
    private static void zipEntry(File sourceFile, String sourcePath, ZipOutputStream zos) throws Exception {
        // sourceFile ??? ??????????????? ?????? ?????? ?????? ????????? ????????? ????????????
        if (sourceFile.isDirectory()) {
        	Log.d("dir >>>","dir = " +sourceFile.getPath());
            if (sourceFile.getName().equalsIgnoreCase(".metadata")) { // .metadata ???????????? return
                return;
            }
            File[] fileArray = sourceFile.listFiles(); // sourceFile ??? ?????? ?????? ?????????
            for (int i = 0; i < fileArray.length; i++) {
                zipEntry(fileArray[i], sourcePath, zos); // ?????? ??????
            }
        } else { // sourcehFile ??? ??????????????? ?????? ??????
            BufferedInputStream bis = null;
            
            try {
                String sFilePath = sourceFile.getPath();
                Log.i("file >>>", sFilePath);
                //String zipEntryName = sFilePath.substring(sourcePath.length() + 1, sFilePath.length());
                StringTokenizer tok = new StringTokenizer(sFilePath,"/");
    			
    			int tok_len = tok.countTokens();
    			String zipEntryName=tok.toString();
    			while(tok_len != 0){
    				tok_len--;
    				zipEntryName = tok.nextToken();
    			}
                bis = new BufferedInputStream(new FileInputStream(sourceFile));
                
                ZipEntry zentry = new ZipEntry(zipEntryName);
                zentry.setTime(sourceFile.lastModified());
                zos.putNextEntry(zentry);

                byte[] buffer = new byte[BUFFER_SIZE];
                int cnt = 0;
                
                while ((cnt = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    zos.write(buffer, 0, cnt);
                }
                zos.closeEntry();
            } finally {
                if (bis != null) {
                    bis.close();
                }
            }
        }
    }

    /**
     * Zip ????????? ????????? ??????.
     *
     * @param zipFile - ?????? ??? Zip ??????
     * @param targetDir - ?????? ??? ????????? ????????? ????????????
     * @param fileNameToLowerCase - ???????????? ???????????? ????????? ??????
     * @throws Exception
     */
    public static void unzip(String zipFile, String targetDir, boolean fileNameToLowerCase) throws Exception {
        FileInputStream fis = null;
        ZipInputStream zis = null;
        ZipEntry zentry = null;

        try {
            fis = new FileInputStream(zipFile); // FileInputStream
            zis = new ZipInputStream(fis); // ZipInputStream

            while ((zentry = zis.getNextEntry()) != null) {
                String fileNameToUnzip = zentry.getName();
                if (fileNameToLowerCase) { // fileName toLowerCase
                    fileNameToUnzip = fileNameToUnzip.toLowerCase();
                }

                File targetFile = new File(targetDir, fileNameToUnzip);

                if (zentry.isDirectory()) {// Directory ??? ??????
                    //FileUtils.makeDir(targetFile.getAbsolutePath()); // ???????????? ??????
                    File path = new File(targetFile.getAbsolutePath());
                    path.mkdirs(); 
                } else { // File ??? ??????
                    // parent Directory ??????
                   //FileUtils.makeDir(targetFile.getParent());
                    File path = new File(targetFile.getParent());
                    path.mkdirs(); 
                    unzipEntry(zis, targetFile);
                }
            }
        } finally {
            if (zis != null) {
                zis.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * Zip ????????? ??? ??? ???????????? ????????? ??????.
     *
     * @param zis - Zip Input Stream
     * @param filePath - ?????? ?????? ????????? ??????
     * @return
     * @throws Exception
     */
    protected static File unzipEntry(ZipInputStream zis, File targetFile) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int len = 0;
            while ((len = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return targetFile;
    }

	
}

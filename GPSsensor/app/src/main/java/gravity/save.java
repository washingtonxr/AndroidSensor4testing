package gravity;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class save {
    public static void Write(String str){
        FileOutputStream o = null;
        String path="/mnt/sdcard/huawei/";
        String filename="gravity.txt";
        byte[] buff = new byte[]{};
        try{
            File file = new File(path+filename);
            if(!file.exists()){
                file.createNewFile();
            }
            buff=str.getBytes();
            o=new FileOutputStream(file,true);
            o.write(buff);
            o.flush();
            o.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


}

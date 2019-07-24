package com.location.sensor;

import java.io.File;
import java.io.FileOutputStream;

public class Save {
    public static void writeToTXT(String filename, String str){
        FileOutputStream o = null;
        String path="/mnt/sdcard/huawei/";
        byte[] buff = new byte[]{};
        try{
            File file = new File(path+filename);
            if(!file.exists()){
                file.createNewFile();
            }

            buff=str.getBytes();
            o=new FileOutputStream(file,true);
            o.write(buff);
            // o.flush();//
            o.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


}

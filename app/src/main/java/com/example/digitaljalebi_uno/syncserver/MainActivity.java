package com.example.digitaljalebi_uno.syncserver;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/*
 * Permission needed:
 * <uses-permission android:name="android.permission.INTERNET"/>
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
 */

public class MainActivity extends Activity {

    TextView infoIp, infoPort;

    static final int SocketServerPORT = 8880;
    ServerSocket serverSocket;

    ServerSocketThread serverSocketThread;
    File file= new File(android.os.Environment.getExternalStorageDirectory(),"lvmh");
    public int h = file.listFiles().length+1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
       // verifyStoragePermissions(getCallingActivity());
        setContentView(R.layout.activity_main);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);

        infoIp.setText(getIpAddress());

        serverSocketThread = new ServerSocketThread();
        serverSocketThread.start();
     //   Log.d("naval", "v"+Environment.getExternalStorageDirectory());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    public class ServerSocketThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("I'm waiting here: "
                                + serverSocket.getLocalPort());
                    }});

                while (true) {
                    Log.d("naval", "waiting for connection");
                    socket = serverSocket.accept();
                    Log.d("naval", "Got connection");
                    FileTxThread fileTxThread = new FileTxThread(socket);
                    Log.d("naval", "created file thread");
                    fileTxThread.start();
                    Log.d("naval", "started file thread");
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public class FileTxThread extends Thread {
        private  ServerSocket serverSocket;
        private  Socket clientSocket;
        private  InputStream inputStream;
        private  FileOutputStream fileOutputStream;
        private  BufferedOutputStream bufferedOutputStream;
        private  int filesize = 10000000; // filesize temporary hardcoded
        private  int bytesRead;
        private  int current = 0;
        Socket socket;

        FileTxThread(Socket socket){
            this.socket= socket;
            Log.d("naval", "constructor file thread initialized");
        }

        @Override
        public void run() {

            Log.d("naval", "Run function  file thread");
            byte[] mybytearray = new byte[filesize];    //create byte array to buffer the file
            try {
                inputStream = socket.getInputStream();
                Log.d("naval", "setting input stream");

                // check for SD card mounted
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
                {
                    Log.d("naval", "Mounted");
                }



               // Log.d("naval", "Tab file location :" + path);


                try {
                    if(h>=29)// for LVMH we will save only 28 images and then we will replace them
                        h=0;
                    fileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/lvmh/"+h+"img.jpg");

                }catch (IOException e)
                {
                    e.printStackTrace();
                }
                Log.d("naval", "setting path for file");
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                h++;// count increase for file name
                Log.d("naval-count pic","val"+h);

                System.out.println("Receiving...");
                Log.d("naval", "Receiving");

                //following lines read the input slide file byte by byte
                bytesRead = inputStream.read(mybytearray, 0, mybytearray.length);
                current = bytesRead;

                do {
                    bytesRead = inputStream.read(mybytearray, current, (mybytearray.length - current));
                    if (bytesRead >= 0) {
                        current += bytesRead;
                    }
                } while (bytesRead > -1);


                bufferedOutputStream.write(mybytearray, 0, current);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                inputStream.close();
                //clientSocket.close();
                //serverSocket.close();

                System.out.println("Sever recieved the file");
                Log.d("naval","server received file");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


        }
    }
}
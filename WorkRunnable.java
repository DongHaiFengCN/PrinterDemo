package com.example.ydd.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gprinter.io.PortManager;

import java.io.IOException;

import static com.example.ydd.myapplication.PrinterChangeListener.PRINTER_URL;
import static com.example.ydd.myapplication.PrinterChangeListener.READ_BUFFER_ARRAY;
import static com.example.ydd.myapplication.PrinterChangeListener.READ_DATA;
import static com.example.ydd.myapplication.PrinterChangeListener.READ_NAME;

public class WorkRunnable implements Runnable {

    private PortManager portManager;

    /**
     * 打印机名称
     */
    private String name;

    volatile private boolean isRunning = true;

    private LocalBroadcastManager localBroadcastManager;

    public WorkRunnable(Context context, PortManager portManager, String name) {

        this.portManager = portManager;


        this.name = name;

        localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());
    }

    @Override
    public void run() {

        byte[] buffer = new byte[100];
        int len = 0;

        {

            while (isRunning) {

                try {
                    len = portManager.readData(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (len > 0) {

                    Intent intent = new Intent(PRINTER_URL);
                    Bundle bundle = new Bundle();
                    bundle.putByteArray(READ_BUFFER_ARRAY, buffer);
                    bundle.putString(READ_NAME, name);
                    intent.putExtra(READ_DATA, bundle);
                    localBroadcastManager.sendBroadcast(intent);
                }



            }

        }

    }

    /**
     * 设置监听监听状态
     *
     * @param isRunning
     */
    public void setListenerStatus(boolean isRunning) {

        this.isRunning = isRunning;

    }

    public String getName() {

        return name;
    }
}

package com.example.ydd.myapplication;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.gprinter.io.PortManager;

import java.io.IOException;
import java.sql.Time;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PrinterChangeListener {

    /**
     * 监听本地打印机状态广播的地址
     */
    public static final String PRINTER_URL = "com.ydd.platform.printer.broadcast.message";

    /**
     * 广播信息Bundle的key
     */
    public static final String READ_DATA = "read_data";
    public static final String READ_BUFFER_ARRAY = "read_buffer_array";
    public static final String READ_NAME = "read_name";

    private static Context myContext;

    public static ExecutorService executorService;

    //参数初始化
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();


    /**
     *
     */
    public static ConcurrentHashMap<String, PortManager> concurrentMap;


    /**
     * 获取最大监听打印机的个数
     *
     * @return maximumPoolSize
     */
    public static int getMaximumPoolSize() {

        return maximumPoolSize;
    }

    /**
     * 线程池最大容纳线程数(这里限制连接打印机的数量：测试华为平板为最大监听 9个打印机  )
     */
    private static final int maximumPoolSize = CPU_COUNT * 2 + 1;


    //初始化工作线程映射表（打印机名称，runnable）
    public static HashMap<String, WorkRunnable> workPool = new HashMap(maximumPoolSize);


    private static PrinterChangeListener printerChangeListener;

    /**
     * ESC查询打印机实时状态指令
     */
    public static byte[] esc = {0x10, 0x04, 0x02};

    /**
     * ESC查询打印机实时状态 缺纸状态
     */
    public static final int ESC_STATE_PAPER_ERR = 0x20;

    /**
     * ESC指令查询打印机实时状态 打印机开盖状态
     */
    public static final int ESC_STATE_COVER_OPEN = 0x04;

    /**
     * ESC指令查询打印机实时状态 打印机报错状态
     */
    public static final int ESC_STATE_ERR_OCCURS = 0x40;


    private static Vector<Byte> data;

    private static Timer timer;

    private static Long period = 2000L;

    public static PrinterChangeListener getInstance(Context context) {

        if (printerChangeListener == null) {

            PrinterChangeListener.myContext = context.getApplicationContext();

            printerChangeListener = new PrinterChangeListener();

            timer = new Timer();

            data = new Vector<>(esc.length);

            for (int i = 0; i < esc.length; i++) {

                data.add(esc[i]);
            }

            //最大线程数 maximumPoolSize 个
            executorService = newFixThreadPool(maximumPoolSize);

            concurrentMap = new ConcurrentHashMap();


        }

        return printerChangeListener;

    }

    /**
     * 定时向所有打印机发送询问指令
     */
    public  void openPrinterListener() {



       PortManager portManager = concurrentMap.get("101");

        try {
            portManager.writeDataImmediately(data, 0, data.size());

        } catch (IOException e) {
            e.printStackTrace();
        }

        Objects.requireNonNull(timer, "首先要初始化PrinterChangeListener监听器～");

/*
        timer.schedule(new TimerTask() {
            @Override
            public void run() {


         *//*       try {
                    concurrentMap.get("101").writeDataImmediately(data, 0, data.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
*//*
        *//*        Iterator iterator = concurrentMap.entrySet().iterator();

                while (iterator.hasNext()) {

                    Map.Entry<String, PortManager> entry = (Map.Entry<String, PortManager>) iterator.next();

                    try {
                        entry.getValue().writeDataImmediately(data, 0, data.size());

                        Log.e("DOAING",entry.getKey());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }*//*
            }
        }, 0, period);*/

    }

    /**
     * 关闭定时器
     */
    public  void closePrinterListener() {

        if (timer != null) {

            timer.cancel();

        }
    }

    /**
     * 添加一个新的打印机监听
     *
     * @param workRunnable
     */
    public  void addPoolThread(WorkRunnable workRunnable) {

        Objects.requireNonNull(executorService, "请首先初始化线程池～");

        if (workPool.size() < maximumPoolSize) {

            //执行当前打印机监听线程
            executorService.execute(workRunnable);

            //缓存当前打印机监听线程到工作线程map
            workPool.put(workRunnable.getName(), workRunnable);


        } else {

            Toast.makeText(myContext, "当前设备最多监听" + maximumPoolSize + "个打印设备", Toast.LENGTH_SHORT).show();

        }

    }


    public static ExecutorService newFixThreadPool(int size) {
        return new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 关闭指定名称的打印机实时监听，从map中拿到指定的打印机监听线程
     *
     * @param name
     */
    public  boolean freeWorkPool(String name) {

        //获取工作线程
        WorkRunnable workRunnable = workPool.get(name);

        if (workRunnable != null) {

            workRunnable.setListenerStatus(false);

            //移除当前打印机指令查询
            concurrentMap.remove(name);

            if (concurrentMap.isEmpty()) {

                closePrinterListener();
            }

            return true;
        }


        return false;
    }

    /**
     * 终止所有的打印机监听
     */
    public  void shutdownAllPool() {

        if (executorService != null) {

            executorService.shutdownNow();

            Toast.makeText(myContext, "解除所有打印机监听", Toast.LENGTH_SHORT).show();


            //关闭所有的打印机指令查询
            closePrinterListener();
        }


    }

}

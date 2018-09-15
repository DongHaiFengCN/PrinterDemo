package com.example.ydd.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.EthernetPort;
import com.gprinter.io.PortManager;

import java.io.IOException;
import java.util.Vector;

import static com.example.ydd.myapplication.PrinterChangeListener.ESC_STATE_COVER_OPEN;
import static com.example.ydd.myapplication.PrinterChangeListener.ESC_STATE_ERR_OCCURS;
import static com.example.ydd.myapplication.PrinterChangeListener.ESC_STATE_PAPER_ERR;
import static com.example.ydd.myapplication.PrinterChangeListener.PRINTER_URL;
import static com.example.ydd.myapplication.PrinterChangeListener.READ_BUFFER_ARRAY;
import static com.example.ydd.myapplication.PrinterChangeListener.READ_DATA;
import static com.example.ydd.myapplication.PrinterChangeListener.READ_NAME;
import static com.example.ydd.myapplication.PrinterChangeListener.esc;

public class MainActivity extends AppCompatActivity {


    private EthernetPort ethernetPort1;
    private EthernetPort ethernetPort2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PRINTER_URL);
        LocalReceiver localReceiver = new LocalReceiver();

        //注册本地接收器
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter);


        //初始化监听监听器
        PrinterChangeListener.getInstance(getApplicationContext());

        //定时发送监听任务
        PrinterChangeListener.openPrinterListener((long) 2000);




        final Vector<Byte> data = new Vector<>(esc.length);



        for (int i = 0; i < esc.length; i++) {

            data.add(esc[i]);
        }

        ethernetPort1 = new EthernetPort("192.168.2.101", 9100);

        Log.e("DOAING", ethernetPort1.openPort() + " 第一个打开");

        PrinterChangeListener.addPoolThread(new WorkRunnable(getApplicationContext(), ethernetPort1, "101"));





        ethernetPort2 = new EthernetPort("192.168.2.201", 9100);

        Log.e("DOAING", ethernetPort2.openPort() + " 第二个打开");

        PrinterChangeListener.addPoolThread(new WorkRunnable(getApplicationContext(), ethernetPort2, "201"));

        findViewById(R.id.printa_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PrinterChangeListener.closePrinterListener();

/*
                try {
                    ethernetPort1.writeDataImmediately(data);

                } catch (IOException e) {
                    e.printStackTrace();
                }*/

            }
        });

        findViewById(R.id.printb_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    ethernetPort2.writeDataImmediately(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });

        findViewById(R.id.printc_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                PrinterChangeListener.freeWorkPool("101");

            }
        });
        findViewById(R.id.printd_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrinterChangeListener.freeWorkPool("201");
            }
        });
        findViewById(R.id.printe_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrinterChangeListener.shutdownAllPool();
            }
        });
    }

    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {


            Bundle bundle = intent.getBundleExtra(READ_DATA);

            String name = bundle.getString(READ_NAME);

            byte[] buffer = bundle.getByteArray(READ_BUFFER_ARRAY);

            if ((buffer[0] & ESC_STATE_PAPER_ERR) > 0) {

                Toast.makeText(MainActivity.this, name + "缺纸了", Toast.LENGTH_SHORT).show();


            }
            if ((buffer[0] & ESC_STATE_COVER_OPEN) > 0) {
                Toast.makeText(MainActivity.this, name + "没扣好盖子", Toast.LENGTH_SHORT).show();


            }
            if ((buffer[0] & ESC_STATE_ERR_OCCURS) > 0) {

                Toast.makeText(MainActivity.this, name + "打印错误", Toast.LENGTH_SHORT).show();


            }

        }
    }

    /**
     * 发送票据
     */
    void sendReceiptWithResponse(PortManager mPort) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        // 设置打印居中
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        // 设置为倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
        // 打印文字
        esc.addText("Sample\n");
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        // 取消倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        // 打印文字
        esc.addText("Print text\n");
        // 打印文字
        esc.addText("Welcome to use SMARNET printer!\n");

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        esc.addText(message, "GB2312");
        esc.addPrintAndLineFeed();

        /* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("智汇");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("网络");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("设备");
        esc.addPrintAndLineFeed();

        /*        *//* 打印图片 *//*
        // 打印文字
        esc.addText("Print bitmap!\n");
        Bitmap b = BitmapFactory.decodeResource(getResources(),
                R.mipmap.gprinter);*/
        // 打印图片
        //  esc.addOriginRastBitImage(b, 384, 0);

        /* 打印一维条码 */
        // 打印文字
        esc.addText("Print code128\n");
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
        // 设置条码可识别字符位置在条码下方
        // 设置条码高度为60点
        esc.addSetBarcodeHeight((byte) 60);
        // 设置条码单元宽度为1
        esc.addSetBarcodeWidth((byte) 1);
        // 打印Code128码
        esc.addCODE128(esc.genCodeB("SMARNET"));
        esc.addPrintAndLineFeed();

        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        // 打印文字
        esc.addText("Print QRcode\n");
        // 设置纠错等级
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
        // 设置qrcode模块大小
        esc.addSelectSizeOfModuleForQRCode((byte) 3);
        // 设置qrcode内容
        esc.addStoreQRCodeData("www.smarnet.cc");
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        //打印文字
        esc.addText("Completed!\r\n");

        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);
        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播
        esc.addQueryPrinterStatus();
        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        try {
            mPort.writeDataImmediately(datas);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ethernetPort1.closePort();
        ethernetPort2.closePort();

        //  printerReader.isRun = false;


        Log.e("DOAING", "onDestroy");
    }


    /**
     * 判断是实时状态（10 04 02）还是查询状态（1D 72 01）
     */
    private int judgeResponseType(byte r) {
        byte FLAG = 0x10;
        return (byte) ((r & FLAG) >> 4);
    }
}

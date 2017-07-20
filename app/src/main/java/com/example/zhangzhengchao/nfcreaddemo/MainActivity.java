package com.example.zhangzhengchao.nfcreaddemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.com.fmsh.util.FM_Bytes;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // NFC适配器
    private NfcAdapter nfcAdapter = null;
    // 传达意图
    private PendingIntent pi = null;
    // 滤掉组件无法响应和处理的Intent
    private IntentFilter tagDetected = null;
    // 是否支持NFC功能的标签
    private boolean isNFC_support = false;
    // NFC TAG
    private Tag tagFromIntent;
    private String apdu;
    private NfcV nfcV;

    @BindView(R.id.promt)
     TextView textInfo;
    @BindView(R.id.tv_tagid)
    TextView tv_tagid;
    @BindView(R.id.tv_tag_status)
    TextView tv_tag_status;
    @OnClick(R.id.delete_btn)
    public void delete(){
        textInfo.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //绑定控件
        ButterKnife.bind(this);
        //初始化NFC
        initNFCData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNFC_support == false) {
            // 如果设备不支持NFC或者NFC功能没开启，就return掉
            return;
        }
        // 开始监听NFC设备是否连接
        startNFC_Listener();

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(this.getIntent()
                .getAction())) {
            // 注意这个if中的代码几乎不会进来，因为刚刚在上一行代码开启了监听NFC连接，下一行代码马上就收到了NFC连接的intent，这种几率很小
            // 处理该intent
            processIntent(this.getIntent());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isNFC_support == true) {
            // 当前Activity如果不在手机的最前端，就停止NFC设备连接的监听
            stopNFC_Listener();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 当前app正在前端界面运行，这个时候有intent发送过来，那么系统就会调用onNewIntent回调方法，将intent传送过来
        // 我们只需要在这里检验这个intent是否是NFC相关的intent，如果是，就调用处理方法
        Log.i(TAG, "进入NEWINTENT");
        Log.i(TAG,"action为："+intent.getAction());
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            processIntent(intent);
        }
    }


    private void initNFCData() {
        // 初始化设备支持NFC功能
        isNFC_support = true;
        // 得到默认nfc适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        // 提示信息定义
        String metaInfo = "";
        // 判定设备是否支持NFC或启动NFC
        if (nfcAdapter == null) {
            metaInfo = "设备不支持NFC！";
            Toast.makeText(this, metaInfo, Toast.LENGTH_SHORT).show();
            isNFC_support = false;
        }
        if (!nfcAdapter.isEnabled()) {
            metaInfo = "请在系统设置中先启用NFC功能！";
            Toast.makeText(this, metaInfo, Toast.LENGTH_SHORT).show();
            isNFC_support = false;
        }

        if (isNFC_support == true) {
            init_NFC();
        } else {
            textInfo.setTextColor(Color.RED);
            textInfo.setText(metaInfo);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.nfc_demo, menu);
        return true;
    }



    public void processIntent(Intent intent) {
        if (isNFC_support == false){
            return;
        }
        // 取出封装在intent中的TAG
        StringBuilder sb=new StringBuilder();
        tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        textInfo.setTextColor(Color.BLUE);
        tv_tag_status.setTextColor(Color.GREEN);
        tv_tag_status.setText("获取标签成功");
        String tagId = FM_Bytes.bytesToHexString(tagFromIntent.getId());
        apdu= "22B21d"+tagId+"11223344";
        tv_tagid.setText("标签ID为:"+ tagId);
        Toast.makeText(this, "找到卡片", Toast.LENGTH_SHORT).show();
        nfcV = NfcV.get(tagFromIntent);
        Log.i(TAG,"获得了nfcV");
        try {
            nfcV.connect();
            Log.i(TAG,"NFCV已连接");
            Log.i(TAG,nfcV.toString());
            String read=read(tagFromIntent, apdu);
            Log.i(TAG,"获得了read为："+read);
            sb.append("读取到的数据为:\n");
            sb.append(read+"\n");
            textInfo.append(sb);
            sb.delete(0,sb.length());

        } catch (IOException e) {
            Log.i(TAG,"IOException");
            e.printStackTrace();
        } catch (FormatException e) {
            Log.i(TAG,"FormatException");
            e.printStackTrace();
        }


    }

    // 读取方法
    private String read(Tag tag,String apdu) throws IOException, FormatException {
        if (tag != null) {
            //打开连接
            byte[] bytes = nfcV.transceive(FM_Bytes.hexStringToBytes(apdu));

            String read = FM_Bytes.bytesToHexString(bytes);

            return  read ;

        } else {
            Toast.makeText(MainActivity.this, "设备与nfc卡连接断开，请重新连接...",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

    }


    private void startNFC_Listener() {
        // 开始监听NFC设备是否连接，如果连接就发pi意图
        nfcAdapter.enableForegroundDispatch(this, pi,
				/*new IntentFilter[] { tagDetected }*/null, null);
        Log.i(TAG, "开启监听了");
    }

    private void stopNFC_Listener() {
        // 停止监听NFC设备是否连接
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void init_NFC() {
        // 初始化PendingIntent，当有NFC设备连接上的时候，就交给当前Activity处理
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // 新建IntentFilter，使用的是第二种的过滤机制
        tagDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
    }
}

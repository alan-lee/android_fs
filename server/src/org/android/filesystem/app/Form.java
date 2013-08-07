package org.android.filesystem.app;

import java.io.IOException;

import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.server.Server;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Form extends Activity {
	private Button start;
	private Button stop;
	private Button refresh;
	private TextView hostText;
	private EditText portText;
	private TextView tipText;
	private Server server;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        GlobalSetting.setRootDir(Environment.getExternalStorageDirectory().getPath());
        start = (Button) findViewById(R.id.start_button);
        stop = (Button) findViewById(R.id.stop_button);
        refresh = (Button) findViewById(R.id.refresh_button);
        hostText = (TextView) findViewById(R.id.host_text);
        portText = (EditText) findViewById(R.id.port_text);
        tipText = (TextView) findViewById(R.id.tip_text);
        
        fresh();
        
        start.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String host = hostText.getText().toString();
				int port = Integer.valueOf(portText.getText().toString());
				
				String mountTipFormat = getString(R.string.mount_tip_format);
				String mountTip = String.format(mountTipFormat, host, port);
				
				tipText.setText(mountTip);
				
				server = new Server(port);
				try {
					server.start();
					portText.setEnabled(false);
					stop.setEnabled(true);
					start.setEnabled(false);
					refresh.setEnabled(false);
				} catch (IOException e) {
					e.printStackTrace();
					try {
						server.stop();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
        });
        
        stop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(server != null){
					try {
						server.stop();
						fresh();
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
			}
        });
        
        refresh.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				fresh();
			}
        	
        });
        
    }
    
    private void fresh(){
    	 WifiManager wifiMgr=(WifiManager)getSystemService(Context.WIFI_SERVICE);
 		if(wifiMgr.isWifiEnabled())  {
 			WifiInfo wifiinfo= wifiMgr.getConnectionInfo();
 			int ipInt = wifiinfo.getIpAddress();
 			String ipAddress = (ipInt & 0xFF)+ "." + ((ipInt >> 8 ) & 0xFF) + "." + ((ipInt >> 16 ) & 0xFF) +"."+((ipInt >> 24 ) & 0xFF );
 			hostText.setText(ipAddress);
 			portText.setEnabled(true);
 			tipText.setText("");
 			start.setEnabled(true);
 			stop.setEnabled(false);
 			refresh.setEnabled(true);
 		}else{
 			tipText.setText(getString(R.string.no_network_tip));
         	start.setEnabled(false);
         	stop.setEnabled(false);
 			refresh.setEnabled(true);
         }
    }
    
    
}
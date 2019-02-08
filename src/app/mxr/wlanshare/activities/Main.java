package app.mxr.wlanshare.activities;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.github.ghmxr.wlanshare.R;


public class Main extends BaseActivity {
	
	Button Send,Receive;
	TextView tv_share;
	AlertDialog dialog_editname,dialog_about;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);
		findView();	
		
   
      Send.setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			LayoutInflater layoutInflater = LayoutInflater.from(Main.this);
			View dialogview = layoutInflater.inflate(R.layout.dialog_chose_selector,(ViewGroup)findViewById(R.id.relativelayout_dialog_chose_selector));
			ListView selector =(ListView) dialogview.findViewById(R.id.selectorlist);
			TextView selector_att = (TextView)dialogview.findViewById(R.id.selector_att);
			selector_att.setText(Main.this.getResources().getString(R.string.dialog_sendfileatt_message));
			
			List<Map<String,Object>> selectors = new ArrayList<Map <String,Object>>();
			
			Map<String,Object> item1 = new HashMap<String,Object>();
			item1.put("imgint", R.drawable.icon_filemanager);
			item1.put("selectorname", "选择文件");
			selectors.add(item1);
			
			Map<String,Object> item2 = new HashMap<String,Object>();
			item2.put("imgint", R.drawable.icon_media);
			item2.put("selectorname", "选择图片");
			selectors.add(item2);
			
			SimpleAdapter adapter=new SimpleAdapter(Main.this,selectors,R.layout.item_selector,new String[]{"imgint","selectorname"},new int[]{R.id.img_selector,R.id.text_selector});
			selector.setAdapter(adapter);
			
			
		final	AlertDialog attention =new AlertDialog.Builder(Main.this).setTitle(Main.this.getResources().getString(R.string.dialog_sendfileatt_title))
					.setView(dialogview).setNegativeButton(Main.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					}).create();
			
			
			attention.show();
			selector.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// TODO Auto-generated method stub
					attention.cancel();
					switch(position){
						default:break;
						case 0:{	//chose file					 						 
							Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  							
							intent.setType("*/*");
							intent.addCategory(Intent.CATEGORY_OPENABLE);  
							try {
							    Main.this.startActivityForResult( Intent.createChooser(intent, "选择文件"), 0);
							} catch (android.content.ActivityNotFoundException ex) {
							    Toast.makeText(Main.this, "未发现文件选择器",  Toast.LENGTH_SHORT).show();
							} 
						}
						break;
						case 1:{
							Intent intent = new Intent(
							        Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
							intent.setType("image/*");
							try {
							    Main.this.startActivityForResult( Intent.createChooser(intent, "选择图片"), 1);
							} catch (android.content.ActivityNotFoundException ex) {
							    Toast.makeText(Main.this, "未发现文件选择器",  Toast.LENGTH_SHORT).show();
							} 
						}
						break;
					}
				}
			});
			
		}
	});
      
      
      Receive.setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(Activity_Send.queue.size()>0){
				new AlertDialog.Builder(Main.this)
				.setTitle(Main.this.getResources().getString(R.string.dialog_main_receive_warn_title))
				.setMessage(Main.this.getResources().getString(R.string.dialog_main_receive_warn_message))
				.setIcon(R.drawable.icon_alertdialog_warn)
				.setPositiveButton(Main.this.getResources().getString(R.string.button_turnto), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent i = new Intent();
						i.setClass(Main.this, Activity_Send.class);
						Main.this.startActivity(i);
						Main.this.finish();
					}
				})
				.setNegativeButton(Main.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				})
				.show();
			}
			else{
				Intent i = new Intent();
				i.setClass(Main.this, Activity_Receive.class);
				Main.this.startActivity(i);
			}
		}
	});
        
      tv_share.setClickable(true);
      tv_share.setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			PackageManager pm=Main.this.getPackageManager();
			PackageInfo pinfo;
			File apkres;
			try{
				pinfo=pm.getPackageInfo("app.mxr.wlanshare", PackageManager.GET_ACTIVITIES);
				apkres=new File(pinfo.applicationInfo.sourceDir);
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("application/vnd.android.package-archive");
				intent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(apkres));
				intent.putExtra(Intent.EXTRA_SUBJECT, "分享"); 
				intent.putExtra(Intent.EXTRA_TEXT, "分享本应用");  
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(Intent.createChooser(intent,  "分享本应用"   )  );
			}catch(NameNotFoundException nf){
				nf.printStackTrace();
			}
			
			
		}
	});
       
	}
	
	private void findView(){
		 Send=(Button)findViewById(R.id.sendbt);
		 Receive=(Button)findViewById(R.id.receivebt);
		 tv_share=(TextView)findViewById(R.id.text_share);
	}
	
	public void onWifiStateChanged(int state){
		
	}
	
	public void onAPStateChanged(int state){
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onMenuOpened(int featureId, Menu menu)  {  
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {  
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {  
                try {  
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);  
                    m.setAccessible(true);  
                    m.invoke(menu, true);  
                } catch (Exception e) {  
                }  
            }  
        }  
        return super.onMenuOpened(featureId, menu);  
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		final SharedPreferences settings=getSharedPreferences("settings",Activity.MODE_PRIVATE);
		final SharedPreferences.Editor editor = settings.edit();
		
		
		if (id == R.id.action_settings) {
			
			LayoutInflater layoutInflater = LayoutInflater.from(Main.this);
			View dialogview = layoutInflater.inflate(R.layout.dialog_editname,(ViewGroup)findViewById(R.id.relativelayout_dialog_editname));
			final EditText editname =(EditText)dialogview.findViewById(R.id.devicename_edit);
			TextView editatt = (TextView)dialogview.findViewById(R.id.editname_att);
			if(dialog_editname!=null){
				dialog_editname.cancel();
				dialog_editname=null;
			}
			
				dialog_editname=new AlertDialog.Builder(this)
						.setTitle(this.getResources().getString(R.string.dialog_editname_title))
						.setView(dialogview)
						.setIcon(android.R.drawable.ic_menu_manage)
						.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), null)
						.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), null)
						.show();
				editatt.setText(this.getResources().getString(R.string.dialog_editname_message));
				editname.setText(settings.getString(Main.KEY_DEVICENAME,"Android" ));
				dialog_editname.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						String getstring = editname.getText().toString().trim();
						if(getstring.equals("")||getstring.length()==0){
							Toast.makeText(Main.this, "请输入有效字符", Toast.LENGTH_SHORT).show();
							return;
						}
						else{
							editor.putString(Main.KEY_DEVICENAME, getstring);
							editor.commit();
							dialog_editname.cancel();
						}
					}
				});
				dialog_editname.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						dialog_editname.cancel();
					}
				});
			
			
			return true;
		}	
		if(id==R.id.action_about){
			if(dialog_about!=null){
				dialog_about.cancel();
				dialog_about=null;
			}
			
			LayoutInflater layoutInflater = LayoutInflater.from(Main.this);
			View dialogview = layoutInflater.inflate(R.layout.dialog_about,(ViewGroup)findViewById(R.id.scrollview_dialog_about));
		//	TextView tv_about_1=(TextView)dialogview.findViewById(R.id.about_1);
		//	TextView tv_about_2=(TextView)dialogview.findViewById(R.id.about_email);
			dialog_about = new AlertDialog.Builder(this)
					.setTitle(this.getResources().getString(R.string.dialog_about_title))
					.setIcon(R.drawable.ic_launcher)
					.setView(dialogview)
					.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					})
					.show();
			
			return true;
			
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void finish(){
		super.finish();		
	}
	
	protected void onActivityResult(int requestCode,int resultCode,Intent data){
		//super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == BaseActivity.RESULT_OK) {
			switch(requestCode){
				default:break;
				case 0:{  //result is a file;
					Intent intent0 = new Intent(Intent.ACTION_SEND);
					intent0.setType("*/*");
					intent0.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(new File(data.getData().getPath())));
					intent0.setClass(this, Activity_Send.class);
					startActivity(intent0);
				}
				break;
				case 1:{
					Intent intent1 = new Intent(Intent.ACTION_SEND);
					intent1.setType("image/*");
					intent1.putExtra(Intent.EXTRA_STREAM,data.getData());
					intent1.setClass(this, Activity_Send.class);
					startActivity(intent1);
					
				}
				break;
			}
		
		} 
		
		
	}
		
	
}

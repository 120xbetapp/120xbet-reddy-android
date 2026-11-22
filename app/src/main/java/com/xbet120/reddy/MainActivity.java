package com.xbet120.reddy;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.net.Uri;
import android.webkit.CookieManager;

public class MainActivity extends Activity {
  private WebView web; private FrameLayout root; private ValueCallback<Uri[]> uploadCallback;
  private static final int PICK_FILE = 42;
  @Override public void onCreate(Bundle state){super.onCreate(state);
    createNotificationChannel();
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},73);}
    getWindow().setStatusBarColor(Color.WHITE);getWindow().setNavigationBarColor(Color.WHITE);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    root=new FrameLayout(this);root.setBackgroundColor(Color.WHITE);setContentView(root);
    ImageView splash=new ImageView(this);splash.setImageResource(R.drawable.splash_logo);splash.setScaleType(ImageView.ScaleType.FIT_CENTER);
    FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(145),dp(145),Gravity.CENTER);root.addView(splash,sp);
    new Handler(Looper.getMainLooper()).postDelayed(()->{root.removeView(splash);showWeb();},1350);
  }
  private void createNotificationChannel(){
    if(Build.VERSION.SDK_INT>=26){
      NotificationChannel channel=new NotificationChannel("reddy_alerts","Reddy notifications",NotificationManager.IMPORTANCE_HIGH);
      channel.setDescription("Balance, request and admin messages");
      NotificationManager manager=getSystemService(NotificationManager.class);
      if(manager!=null)manager.createNotificationChannel(channel);
    }
  }
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private void showWeb(){
    web=new WebView(this);web.setBackgroundColor(Color.WHITE);web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);web.getSettings().setDatabaseEnabled(true);
    web.getSettings().setAllowFileAccess(true);web.getSettings().setAllowContentAccess(true);
    web.getSettings().setMediaPlaybackRequiresUserGesture(false);
    web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
    web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,String url){
      if(url.startsWith("https://")||url.startsWith("http://")){v.loadUrl(url);return true;}return false;
    }});
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams params){
        if(uploadCallback!=null)uploadCallback.onReceiveValue(null);uploadCallback=cb;
        try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}
        catch(Exception ex){uploadCallback=null;return false;}
      }
      @Override public void onPermissionRequest(PermissionRequest request){runOnUiThread(()->request.deny());}
    });
    root.addView(web,new FrameLayout.LayoutParams(-1,-1));
    web.loadUrl("file:///android_asset/reddy-3.html");
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
    if(request==PICK_FILE && uploadCallback!=null){uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));uploadCallback=null;}
  }
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}

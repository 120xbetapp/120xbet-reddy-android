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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
  private static final String RELEASE="https://120xbetapp.github.io/120xbet-reddy-android/web-release.json";
  private final java.util.concurrent.ExecutorService updates=java.util.concurrent.Executors.newSingleThreadExecutor();
  private boolean checking=false;
  private java.io.File updatedHtml(){return new java.io.File(getFilesDir(),"reddy-index.html");}
  private java.net.HttpURLConnection connect(String url) throws Exception {
    if(!url.startsWith("https://120xbetapp.github.io/120xbet-reddy-android/")) throw new java.io.IOException("Invalid update URL");
    java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
    c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setUseCaches(false);
    if(c.getResponseCode()!=200){c.disconnect();throw new java.io.IOException("Update unavailable");}return c;
  }
  private void checkUpdate(){
    if(checking)return;checking=true;
    updates.execute(()->{try{
      java.net.HttpURLConnection c=connect(RELEASE+"?t="+System.currentTimeMillis());
      java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();
      try(java.io.InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){b.write(buf,0,n);if(b.size()>65536)throw new java.io.IOException("Manifest too large");}}finally{c.disconnect();}
      org.json.JSONObject m=new org.json.JSONObject(b.toString("UTF-8"));
      int version=m.getInt("versionCode");
      if(version>getPreferences(0).getInt("webVersion",0))runOnUiThread(()->offerUpdate(m));
    }catch(Exception e){android.util.Log.w("ReddyUpdate","Check failed",e);}finally{runOnUiThread(()->checking=false);}});
  }
  private void offerUpdate(org.json.JSONObject m){
    if(isFinishing()||isDestroyed())return;
    new android.app.AlertDialog.Builder(this,android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK)
      .setTitle("A new version available").setMessage("Version: "+m.optString("versionName")+"\n\nAdded:\n"+m.optString("notes")+"\n\nHTML update · No APK installation required")
      .setNegativeButton("Later",null).setPositiveButton("Update",(d,w)->downloadUpdate(m)).show();
  }
  private void downloadUpdate(org.json.JSONObject m){
    android.app.ProgressDialog progress=new android.app.ProgressDialog(this,android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK);
    progress.setTitle("Downloading update");progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);progress.setMax(100);progress.setCancelable(false);
    progress.setButton(android.content.DialogInterface.BUTTON_NEGATIVE,"Hide",(d,w)->{});progress.show();
    updates.execute(()->{java.io.File tmp=new java.io.File(getFilesDir(),"reddy-index.tmp");try{
      java.net.HttpURLConnection c=connect(m.getString("htmlUrl"));long expected=m.getLong("bytes");if(expected<=0||expected>10000000)throw new java.io.IOException("Invalid size");
      java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");long total=0;
      try(java.io.InputStream in=c.getInputStream();java.io.FileOutputStream out=new java.io.FileOutputStream(tmp)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){total+=n;if(total>expected)throw new java.io.IOException("Size mismatch");out.write(buf,0,n);digest.update(buf,0,n);final int pct=(int)(total*100/expected);runOnUiThread(()->progress.setProgress(pct));}out.getFD().sync();}finally{c.disconnect();}
      StringBuilder hash=new StringBuilder();for(byte x:digest.digest())hash.append(String.format(java.util.Locale.US,"%02x",x&255));
      if(total!=expected||!hash.toString().equalsIgnoreCase(m.getString("sha256")))throw new java.io.IOException("Integrity check failed");
      android.system.Os.rename(tmp.getAbsolutePath(),updatedHtml().getAbsolutePath());getPreferences(0).edit().putInt("webVersion",m.getInt("versionCode")).commit();
      runOnUiThread(()->{if(isDestroyed()||isFinishing())return;progress.dismiss();web.reload();});
    }catch(Exception e){tmp.delete();runOnUiThread(()->{if(isDestroyed()||isFinishing())return;progress.dismiss();new android.app.AlertDialog.Builder(this).setMessage("Update failed. Previous version retained.").setPositiveButton("OK",null).show();});}});
  }

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
    web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(true);
    web.getSettings().setMediaPlaybackRequiresUserGesture(false);
    web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
      .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request){
        Uri u=request.getUrl();
        if("https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&"/assets/index.html".equals(u.getPath())&&updatedHtml().isFile()){
          try{return new WebResourceResponse("text/html","UTF-8",new java.io.FileInputStream(updatedHtml()));}catch(java.io.IOException ignored){}
        }
        return loader.shouldInterceptRequest(u);
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,String url){
      if("reddy-local://exit".equals(url)&&"https://appassets.androidplatform.net/assets/index.html".equals(v.getUrl())){finish();return true;}
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
    web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    checkUpdate();
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
    if(request==PICK_FILE && uploadCallback!=null){uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));uploadCallback=null;}
  }
  @Override public void onBackPressed(){if(web==null){super.onBackPressed();return;} web.evaluateJavascript("typeof window.reddyBack === 'function' && window.reddyBack()", result -> {if("\"exit-confirm\"".equals(result)){web.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);new android.app.AlertDialog.Builder(this).setIcon(R.drawable.splash_logo).setTitle("Reddy").setMessage("Do you want to exit the app?").setNegativeButton("Cancel",null).setPositiveButton("Exit",(d,w)->finish()).show();return;}if(!"true".equals(result)){if(web.canGoBack())web.goBack();else finish();}});}
  @Override protected void onDestroy(){updates.shutdown();if(web!=null)web.destroy();super.onDestroy();}
}

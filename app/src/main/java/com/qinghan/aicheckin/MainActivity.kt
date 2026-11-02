package com.qinghan.aicheckin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity:Activity(){
 private val bg=Color.rgb(11,20,36)
 private val surface=Color.rgb(27,43,63)
 private val textColor=Color.rgb(232,244,249)
 private val secondary=Color.rgb(150,174,190)
 private val accent=Color.rgb(122,221,229)
 private var current="home"
 private var busy=false
 private lateinit var content:LinearLayout
 private val vault by lazy{Vault(applicationContext)}

 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=bg;window.navigationBarColor=bg
  Scheduler.update(this,vault.isAuto())
  showScreen()
 }
 override fun onResume(){super.onResume();if(::content.isInitialized)showScreen()}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun shape(color:Int,r:Int=20,stroke:Int=0):GradientDrawable{
  return GradientDrawable().apply{
   cornerRadius=dp(r).toFloat();setColor(color)
   if(stroke!=0)setStroke(dp(1),stroke)
  }
 }
 private fun label(v:String,size:Float=15f,bold:Boolean=false,color:Int=textColor):TextView{
  return TextView(this).apply{
   text=v;textSize=size;setTextColor(color)
   if(bold)typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
   setLineSpacing(dp(3).toFloat(),1f)
  }
 }
 private fun space(n:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(n))}
 private fun stack():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 private fun gapBox(view:View,marginTop:Int=0){
  val params=LinearLayout.LayoutParams(-1,-2)
  params.topMargin=dp(marginTop)
  content.addView(view,params)
 }
 private fun panel():LinearLayout=stack().apply{
  background=shape(surface,23,Color.rgb(50,78,96))
  setPadding(dp(18),dp(18),dp(18),dp(18))
 }
 private fun action(label:String,primary:Boolean=false,click:()->Unit):Button{
  val b=Button(this)
  b.text=label;b.isAllCaps=false;b.textSize=13f
  b.setTextColor(if(primary)bg else textColor)
  b.background=shape(if(primary)accent else Color.rgb(44,64,86),15)
  b.setPadding(dp(12),dp(4),dp(12),dp(4))
  b.setOnClickListener{if(!busy)click()}
  return b
 }
 private fun note(msg:String){Toast.makeText(this,msg,Toast.LENGTH_LONG).show()}
 private fun screenHeader(){
  gapBox(label("领签  /  LINGQIAN",13f,true,accent))
  gapBox(label(if(current=="home")"把每日奖励，交给时间。" else "执行记录",27f,true),11)
  gapBox(label("本机授权 · 本机存储 · 无云服务器",12f,false,secondary),7)
 }
 private fun showScreen(){
  val frame=stack()
  frame.setBackgroundColor(bg)
  setContentView(frame)
  val scroll=ScrollView(this)
  scroll.isFillViewport=false
  frame.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  content=stack();content.setPadding(dp(20),dp(26),dp(20),dp(24))
  scroll.addView(content)
  screenHeader()
  if(current=="home")home() else logs()
  val nav=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
   background=shape(Color.rgb(23,41,60),27,Color.rgb(59,93,110))
   setPadding(dp(8),dp(7),dp(8),dp(7))
  }
  val homeBtn=action(if(current=="home")"⌂  首页" else "首页",current=="home"){current="home";showScreen()}
  val logBtn=action(if(current=="logs")"▤  日志" else "日志",current=="logs"){current="logs";showScreen()}
  val hp=LinearLayout.LayoutParams(0,dp(52),1f);hp.rightMargin=dp(8)
  nav.addView(homeBtn,hp);nav.addView(logBtn,LinearLayout.LayoutParams(0,dp(52),1f))
  val nlp=LinearLayout.LayoutParams(-1,-2)
  nlp.setMargins(dp(20),dp(6),dp(20),dp(20))
  frame.addView(nav,nlp)
 }
 private fun home(){
  val accounts=try{vault.accounts()}catch(e:Exception){note("读取本地加密账户失败："+e.message);emptyList()}
  val success=accounts.count{it.lastDay==today()&&(it.lastState.startsWith("已领取")||it.lastState.startsWith("已签到"))}
  val summary=panel()
  summary.addView(label("今日签到",13f,false,secondary))
  summary.addView(space(6))
  summary.addView(label(success.toString()+" / "+accounts.size,33f,true,accent))
  summary.addView(label(if(accounts.isEmpty())"尚未绑定账号" else "已绑定 "+accounts.size+" 个账号",12f,false,secondary))
  gapBox(summary,23)
  val buttons=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  val w=action("+ WorkBuddy",false){startLogin("workbuddy")}
  val q=action("+ Qoder CN",false){startLogin("qoder-cn")}
  val bp=LinearLayout.LayoutParams(0,dp(52),1f);bp.rightMargin=dp(8)
  buttons.addView(w,bp);buttons.addView(q,LinearLayout.LayoutParams(0,dp(52),1f))
  gapBox(buttons,15)
  val all=action(if(busy)"正在执行…" else "立即签到全部账号",true){runCheckin(null)}
  gapBox(all,13)
  val autoPanel=panel()
  val toggle=Switch(this)
  toggle.text="后台自动签到"
  toggle.textSize=15f;toggle.setTextColor(textColor)
  toggle.isChecked=vault.isAuto()
  toggle.setOnCheckedChangeListener{_,v->
   vault.auto(v);Scheduler.update(this,v)
   vault.log(if(v)"已开启系统后台周期任务" else "已关闭系统后台任务")
   note(if(v)"已提交系统每 6 小时巡检任务" else "后台任务已关闭")
  }
  autoPanel.addView(toggle)
  autoPanel.addView(space(5))
  autoPanel.addView(label("系统调度执行，不保证精确时间；需网络和有效登录态。",12f,false,secondary))
  gapBox(autoPanel,14)
  gapBox(label("已绑定账号",19f,true),23)
  if(accounts.isEmpty()){
   gapBox(label("点击上方平台按钮，在官方网页完成授权。",13f,false,secondary),12)
  }
  accounts.forEach{a->
   val card=panel()
   val title=if(a.provider=="workbuddy")"WorkBuddy  ·  国内版" else "Qoder  ·  国内版"
   card.addView(label(title,12f,true,accent))
   card.addView(space(6))
   card.addView(label(a.name,18f,true))
   card.addView(space(4))
   card.addView(label(a.lastState,12f,false,secondary))
   card.addView(space(12))
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
   val sign=action("签到",true){runCheckin(a.id)}
   val remove=action("移除账号"){confirmRemove(a)}
   val sp=LinearLayout.LayoutParams(0,dp(45),1f);sp.rightMargin=dp(10)
   row.addView(sign,sp);row.addView(remove,LinearLayout.LayoutParams(0,dp(45),1f))
   card.addView(row)
   gapBox(card,12)
  }
  gapBox(label("提示：Qoder 国际版签到需要官方设备身份验证，当前版本不支持。",12f,false,secondary),16)
 }
 private fun logs(){
  gapBox(label("最近操作",19f,true),22)
  val arr=vault.logs()
  if(arr.isEmpty())gapBox(label("还没有执行记录。",13f,false,secondary),12)
  arr.forEach{line->
   val p=panel();p.addView(label(line,13f))
   gapBox(p,9)
  }
 }
 private fun today():String=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).apply{
  timeZone=java.util.TimeZone.getTimeZone("Asia/Shanghai")
 }.format(java.util.Date())
 private fun runCheckin(id:String?){
  busy=true;note("签到执行中，正在向官方接口请求")
  Thread{
   try{
    val out=if(id==null)CheckinEngine.runAll(applicationContext,false).joinToString("\n")
    else{
     val acc=vault.accounts().find{it.id==id}?:throw Exception("账号不存在")
     CheckinEngine.runOne(applicationContext,acc)
    }
    runOnUiThread{busy=false;showScreen();note(out.take(300).ifBlank{"没有绑定账号"})}
   }catch(e:Exception){runOnUiThread{busy=false;note("执行失败："+e.message);showScreen()}}
  }.start()
 }
 private fun startLogin(provider:String){
  busy=true
  Thread{
   try{
    val s=AuthFlow.start(provider)
    runOnUiThread{
     busy=false
     note("请在浏览器完成授权，再返回本应用")
     startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(s.url)))
    }
    val timeout=if(provider=="workbuddy")10*60*1000L else 5*60*1000L
    while(System.currentTimeMillis()-s.startedAt<timeout){
     Thread.sleep(if(provider=="workbuddy")3000 else 2000)
     val a=AuthFlow.poll(s)
     if(a!=null){
      vault.put(a);vault.log("授权成功："+a.provider+" / "+a.name.take(24))
      runOnUiThread{note("授权完成："+a.name);showScreen()}
      return@Thread
     }
    }
    runOnUiThread{note("授权超时，请重新添加")}
   }catch(e:Exception){
    runOnUiThread{busy=false;note("授权失败："+e.message);showScreen()}
   }
  }.start()
 }
 private fun confirmRemove(a:SignAccount){
  AlertDialog.Builder(this).setTitle("移除账号？")
   .setMessage("仅删除本机的账号凭据，不会注销官方账号。")
   .setPositiveButton("移除"){_,_->vault.remove(a.id);vault.log("移除账号："+a.provider);showScreen()}
   .setNegativeButton("取消",null).show()
 }
}

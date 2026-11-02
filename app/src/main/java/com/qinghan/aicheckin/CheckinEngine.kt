package com.qinghan.aicheckin

import android.content.Context
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CheckinEngine {
 private fun day():String=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{
  timeZone=TimeZone.getTimeZone("Asia/Shanghai")
 }.format(Date())
 fun runAll(ctx:Context,skipDone:Boolean=true):List<String>{
  val vault=Vault(ctx)
  val out=mutableListOf<String>()
  for(a in vault.accounts()){
   if(skipDone && a.lastDay==day() && (a.lastState.startsWith("已领取")||a.lastState.startsWith("已签到")))continue
   val m=runOne(ctx,a)
   out.add(a.name+": "+m)
  }
  if(out.isEmpty())vault.log("当前无需重复签到")
  return out
 }
 fun runOne(ctx:Context,a:SignAccount):String {
  val vault=Vault(ctx)
  val result=try {
   when(a.provider){"workbuddy"->workbuddy(a);"qoder-cn"->qoder(a);else->"不支持此平台"}
  }catch(e:Exception){
   "失败："+(e.message?:"网络或接口错误").take(90)
  }
  a.lastDay=day();a.lastState=result
  vault.put(a)
  vault.log(a.provider+" / "+a.name.take(24)+" / "+result)
  return result
 }
 private fun refresh(a:SignAccount){
  if(a.refreshToken.isBlank())throw Exception("凭据过期，需重新登录")
  val host="www.codebuddy.cn"
  val h=mapOf(
   "X-Refresh-Token" to a.refreshToken,"X-Auth-Refresh-Source" to "plugin",
   "X-Domain" to host,"X-Product" to "SaaS",
   "User-Agent" to "CLI/2.108.1 CodeBuddy/2.108.1",
   "X-Requested-With" to "XMLHttpRequest"
  )
  val r=Net.send("https://"+host+"/v2/plugin/auth/token/refresh","POST",h)
  val obj=r.data.optJSONObject("data")
  if(r.status!=200||r.code!=0||obj==null||obj.optString("accessToken").isBlank())throw Exception("续期失败，需重新授权")
  a.token=obj.optString("accessToken")
  a.refreshToken=obj.optString("refreshToken",a.refreshToken)
  if(obj.optLong("expiresIn")>0)a.expiresAt=System.currentTimeMillis()+obj.optLong("expiresIn")*1000
  a.uid=Net.jwtSub(a.token).ifBlank{a.uid}
 }
 private fun wbRequest(a:SignAccount,path:String):Reply{
  return Net.send("https://www.codebuddy.cn"+path,"POST",
   Net.wbHeaders(a.token,"www.codebuddy.cn",a.uid))
 }
 private fun workbuddy(a:SignAccount):String{
  if(a.expiresAt in 1..(System.currentTimeMillis()+60_000))refresh(a)
  var st=wbRequest(a,"/v2/billing/meter/checkin-activity-status")
  if(st.status==401){refresh(a);st=wbRequest(a,"/v2/billing/meter/checkin-activity-status")}
  if(st.status==401||st.status==403)return "授权已失效，请重新登录"
  val sd=if(st.status==200&&st.code==0)st.data.optJSONObject("data") else null
  if(sd?.optBoolean("today_checked_in")==true||sd?.optBoolean("checked_in")==true)
   return "已签到（今日已领取）"
  val res=wbRequest(a,"/v2/billing/meter/daily-checkin")
  val code=res.data.optInt("code",-1)
  if(res.status in 200..299 && (code==0||code==-1)){
   val ds=res.data.optJSONObject("data")
   val credit=ds?.optInt("credit",ds.optInt("today_credit",0))?:0
   return "已领取"+(if(credit>0)" +"+credit+" Credits" else "（金额待官方核实）")
  }
  val msg=res.data.optString("msg",res.data.optString("message"))
  if(code==10001||msg.contains("已签到")||msg.contains("已领取"))return "已签到（今日已领取）"
  return "失败：HTTP "+res.status+" code "+code
 }
 private fun qoder(a:SignAccount):String{
  if(a.expiresAt>0&&a.expiresAt<System.currentTimeMillis())return "授权已过期，需重新登录"
  val base="https://openapi.qoder.com.cn"
  val h=Net.qHeaders(a.token)
  val list=Net.send(base+"/sash/api/v1/me/campaigns?clientType=10","GET",h)
  if(list.status==401||list.status==403)return "授权已失效，请重新登录"
  if(list.status!=200)return "失败：活动列表 HTTP "+list.status
  val arr=list.data.optJSONArray("campaigns")?:return "失败：活动列表格式变化"
  a.uid=list.data.optString("uid",a.uid)
  var count=0
  var hasClaim=false
  var total=0
  var failed=0
  for(i in 0 until arr.length()){
   val item=arr.optJSONObject(i)?:continue
   if(item.optString("actionType")!="CLAIM_BENEFIT")continue
   hasClaim=true
   if(item.optString("claimStatus")!="CLAIMABLE")continue
   val id=item.optString("campaignId")
   if(id.isBlank())continue
   val r=Net.send(base+"/sash/api/v1/me/campaigns/"+URLEncoder.encode(id,"UTF-8")+"/claim","POST",h)
   if(r.status in 200..299){
    count++
    val amount=r.data.optJSONObject("benefit")?.optInt("amount",item.optJSONObject("benefit")?.optInt("amount",0)?:0)?:0
    total+=amount
   }else failed++
  }
  if(count>0)return "已领取 "+count+" 项"+(if(total>0)"，+"+total+" Credits"else "（金额待核实）")+(if(failed>0)"，"+failed+"项失败"else "")
  if(failed>0)return "失败："+failed+" 项领取失败"
  return if(hasClaim)"已签到（当前活动已领取）"else "无可领取的活动（并非已签到）"
 }
}

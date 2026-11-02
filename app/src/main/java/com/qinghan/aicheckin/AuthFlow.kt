package com.qinghan.aicheckin

import android.util.Base64
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class LoginSession(
 val provider:String,val url:String,val nonce:String="",val verifier:String="",
 var token:JSONObject?=null,var accountAttempts:Int=0,
 val startedAt:Long=System.currentTimeMillis()
)
object AuthFlow {
 private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
 fun start(provider:String):LoginSession{
  if(provider=="workbuddy"){
   val host="www.codebuddy.cn"
   val r=Net.send("https://"+host+"/v2/plugin/auth/state?platform=WorkBuddy",
    "POST",Net.wbLoginHeaders())
   val data=r.data.optJSONObject("data")
   if(r.status!=200||r.code!=0||data==null||data.optString("state").isBlank())throw Exception("WorkBuddy 初始化授权失败")
   val u=data.optString("authUrl")
   require(u.startsWith("https://")){"官方授权链接不是HTTPS"}
   return LoginSession(provider,u,nonce=data.getString("state"))
  }
  require(provider=="qoder-cn"){"不支持的账号来源"}
  val raw=ByteArray(32);SecureRandom().nextBytes(raw)
  val verifier=Base64.encodeToString(raw,Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  val sha=MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8))
  val challenge=Base64.encodeToString(sha,Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  val nonce=UUID.randomUUID().toString()
  val url="https://qoder.cn/device/selectAccounts?challenge="+enc(challenge)+
   "&challenge_method=S256&machine_id="+enc(UUID.randomUUID().toString())+"&nonce="+enc(nonce)
  return LoginSession(provider,url,nonce,verifier)
 }
 fun poll(s:LoginSession):SignAccount? {
  return if(s.provider=="workbuddy")pollWorkbuddy(s) else pollQoder(s)
 }
 private fun pollWorkbuddy(s:LoginSession):SignAccount? {
  val host="www.codebuddy.cn"
  val st=s.nonce
  if(s.token==null){
   val h=Net.wbLoginHeaders()+mapOf("X-No-Enterprise-Id" to "true","X-No-Department-Info" to "true")
   val r=Net.send("https://"+host+"/v2/plugin/auth/token?state="+enc(st),"GET",h)
   if(r.code==11217||r.status==429||r.status>=500)return null
   val data=r.data.optJSONObject("data")
   if(r.status!=200||r.code!=0||data==null||data.optString("accessToken").isBlank())throw Exception("WorkBuddy 授权失败："+r.msg)
   s.token=data
  }
  val data=s.token!!
  val access=data.optString("accessToken")
  var name="WorkBuddy 账号"
  var uid=Net.jwtSub(access)
  try {
   val h=Net.wbHeaders(access,host,uid)+mapOf(
    "X-No-User-Id" to "true","X-No-Enterprise-Id" to "true","X-No-Department-Info" to "true")
   val a=Net.send("https://"+host+"/v2/plugin/login/account?state="+enc(st),"GET",h)
   val info=a.data.optJSONObject("data")
   if(a.code==12151&&s.accountAttempts++<7)return null
   if(a.code==0&&info!=null){
    uid=info.optString("uid",uid)
    name=info.optString("name",info.optString("nickname","WorkBuddy 账号")).ifBlank{"WorkBuddy 账号"}
   }
  }catch(_:Exception){
   if(s.accountAttempts++<7)return null
  }
  val exp=if(data.optLong("expiresIn")>0)System.currentTimeMillis()+data.optLong("expiresIn")*1000 else 0L
  return SignAccount(UUID.randomUUID().toString(),"workbuddy",name,uid,access,
   data.optString("refreshToken"),host,exp)
 }
 private fun pollQoder(s:LoginSession):SignAccount?{
  val url="https://openapi.qoder.com.cn/api/v1/deviceToken/poll?nonce="+enc(s.nonce)+
   "&verifier="+enc(s.verifier)+"&challenge_method=S256"
  val r=Net.send(url,"GET",mapOf("User-Agent" to "Go-http-client/2.0"))
  if(r.status==202||r.status==404)return null
  if(r.status!=200)throw Exception("Qoder 授权失败 HTTP "+r.status)
  val token=r.data.optString("token")
  if(token.isBlank())throw Exception("Qoder 授权成功响应缺少令牌")
  var name="Qoder CN 账号"
  try {
   val user=Net.send("https://openapi.qoder.com.cn/api/v1/userinfo","GET",mapOf(
    "Authorization" to "Bearer "+token,"User-Agent" to "Go-http-client/2.0"))
   name=user.data.optString("name",user.data.optString("username",name)).ifBlank{name}
  }catch(_:Exception){}
  val expire=r.data.optLong("expires_in")
  return SignAccount(UUID.randomUUID().toString(),"qoder-cn",name,
   r.data.optString("user_id"),token,r.data.optString("refresh_token"),"",
   if(expire>0)System.currentTimeMillis()+expire*1000 else 0L)
 }
}

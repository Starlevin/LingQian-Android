package com.qinghan.aicheckin

import android.util.Base64
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class Reply(val status:Int,val data:JSONObject){
 val code:Int get()=data.optInt("code",status)
 val msg:String get()=data.optString("msg","HTTP "+status).take(120)
}
object Net{
 fun send(url:String,method:String="GET",headers:Map<String,String> = emptyMap(),body:String="{}"):Reply{
  val u=URL(url)
  require(u.protocol=="https"){"只允许HTTPS"}
  require(u.host in setOf("www.codebuddy.cn","openapi.qoder.com.cn")){"非允许的服务端"}
  val c=u.openConnection() as HttpsURLConnection
  c.requestMethod=method;c.connectTimeout=12000;c.readTimeout=12000;c.instanceFollowRedirects=false
  c.setRequestProperty("Accept","application/json")
  headers.forEach{(k,v)->c.setRequestProperty(k,v)}
  try{
   if(method=="POST"){
    c.doOutput=true;c.setRequestProperty("Content-Type","application/json")
    c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
   }
   val status=c.responseCode
   val stream=if(status<400)c.inputStream else c.errorStream
   val str=stream?.bufferedReader(Charsets.UTF_8)?.use{it.readText().take(300000)}?:""
   return Reply(status,try{JSONObject(str)}catch(_:Exception){JSONObject()})
  }finally{c.disconnect()}
 }
 fun jwtSub(jwt:String):String=try{
  JSONObject(String(Base64.decode(jwt.split(".")[1],Base64.URL_SAFE or Base64.NO_WRAP),Charsets.UTF_8)).optString("sub")
 }catch(_:Exception){""}
 fun wbHeaders(token:String,host:String,uid:String)=mapOf(
  "Authorization" to "Bearer "+token,"X-User-Id" to uid,"X-Domain" to host,
  "X-Product" to "SaaS","User-Agent" to "CLI/2.108.1 CodeBuddy/2.108.1"
 )
 fun wbLoginHeaders()=mapOf(
  "X-Domain" to "www.codebuddy.cn","X-Product" to "SaaS",
  "User-Agent" to "CLI/2.108.1 CodeBuddy/2.108.1",
  "X-Requested-With" to "XMLHttpRequest","X-No-Authorization" to "true","X-No-User-Id" to "true"
 )
 fun qHeaders(token:String)=mapOf(
  "Authorization" to "Bearer "+token,"User-Agent" to "Qoder",
  "Cosy-ClientType" to "10","Cosy-Version" to "0.3.3","Cosy-MachineOS" to "android"
 )
}

package com.qinghan.aicheckin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SignAccount(
 val id: String, val provider: String, var name: String, var uid: String,
 var token: String, var refreshToken: String = "", var host: String = "",
 var expiresAt: Long = 0L, var lastDay: String = "", var lastState: String = "等待签到"
) {
 fun json() = JSONObject().apply {
  put("id",id); put("provider",provider); put("name",name); put("uid",uid)
  put("token",token); put("refreshToken",refreshToken); put("host",host)
  put("expiresAt",expiresAt); put("lastDay",lastDay); put("lastState",lastState)
 }
 companion object { fun from(j:JSONObject)=SignAccount(
  j.optString("id"),j.optString("provider"),j.optString("name"),j.optString("uid"),
  j.optString("token"),j.optString("refreshToken"),j.optString("host"),
  j.optLong("expiresAt"),j.optString("lastDay"),j.optString("lastState","等待签到")
 ) }
}
class Vault(private val ctx:Context) {
 private val prefs=ctx.getSharedPreferences("local_vault",Context.MODE_PRIVATE)
 private fun key():SecretKey {
  val ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null)
  (ks.getKey("lingqian_aes_v1",null) as? SecretKey)?.let{return it}
  val g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
  g.init(KeyGenParameterSpec.Builder("lingqian_aes_v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
  return g.generateKey()
 }
 @Synchronized fun accounts():MutableList<SignAccount> {
  val value=prefs.getString("accounts",null)?:return mutableListOf()
  val raw=Base64.decode(value,Base64.NO_WRAP)
  require(raw.size>=13){"账号密文已损坏"}
  val c=Cipher.getInstance("AES/GCM/NoPadding")
  c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,raw.copyOfRange(0,12)))
  val a=JSONArray(String(c.doFinal(raw.copyOfRange(12,raw.size)),StandardCharsets.UTF_8))
  return (0 until a.length()).map{SignAccount.from(a.getJSONObject(it))}.toMutableList()
 }
 @Synchronized fun put(a:SignAccount) {
  val list=accounts()
  val i=list.indexOfFirst{it.provider==a.provider && (it.uid.isNotBlank() && it.uid==a.uid || it.id==a.id)}
  if(i>=0)list[i]=a else list.add(a)
  save(list)
 }
 @Synchronized fun remove(id:String){save(accounts().filterNot{it.id==id})}
 private fun save(list:List<SignAccount>){
  val arr=JSONArray();list.forEach{arr.put(it.json())}
  val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key())
  val bytes=c.iv+c.doFinal(arr.toString().toByteArray(StandardCharsets.UTF_8))
  check(prefs.edit().putString("accounts",Base64.encodeToString(bytes,Base64.NO_WRAP)).commit()){"保存账户失败"}
 }
 fun isAuto()=prefs.getBoolean("auto",false)
 fun auto(v:Boolean){prefs.edit().putBoolean("auto",v).apply()}
 @Synchronized fun log(s:String){
  val p=ctx.getSharedPreferences("local_logs",Context.MODE_PRIVATE)
  val old=JSONArray(p.getString("items","[]")?:"[]");val next=JSONArray()
  val whenText=java.text.SimpleDateFormat("MM-dd HH:mm",java.util.Locale.CHINA).format(java.util.Date())
  next.put(whenText+"  "+s.take(180))
  for(i in 0 until minOf(old.length(),99))next.put(old.optString(i))
  p.edit().putString("items",next.toString()).apply()
 }
 fun logs():List<String>{
  val p=ctx.getSharedPreferences("local_logs",Context.MODE_PRIVATE)
  val a=JSONArray(p.getString("items","[]")?:"[]")
  return (0 until a.length()).map{a.optString(it)}
 }
}

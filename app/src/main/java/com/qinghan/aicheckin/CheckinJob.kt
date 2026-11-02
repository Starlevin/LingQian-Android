package com.qinghan.aicheckin

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

class CheckinJob:JobService(){
 override fun onStartJob(params:JobParameters?):Boolean{
  Thread {
   try {
    if(Vault(applicationContext).isAuto())CheckinEngine.runAll(applicationContext)
   }catch(e:Exception){Vault(applicationContext).log("后台调度失败："+(e.message?:"未知").take(70))}
   finally{jobFinished(params,false)}
  }.start()
  return true
 }
 override fun onStopJob(params:JobParameters?):Boolean=true
}
object Scheduler {
 private const val JOB_ID=10880
 fun update(ctx:Context,enabled:Boolean){
  val js=ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
  if(!enabled){js.cancel(JOB_ID);return}
  val info=JobInfo.Builder(JOB_ID,ComponentName(ctx,CheckinJob::class.java))
   .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
   .setPersisted(true)
   .setPeriodic(6*60*60*1000L,60*60*1000L)
   .build()
  js.schedule(info)
 }
}

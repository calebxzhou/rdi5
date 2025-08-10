package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import net.minecraft.CrashReport
import org.bson.types.ObjectId

object CrashUploader {
    @JvmStatic
    fun start(report: CrashReport){
        val uid = RAccount.now?._id?.toString()?: ObjectId().toString()
        RServer.default.hqRequest(true,"crash-report",false,
            listOf("report" to report.getFriendlyReport(net.minecraft.ReportType.CRASH),
                "uid" to uid)){
            println("上传崩溃日志成功")
        }
    }
}
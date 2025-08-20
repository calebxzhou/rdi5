package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import net.minecraft.CrashReport
import net.minecraft.ReportType
import org.bson.types.ObjectId

object CrashUploader {
    @JvmStatic
    fun start(report: CrashReport){
        val uid = RAccount.now?._id?.toString()?: ObjectId().toString()
        RServer.default.hqRequest(true,"crash-report",false,
            listOf("report" to report.getFriendlyReport(ReportType.CRASH),
                "uid" to uid)){
            println("上传崩溃日志成功")
        }
    }
}
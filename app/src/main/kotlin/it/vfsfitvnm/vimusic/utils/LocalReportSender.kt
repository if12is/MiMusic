package it.vfsfitvnm.vimusic.utils

import android.content.Context
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.acra.sender.ReportSenderFactory

class LocalReportSender : ReportSender {
    @Throws(ReportSenderException::class)
    override fun send(context: Context, errorContent: CrashReportData) {
        PlaybackLogStore.writeCrash(errorContent.toJSON())
    }
}

class LocalReportSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender = LocalReportSender()
}

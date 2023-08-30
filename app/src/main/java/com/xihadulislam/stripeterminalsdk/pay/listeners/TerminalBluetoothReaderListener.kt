package com.xihadulislam.stripeterminalsdk.pay.listeners

import com.stripe.stripeterminal.external.callable.BluetoothReaderListener
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException

class TerminalBluetoothReaderListener : BluetoothReaderListener {

    override fun onRequestReaderInput(options: ReaderInputOptions) {}

    override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {}

    override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: Cancelable?) {
        // Show UI communicating that a required update has started installing
    }

    override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
        // Update the progress of the install
    }

    override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
        // Report success or failure of the update
    }

    override fun onReportAvailableUpdate(update: ReaderSoftwareUpdate) {}

    override fun onReportLowBatteryWarning() {}

    override fun onReportReaderEvent(event: ReaderEvent) {}
}

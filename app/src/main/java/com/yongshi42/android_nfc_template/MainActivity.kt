package com.yongshi42.android_nfc_template

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nfc_stuff.NdefMessageParser

class MainActivity : AppCompatActivity() {

    /**
     * creating activity wide variables
     * specific type with ? to be able to set to null
     * using !! to use these variables in a non-nullable way
     */
    private var nfcadapter: NfcAdapter? = null
    private var pendingintent: PendingIntent? = null

    private var nfctext: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * on creation of the app, get the textview references and nfc adapter instance.
         * if no nfc then make a toast.
         */
        nfctext = findViewById(R.id.nfcresult)
        nfcadapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcadapter == null) {
            Toast.makeText(this, "No NFC", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        pendingintent = PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

    }

    /**
     * check if nfc is enabled, if not show wireless settings
     * if enabled enable the foreground scanner when app resumes
     */
    override fun onResume() {
        super.onResume()
        if (nfcadapter != null) {
            if (nfcadapter!!.isEnabled == false) {
                showBluetoothSettings()
            }
            nfcadapter!!.enableForegroundDispatch(this, pendingintent, null, null)
        }
    }

    /**
     * disable the foreground nfc scanner when app is paused
     */
    override fun onPause() {
        super.onPause()
        if (nfcadapter != null) {
            nfcadapter!!.disableForegroundDispatch(this)
        }
    }

    /**
     * When new nfc tag is scanned, create a new intent to do something with it
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolveIntent(intent)
    }

    /**
     * resolve the nfc intent received
     */
    private fun resolveIntent(intent: Intent) {
        val action = intent.action

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            val msgs: Array<NdefMessage?>

            if (rawMsgs != null) {

                msgs = arrayOfNulls(rawMsgs.size)
                for (i in rawMsgs.indices) {
                    msgs[i] = rawMsgs[i] as NdefMessage
                }

            } else {
                val empty = ByteArray(0)
                val id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
                val tag: Tag = intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG) as Tag
                val payload = dumpTagData(tag)!!.toByteArray()
                val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload)
                val msg = NdefMessage(arrayOf(record))

                msgs = arrayOf(msg)
            }
            displayMsgs(msgs)
        }
    }

    /**
     * android open source method to dump the data of the current nfc tag
     */
    private fun dumpTagData(tag: Tag): String? {
        val sb = StringBuilder()
        val id: ByteArray = tag.id
        sb.append("ID (hex): ").append(toHex(id)).append('\n')
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n')
        sb.append("ID (dec): ").append(toDec(id)).append('\n')
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n')

        val prefix = "android.nfc.tech."
        sb.append("Technologies: ")
        for (tech in tag.techList) {
            sb.append(tech.substring(prefix.length))
            sb.append(", ")
        }
        sb.delete(sb.length - 2, sb.length)
        for (tech in tag.techList) {
            if (tech == MifareClassic::class.java.name) {
                sb.append('\n')
                var type = "Unknown"
                try {
                    val mifareTag = MifareClassic.get(tag)
                    when (mifareTag.type) {
                        MifareClassic.TYPE_CLASSIC -> type = "Classic"
                        MifareClassic.TYPE_PLUS -> type = "Plus"
                        MifareClassic.TYPE_PRO -> type = "Pro"
                    }
                    sb.append("Mifare Classic type: ")
                    sb.append(type)
                    sb.append('\n')
                    sb.append("Mifare size: ")
                    sb.append(mifareTag.size.toString() + " bytes")
                    sb.append('\n')
                    sb.append("Mifare sectors: ")
                    sb.append(mifareTag.sectorCount)
                    sb.append('\n')
                    sb.append("Mifare blocks: ")
                    sb.append(mifareTag.blockCount)
                } catch (e: Exception) {
                    sb.append("Mifare classic error: " + e.message)
                }
            }
            if (tech == MifareUltralight::class.java.name) {
                sb.append('\n')
                val mifareUlTag = MifareUltralight.get(tag)
                var type = "Unknown"
                when (mifareUlTag.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> type = "Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> type = "Ultralight C"
                }
                sb.append("Mifare Ultralight type: ")
                sb.append(type)
            }
        }
        return sb.toString()
    }

    /**
     * display text as messages
     */
    private fun displayMsgs(msgs: Array<NdefMessage?>?) {
        if (msgs == null || msgs.isEmpty()) return
        val builder = java.lang.StringBuilder()
        val records =
            NdefMessageParser.parse(msgs[0])
        val size = records.size
        for (i in 0 until size) {
            val record = records[i]
            val str = record.str()
            builder.append(str).append("\n")
        }
        nfctext!!.text = builder.toString()
    }

    /**
     * open bluetooth settings
     */
    private fun showBluetoothSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }

    /**
     * Android open source methods for converting
     */
    private fun toHex(bytes: ByteArray): String? {
        val sb = java.lang.StringBuilder()
        for (byteint in bytes.indices.reversed()) {
            val b: Int = byteint and 0xff
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
            if (byteint > 0) {
                sb.append(" ")
            }
        }
        return sb.toString()
    }

    private fun toReversedHex(bytes: ByteArray): String? {
        val sb = java.lang.StringBuilder()
        for (byteint in bytes.indices) {
            if (byteint > 0) {
                sb.append(" ")
            }
            val b: Int = byteint and 0xff
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
        }
        return sb.toString()
    }

    private fun toDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (byteint in bytes.indices) {
            val value: Long = byteint.toLong() and 0xffL
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun toReversedDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (byteint in bytes.indices.reversed()) {
            val value: Long = byteint.toLong() and 0xffL
            result += value * factor
            factor *= 256L
        }
        return result
    }

}
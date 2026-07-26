package com.hambalapps.chameleon.desktop.vpn

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

data class ProcessInfo(
    val name: String,
    val pid: String,
    val path: String = ""
)

object ProcessPickerUtils {

    /**
     * Enumerates unique running executable process names on Windows using tasklist.
     */
    fun getRunningProcesses(): List<ProcessInfo> {
        val processes = mutableSetOf<String>()
        try {
            val process = ProcessBuilder("tasklist", "/FO", "CSV", "/NH").start()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("\",\"")
                    if (parts.isNotEmpty()) {
                        val name = parts[0].replace("\"", "").trim()
                        if (name.endsWith(".exe", ignoreCase = true) && !name.contains("svchost", ignoreCase = true)) {
                            processes.add(name)
                        }
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return processes.sorted().map { ProcessInfo(name = it, pid = "") }
    }

    /**
     * Opens a native file dialog to let the user select a .exe file.
     */
    fun selectExecutableFile(): String? {
        return try {
            val chooser = JFileChooser().apply {
                dialogTitle = "Select Application Executable for Split Tunneling"
                fileFilter = FileNameExtensionFilter("Executable Files (*.exe)", "exe")
                isMultiSelectionEnabled = false
            }
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.name
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

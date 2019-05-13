/*
 * MAME RECORD FILE - MAME record file (.inp) management tool
 * Copyright (c) 2017 - 2019.  Author phweda : phweda1@yahoo.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.phweda.utils

import java.io.OutputStream
import java.io.PrintWriter
import java.lang.management.ManagementFactory
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Debug {
    private var separateChar = '*'
    private var outputStream: OutputStream = System.out
    private var writer: PrintWriter? = PrintWriter(outputStream)
    private var listBool = false
    private var separateString = "********************************************************************************"

    private val list = ArrayList<String>()
    private val sdateFormat = SimpleDateFormat("d MMM HH:mm:ss.S")

    constructor() : this(System.out, true)

    constructor(outputStream: OutputStream) : this(outputStream, true)

    private constructor(outputStream: OutputStream, flush: Boolean) {
        this.setOutput(outputStream, flush)
    }

    constructor(outputStream: OutputStream, flush: Boolean, ch: Char) {
        setOutput(outputStream, flush)
        this.setSeparateLineChar(ch)
    }

    fun separateLine(numIn: Int) {
        var num = numIn
        while (num-- > 0) {
            separateLine()
        }
    }

    private fun setOutput(outputStreamIn: OutputStream, flush: Boolean) {
        outputStream = outputStreamIn
        writer = PrintWriter(outputStream, flush)
    }

    private fun setSeparateLineChar(ch: Char) {
        separateChar = ch
        separateString = makeSeparateLineString()
    }

    private fun makeSeparateLineString(): String {
        val stringBuilder = StringBuilder(80)
        for (i in 0 until stringBuilder.length) {
            stringBuilder.append(separateChar)
        }
        return stringBuilder.toString()
    }

    fun out(output: String) {
        if (listBool) {
            list.add("[" + sdateFormat.format(GregorianCalendar().time) + "] " + output)
        } else {
            writer!!.println("[" + sdateFormat.format(GregorianCalendar().time) + "] " + output)
        }
    }

    /**
     * Add to the output list to be written later
     */
    fun addToList(output: String) {
        list.add("[" + sdateFormat.format(GregorianCalendar().time) + "] " + output)
    }

    /**
     * Add to the output list to be written later
     */
    fun addToList(output: Any) {
        list.add("[" + sdateFormat.format(GregorianCalendar().time) + "] " + output.toString())
    }

    fun addToList(output: String, flush: Boolean) {
        if (flush) {
            flushList()
            out(output)
        }
    }

    /* Make PrintWriter available for outputting stacktraces to log */
    fun writer(): PrintWriter? {
        // Clear out the list
        flushList()
        out("") // Force date time stamp output before printing stacktrace
        return writer
    }

    private fun flushList() {
        val itor = list.listIterator(0)
        while (itor.hasNext()) {
            writer!!.println(itor.next())
        }
        list.clear()
    }

    fun formatMillis(nanos: Long): String {
        return String.format(
            "%02d:%02d:%02d.%03d", TimeUnit.NANOSECONDS.toHours(nanos),
            TimeUnit.NANOSECONDS.toMinutes(nanos) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.NANOSECONDS.toSeconds(nanos) % TimeUnit.MINUTES.toSeconds(1),
            TimeUnit.NANOSECONDS.toMillis(nanos) % TimeUnit.SECONDS.toMillis(1)
        )
    }

    fun separateLine() {
        addToList("\n" + separateString, true)
    }

    fun logEnvironment(debug: Boolean, systemDebug: Boolean) {
        out("Running in ${Paths.get("").toAbsolutePath()}")
        out("Temp dir is " + System.getProperty("java.io.tmpdir"))

        // Get the VM arguments and log
        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        val arguments = runtimeMXBean.inputArguments
        separateLine()
        out(System.getProperty("os.name") + '\n')

        if (debug) {
            val env = System.getenv()
            for (envName in env.keys) {
                if (envName.contains("PROCESSOR") && !systemDebug) {
                    out(envName + " : " + env[envName])
                } else if (systemDebug) {
                    // out(envName + " : " + env[envName]) // NOTE Comment out to protect users' configs
                }
            }
        }
        separateLine()
        val javaVersion = System.getProperty("java.version")
        val jvmVersion = ManagementFactory.getRuntimeMXBean().vmVersion
        addToList("Java version : $javaVersion")
        addToList("JVM version : $jvmVersion")
        for (arg in arguments) {
            addToList(arg)
        }
        separateLine()
        flushList()
    }
}


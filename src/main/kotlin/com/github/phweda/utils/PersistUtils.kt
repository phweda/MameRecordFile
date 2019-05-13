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

@file:Suppress("unused")

package com.github.phweda.utils

import org.w3c.dom.Document
import org.w3c.dom.DocumentType
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.XMLConstants
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Created by IntelliJ IDEA.
 * User: phweda
 * Date: 12/4/11
 * Time: 3:47 PM
 */
object PersistUtils {

    fun saveAnObject(obj: Any, path: String) = try {
        ObjectOutputStream(FileOutputStream(path)).use { fos ->
            fos.writeObject(obj)
            fos.flush()
            fos.close()
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    @Throws(IOException::class)
    fun loadAnObject(path: String): Any? {
        if (File(path).exists()) {
            try {
                var obj: Any? = null
                ObjectInputStream(FileInputStream(path)).use { fis ->
                    fis.run { obj = readObject() }
                }
                return obj
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * DO NOT close outputstream so other files may be added to zip from the caller
     *
     * @param obj             Object to save
     * @param zipOutputStream Stream to save to
     * @param fileName        Name of the file to put into the zip
     */
    fun saveAnObjecttoZip(obj: Any, zipOutputStream: ZipOutputStream, fileName: String) {
        try {
            val zipEntry = ZipEntry(fileName)
            zipOutputStream.putNextEntry(zipEntry)
            val oos = ObjectOutputStream(zipOutputStream)
            oos.writeObject(obj)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun loadAnObjectFromZip(zipPath: String, fileName: String): Any? {
        try {
            ZipFile(zipPath).use { zipFile ->
                val zipEntry = ZipEntry(fileName)
                val ois = ObjectInputStream(zipFile.getInputStream(zipEntry))
                val obj = ois.readObject()
                ois.close()
                return obj
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        return null
    }

    fun saveAnObjectXML(obj: Any, path: String) {
        try {
            // Serialize object into XML
            val encoder = XMLEncoder(
                BufferedOutputStream(
                    FileOutputStream(path)
                )
            )
            encoder.writeObject(obj)
            encoder.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    @Throws(FileNotFoundException::class)
    fun loadAnObjectXML(path: String): Any {
        try {
            val decoder = XMLDecoder(BufferedInputStream(FileInputStream(path)))
            val obj = decoder.readObject()
            decoder.close()
            return obj
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            throw e
        }

    }

    @Throws(TransformerException::class)
    fun saveXMLDoctoFile(doc: Document, documentType: DocumentType, path: String) {
        // Save DOM XML doc to File
        val transformerFactory = TransformerFactory.newInstance()
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        val transformer = transformerFactory.newTransformer()

        doc.xmlVersion = "1.0"
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, documentType.publicId)
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, documentType.systemId)

        transformer.transform(DOMSource(doc), StreamResult(File(path)))
    }

}

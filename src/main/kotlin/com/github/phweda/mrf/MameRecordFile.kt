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

package com.github.phweda.mrf

import com.github.phweda.utils.Debug
import com.github.phweda.utils.PersistUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.NumericShaper
import java.awt.font.TextAttribute
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * Created by IntelliJ IDEA.
 * User: Phweda
 * Date: 3/19/2017
 * Time: 8:37 PM
 */
@Suppress("kotlin:S1144", "unused")
class MameRecordFile private constructor() {
    private val recordsDirPathFile = File("Records_dir_path")
    private val gameGoalsFile = File("Game_goals.xml")
    private var recordsDir: File? = null
    private var recordsDirPath: String? = null
    private var scores: TreeMap<String, SortedSet<Int>> = TreeMap()

    init {
        if (recordsDirPathFile.exists()) {
            try {
                val brTest = BufferedReader(FileReader(recordsDirPathFile))
                recordsDirPath = brTest.readLine()
                recordsDir = File(recordsDirPath!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else {
            val jfc = JFileChooser()
            jfc.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            jfc.dialogTitle = "Select Records folder"
            jfc.showDialog(null, "OK")
            recordsDir = jfc.selectedFile
            try {
                Files.write(recordsDirPathFile.toPath(), recordsDir!!.absolutePath.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        if (gameGoalsFile.exists()) {
            goals = PersistUtils.loadAnObjectXML(gameGoalsFile.path) as HashMap<String, String>
        }

        val fontAttributes = HashMap<TextAttribute, Int>()
        fontAttributes[TextAttribute.NUMERIC_SHAPING] = NumericShaper.ARABIC
        val ttFont = Font("SansSerif", Font.BOLD + Font.ITALIC, 18).deriveFont(fontAttributes)

        // global tooltip font
        UIManager.put("ToolTip.font", ttFont)
    }

    private fun showScores() {
        parseScores()
        showTree()
    }

    private fun showTree() {
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.preferredSize = Dimension(screenSize.width / 6, screenSize.height - 25)

        tree = createTree()
        tree!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    val selPath = with(tree!!) { getPathForLocation(e.x, e.y) }
                    val node: DefaultMutableTreeNode = selPath.lastPathComponent as DefaultMutableTreeNode
                    addGoal(node, getGoal())
                }
            }
        })

        setTree()

        // Listen for files dropped onto the frame -
        frame.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false
                }
                return true
            }

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) {
                    return false
                }

                val t = support.transferable

                try {
                    val l = t.getTransferData(DataFlavor.javaFileListFlavor) as List<File>

                    for (file in l) {
                        if (file.extension == "inp") {
                            addScore(file.toPath())
                            frame.revalidate()
                        } else {
                            JOptionPane.showMessageDialog(frame, "Only files with .inp extension allowed")
                        }
                    }
                } catch (e: UnsupportedFlavorException) {
                    return false
                } catch (e: IOException) {
                    return false
                }
                return true
            }
        }

        frame.pack()
        frame.setLocation(screenSize.width / 6 * 5, 0)
        frame.isVisible = true
    }

    private fun setTree() {
        tree!!.expandRow(0)
        val pane = JScrollPane(tree)
        frame.contentPane.add(pane, BorderLayout.CENTER)
        frame.contentPane.validate()
    }

    private fun expandTree() {
        for (i in 0 until tree!!.rowCount) {
            tree!!.expandRow(i)
        }
    }

    private fun parseScores() = try {
        Files.list(recordsDir!!.toPath())
            .filter { path -> Files.isRegularFile(path) }
            .forEach(this@MameRecordFile::addScore)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    private fun addScore(path: Path) {
        val fileName = path.toString()
        if (fileName.matches("^.+?\\d$".toRegex()) && fileName.contains("-") && fileName.contains(".") &&
            fileName.indexOf('.') == fileName.lastIndexOf('.')
        ) {
            // System.out.println(fileName);
            val gameName = getGameNamefromFile(fileName)
            // Skip since there is more than 1
            if (fileName.contains(separator)) {
                return
            }
            // Convert String to Int
            val score = (fileName.substring(fileName.lastIndexOf('-') + 1)).toInt()
            //    System.out.println(name + " : " + score);

            if (tree == null) {
                if (scores.containsKey(gameName)) {
                    scores[gameName]!!.add(score)
                } else {
                    val set = TreeSet(Collections.reverseOrder<Int>())
                    set.add(score)
                    scores[gameName] = set
                }
            } else {
                insertScore(gameName, score)
            }
            // Detect if we are running already
        } else if (tree != null && fileName.endsWith(".inp")) {
            val gameName = getGameNamefromFile(fileName)
            var score = JOptionPane.showInputDialog("Enter score")
            if (!isNumeric(score)) {
                score = JOptionPane.showInputDialog("Enter score must be numeric")
            }
            copyFile(File(fileName), score)
            insertScore(gameName, score.toInt())
        }
    }

    private fun getGameNamefromFile(fileName: String): String {
        return fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1, fileName.lastIndexOf('.'))
    }

    private fun insertScore(name: String, score: Int?) {
        if (tree != null) {
            val root = tree!!.model.root as DefaultMutableTreeNode
            var found = false
            var child: DefaultMutableTreeNode

            for (i in 0 until root.childCount) {
                child = root.getChildAt(i) as DefaultMutableTreeNode
                if (child.userObject == name) {
                    insertScore(child, score)
                    found = true
                    break
                }
            }
            if (!found) {
                insertGameScore(this, root, name, score)
            }
            update()
        }
    }

    private fun update() {
        tree!!.updateUI()
    }

    private fun insertScore(child: DefaultMutableTreeNode, score: Int?) {
        var index = 0
        while (child.childCount > index &&
            ((child.getChildAt(index) as DefaultMutableTreeNode).userObject as Int) > score!!
        ) {
            index++
        }
        val newScore = DefaultMutableTreeNode(score)
        child.insert(newScore, index)
        selectNewNode(newScore)
    }

    private val fileMap: HashMap<String, File>
        get() {
            val map = HashMap<String, File>()
            for (file in files) {
                map[file.name] = file
            }
            return map
        }

    private val files: ArrayList<File>
        get() {
            val files = ArrayList<File>()
            try {
                Files.list(Paths.get("."))
                    .filter { path -> Files.isRegularFile(path) }
                    .forEach { path -> files.add(path.toFile()) }

            } catch (ioexc: IOException) {
                ioexc.printStackTrace()
            }

            return files
        }

    private fun copyFile(file: File, score: String) {
        val path = file.toPath()
        val newPath = Paths.get(recordsDir!!.path + File.separator + file.name + "-" + score)
        try {
            if (newPath.toFile().exists()) {
                // JOptionPane.showMessageDialog(null, "You already have a file for that score");
                saveDuplicateScore(path, newPath)
                return
            }
            Files.copy(path, newPath)
        } catch (e: IOException) {
            println("Path is ; $path")
            println("newPath is ; $newPath")
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    private fun saveDuplicateScore(path: Path, moveToPath: Path) {
        var newPath = moveToPath
        var integer = 1
        newPath = Paths.get(newPath.toString() + separator + integer.toString())
        while (Files.exists(newPath)) {
            integer += 1
            newPath = Paths.get(
                newPath.toString().substring(
                    0,
                    newPath.toString().length - 1
                ) + integer.toString()
            )
        }
        Files.copy(path, newPath)
    }

    private fun createTree(): MRFjTree {
        val root = DefaultMutableTreeNode("My Scores")
        val tree = MRFjTree(root)
        ToolTipManager.sharedInstance().registerComponent(tree)
        val font = tree.font
        val newFont = Font(font.name, Font.BOLD, 18)
        tree.font = newFont

        for (game in scores.keys) {
            val node = DefaultMutableTreeNode(game)
            for (score in scores[game].orEmpty()) {
                val scoreNode = DefaultMutableTreeNode(score)
                node.add(scoreNode)
            }
            root.add(node)
        }
        return tree
    }

    private fun selectNewNode(node: DefaultMutableTreeNode) {
        val nodePath = TreePath(node.path)
        tree!!.selectionPath = nodePath
        tree!!.scrollPathToVisible(nodePath)
    }

    private fun refreshScores() {
        scores = TreeMap()
        parseScores()
        tree = this.createTree()
        setTree()
        frame.revalidate()
    }

    private fun addGoal(node: DefaultMutableTreeNode, score: String) {
        if (goals == null) {
            goals = HashMap(20)
        }
        goals!![node.userObject.toString()] = score
        with(PersistUtils) { saveAnObjectXML(goals!!, gameGoalsFile.path) }
    }

    companion object {
        private val frame = JFrame("Mame Scores")
        // need to convert creation sequence/non UI run for Kotlin so this is Not nullable
        private var tree: MRFjTree? = null
        private const val separator = "~"
        private val screenSize = Toolkit.getDefaultToolkit().screenSize
        private var goals: HashMap<String, String>? = null

        @JvmStatic
        fun main(args: Array<String>) {
            val logger = Debug()
            val mrf = MameRecordFile()
            val fileName: String
            val icon = ImageIcon("src/main/resources/MAME_Record_File_48_icon.png")
            frame.setIconImage(icon.getImage())

            if (args.isNotEmpty() && args[0].isNotEmpty()) {
                if (args[0] == "-S") {
                    logger.logEnvironment(debug = true, systemDebug = true)
                    mrf.showScores()
                } else {
                    fileName = args[0]
                    val score = JOptionPane.showInputDialog("Enter score")
                    mrf.copyFile(File(fileName), score)
                }
            } else {
                mrf.showScores()
            }
        }

        internal fun isNumeric(str: String): Boolean {
            for (c in str.toCharArray()) {
                if (!Character.isDigit(c)) return false
            }
            return true
        }

        private fun insertGameScore(
            mameRecordFile: MameRecordFile,
            root: DefaultMutableTreeNode, gameName: String, score: Int?
        ) {
            val gameNode = DefaultMutableTreeNode(gameName)
            gameNode.add(DefaultMutableTreeNode(score))

            var index = 0
            val children = root.children()
            while (children.hasMoreElements()) {
                if (((children.nextElement() as DefaultMutableTreeNode).userObject as String) > gameName) {
                    break
                } else {
                    index++
                }
            }
            root.insert(gameNode, index)
            // Need to reload for row expansion below
            (tree!!.model as DefaultTreeModel).reload()

            mameRecordFile.selectNewNode(gameNode)
            val row = tree!!.getRowForPath(tree!!.selectionPath)
            tree!!.expandRow(row)
        }

        fun getGoal(): String {
            return JOptionPane.showInputDialog(tree, "Enter game score goal")
        }

        fun getGoalTip(node: DefaultMutableTreeNode): String? {
            val name = node.toString()
            return if (goals != null) {
                goals!![name]
            } else null
        }

    }

}

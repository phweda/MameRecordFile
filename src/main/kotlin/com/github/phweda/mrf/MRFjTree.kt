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

import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

/**
 * Created by IntelliJ IDEA.
 * User: Phweda
 * Date: 3/22/2018
 * Time: 8:48 PM
 */
class MRFjTree internal constructor(root: TreeNode) : JTree(root) {

    override fun getToolTipText(event: MouseEvent?): String? {
        val path = getPathForLocation(event!!.x, event.y)
        try {
            val node = path!!.lastPathComponent as DefaultMutableTreeNode
            try {
                return MameRecordFile.getGoalTip(node)
            } catch (e: NoSuchElementException) {
                // Expected
            }
        } catch (e: NullPointerException) {
            // Expected
        }
        return null
    }

    /* Retained for history = displays highest score so node doesn't need expansion
        Maybe we want to display both highest score and Goal?
    override fun getToolTipText(event: MouseEvent?): String? {
        val path = getPathForLocation(event!!.x, event.y)
        try {
            val node = path!!.lastPathComponent as DefaultMutableTreeNode

            try {
                val child = node.firstChild ?: return null
                return child.toString()
            } catch (e: NoSuchElementException) {
                // Expected
            }
        } catch (e: NullPointerException) {
            // Expected
        }
        return null
    }
*/
    override fun getToolTipLocation(event: MouseEvent?): Point {
        return Point(event!!.x + 25, event.y + 15)
    }

}


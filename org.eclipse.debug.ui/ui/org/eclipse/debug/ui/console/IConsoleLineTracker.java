package org.eclipse.debug.ui.console;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.jface.text.IRegion;

/**
 * Notified of lines appended to the console. A line tracker registers itself
 * as an extension via plug-in XML, via the extension point
 * <code>org.eclipse.debug.ui.consoleLineTrackers</code>. A line tracker is
 * associated with a type of process. Following is an example definition of
 * a console line tracker extension.
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.consoleLineTrackers"&gt;
 *   &lt;consoleLineTracker 
 *      id="com.example.ExampleConsoleLineTracker"
 *      class="com.example.ExampleConsoleLineTrackerClass"
 *      processType="ExampleProcessType"&gt;
 *   &lt;/consoleLineTracker&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this line tracker.</li>
 * <li><code>class</code> specifies a fully qualified name of a Java class
 *  that implements <code>IConsoleLineTracker</code>.</li>
 * <li><code>processType</code> specifies the identifier of the process type
 * this line tracker is associated with (which corresponds to the
 * <code>ATTR_PROCESS_TYPE</code> attribute on a process).</li>
 * </ul>
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * <b>This interface is still evolving</b>
 * </p>
 * @since 2.1
 */
public interface IConsoleLineTracker {
	
	/**
	 * Notification that a console document has been created for which this
	 * listener is registered. 
	 *  
	 * @param console console that has been created
	 */
	public void init(IConsole console);

	/**
	 * Notification that a line of text has been appended to the console. The
	 * given region describes the offset and length of the line appended to the
	 * console, excluding the line delimiter.
	 * 
	 * @param line region describing the offset and length of line appended to
	 * the console, excluding the line delimiter
	 */
	public void lineAppended(IRegion line);
	
	/**
	 * Disposes this console line tracker. 
	 */
	public void dispose();
}

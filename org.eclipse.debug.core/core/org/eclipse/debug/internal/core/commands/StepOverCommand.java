/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.core.commands;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IStepOverHandler;
import org.eclipse.debug.core.model.IStep;

/**
 * Default step over command for the standard debug model.
 *
 * @since 3.3
 */
public class StepOverCommand extends StepCommand implements IStepOverHandler {


	@Override
	protected void step(Object target) throws CoreException {
		((IStep)target).stepOver();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.commands.StepCommand#isSteppable(java.lang.Object)
	 */
	@Override
	protected boolean isSteppable(Object target) {
		return ((IStep)target).canStepOver();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.commands.AbstractDebugCommand#getEnabledStateJobFamily(org.eclipse.debug.core.commands.IDebugCommandRequest)
	 */
	@Override
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return IStepOverHandler.class;
	}

}

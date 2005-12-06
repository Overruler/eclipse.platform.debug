/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.viewers.AsynchronousTreeContentAdapter;
import org.eclipse.debug.internal.ui.viewers.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;

public class MemoryRetrievalContentAdapter extends AsynchronousTreeContentAdapter{

	protected Object[] getChildren(Object parent, IPresentationContext context) throws CoreException {
		String id = context.getPart().getSite().getId();
		if (id.equals(IDebugUIConstants.ID_MEMORY_VIEW))
        {
			if (parent instanceof IMemoryBlockRetrieval)
			{
        		return DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks((IMemoryBlockRetrieval)parent);
			}
        }
		return EMPTY;
	}

	protected boolean hasChildren(Object element, IPresentationContext context) throws CoreException {
		String id = context.getPart().getSite().getId();
		if (id.equals(IDebugUIConstants.ID_MEMORY_VIEW))
        {
			if (element instanceof IMemoryBlockRetrieval)
			{
				if (((IMemoryBlockRetrieval)element).supportsStorageRetrieval())
        			return DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks((IMemoryBlockRetrieval)element).length > 0;
			}
        }
        return false;
	}

	protected boolean supportsPartId(String id) {
		return id.equals(IDebugUIConstants.ID_MEMORY_VIEW);
	}

}

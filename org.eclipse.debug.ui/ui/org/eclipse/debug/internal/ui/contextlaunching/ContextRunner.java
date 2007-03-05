/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.contextlaunching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutSelectionDialog;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.dialogs.ListDialog;

import com.ibm.icu.text.MessageFormat;

/**
 * Static runner for context launching to provide the base capability of context 
 * launching to more than one form of action (drop down, toolbar, view, etc)
 * 
 * @see org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction
 * @see org.eclipse.debug.ui.actions.LaunchShortcutsAction
 * @see org.eclipse.debug.ui.actions.ContextualLaunchAction
 * @see org.eclipse.debug.internal.ui.preferences.ContextLaunchingPreferencePage
 * 
 *  @since 3.3
 *  EXPERIMENTAL
 *  CONTEXTLAUNCHING
 */
public final class ContextRunner {
	
	private static boolean DEBUG_CONTEXTUAL_LAUNCH = false;
	
	static {
		DEBUG_CONTEXTUAL_LAUNCH = DebugUIPlugin.DEBUG && "true".equals(Platform.getDebugOption("org.eclipse.debug.ui/debug/contextlaunching")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * The singleton instance of the context runner
	 */
	private static ContextRunner fgInstance = null;
	
	/**
	 * Returns the singleton instance of <code>ContextRunner</code>
	 * @return the singleton instance of <code>ContextRunner</code>
	 */
	public static ContextRunner getDefault() {
		if(fgInstance == null) {
			fgInstance = new ContextRunner();
		}
		return fgInstance;
	}
	
	/**
	 * Performs the context launching given the object context and the mode to launch in.
	 * @param mode the mode to launch in
	 */
	public void launch(String mode) {
		try {
			IResource resource = getSelectedResource();//SelectedResourceManager.getDefault().getSelectedResource();
			//1. resolve resource
			if(resource != null) {
				if(DEBUG_CONTEXTUAL_LAUNCH) {
					System.out.println("LAUNCH -> Selecting and Launching with resource: "+resource.getName()+ " in mode: "+mode); //$NON-NLS-1$ //$NON-NLS-2$
				}
				selectAndLaunch(resource, mode);
				return;
			}
			//2. launch last if no resource
			ILaunchConfiguration config = getLastLaunch(mode);
			if(config != null) {
				if(DEBUG_CONTEXTUAL_LAUNCH) {
					System.out.println("LAUNCH -> Using last launch, context is not valid"); //$NON-NLS-1$
				}
				DebugUITools.launch(config, mode);
				return;
			}
			//3. might be empty workspace try to get shortcuts
			List shortcuts = getLaunchShortcutsForEmptySelection();
			if(!shortcuts.isEmpty()) {
				showShortcutSelectionDialog(resource, shortcuts, mode);
			}
			else {
				MessageDialog.openInformation(DebugUIPlugin.getShell(), ContextMessages.ContextRunner_0, ContextMessages.ContextRunner_7);
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
	}
	
	/**
	 * Creates a listing of the launch shortcut extensions that are applicable to the underlying resource
	 * @param resource the underlying resource
	 * @return a listing of applicable launch shortcuts or an empty list, never <code>null</code>
	 * @throws CoreException
	 * @since 3.3
	 */
	public List getLaunchShortcutsForEmptySelection() throws CoreException {
		List list = new ArrayList(); 
		List sc = getLaunchConfigurationManager().getLaunchShortcuts();
		List ctxt = new ArrayList();
		IEvaluationContext context = new EvaluationContext(null, ctxt);
		context.addVariable("selection", ctxt); //$NON-NLS-1$
		LaunchShortcutExtension ext = null;
		for(Iterator iter = sc.iterator(); iter.hasNext();) {
			ext = (LaunchShortcutExtension) iter.next();
			if(ext.evalEnablementExpression(context, ext.getContextualLaunchEnablementExpression()) && !WorkbenchActivityHelper.filterItem(ext)) {
				if(!list.contains(ext)) {
					list.add(ext);
				}
			}
		}
		return list;
	}
	
	/**
	 * Returns the last thing launched from the launch history 
	 * @param mode the mode
	 * @return the last <code>ILaunchConfiguration</code> launched or <code>null</code> if none
	 */
	protected ILaunchConfiguration getLastLaunch(String mode) {
		ILaunchGroup group = resolveLaunchGroup(mode);
		if(group != null) {
			LaunchHistory history = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchHistory(group.getIdentifier());
			if(history != null) {
				return history.getRecentLaunch();
			}
		}
		return null;
	}
	
	/**
	 * Returns the <code>ILaunchGroup</code> that corresponds to the specified mode
	 * @param mode the mode to find the launch group
	 * @return the <code>ILaunchGroup</code> that corresponds to the specified mode, or <code>null</code>
	 */
	protected ILaunchGroup resolveLaunchGroup(String mode) {
		//TODO might not return the group we want
		ILaunchGroup[] groups = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroups();
		for(int i = 0; i < groups.length; i++) {
			if(groups[i].getMode().equals(mode) && groups[i].getCategory() == null) {
				return groups[i];
			}
		}
		return null;
	}
	
	/**
	 * Returns if the parent project should be checked automatically
	 * @return true if the parent project should checked automatically, false otherwise
	 */
	protected boolean shouldCheckParent() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT);
	}
	
	/**
	 * Prompts the user to select a way of launching the current resource, where a 'way'
	 * is defined as a launch shortcut, and returns if a launch took place
	 * @param resource
	 * @param mode
	 * @return if the context was launched in the given mode or not
	 * @throws CoreException
	 */
	protected boolean selectAndLaunch(IResource resource, String mode) throws CoreException {
		ILaunchConfiguration config = getLaunchConfigurationManager().isSharedConfig(resource);
		if(config != null) {
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("\tSELECTANDLAUNCH -> Shared config, launch it"); //$NON-NLS-1$
			}
			DebugUITools.launch(config, mode);
			return true;
		}
		List configs = getLaunchConfigurationManager().getApplicableLaunchConfigurations(resource); 
		int csize = configs.size();
		if(DEBUG_CONTEXTUAL_LAUNCH) {
			System.out.println("\tSELECTANDLAUNCH -> Selecting from "+csize+" possible launch configurations for "+resource.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(csize == 1) {
			DebugUITools.launch((ILaunchConfiguration) configs.get(0), mode);
			return true;
		}
		if(csize < 1) {
			List exts = getLaunchConfigurationManager().getLaunchShortcuts(resource);
			int esize = exts.size();
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("\tSELECTANDLAUNCH -> No configurations for "+resource.getName()+", choosing from "+esize+" launch shortcuts"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if(esize == 1) {
				LaunchShortcutExtension ext = (LaunchShortcutExtension) exts.get(0);
				ext.launch(new StructuredSelection(resource), mode);
				return true;
			}
			if(esize > 1) {
				return showShortcutSelectionDialog(resource, null, mode);
			}
			if(esize < 1) {
				IProject project = resource.getProject();
				if(DEBUG_CONTEXTUAL_LAUNCH) {
					System.out.println("\tSELECTANDLAUNCH -> No shortcuts apply for "+resource.getName()+" trying project"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if(project != null && !project.equals(resource)) {
					if(!shouldCheckParent()) {
						String msg = MessageFormat.format(ContextMessages.ContextRunner_10, new String[] {resource.getName(), project.getName()});
						MessageDialogWithToggle mdwt = new MessageDialogWithToggle(DebugUIPlugin.getShell(), 
								ContextMessages.ContextRunner_11, 
								null, 
								msg,
								MessageDialog.QUESTION, 
								new String[] {IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL},
								0, 
								ContextMessages.ContextRunner_12,
								false);
						if(mdwt.open() == IDialogConstants.YES_ID) {
							DebugUIPlugin.getDefault().getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT, mdwt.getToggleState());
							selectAndLaunch(project, mode);
						}
					}
					else {
						selectAndLaunch(project, mode);
					}
				}
				else {
					String msg = ContextMessages.ContextRunner_7;
					if(!resource.isAccessible()) {
						msg = MessageFormat.format(ContextMessages.ContextRunner_13, new String[] {resource.getName()});
					}
					MessageDialog.openInformation(DebugUIPlugin.getShell(), ContextMessages.ContextRunner_0, msg);
				}
			}
		}
		else if(csize > 1){
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("\tSELECTANDLAUNCH -> More than one configuration for "+resource.getName()+ " get MRU"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			config = getMRUConfiguration(configs, mode);
			if(config != null) {
				DebugUITools.launch(config, mode);
				return true;
			}
			else {
				return showConfigurationSelectionDialog(configs, mode);
			}
		}
		return false;
	}
	
	/**
	 * Launches the first occurance of any one of the configurations in the provided list, if they are found in the launch history
	 * for the corresponding launch group
	 * @param configurations
	 * @param mode
	 * @return the associated launch configuration from the MRU listing or <code>null</code> if there isn't one
	 */
	protected ILaunchConfiguration getMRUConfiguration(List configurations, String mode) {
		ILaunchGroup group = resolveLaunchGroup(mode);
		if(group != null) {
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("\t\tGETMRUCONFIGURATION -> Looking up MRU for launch group "+DebugUIPlugin.removeAccelerators(group.getLabel())); //$NON-NLS-1$
			}
			ILaunchConfiguration config = getLastLaunch(mode);
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("\t\tGETMRUCONFIGURATION -> Last launch: "+(config != null ? config.getName() : "None")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if(configurations.contains(config)) {
				if(DEBUG_CONTEXTUAL_LAUNCH) {
					System.out.println("\t\tGETMRUCONFIGURATION -> Selecting last launch "+config.getName()); //$NON-NLS-1$
				}
				return config;
			}
			LaunchHistory history = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchHistory(group.getIdentifier());
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("\t\tGETMRUCONFIGURATION -> Checking history"); //$NON-NLS-1$
			}
			if(history != null) {
				ILaunchConfiguration[] configs = history.getCompleteLaunchHistory();
				for(int i = 0; i < configs.length; i++) {
					if(DEBUG_CONTEXTUAL_LAUNCH) {
						System.out.println("\t\tGETMRUCONFIGURATION -> Comparing containment of "+configs[i].getName()+" to "+configurations.toString()); //$NON-NLS-1$ //$NON-NLS-2$
					}
					if(configurations.contains(configs[i])) {
						return configs[i];
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Presents the user with a dialog to pick the launch configuration to launch
	 * @param configurations the listing of applicable configurations to present
	 * @param mode the mode
	 * @return true if something was launched, false otherwise
	 */
	protected boolean showConfigurationSelectionDialog(List configurations, String mode) {
		ListDialog lsd = new ListDialog(DebugUIPlugin.getShell());
		lsd.setContentProvider(new ArrayContentProvider());
		lsd.setLabelProvider(new DefaultLabelProvider());
		lsd.setMessage(ContextMessages.ContextRunner_8);
		lsd.setTitle(ContextMessages.ContextRunner_9);
		lsd.setInput(configurations);
		if(lsd.open() == IDialogConstants.OK_ID) {
			ILaunchConfiguration config = (ILaunchConfiguration) lsd.getResult()[0];
			DebugUITools.launch(config, mode);
			return true;
		}
		return false;
	}
	
	/**
	 * Presents a selection dialog to the user to pick a launch shortcut
	 * @param resource the resource context
	 * @param mode the mode
	 * @return true if something was launched, false otherwise
	 */
	protected boolean showShortcutSelectionDialog(IResource resource, List shortcuts, String mode) {
		LaunchShortcutSelectionDialog dialog = new LaunchShortcutSelectionDialog(resource, mode);
		if(shortcuts != null) {
			dialog.setInput(shortcuts);
		}
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if(result.length > 0) {
				LaunchShortcutExtension method = (LaunchShortcutExtension) result[0];
				if(method != null) {
					method.launch(new StructuredSelection(resource), mode);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns the associated launch configuration name of the currently selected context, or the empty string.
	 * @param mode
	 * @return the associated launch configuration name of the currently selected context or the empty string. 
	 */
	public String getContextLabel(String mode) {
		IResource resource = getSelectedResource();//SelectedResourceManager.getDefault().getSelectedResource();
		ILaunchConfiguration config = getLaunchConfigurationManager().isSharedConfig(resource);
		if(config != null) {
			if(DEBUG_CONTEXTUAL_LAUNCH) {
				System.out.println("GETCONTEXTNAME -> Shared config, return name"); //$NON-NLS-1$
			}
			return config.getName();
		}
		if(resource != null) {
			return getResourceLabel(resource, mode);
		}
		if(DEBUG_CONTEXTUAL_LAUNCH) {
			System.out.println("GETCONTEXTNAME -> Not shared config or otherwise, use last launch name if one"); //$NON-NLS-1$
		}
		config = getLastLaunch(mode);
		if(config != null) {
			return config.getName();
		}
		return ""; //$NON-NLS-1$
	}
	
	private IResource getSelectedResource() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if(window != null) {
			IWorkbenchPage page = window.getActivePage();
			if(page != null) {
				IWorkbenchPart part = page.getActivePart();
				if(part instanceof IEditorPart) {
					IEditorPart epart = (IEditorPart) part;
					return (IResource) epart.getEditorInput().getAdapter(IResource.class);
				}
				else {
					IWorkbenchPartSite site = part.getSite();
					if(site != null) {
						ISelection sel = site.getSelectionProvider().getSelection();
						if(sel instanceof IStructuredSelection) {
							IStructuredSelection ss = (IStructuredSelection) sel;
							if(!ss.isEmpty()) {
								Object o = ss.getFirstElement();
								if(o instanceof IAdaptable) {
									return (IResource) ((IAdaptable)o).getAdapter(IResource.class);
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the label for the specified resource or the empty string, never <code>null</code>
	 * @param resource
	 * @param mode
	 * @return the label for the resource or the empty string, never <code>null</code>
	 */
	protected String getResourceLabel(IResource resource, String mode) {
		if(DEBUG_CONTEXTUAL_LAUNCH) {
			System.out.println("\tGETRESOURCELABEL -> Finding name for: "+resource.getName()); //$NON-NLS-1$
		}
		List configs = getLaunchConfigurationManager().getApplicableLaunchConfigurations(resource);
		int csize = configs.size();
		if(DEBUG_CONTEXTUAL_LAUNCH) {
			System.out.println("\tGETRESOURCELABEL -> Selecting from "+csize+" possible configurations for "+resource.getName()+ " choices: "+configs.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		ILaunchConfiguration config = null;
		if(csize == 1) {
			return ((ILaunchConfiguration)configs.get(0)).getName();
		}
		else if(csize > 1) {
			config = getMRUConfiguration(configs, mode);
			if(config != null) {
				return config.getName();
			}
			else {
				//TODO could cause TVT issues
				return ContextMessages.ContextRunner_14;
			}
		}
		else {
			try {
				List exts = getLaunchConfigurationManager().getLaunchShortcuts(resource);
				int esize = exts.size();
				if(DEBUG_CONTEXTUAL_LAUNCH) {
					System.out.println("\tGETRESOURCELABEL -> Selecting from: "+esize+" shortcuts"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if(esize == 0) {
					IProject project = resource.getProject();
					if(DEBUG_CONTEXTUAL_LAUNCH) {
						System.out.println("\tGETRESOURCELABEL -> No shortcuts apply for "+resource.getName()+" trying project"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					if(project != null && !project.equals(resource)) {
						if(shouldCheckParent()) {
							return getResourceLabel(project, mode);
						}
						else {
							//TODO could cause TVT issues
							return ContextMessages.ContextRunner_15;
						}
					}
				}
				if(esize == 1) {
					return resource.getName();
				}
				else {
					//TODO could cause TVT issues
					return ContextMessages.ContextRunner_14;
				}
			}
			catch(CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Returns if context launching is enabled
	 * @return if context launching is enabled
	 */
	public boolean isContextLaunchEnabled() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH);
	}
	
	/**
	 * Returns the launch configuration manager
	 * @return the launch configuration manager
	 */
	protected LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}
}
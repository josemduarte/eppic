package ch.systemsx.sybit.crkwebui.client.handlers;

import ch.systemsx.sybit.crkwebui.client.events.HideWaitingEvent;

import com.google.gwt.event.shared.EventHandler;

/**
 * Hide waiting event handler.
 * @author AS
 */
public interface HideWaitingHandler extends EventHandler 
{
	/**
	 * Method called when waiting window is to be closed.
	 * @param event Hide waiting event
	 */
	 public void onHideWaiting(HideWaitingEvent event);
}

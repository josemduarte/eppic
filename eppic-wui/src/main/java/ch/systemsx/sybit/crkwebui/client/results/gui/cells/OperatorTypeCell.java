package ch.systemsx.sybit.crkwebui.client.results.gui.cells;

import ch.systemsx.sybit.crkwebui.client.results.data.InterfaceItemModel;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.sencha.gxt.data.shared.ListStore;

/**
 * Cell used to style operator type.
 * @author nikhil
 *
 */
public class OperatorTypeCell extends AbstractCell<String> {
	
	private ListStore<InterfaceItemModel> itemsStore;
	
	public OperatorTypeCell(ListStore<InterfaceItemModel> itemsStore){
		this.itemsStore = itemsStore;
	}

	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context,
			String value, SafeHtmlBuilder sb) {
		String iconsDir = "resources/icons/";

		if (value != null) {
			String tooltipText = null;
			String icon = "optype_" + value ;
			
			int row = context.getIndex();
			InterfaceItemModel item = itemsStore.get(row);
		
			tooltipText = item.getOperator();
			if (item.isInfinite())
			    icon += "_inf";
			
			icon+=".png"; // either optype_2S.png or optype_2S_red.png
			
			String source = iconsDir+icon;
			
			tooltipText = "<div class=\"eppic-default-font eppic-operator-tooltip\">" + tooltipText + "</div>";
			sb.appendHtmlConstant("<img src='"+ source + 
					"' qtip='" + tooltipText + "' />");
		
		// to make it compatible with older versions without operatorType in the model, 
		// we return the operator string if no operator type is present			
		} else { 
			int row = context.getIndex();
			InterfaceItemModel item = itemsStore.get(row);
			value = item.getOperator();
		}	

	}
}

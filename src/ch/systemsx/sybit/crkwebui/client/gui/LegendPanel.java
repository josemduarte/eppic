package ch.systemsx.sybit.crkwebui.client.gui;

import java.sql.RowIdLifetime;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;

public class LegendPanel extends LayoutContainer 
{
	public LegendPanel()
	{
		this.setHeight(30);
		this.setStyleAttribute("padding", "0px");
		
		VBoxLayout legendPanelLayout = new VBoxLayout();  
		legendPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.CENTER);  
		this.setLayout(legendPanelLayout);
//		this.setLayout(new RowLayout(Orientation.HORIZONTAL));
		
		LayoutContainer legendContainer = new LayoutContainer();
		legendContainer.setLayout(new RowLayout(Orientation.HORIZONTAL));
		legendContainer.setHeight(30);
		legendContainer.setWidth(400);

		String typesString = "Rim,Core,Totally buried,Others";
		String[] types = typesString.split(",");
		
		String colorsString = "#bedeff,#ff7979,#EDEDED,white";
		String[] colors = colorsString.split(",");
		
		for(int i=0; i<types.length; i++)
		{
			if(colors.length >= i)
		    {
				LayoutContainer rimContainer = new LayoutContainer();
				HBoxLayout rimContainerLayout = new HBoxLayout();  
			    rimContainerLayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);  
			    rimContainer.setLayout(rimContainerLayout);
				
			    LayoutContainer rimTypePanel = new LayoutContainer();
				rimTypePanel.setBorders(true);
				rimTypePanel.setStyleAttribute("background", colors[i]);
				rimTypePanel.setWidth(20);
				rimTypePanel.setHeight(20);
				
				Label rimTypeLabel = new Label(types[i]);
				
				rimContainer.add(rimTypePanel, new HBoxLayoutData(new Margins(0, 10, 0, 0)));
				rimContainer.add(rimTypeLabel);
				
				legendContainer.add(rimContainer, new RowData(0.25, 1, new Margins(0)));
		    }
		}
		
		this.add(legendContainer);
	}
}

package ch.systemsx.sybit.crkwebui.client.gui;

import java.util.ArrayList;
import java.util.List;

import ch.systemsx.sybit.crkwebui.client.controllers.MainController;
import ch.systemsx.sybit.crkwebui.client.model.InterfaceResidueSummaryModel;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceScoreItem;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridViewConfig;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;

/**
 * Panel used to display the residues summary for one structure.
 * @author srebniak_a
 *
 */
public class ResiduesSummaryPanel extends ContentPanel
{
	private List<ColumnConfig> residuesConfigs;
	private ListStore<InterfaceResidueSummaryModel> residuesStore;
	private ColumnModel residuesColumnModel;
	private Grid<InterfaceResidueSummaryModel> residuesGrid;
	private List<Integer> initialColumnWidth;
	
	private MainController mainController;
	
	private int structure;
	
	private boolean useBufferedView = false;
	
	public ResiduesSummaryPanel(
						 String header, 
						 final MainController mainController,
						 int height,
						 int structure) 
	{
		if(GXT.isIE8)
		{
			useBufferedView = true;
		}
		
		this.mainController = mainController;
		this.structure = structure;
		this.setBodyBorder(false);
		this.setBorders(false);
		this.setLayout(new FitLayout());
		this.getHeader().setVisible(false);
		this.setScrollMode(Scroll.NONE);
		this.setHeight(height);

		residuesConfigs = createColumnConfig();

		residuesStore = new ListStore<InterfaceResidueSummaryModel>();
		residuesColumnModel = new ColumnModel(residuesConfigs);
		
		residuesGrid = new Grid<InterfaceResidueSummaryModel>(residuesStore, residuesColumnModel);
		residuesGrid.setBorders(false);
		residuesGrid.setStripeRows(true);
		residuesGrid.setColumnLines(false);
		residuesGrid.setHideHeaders(true);
		residuesGrid.getSelectionModel().setLocked(true);
		residuesGrid.setLoadMask(true);
		
		residuesGrid.getView().setViewConfig(new GridViewConfig(){
			@Override
			public String getRowStyle(ModelData model, int rowIndex,
					ListStore<ModelData> ds) 
			{
				return "summary";
			}
		});
		
		residuesGrid.disableTextSelection(false);
		residuesGrid.getView().setForceFit(false);
		this.add(residuesGrid);
	}
	
	/**
	 * Creates columns configurations for residues summary grid.
	 * @return columns configurations for residues summary grid
	 */
	private List<ColumnConfig> createColumnConfig() 
	{
		List<ColumnConfig> configs = GridColumnConfigGenerator.createColumnConfigs(mainController,
				   "residues_summary",
				   new InterfaceResidueSummaryModel());

		if(configs != null)
		{
			initialColumnWidth = new ArrayList<Integer>();
			
			for(ColumnConfig columnConfig : configs)
			{
				initialColumnWidth.add(columnConfig.getWidth());
			}
		}
		
		return configs;

	}

	/**
	 * Sets content of residues summary grid.
	 */
	public void fillResiduesGrid() 
	{
		residuesStore.removeAll();
		
		List<InterfaceResidueSummaryModel> interfaceSummaryItems = new ArrayList<InterfaceResidueSummaryModel>();

		double entropyCoreValue = Double.NaN;
		double entropyRimValue = Double.NaN;
		double entropyRatioValue = Double.NaN;
		
		for (InterfaceScoreItem scoreItem : mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getInterfaceScores()) 
		{
			if(scoreItem.getMethod().equals("Entropy"))
			{
				if(structure == 1)
				{
					entropyCoreValue = scoreItem.getUnweightedCore1Scores();
					entropyRimValue = scoreItem.getUnweightedRim1Scores();
					entropyRatioValue = scoreItem.getUnweightedRatio1Scores();
				}
				else
				{
					entropyCoreValue = scoreItem.getUnweightedCore2Scores();
					entropyRimValue = scoreItem.getUnweightedRim2Scores();
					entropyRatioValue = scoreItem.getUnweightedRatio2Scores();
				}
			}
		}
	
		InterfaceResidueSummaryModel model = new InterfaceResidueSummaryModel();
		model.setTitle(MainController.CONSTANTS.interfaces_residues_aggergation_total_cores());
		
		double asa = 0;
		double bsa = 0;
		
		if(structure == 1)
		{
			asa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getAsaC1();
			bsa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getBsaC1();
		}
		else
		{
			asa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getAsaC2();
			bsa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getBsaC2();
		}
		
		model.setAsa(asa);
		model.setBsa(bsa);
		model.setEntropyScore(entropyCoreValue);
		
		interfaceSummaryItems.add(model);
		
		
		model = new InterfaceResidueSummaryModel();
		model.setTitle(MainController.CONSTANTS.interfaces_residues_aggergation_total_rims());
		
		asa = 0;
		bsa = 0;
		
		if(structure == 1)
		{
			asa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getAsaR1();
			bsa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getBsaR1();
		}
		else
		{
			asa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getAsaR2();
			bsa = mainController.getPdbScoreItem().getInterfaceItem(mainController.getMainViewPort().getInterfacesResiduesWindow().getSelectedInterface() - 1).getBsaR2();
		}
		
		model.setAsa(asa);
		model.setBsa(bsa);
		model.setEntropyScore(entropyRimValue);
		
		interfaceSummaryItems.add(model);
		
		
		model = new InterfaceResidueSummaryModel();
		model.setTitle(MainController.CONSTANTS.interfaces_residues_aggergation_ratios());
		model.setEntropyScore(entropyRatioValue);
		
		interfaceSummaryItems.add(model);
		
		residuesStore.add(interfaceSummaryItems);
	}
	
	/**
	 * Cleans content of residues summary grid.
	 */
	public void cleanResiduesGrid()
	{
		residuesStore.removeAll();
	}
	
	/**
	 * Adjusts size of the residues summary grid based on the size of the screen
	 * and initial settings for the grid. 
	 * @param assignedWidth width assigned for the grid
	 */
	public void resizeGrid(int assignedWidth) 
	{
		int scoresGridWidthOfAllVisibleColumns = GridUtil.calculateWidthOfVisibleColumns(residuesGrid, initialColumnWidth) + 10;
		
		if(useBufferedView)
		{
			scoresGridWidthOfAllVisibleColumns += 20;
		}

//		int assignedWidth = (int)((mainController.getMainViewPort().getInterfacesResiduesWindow().getWindowWidth() - 30) * 0.48);
		
		int nrOfColumn = residuesGrid.getColumnModel().getColumnCount();
		
		if (GridUtil.checkIfForceFit(scoresGridWidthOfAllVisibleColumns, 
									 assignedWidth)) 
		{
			float gridWidthMultiplier = (float)assignedWidth / scoresGridWidthOfAllVisibleColumns;
			
			for (int i = 0; i < nrOfColumn; i++) 
			{
				residuesGrid.getColumnModel().setColumnWidth(i, (int)(initialColumnWidth.get(i) * gridWidthMultiplier), true);
			}
		} 
		else 
		{
			for (int i = 0; i < nrOfColumn; i++) 
			{
				residuesGrid.getColumnModel().getColumn(i).setWidth(initialColumnWidth.get(i));
			}
			
			assignedWidth = scoresGridWidthOfAllVisibleColumns;
		}
		
		residuesGrid.setWidth(assignedWidth);
		this.setWidth(assignedWidth + 10);
		
//		if(useBufferedView)
		{
			residuesGrid.getView().refresh(true);
		}
		
		this.layout();
	}
	
	/**
	 * Retrieves residues summary grid.
	 * @return residues summary grid
	 */
	public Grid<InterfaceResidueSummaryModel> getResiduesGrid() 
	{
		return residuesGrid;
	}
}

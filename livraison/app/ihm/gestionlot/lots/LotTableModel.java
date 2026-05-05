package app.ihm.gestionlot.lots;

import javax.swing.table.DefaultTableModel;

/**
 * Modèle de table pour les lots, non éditable.
 */
public class LotTableModel extends DefaultTableModel
{
	public LotTableModel(Object[] columnNames, int rowCount)
	{
		super(columnNames, rowCount);
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}
}
package app.ihm.gestionlot.societes;

import javax.swing.table.DefaultTableModel;

/**
 * Modèle de table pour les sociétés, non éditable.
 */
public class SocieteTableModel extends DefaultTableModel
{
	public SocieteTableModel(Object[] columnNames, int rowCount)
	{
		super(columnNames, rowCount);
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}
}
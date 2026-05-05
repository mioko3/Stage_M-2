package app.ihm.gestionlot.lots;

import app.ihm.IhmUtils;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer pour la colonne "Statut échant." qui colore selon VA/BL/EP.
 */
public class StatutRenderer extends DefaultTableCellRenderer
{
	@Override
	public Component getTableCellRendererComponent(JTable t, Object v,
			boolean sel, boolean foc, int r, int c)
	{
		super.getTableCellRendererComponent(t, v, sel, foc, r, c);
		String s = v != null ? v.toString() : "";
		if (s.startsWith("VA")) setForeground(IhmUtils.VERT);
		else if (s.startsWith("BL")) setForeground(IhmUtils.ROUGE);
		else setForeground(IhmUtils.AMBER);
		if (!sel) setBackground(Color.WHITE);
		return this;
	}
}
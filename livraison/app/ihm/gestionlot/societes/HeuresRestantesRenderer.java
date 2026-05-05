package app.ihm.gestionlot.societes;

import app.ihm.IhmUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer pour la colonne "H restantes" qui colore selon le nombre d'heures.
 */
public class HeuresRestantesRenderer extends DefaultTableCellRenderer
{
	@Override
	public Component getTableCellRendererComponent(JTable t, Object v,
			boolean sel, boolean foc, int r, int c)
	{
		super.getTableCellRendererComponent(t, v, sel, foc, r, c);
		try
		{
			int h = Integer.parseInt(v.toString().replace("h","").trim());
			setForeground(h > 100 ? IhmUtils.VERT : h > 30 ? IhmUtils.AMBER : IhmUtils.ROUGE);
			setFont(getFont().deriveFont(Font.BOLD));
		}
		catch (NumberFormatException ex)
		{
			setForeground(Color.BLACK);
		}
		if (!sel) setBackground(Color.WHITE);
		return this;
	}
}
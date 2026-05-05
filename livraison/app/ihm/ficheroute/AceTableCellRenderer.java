package app.ihm.ficheroute;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer pour les cellules du tableau FicheRoute qui colore les lignes d'en-tête ACE.
 */
public class AceTableCellRenderer extends DefaultTableCellRenderer
{
	private final PanelFicheRoute panel;

	public AceTableCellRenderer(PanelFicheRoute panel)
	{
		this.panel = panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable t, Object v,
			boolean sel, boolean foc, int r, int c)
	{
		super.getTableCellRendererComponent(t, v, sel, foc, r, c);
		if (panel.estLigneAce(r))
		{
			Color bg = panel.couleurAcePourLigne(r);
			setBackground(bg);
			setForeground(Color.WHITE);
			setFont(getFont().deriveFont(Font.BOLD, 12f));
			setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		}
		return this;
	}
}

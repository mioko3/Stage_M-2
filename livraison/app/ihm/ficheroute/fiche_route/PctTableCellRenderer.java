package app.ihm.ficheroute.fiche_route;

import app.ihm.IhmUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer pour les colonnes de pourcentage dans le tableau FicheRoute.
 */
public class PctTableCellRenderer extends DefaultTableCellRenderer
{
	private final PanelFicheRoute panel;

	public PctTableCellRenderer(PanelFicheRoute panel)
	{
		this.panel = panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable t, Object v,
			boolean sel, boolean foc, int r, int c)
	{
		if (panel.estLigneAce(r))
		{
			setBackground(panel.couleurAcePourLigne(r));
			setText("");
			return this;
		}
		super.getTableCellRendererComponent(t, v, sel, foc, r, c);
		setHorizontalAlignment(SwingConstants.CENTER);
		try
		{
			double pct = Double.parseDouble(v.toString().replace("%","").trim());
			if (!sel) setBackground(pct >= 80 ? new Color(210,240,210)
				: pct >= 50 ? new Color(255,250,200) : new Color(255,220,220));
			setForeground(pct >= 80 ? IhmUtils.VERT : pct >= 50 ? IhmUtils.AMBER : IhmUtils.ROUGE);
			setFont(getFont().deriveFont(Font.BOLD));
		}
		catch (Exception ex) { setBackground(Color.WHITE); setForeground(Color.BLACK); }
		return this;
	}
}

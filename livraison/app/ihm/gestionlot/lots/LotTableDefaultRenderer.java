package app.ihm.gestionlot.lots;

import app.metier.lot.Lot;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer par défaut pour la table des lots, colore les lignes sous douane.
 */
public class LotTableDefaultRenderer extends DefaultTableCellRenderer
{
	private final PanelLots panel;

	public LotTableDefaultRenderer(PanelLots panel)
	{
		this.panel = panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable t, Object v,
			boolean sel, boolean foc, int r, int c)
	{
		super.getTableCellRendererComponent(t, v, sel, foc, r, c);
		if (!sel)
		{
			Lot lot = panel.getLotLigne(r);
			if (lot != null && lot.isEstSousDouane())
				setBackground(new Color(255, 100, 100)); // rouge clair pour sous douane
			else
				setBackground(Color.WHITE);
		}
		return this;
	}
}
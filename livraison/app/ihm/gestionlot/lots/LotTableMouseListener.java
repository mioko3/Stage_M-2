package app.ihm.gestionlot.lots;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Listener pour double-clic sur la table des lots.
 */
public class LotTableMouseListener extends MouseAdapter
{
	private final PanelLots panel;

	public LotTableMouseListener(PanelLots panel)
	{
		this.panel = panel;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
			panel.ouvrirEdition();
	}
}
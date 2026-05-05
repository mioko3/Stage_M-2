package app.ihm.gestionlot.societes;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Listener pour double-clic sur la table des sociétés.
 */
public class SocieteTableMouseListener extends MouseAdapter
{
	private final PanelSocietes panel;

	public SocieteTableMouseListener(PanelSocietes panel)
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
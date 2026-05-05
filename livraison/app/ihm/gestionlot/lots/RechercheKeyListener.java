package app.ihm.gestionlot.lots;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Listener pour la recherche en temps réel.
 */
public class RechercheKeyListener extends KeyAdapter
{
	private final PanelLots panel;

	public RechercheKeyListener(PanelLots panel)
	{
		this.panel = panel;
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		panel.rafraichir();
	}
}
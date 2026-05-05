package app.ihm.dialogue.ajout_lot;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Listener pour recalculer les heures en temps réel dans DialogAjoutLot.
 */
public class CalculHeuresKeyListenerAjout extends KeyAdapter
{
	private final DialogAjoutLot dialog;

	public CalculHeuresKeyListenerAjout(DialogAjoutLot dialog)
	{
		this.dialog = dialog;
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		dialog.calculerHeures();
	}
}

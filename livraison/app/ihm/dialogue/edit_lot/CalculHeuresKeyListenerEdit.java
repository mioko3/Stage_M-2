package app.ihm.dialogue.edit_lot;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Listener pour recalculer les heures en temps réel dans DialogEditLot.
 */
public class CalculHeuresKeyListenerEdit extends KeyAdapter
{
	private final DialogEditLot dialog;

	public CalculHeuresKeyListenerEdit(DialogEditLot dialog)
	{
		this.dialog = dialog;
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		dialog.calculerHeures();
	}
}

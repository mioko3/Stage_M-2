package app.ihm.automatisation;

import javax.swing.JPanel;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import java.awt.BorderLayout;
/**
 * cette class sera pour automatiser et voir ce qui et normalement le mieux a faire 
 */
public class PanelAuto extends JPanel
{
	private Controleur ctrl;
	private FenetrePrincipale fP;

	public PanelAuto(Controleur ctrl, FenetrePrincipale fP)
	{
		this.ctrl = ctrl;
		this.fP = fP;
		setLayout(new BorderLayout());
		setBackground(IhmUtils.FOND);

	}

	
}

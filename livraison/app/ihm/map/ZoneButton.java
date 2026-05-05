package app.ihm.map;

import app.ihm.IhmUtils;
import app.metier.lot.Lot;
import java.awt.*;
import java.util.List;
import javax.swing.JButton;

/**
 * Bouton représentant une zone de l'entrepôt.
 */
public class ZoneButton extends JButton
{
	private final String code;
	private final String libelle;
	private int     nbLots   = 0;
	private Color   couleur  = IhmUtils.GRIS_C;
	private boolean selected = false;

	public ZoneButton(String code, String libelle)
	{
		this.code    = code;
		this.libelle = libelle;
		setPreferredSize(new Dimension(200, 120));
		setMinimumSize(new Dimension(160, 100));
		setFocusPainted(false);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public void mettreAJour(List<Lot> lots)
	{
		nbLots  = lots.size();
		couleur = couleurDominante(lots);
		repaint();
	}

	public void setSelectionnee(boolean sel)
	{
		selected = sel;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth(), h = getHeight();

		// Fond
		Color bg = couleur;
		g2.setColor(bg);
		g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);

		// Bordure sélection
		if (selected)
		{
			g2.setColor(new Color(30, 80, 180));
			g2.setStroke(new BasicStroke(3));
		}
		else
		{
			g2.setColor(new Color(0, 0, 0, 60));
			g2.setStroke(new BasicStroke(1.5f));
		}
		g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);

		// Hover effect
		if (getModel().isRollover())
		{
			g2.setColor(new Color(255, 255, 255, 40));
			g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
		}

		// Code de zone (gros)
		g2.setColor(textColor(bg));
		g2.setFont(new Font("SansSerif", Font.BOLD, 28));
		FontMetrics fm = g2.getFontMetrics();
		int tw = fm.stringWidth(code);
		g2.drawString(code, (w - tw) / 2, h / 2);

		// Libellé (petit, en dessous)
		g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
		fm = g2.getFontMetrics();
		tw = fm.stringWidth(libelle);
		g2.setColor(new Color(textColor(bg).getRed(),
			textColor(bg).getGreen(), textColor(bg).getBlue(), 180));
		g2.drawString(libelle, (w - tw) / 2, h / 2 + 18);

		// Badge nombre de lots
		String badge = nbLots > 0 ? nbLots + " lot" + (nbLots > 1 ? "s" : "") : "vide";
		g2.setFont(new Font("SansSerif", Font.BOLD, 12));
		fm = g2.getFontMetrics();
		int bw = fm.stringWidth(badge) + 14;
		int bh = fm.getHeight() + 6;
		int bx = (w - bw) / 2;
		int by = h - bh - 10;
		g2.setColor(new Color(0, 0, 0, 80));
		g2.fillRoundRect(bx, by, bw, bh, 8, 8);
		g2.setColor(Color.WHITE);
		g2.drawString(badge, bx + 7, by + fm.getAscent() + 3);

		g2.dispose();
	}

	private Color textColor(Color bg)
	{
		// Calcul luminosité pour choisir texte blanc ou noir
		double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
		return lum < 140 ? Color.WHITE : new Color(30, 30, 30);
	}

	private static Color couleurDominante(List<Lot> lots)
	{
		if (lots.isEmpty()) return IhmUtils.GRIS_C;

		boolean sousdouane = lots.stream().anyMatch(Lot::isEstSousDouane);
		if (sousdouane) return new Color(180, 100, 200);

		long va = lots.stream().filter(l -> s(l.getStatutEchant()).startsWith("VA")).count();
		long bl = lots.stream().filter(l -> s(l.getStatutEchant()).startsWith("BL")).count();
		long ep = lots.stream().filter(l -> s(l.getStatutEchant()).startsWith("EP")).count();

		if (bl > 0)       return new Color(220, 80,  80);
		if (ep > va)      return IhmUtils.AMBER;
		return new Color(70, 160, 70);
	}

	private static String s(String v) { return v != null ? v : ""; }
}
package app.ihm.map;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.lot.Lot;
import app.metier.personelle.Societe;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Onglet "🗺 Carte entrepôt".
 *
 * Affiche un plan schématique de l'entrepôt avec les zones :
 *   B42 · A99 · LTS · HD · B21 · C
 *
 * Chaque zone montre :
 *   - Le nombre de lots présents
 *   - La couleur selon le statut dominant (VA=vert, BL=rouge, EP=orange)
 *   - Un clic ouvre le détail des lots de cette zone
 *
 * Un panneau latéral liste les lots de la zone sélectionnée.
 */
public class PanelMap extends JPanel
{
	private final Controleur        ctrl;
	private final FenetrePrincipale fenetre;

	// Layout de l'entrepôt : rangées de zones
	// Chaque zone = { code, nom complet, ligne, colonne, colspan }
	private static final Object[][] ZONES_DEF = {
		//  code    libellé complet      row  col  cols  rows
		{ "B42",  "Zone B42",            0,   0,   1,    1 },
		{ "B21",  "Zone B21",            0,   1,   1,    1 },
		{ "A99",  "Zone A99",            1,   0,   1,    1 },
		{ "C",    "Zone C",              1,   1,   1,    1 },
		{ "LTS",  "Long Term Storage",   2,   0,   1,    1 },
		{ "HD",   "Hors Douane (HD)",    2,   1,   1,    1 },
	};

	private final Map<String, ZoneButton> boutons = new LinkedHashMap<>();
	private JPanel          panelDetail;
	private JLabel          lblZoneSelectionnee;
	private DefaultListModel<String> listModel;
	private JList<String>   listeLots;
	private String          zoneSelectionnee = null;

	// ── Construction ──────────────────────────────────────────────────────

	public PanelMap(Controleur ctrl, FenetrePrincipale fenetre)
	{
		this.ctrl    = ctrl;
		this.fenetre = fenetre;

		setLayout(new BorderLayout(12, 0));
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		setBackground(IhmUtils.FOND);

		add(creerPlanEntrepot(), BorderLayout.CENTER);
		add(creerPanelDetail(),  BorderLayout.EAST);
		add(creerLegende(),      BorderLayout.SOUTH);
	}

	// ── Plan de l'entrepôt ────────────────────────────────────────────────

	private JPanel creerPlanEntrepot()
	{
		JPanel wrapper = new JPanel(new BorderLayout(0, 8));
		wrapper.setBackground(IhmUtils.FOND);

		JLabel titre = new JLabel("🏭  Plan de l'entrepôt — emplacements des lots");
		titre.setFont(new Font("SansSerif", Font.BOLD, 15));
		titre.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		wrapper.add(titre, BorderLayout.NORTH);

		// Grille 3 lignes × 2 colonnes
		JPanel grille = new JPanel(new GridBagLayout());
		grille.setBackground(new Color(235, 237, 242));
		grille.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(180, 180, 190), 2),
			BorderFactory.createEmptyBorder(16, 16, 16, 16)
		));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill    = GridBagConstraints.BOTH;
		gbc.insets  = new Insets(8, 8, 8, 8);
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		for (Object[] def : ZONES_DEF)
		{
			String code    = (String) def[0];
			String libelle = (String) def[1];
			gbc.gridy      = (int) def[2];
			gbc.gridx      = (int) def[3];
			gbc.gridwidth  = (int) def[4];
			gbc.gridheight = (int) def[5];

			ZoneButton btn = new ZoneButton(code, libelle);
			boutons.put(code, btn);
			String c = code; // effectively final for lambda
			btn.addActionListener(e -> selectionnerZone(c));
			grille.add(btn, gbc);
		}

		// Entrée entrepôt (décoratif)
		JPanel entree = new JPanel();
		entree.setBackground(new Color(60, 60, 70));
		entree.setPreferredSize(new Dimension(0, 32));
		JLabel lblEntree = new JLabel("▼  ENTRÉE ENTREPÔT  ▼");
		lblEntree.setForeground(Color.WHITE);
		lblEntree.setFont(new Font("SansSerif", Font.BOLD, 11));
		entree.add(lblEntree);
		gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2; gbc.gridheight = 1;
		gbc.weighty = 0.15;
		grille.add(entree, gbc);

		wrapper.add(grille, BorderLayout.CENTER);
		return wrapper;
	}

	// ── Panneau de détail (droite) ────────────────────────────────────────

	private JPanel creerPanelDetail()
	{
		panelDetail = new JPanel(new BorderLayout(0, 8));
		panelDetail.setBackground(Color.WHITE);
		panelDetail.setPreferredSize(new Dimension(300, 0));
		panelDetail.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(IhmUtils.BORD),
			BorderFactory.createEmptyBorder(12, 12, 12, 12)
		));

		lblZoneSelectionnee = new JLabel("← Cliquez sur une zone");
		lblZoneSelectionnee.setFont(new Font("SansSerif", Font.BOLD, 13));
		lblZoneSelectionnee.setForeground(IhmUtils.BLEU);

		listModel = new DefaultListModel<>();
		listeLots = new JList<>(listModel);
		listeLots.setFont(new Font("Monospaced", Font.PLAIN, 12));
		listeLots.setSelectionBackground(IhmUtils.SEL);
		listeLots.setCellRenderer(new LotCellRenderer());

		JScrollPane scroll = new JScrollPane(listeLots);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));

		panelDetail.add(lblZoneSelectionnee, BorderLayout.NORTH);
		panelDetail.add(scroll,              BorderLayout.CENTER);
		return panelDetail;
	}

	// ── Légende ───────────────────────────────────────────────────────────

	private JPanel creerLegende()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
		p.setBackground(IhmUtils.FOND);

		p.add(badge(IhmUtils.VERT,  "VA - Validé"));
		p.add(badge(IhmUtils.ROUGE, "BL - Bloqué"));
		p.add(badge(IhmUtils.AMBER, "EP - En attente"));
		p.add(badge(new Color(180, 100, 200), "Sous douane"));
		p.add(badge(IhmUtils.GRIS_C, "Vide"));

		JLabel note = new JLabel("  · Cliquez sur une zone pour voir les lots");
		note.setForeground(Color.GRAY);
		note.setFont(new Font("SansSerif", Font.ITALIC, 11));
		p.add(note);

		return p;
	}

	private JPanel badge(Color c, String label)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setBackground(IhmUtils.FOND);
		JPanel carre = new JPanel();
		carre.setBackground(c);
		carre.setPreferredSize(new Dimension(14, 14));
		carre.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		JLabel l = new JLabel(label);
		l.setFont(new Font("SansSerif", Font.PLAIN, 11));
		p.add(carre);
		p.add(l);
		return p;
	}

	// ── Logique de sélection ──────────────────────────────────────────────

	private void selectionnerZone(String code)
	{
		zoneSelectionnee = code;

		// Mettre à jour la bordure des boutons
		boutons.forEach((c, btn) -> btn.setSelectionnee(c.equals(code)));

		// Remplir le panneau de détail
		List<Lot> lots = getLotsZone(code);
		lblZoneSelectionnee.setText("📦  Zone " + code + "  —  " + lots.size() + " lot(s)");

		listModel.clear();
		if (lots.isEmpty())
		{
			listModel.addElement("  (aucun lot dans cette zone)");
		}
		else
		{
			for (Lot l : lots)
			{
				Societe soc = ctrl.getSocieteDuLot(l);
				String sNom = soc != null ? soc.getNom() : "—";
				// Format: statut | N°CDE | Société | Heures
				String icone = iconeStatut(l);
				listModel.addElement(icone + " " + l.getNumCDE()
					+ "  " + s(l.getTypologie())
					+ "  [" + sNom + "]"
					+ "  " + String.format("%.1fh", l.getHeures()));
			}
		}

		repaint();
	}

	private List<Lot> getLotsZone(String code)
	{
		List<Lot> res = new ArrayList<>();
		for (Lot l : ctrl.getLots())
		{
			if (code.equalsIgnoreCase(s(l.getEmplacement())))
				res.add(l);
		}
		return res;
	}

	// ── Rafraîchissement (appelé par FenetrePrincipale) ───────────────────

	public void rafraichir()
	{
		// Calculer les stats par zone et mettre à jour les boutons
		for (Object[] def : ZONES_DEF)
		{
			String code = (String) def[0];
			List<Lot> lots = getLotsZone(code);
			ZoneButton btn = boutons.get(code);
			if (btn != null) btn.mettreAJour(lots);
		}

		// Rafraîchir le détail si une zone est sélectionnée
		if (zoneSelectionnee != null)
			selectionnerZone(zoneSelectionnee);

		repaint();
	}

	// ── Utilitaires ───────────────────────────────────────────────────────

	private static String s(String v) { return v != null ? v : ""; }

	private static String iconeStatut(Lot l)
	{
		if (l.isEstSousDouane()) return "🔴";
		String st = s(l.getStatutEchant());
		if (st.startsWith("VA")) return "🟢";
		if (st.startsWith("BL")) return "🔴";
		return "🟡";
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

	// ══════════════════════════════════════════════════════════════════════
	// Bouton de zone (composant interne)
	// ══════════════════════════════════════════════════════════════════════

	private static class ZoneButton extends JButton
	{
		private final String code;
		private final String libelle;
		private int     nbLots   = 0;
		private Color   couleur  = IhmUtils.GRIS_C;
		private boolean selected = false;

		ZoneButton(String code, String libelle)
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

		void mettreAJour(List<Lot> lots)
		{
			nbLots  = lots.size();
			couleur = couleurDominante(lots);
			repaint();
		}

		void setSelectionnee(boolean sel)
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
	}

	// ══════════════════════════════════════════════════════════════════════
	// Renderer de liste de lots
	// ══════════════════════════════════════════════════════════════════════

	private static class LotCellRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel l = (JLabel) super.getListCellRendererComponent(
				list, value, index, isSelected, cellHasFocus);
			l.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
			if (!isSelected && index % 2 == 0)
				l.setBackground(new Color(248, 249, 252));
			return l;
		}
	}
}
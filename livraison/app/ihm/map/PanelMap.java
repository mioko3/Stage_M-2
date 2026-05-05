package app.ihm.map;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.lot.Lot;
import app.metier.personelle.Societe;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Carte interactive de l'entrepôt.
 *
 * Chaque zone est identifiée par une lettre (A, B, C, D, LTS, HD).
 * Dans les zones à rangées (A–D) chaque numéro de rangée est affiché
 * comme une cellule cliquable (ex : B21, B42).
 * Les zones spéciales LTS et HD sont une seule cellule sans numéro.
 *
 * Un clic sur une cellule sélectionne l'emplacement et liste les lots
 * dans le panneau de droite. Un survol affiche un tooltip récapitulatif.
 */
public class PanelMap extends JPanel
{
	private final Controleur        ctrl;
	private final FenetrePrincipale fenetre;

	private JTextArea         infoLot;
	// ── Zones ─────────────────────────────────────────────────────────────
	private static final String[] ZONES_RANGEES   = { "A", "B", "C", "D" };
	private static final String[] ZONES_SPECIALES = { "LTS", "HD" };

	private static final Map<String, Color> COULEUR_ZONE = new LinkedHashMap<>();
	static {
		COULEUR_ZONE.put("A",   new Color(210, 230, 255));
		COULEUR_ZONE.put("B",   new Color(210, 255, 215));
		COULEUR_ZONE.put("C",   new Color(255, 235, 200));
		COULEUR_ZONE.put("D",   new Color(240, 215, 255));
		COULEUR_ZONE.put("LTS", new Color(200, 200, 200));
		COULEUR_ZONE.put("HD",  new Color(255, 210, 210));
	}

	private String    emplacementSel = null;
	private PlanPanel planPanel;
	private List<Lot> lotsCourants = new ArrayList<>();
	private Map<String, List<Lot>> lotsParEmplacement = new HashMap<>();
	private Map<String, List<String>> numerosParZone = new HashMap<>();

	// Panneau détail
	private JLabel                   lblEmpl;
	private DefaultListModel<String> listModel;
	private JList<String>            listeLots;

	// ── Constructeur ──────────────────────────────────────────────────────

	public PanelMap(Controleur ctrl, FenetrePrincipale fenetre)
	{
		this.ctrl    = ctrl;
		this.fenetre = fenetre;
		setLayout(new BorderLayout(10, 0));
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		setBackground(IhmUtils.FOND);

		construireCacheLots();
		planPanel = new PlanPanel();
		add(planPanel,          BorderLayout.CENTER);
		add(creerPanelDetail(), BorderLayout.EAST);
		add(creerLegende(),     BorderLayout.SOUTH);
	}

	// ══════════════════════════════════════════════════════════════════════
	// Plan dessiné
	// ══════════════════════════════════════════════════════════════════════

	private class PlanPanel extends JPanel
	{
		// Hitboxes pour les clics et tooltips
		private final Map<String, Rectangle> hitboxes = new LinkedHashMap<>();

		PlanPanel()
		{
			setBackground(new Color(238, 240, 244));
			setPreferredSize(new Dimension(820, 560));
			ToolTipManager.sharedInstance().registerComponent(this);
			addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					for (Map.Entry<String, Rectangle> en : hitboxes.entrySet())
						if (en.getValue().contains(e.getPoint())) {
							selectionnerEmplacement(en.getKey());
							repaint();
							return;
						}
				}
			});
		}

		@Override
		public String getToolTipText(MouseEvent e)
		{
			for (Map.Entry<String, Rectangle> en : hitboxes.entrySet())
				if (en.getValue().contains(e.getPoint()))
					return buildTooltip(en.getKey());
			return null;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			hitboxes.clear();
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int W = getWidth(), H = getHeight();

			// ── Contour entrepôt ──────────────────────────────────────────
			g2.setColor(new Color(220, 222, 228));
			g2.fillRoundRect(8, 8, W - 16, H - 16, 16, 16);
			g2.setColor(new Color(160, 165, 175));
			g2.setStroke(new BasicStroke(2));
			g2.drawRoundRect(8, 8, W - 16, H - 16, 16, 16);

			// Titre
			g2.setFont(new Font("SansSerif", Font.BOLD, 14));
			g2.setColor(new Color(50, 55, 65));
			g2.drawString("Plan de l'entrepôt", 24, 30);

			// ── Dimensions layout ─────────────────────────────────────────
			int mX  = 22, mY  = 42;
			int gap = 10;
			int specH = 80; // hauteur zones LTS / HD

			// Hauteur disponible pour les zones à rangées
			int zoneH = H - mY - specH - gap * 3 - 22;
			// Largeur d'une zone (4 zones côte à côte)
			int zoneW = (W - mX * 2 - gap * 3) / 4;

			// ── Zones à rangées (A B C D) ─────────────────────────────────
			for (int z = 0; z < ZONES_RANGEES.length; z++)
			{
				int x = mX + z * (zoneW + gap);
				dessinerZoneRangees(g2, ZONES_RANGEES[z], x, mY, zoneW, zoneH);
			}

			// ── Zones spéciales (LTS large, HD plus petit) ────────────────
			int y1   = mY + zoneH + gap;
			int ltsW = zoneW * 3 + gap * 2;
			int hdW  = zoneW;

			dessinerZoneSpeciale(g2, "LTS", mX,              y1, ltsW, specH);
			dessinerZoneSpeciale(g2, "HD",  mX + ltsW + gap, y1, hdW,  specH);

			// ── Entrée ────────────────────────────────────────────────────
			int eY = H - 14;
			g2.setFont(new Font("SansSerif", Font.BOLD, 10));
			g2.setColor(new Color(70, 75, 85));
			String entree = "▲  ENTRÉE";
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(entree, W / 2 - fm.stringWidth(entree) / 2, eY);
		}

		// ── Zone avec rangées numérotées ──────────────────────────────────

		private void dessinerZoneRangees(Graphics2D g2, String lettre,
		                                  int x, int y, int w, int h)
		{
			Color cFond = COULEUR_ZONE.getOrDefault(lettre, Color.LIGHT_GRAY);

			// Fond + bordure
			g2.setColor(cFond);
			g2.fillRoundRect(x, y, w, h, 10, 10);
			g2.setColor(cFond.darker());
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(x, y, w, h, 10, 10);

			// Titre zone
			g2.setFont(new Font("SansSerif", Font.BOLD, 14));
			g2.setColor(new Color(35, 40, 50));
			g2.drawString("Zone " + lettre, x + 7, y + 17);

			// Emplacements
			List<String> nums = getNumerosZone(lettre);
			if (nums.isEmpty())
			{
				g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
				g2.setColor(new Color(130, 135, 145));
				g2.drawString("vide", x + w / 2 - 12, y + h / 2 + 4);
				return;
			}

			// Calcul grille de cellules
			int n    = nums.size();
			int cols = Math.max(1, (int) Math.ceil(Math.sqrt(n)));
			int rows = (int) Math.ceil((double) n / cols);
			int pad  = 6, topPad = 22;
			int cw   = (w - pad * 2 - (cols - 1) * 5) / cols;
			int ch   = (h - topPad - pad - (rows - 1) * 5) / rows;
			cw = Math.max(cw, 32); ch = Math.max(ch, 26);

			for (int i = 0; i < n; i++)
			{
				String num  = nums.get(i);
				String empl = lettre + num;
				int col = i % cols, row = i / cols;
				int cx  = x + pad  + col * (cw + 5);
				int cy  = y + topPad + row * (ch + 5);
				dessinerCellule(g2, empl, cx, cy, cw, ch);
				hitboxes.put(empl, new Rectangle(cx, cy, cw, ch));
			}
		}

		// ── Zone spéciale ─────────────────────────────────────────────────

		private void dessinerZoneSpeciale(Graphics2D g2, String code,
		                                   int x, int y, int w, int h)
		{
			Color cFond = COULEUR_ZONE.getOrDefault(code, Color.LIGHT_GRAY);

			g2.setColor(cFond);
			g2.fillRoundRect(x, y, w, h, 10, 10);
			g2.setColor(cFond.darker());
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(x, y, w, h, 10, 10);

			String lib = code.equals("LTS") ? "LTS — Long Term Storage" : "HD — Hors Douane";
			g2.setFont(new Font("SansSerif", Font.BOLD, 12));
			g2.setColor(new Color(35, 40, 50));
			g2.drawString(lib, x + 7, y + 15);

			int pad = 7;
			dessinerCellule(g2, code, x + pad, y + 20, w - pad * 2, h - 26);
			hitboxes.put(code, new Rectangle(x + pad, y + 20, w - pad * 2, h - 26));
		}

		// ── Cellule d'emplacement ─────────────────────────────────────────

		private void dessinerCellule(Graphics2D g2, String empl,
		                              int cx, int cy, int cw, int ch)
		{
			List<Lot> lots = getLotsEmplacement(empl);
			boolean sel    = empl.equals(emplacementSel);

			Color cFond = sel ? new Color(45, 105, 215) : couleurLots(lots);
			Color cText = luminance(cFond) < 140 ? Color.WHITE : new Color(25, 30, 40);

			// Fond
			g2.setColor(cFond);
			g2.fillRoundRect(cx, cy, cw, ch, 7, 7);

			// Bordure
			g2.setStroke(new BasicStroke(sel ? 2.5f : 1f));
			g2.setColor(sel ? new Color(20, 75, 195) : new Color(0, 0, 0, 45));
			g2.drawRoundRect(cx, cy, cw, ch, 7, 7);

			// Label emplacement
			g2.setFont(new Font("SansSerif", Font.BOLD, 11));
			g2.setColor(cText);
			FontMetrics fm = g2.getFontMetrics();
			int ly = lots.isEmpty()
				? cy + ch / 2 + fm.getAscent() / 2 - 1
				: cy + ch / 2 + 2;
			g2.drawString(empl, cx + (cw - fm.stringWidth(empl)) / 2, ly);

			// Badge nb lots
			if (!lots.isEmpty())
			{
				g2.setFont(new Font("SansSerif", Font.BOLD, 9));
				fm = g2.getFontMetrics();
				String badge = "" + lots.size();
				int bw = fm.stringWidth(badge) + 7, bh = fm.getAscent() + 4;
				int bx = cx + cw - bw - 2, by = cy + 2;
				g2.setColor(new Color(0, 0, 0, 100));
				g2.fillRoundRect(bx, by, bw, bh, 4, 4);
				g2.setColor(Color.WHITE);
				g2.drawString(badge, bx + 3, by + fm.getAscent());
			}
		}

		private Color couleurLots(List<Lot> lots)
		{
			if (lots.isEmpty()) return new Color(210, 212, 218);
			if (lots.stream().anyMatch(Lot::isEstSousDouane)) return new Color(170, 85, 195);
			long bl = lots.stream().filter(l -> s(l.getStatutEchant()).startsWith("BL")).count();
			long ep = lots.stream().filter(l -> s(l.getStatutEchant()).startsWith("EP")).count();
			long va = lots.stream().filter(l -> s(l.getStatutEchant()).startsWith("VA")).count();
			if (bl > 0)  return new Color(205, 55, 55);
			if (ep > va) return new Color(190, 115, 15);
			return new Color(50, 150, 60);
		}

		private int luminance(Color c) {
			return (int)(0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
		}
	}

	// ── Données ───────────────────────────────────────────────────────────

	private List<String> getNumerosZone(String lettre)
	{
		return numerosParZone.getOrDefault(lettre, new ArrayList<>());
	}

	private List<Lot> getLotsEmplacement(String empl)
	{
		return lotsParEmplacement.getOrDefault(empl, new ArrayList<>());
	}

	private String buildTooltip(String empl)
	{
		List<Lot> lots = getLotsEmplacement(empl);
		if (lots.isEmpty())
			return "<html><b>" + empl + "</b> &mdash; aucun lot</html>";
		StringBuilder sb = new StringBuilder(
			"<html><b>Emplacement " + empl + "</b><hr>");
		for (Lot l : lots)
		{
			Societe soc = ctrl.getSocieteDuLot(l);
			sb.append("• <b>").append(l.getNumCDE()).append("</b>")
			  .append("  ").append(s(l.getTypologie()))
			  .append("  [").append(soc != null ? soc.getNom() : "—").append("]")
			  .append("<br>");
		}
		return sb.append("</html>").toString();
	}

	// ── Sélection ─────────────────────────────────────────────────────────

	private void selectionnerEmplacement(String empl)
	{
		emplacementSel = empl;
		List<Lot> lots = getLotsEmplacement(empl);
		lotsCourants = lots;

		lblEmpl.setText("📦  " + empl + "  —  " + lots.size() + " lot(s)");
		listModel.clear();

		if (lots.isEmpty())
		{
			listModel.addElement("  (aucun lot ici)");
		}
		else
		{
			for (Lot l : lots)
			{
				Societe soc = ctrl.getSocieteDuLot(l);
				listModel.addElement(icone(l)
					+ " " + l.getNumCDE()
					+ "  " + s(l.getTypologie())
					+ "  [" + (soc != null ? soc.getNom() : "—") + "]"
					+ "  " + String.format("%.1fh", l.getHeures()));
			}
		}
	}

	// ── Rafraîchissement ──────────────────────────────────────────────────

	public void rafraichir()
	{
		construireCacheLots();
		if (emplacementSel != null)
			selectionnerEmplacement(emplacementSel);
		planPanel.repaint();
	}

	private void construireCacheLots()
	{
		lotsParEmplacement.clear();
		numerosParZone.clear();

		Map<String, Set<String>> tempNums = new HashMap<>();
		for (String lettre : ZONES_RANGEES)
		{
			tempNums.put(lettre, new TreeSet<>(Comparator.comparingInt(a -> {
				try { return Integer.parseInt(a); } catch (Exception e) { return 0; }
			})));
		}

		for (Lot l : ctrl.getLots())
		{
			String empl = s(l.getEmplacement());
			lotsParEmplacement.computeIfAbsent(empl, k -> new ArrayList<>()).add(l);

			// Calcul des numéros de zone
			if (!empl.isEmpty())
			{
				String lettre = empl.substring(0, 1);
				if (Arrays.asList(ZONES_RANGEES).contains(lettre))
				{
					Set<String> nums = tempNums.get(lettre);
					if (empl.equals(lettre))
						nums.add("");
					else if (empl.length() > lettre.length())
					{
						String reste = empl.substring(lettre.length());
						if (reste.matches("\\d+")) nums.add(reste);
					}
				}
			}
		}

		for (String lettre : ZONES_RANGEES)
		{
			numerosParZone.put(lettre, new ArrayList<>(tempNums.get(lettre)));
		}
	}

	// ── Panel détail ──────────────────────────────────────────────────────

	private JPanel creerPanelDetail()
	{
		JPanel p = new JPanel(new BorderLayout(0, 8));
		p.setBackground(Color.WHITE);
		p.setPreferredSize(new Dimension(270, 0));
		p.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(IhmUtils.BORD),
			BorderFactory.createEmptyBorder(12, 12, 12, 12)));

		lblEmpl = new JLabel("← Cliquez sur un emplacement");
		lblEmpl.setFont(new Font("SansSerif", Font.BOLD, 13));
		lblEmpl.setForeground(IhmUtils.BLEU);

		listModel = new DefaultListModel<>();
		listeLots = new JList<>(listModel);
		listeLots.setFont(new Font("Monospaced", Font.PLAIN, 11));
		listeLots.setSelectionBackground(IhmUtils.SEL);
		listeLots.setCellRenderer(new DefaultListCellRenderer()
		{
			public Component getListCellRendererComponent(JList<?> list, Object val,
					int idx, boolean sel, boolean focus)
			{
				JLabel l = (JLabel) super.getListCellRendererComponent(list, val, idx, sel, focus);
				l.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
				if (!sel && idx % 2 == 0) l.setBackground(new Color(248, 249, 252));
				return l;
			}
		});

		listeLots.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int idx = listeLots.getSelectedIndex();
				if (idx >= 0 && idx < lotsCourants.size()) {
					Lot lot = lotsCourants.get(idx);
					afficherInfoLot(lot);
				} else {
					infoLot.setText("");
				}
			}
		});

		// Infos lot sélectionné
		infoLot = new JTextArea(8, 20);
		infoLot.setEditable(false);
		infoLot.setFont(new Font("Monospaced", Font.PLAIN, 12));
		infoLot.setBackground(IhmUtils.INFO);
		infoLot.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		JScrollPane scrollInfo = new JScrollPane(infoLot);
		scrollInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
		scrollInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

		JScrollPane scroll = new JScrollPane(listeLots);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));

		p.add(lblEmpl, BorderLayout.NORTH);
		p.add(scroll,  BorderLayout.CENTER);
		p.add(scrollInfo, BorderLayout.SOUTH);
		return p;
	}

	// ── Légende ───────────────────────────────────────────────────────────

	private JPanel creerLegende()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
		p.setBackground(IhmUtils.FOND);
		p.add(pastille(new Color(50, 150, 60),   "VA — Validé"));
		p.add(pastille(new Color(205, 55, 55),   "BL — Bloqué"));
		p.add(pastille(new Color(190, 115, 15),  "EP — En attente"));
		p.add(pastille(new Color(170, 85, 195),  "Sous douane"));
		p.add(pastille(new Color(210, 212, 218), "Vide"));
		p.add(pastille(new Color(45, 105, 215),  "Sélectionné"));
		JLabel h = new JLabel("   Survolez pour le détail · Cliquez pour sélectionner");
		h.setFont(new Font("SansSerif", Font.ITALIC, 11));
		h.setForeground(Color.GRAY);
		p.add(h);
		return p;
	}

	private JPanel pastille(Color c, String label)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setBackground(IhmUtils.FOND);
		JPanel sq = new JPanel();
		sq.setBackground(c);
		sq.setPreferredSize(new Dimension(13, 13));
		sq.setBorder(BorderFactory.createLineBorder(c.darker()));
		p.add(sq);
		JLabel l = new JLabel(label);
		l.setFont(new Font("SansSerif", Font.PLAIN, 11));
		p.add(l);
		return p;
	}

	// ── Utilitaires ───────────────────────────────────────────────────────

	private static String s(String v) { return v != null ? v : ""; }

	private static String icone(Lot l)
	{
		if (l.isEstSousDouane()) return "🟣";
		String st = s(l.getStatutEchant());
		if (st.startsWith("VA")) return "🟢";
		if (st.startsWith("BL")) return "🔴";
		return "🟡";
	}

	private void afficherInfoLot(Lot lot)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Numéro de commande: ").append(lot.getNumCDE()).append("\n");
		sb.append("Typologie: ").append(s(lot.getTypologie())).append("\n");
		sb.append("Affaire: ").append(s(lot.getAffaire())).append("\n");
		sb.append("Nombre de pièces: ").append(lot.getNbPieces()).append("\n");
		sb.append("Cadence: ").append(lot.getCadence()).append("\n");
		sb.append("Heures: ").append(String.format("%.2f", lot.getHeures())).append("\n");
		sb.append("Heures ACE: ").append(String.format("%.2f", lot.getHeuresAce())).append("\n");
		sb.append("Valeur de vente: ").append(lot.getValeurVente()).append("\n");
		sb.append("Prix unitaire: ").append(String.format("%.2f", lot.getPrixUnitaire())).append("\n");
		sb.append("Semaine: ").append(s(lot.getSemaine())).append("\n");
		sb.append("Priorité: ").append(lot.getPriorite()).append("\n");
		sb.append("Statut: ").append(s(lot.getStatut())).append("\n");
		sb.append("Statut échantillon: ").append(s(lot.getStatutEchant())).append("\n");
		sb.append("Lot à charge: ").append(s(lot.getLotACharge())).append("\n");
		sb.append("Sous douane: ").append(lot.isEstSousDouane() ? "Oui" : "Non").append("\n");
		sb.append("Date réception: ").append(s(lot.getDateReception())).append("\n");
		sb.append("Date paiement: ").append(s(lot.getDatePaiement())).append("\n");
		sb.append("Commentaire: ").append(s(lot.getCommentaire())).append("\n");
		sb.append("Emplacement: ").append(s(lot.getEmplacement())).append("\n");
		sb.append("Méthode: ").append(s(lot.getMethode())).append("\n");
		sb.append("Distribution: ").append(s(lot.getDistribution())).append("\n");
		sb.append("Format carton: ").append(s(lot.getFormatCarton())).append("\n");
		sb.append("Machine: ").append(lot.isEstMachine() ? "Oui" : "Non").append("\n");
		Societe soc = ctrl.getSocieteDuLot(lot);
		sb.append("Société: ").append(soc != null ? soc.getNom() : "—").append("\n");
		infoLot.setText(sb.toString());
	}

	
}
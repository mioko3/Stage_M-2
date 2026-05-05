package app.ihm.ficheroute;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.ficheroute.FicheRoute;
import app.metier.lot.Lot;
import app.metier.personelle.Ace;
import app.metier.personelle.Societe;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Onglet "Fiches de Route" — calqué sur l'Excel EUP_S17.
 *
 * Pour chaque société :
 *  - En-tête récap global (VVS total, nb pièces, PU moyen, heures CE dispo)
 *  - Récap par ACE (tuiles VVS, pièces, PU moyen pour chaque ACE)
 *  - Tableau des lots regroupés par ACE avec ligne de séparation colorée
 */
public class PanelFicheRoute extends JPanel
{
	private final Controleur        ctrl;
	private final FenetrePrincipale fenetre;

	private JComboBox<String>   combSociete;
	private JLabel              lblVVS, lblPieces, lblPU, lblHeures, lblNbLots;

	// Panneau récap par ACE (reconstruit à chaque chargement)
	private JPanel              panelRecapAce;

	private DefaultTableModel modelFdr;
	private JTable            tblFdr;

	// Mapping row du tableau → lot (les lignes d'en-tête ACE n'ont pas de lot)
	private final List<Lot>     rowToLot = new ArrayList<>();

	private Societe             societeCourante;

	// Colonnes
	private static final String[] COLS = {
		"Prio", "N° CDE", "Désignation", "Typo",
		"VVS (€)", "Nb Pièces", "PU (€)",
		"PRE TRI", "SUR PISTE", "SORTIE ÉTIQ", "TRI", "FINI",
		"Méthode", "Distribution", "Emplacement",
		"Nb P. Étiq.", "Nb P. Parts",
		"Av. Étiq %", "Av. Parts %",
		"H Étiq Rest.", "H Parts Rest.",
		"Commentaire"
	};

	private static final int C_PRETRI      = 7;
	private static final int C_SURPISTE    = 8;
	private static final int C_SORETIQ     = 9;
	private static final int C_TRI         = 10;
	private static final int C_FINI        = 11;
	private static final int C_METHODE     = 12;
	private static final int C_DISTRIB     = 13;
	private static final int C_EMPLACEMENT = 14;
	private static final int C_NBETIQ      = 15;
	private static final int C_NBPARTS     = 16;
	private static final int C_AVATIQ      = 17;
	private static final int C_AVAPARTS    = 18;
	private static final int C_HETIQ       = 19;
	private static final int C_HPARTS      = 20;
	private static final int C_COMMENT     = 21;

	// Marqueur interne pour identifier les lignes d'en-tête ACE
	private static final String ACE_HEADER_MARKER = "__ACE_HEADER__";

	// Couleurs des bandeaux ACE (cycle si plus de 2 ACE)
	private static final Color[] ACE_BG = {
		new Color(13, 71, 161),   // bleu foncé — ACE 1
		new Color(27, 94, 32),    // vert foncé — ACE 2
		new Color(130, 0, 0),     // rouge foncé — ACE 3
		new Color(100, 50, 0),    // marron      — ACE 4
	};

	public PanelFicheRoute(Controleur ctrl, FenetrePrincipale fenetre)
	{
		this.ctrl    = ctrl;
		this.fenetre = fenetre;
		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(IhmUtils.FOND);

		add(creerHaut(),    BorderLayout.NORTH);
		add(creerTableau(), BorderLayout.CENTER);
	}

	// ── Haut : sélection société + récap global + récap par ACE ──────────

	private JPanel creerHaut()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBackground(IhmUtils.FOND);

		// Ligne sélection
		JPanel ligneSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		ligneSelect.setBackground(IhmUtils.FOND);

		JLabel lbl = new JLabel("Société : ");
		lbl.setFont(new Font("SansSerif", Font.BOLD, 13));

		combSociete = new JComboBox<>();
		combSociete.setFont(new Font("SansSerif", Font.PLAIN, 13));
		combSociete.setPreferredSize(new Dimension(280, 28));
		combSociete.addActionListener(e -> changerSociete());

		JButton btnExport = IhmUtils.bouton("🖨 Aperçu / Export texte", IhmUtils.BLEU, Color.WHITE);
		btnExport.addActionListener(e -> exporterTexte());

		JButton btnTerminer = IhmUtils.bouton("✓ Marquer terminé", IhmUtils.VERT, Color.WHITE);
		btnTerminer.addActionListener(e -> marquerTermine());

		ligneSelect.add(lbl);
		ligneSelect.add(combSociete);
		ligneSelect.add(btnExport);
		ligneSelect.add(btnTerminer);

		// ── Totaux GLOBAUX ────────────────────────────────────────────────
		JPanel tuiles = new JPanel(new GridLayout(1, 5, 6, 0));
		tuiles.setBackground(IhmUtils.FOND);
		tuiles.setPreferredSize(new Dimension(0, 64));

		lblNbLots = creerTuile("Lots affectés", "—", IhmUtils.BLEU);
		lblVVS    = creerTuile("VVS Total",     "—", IhmUtils.VERT);
		lblPieces = creerTuile("Nb Pièces",     "—", new Color(0, 80, 140));
		lblPU     = creerTuile("PU Moyen",      "—", IhmUtils.AMBER);
		lblHeures = creerTuile("H CE restantes","—", IhmUtils.ROUGE);

		tuiles.add(lblNbLots);
		tuiles.add(lblVVS);
		tuiles.add(lblPieces);
		tuiles.add(lblPU);
		tuiles.add(lblHeures);

		// ── Totaux PAR ACE (panneau dynamique) ───────────────────────────
		panelRecapAce = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		panelRecapAce.setBackground(IhmUtils.FOND);

		// Bloc nord = select + totaux globaux + titre + totaux ACE
		JPanel nord = new JPanel();
		nord.setLayout(new BoxLayout(nord, BoxLayout.Y_AXIS));
		nord.setBackground(IhmUtils.FOND);

		JLabel titreTotaux = new JLabel("  TOTAUX GLOBAUX");
		titreTotaux.setFont(new Font("SansSerif", Font.BOLD, 11));
		titreTotaux.setForeground(Color.GRAY);

		nord.add(ligneSelect);
		nord.add(Box.createVerticalStrut(4));
		nord.add(titreTotaux);
		nord.add(tuiles);
		nord.add(Box.createVerticalStrut(6));
		nord.add(panelRecapAce);

		p.add(nord, BorderLayout.NORTH);
		return p;
	}

	/**
	 * Reconstruit le bandeau "Totaux par ACE" en fonction des ACE de la société courante.
	 * Chaque ACE reçoit un groupe de 3 tuiles (VVS, Pièces, PU moyen).
	 */
	private void reconstruireRecapAce(FicheRoute fdr)
	{
		panelRecapAce.removeAll();

		if (societeCourante == null || societeCourante.getAces() == null
				|| societeCourante.getAces().isEmpty())
		{
			panelRecapAce.revalidate();
			panelRecapAce.repaint();
			return;
		}

		List<Ace> aces = societeCourante.getAces();

		for (int i = 0; i < aces.size(); i++)
		{
			Ace ace = aces.get(i);
			List<Lot> lots = ace.getLots() != null ? ace.getLots() : new ArrayList<>();

			int vvs = 0;
			int pieces = 0;
			double puSum = 0;
			int cptPu = 0;

			for (Lot l : lots)
			{
				vvs += l.getValeurVente();
				pieces += l.getNbPieces();

				if (l.getNbPieces() > 0)
				{
					puSum += l.getPrixUnitaire();
					cptPu++;
				}
			}

			double puMoy = cptPu > 0 ? puSum / cptPu : 0;

			Color couleur = ACE_BG[i % ACE_BG.length];

			// ── UI ─────────────────────────────────────────────
			JPanel groupe = new JPanel(new BorderLayout(0, 2));
			groupe.setBackground(IhmUtils.FOND);
			groupe.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(couleur, 2),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));

			JLabel titreLbl = new JLabel(" " + ace.getNom(), JLabel.LEFT);
			titreLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
			titreLbl.setForeground(Color.WHITE);
			titreLbl.setOpaque(true);
			titreLbl.setBackground(couleur);
			titreLbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

			JPanel tuiles3 = new JPanel(new GridLayout(1, 3, 4, 0));
			tuiles3.setBackground(IhmUtils.FOND);

			tuiles3.add(creerTuileMini("VVS",
				vvs > 0 ? String.format("%,d €", vvs) : "—", couleur));

			tuiles3.add(creerTuileMini("Pièces",
				String.format("%,d", pieces), couleur));

			tuiles3.add(creerTuileMini("PU Moy.",
				puMoy > 0 ? String.format("%.2f €", puMoy) : "—", couleur));

			groupe.add(titreLbl, BorderLayout.NORTH);
			groupe.add(tuiles3, BorderLayout.CENTER);

			panelRecapAce.add(groupe);
		}

		panelRecapAce.revalidate();
		panelRecapAce.repaint();
	}

	// ── Tuiles ───────────────────────────────────────────────────────────

	private JLabel creerTuile(String titre, String val, Color couleur)
	{
		JLabel l = new JLabel(buildTuileHtml(titre, val, couleur));
		l.setOpaque(true);
		l.setBackground(Color.WHITE);
		l.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(IhmUtils.BORD),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return l;
	}

	private JLabel creerTuileMini(String titre, String val, Color couleur)
	{
		String hex = String.format("#%02x%02x%02x",
			couleur.getRed(), couleur.getGreen(), couleur.getBlue());
		JLabel l = new JLabel(
			"<html><span style='font-size:8px;color:#888;'>" + titre + "</span><br>"
			+ "<b style='font-size:12px;color:" + hex + ";'>" + val + "</b></html>");
		l.setOpaque(true);
		l.setBackground(Color.WHITE);
		l.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(IhmUtils.BORD),
			BorderFactory.createEmptyBorder(2, 6, 2, 6)));
		l.setPreferredSize(new Dimension(100, 46));
		return l;
	}

	private String buildTuileHtml(String titre, String val, Color c)
	{
		String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
		return "<html><span style='font-size:9px;color:#888;'>" + titre + "</span><br>"
			 + "<b style='font-size:14px;color:" + hex + ";'>" + val + "</b></html>";
	}

	private void majTuile(JLabel tuile, String titre, String val, Color couleur)
	{
		tuile.setText(buildTuileHtml(titre, val, couleur));
	}

	// ── Tableau ───────────────────────────────────────────────────────────

	private JPanel creerTableau()
	{
		modelFdr = new DefaultTableModel(COLS, 0)
		{
			@Override
			public boolean isCellEditable(int row, int col)
			{
				// Les lignes d'en-tête ACE ne sont pas éditables
				if (estLigneAce(row)) return false;
				return col == C_NBETIQ   || col == C_NBPARTS
					|| col == C_HETIQ    || col == C_HPARTS
					|| col == C_METHODE  || col == C_DISTRIB || col == C_COMMENT
					|| col == C_PRETRI   || col == C_SURPISTE || col == C_SORETIQ
					|| col == C_TRI      || col == C_FINI;
			}

			@Override
			public Class<?> getColumnClass(int col)
			{
				if (col >= C_PRETRI && col <= C_FINI) return Boolean.class;
				return String.class;
			}
		};

		tblFdr = IhmUtils.creerTable(modelFdr);
		tblFdr.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// Largeurs
		int[] w = {35, 75, 160, 90, 80, 70, 55, 55, 65, 70, 40, 40, 80, 80, 90, 70, 65, 65, 65, 80, 120};
		for (int i = 0; i < w.length && i < tblFdr.getColumnCount(); i++)
			tblFdr.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

		// Renderer phases (case à cocher colorée)
		TableCellRenderer phaseRenderer = new DefaultTableCellRenderer()
		{
			private final JCheckBox cb = new JCheckBox();
			{ cb.setHorizontalAlignment(JCheckBox.CENTER); }

			@Override
			public Component getTableCellRendererComponent(JTable t, Object v,
					boolean sel, boolean foc, int r, int c)
			{
				if (estLigneAce(r)) return rendererEnteteAce(t, v, sel, r, c);
				boolean val = Boolean.TRUE.equals(v);
				cb.setSelected(val);
				cb.setBackground(sel ? IhmUtils.SEL : val ? new Color(210, 240, 210) : Color.WHITE);
				return cb;
			}
		};
		for (int col = C_PRETRI; col <= C_FINI; col++)
			tblFdr.getColumnModel().getColumn(col).setCellRenderer(phaseRenderer);

		// Renderer avancement % coloré
		tblFdr.getColumnModel().getColumn(C_AVATIQ) .setCellRenderer(rendererPct());
		tblFdr.getColumnModel().getColumn(C_AVAPARTS).setCellRenderer(rendererPct());

		// Renderer générique qui colorie les lignes ACE
		TableCellRenderer rendererAce = creerRendererLigneAce();
		for (int col = 0; col < COLS.length; col++)
		{
			if (col >= C_PRETRI && col <= C_FINI) continue;
			if (col == C_AVATIQ || col == C_AVAPARTS) continue;
			tblFdr.getColumnModel().getColumn(col).setCellRenderer(rendererAce);
		}

		// Sauvegarde auto
		modelFdr.addTableModelListener(e ->
		{
			if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
			int row = e.getFirstRow(), col = e.getColumn();
			if (estLigneAce(row)) return;
			if (col == C_NBETIQ || col == C_NBPARTS || col == C_HETIQ || col == C_HPARTS)
				sauvegarderSuiviLigne(row);
			else if (col == C_METHODE || col == C_DISTRIB)
				sauvegarderMethodeDistribution(row, col);
			else if (col >= C_PRETRI && col <= C_FINI)
				sauvegarderPhase(row, col);
		});

		JScrollPane scroll = new JScrollPane(tblFdr,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));

		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.add(creerLegende(), BorderLayout.NORTH);
		p.add(scroll,         BorderLayout.CENTER);
		return p;
	}

	/** Renderer générique qui détecte les lignes d'en-tête ACE et les colorie. */
	private TableCellRenderer creerRendererLigneAce()
	{
		return new AceTableCellRenderer(this);
	}

	/** Renderer pour les colonnes booléennes sur une ligne ACE. */
	private Component rendererEnteteAce(JTable t, Object v, boolean sel, int r, int c)
	{
		JLabel l = new JLabel("");
		l.setOpaque(true);
		l.setBackground(couleurAcePourLigne(r));
		return l;
	}

	// ── Utilitaires lignes ACE ────────────────────────────────────────────

	boolean estLigneAce(int row)
	{
		if (row < 0 || row >= rowToLot.size()) return false;
		return rowToLot.get(row) == null;
	}

	/**
	 * Retourne la couleur ACE pour une ligne d'en-tête en parcourant
	 * vers le haut pour trouver quel ACE est concerné.
	 */
	Color couleurAcePourLigne(int row)
	{
		// Récupère le texte de la première colonne pour trouver l'index ACE
		String txt = modelFdr.getValueAt(row, 0) != null
			? modelFdr.getValueAt(row, 0).toString() : "";
		if (txt.startsWith(ACE_HEADER_MARKER))
		{
			try
			{
				int idx = Integer.parseInt(txt.substring(ACE_HEADER_MARKER.length()));
				return ACE_BG[idx % ACE_BG.length];
			}
			catch (Exception e) { /* ignore */ }
		}
		return new Color(80, 80, 80);
	}

	/** Cherche l'index d'un ACE dont le nom correspond à l'emplacement du lot. */
	private int trouverIndexAce(List<Ace> aces, String emplacement)
	{
		if (emplacement == null) return -1;
		for (int i = 0; i < aces.size(); i++)
			if (emplacement.equalsIgnoreCase(aces.get(i).getNom()))
				return i;
		return -1;
	}

	// ── Legende ───────────────────────────────────────────────────────────

	private JPanel creerLegende()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
		p.setBackground(new Color(248, 250, 252));
		p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, IhmUtils.BORD));
		for (String s : new String[]{
			"✏ Phases & colonnes blanches = éditables",
			"🟩 Avancement ≥ 80%", "🟨 50–79%", "🟥 < 50%",
			"  |  Ligne colorée = en-tête ACE"})
		{
			JLabel l = new JLabel(s);
			l.setFont(new Font("SansSerif", Font.PLAIN, 11));
			l.setForeground(Color.GRAY);
			p.add(l);
		}
		return p;
	}

	private TableCellRenderer rendererPct()
	{
		return new PctTableCellRenderer(this);
	}

	// ── Chargement données ────────────────────────────────────────────────

	public void remplirComboSocietes()
	{
		int sel = combSociete.getSelectedIndex();
		combSociete.removeAllItems();
		combSociete.addItem("— Choisir une société —");
		for (Societe s : ctrl.getSocietes())
			combSociete.addItem(s.getNom() + "  (" + s.getLots().size() + " lots)");
		if (sel >= 0 && sel < combSociete.getItemCount())
			combSociete.setSelectedIndex(sel);
	}

	public void rafraichir()
	{
		remplirComboSocietes();
		changerSociete();
	}

	private void changerSociete()
	{
		int idx = combSociete.getSelectedIndex() - 1;
		if (idx < 0 || idx >= ctrl.getSocietes().size())
		{
			societeCourante = null;
			viderRecap();
			modelFdr.setRowCount(0);
			rowToLot.clear();
			panelRecapAce.removeAll();
			panelRecapAce.revalidate();
			panelRecapAce.repaint();
			return;
		}
		societeCourante = ctrl.getSocietes().get(idx);
		chargerFicheRoute();
	}

	private void chargerFicheRoute()
	{
		if (societeCourante == null) return;

		FicheRoute fdr = ctrl.genererFicheRoute(societeCourante);

		// ── RECALCUL GLOBAL FIABLE (TOUS LES LOTS) ─────────────────────
		int totalVVS = 0;
		int totalPieces = 0;
		double totalPUSum = 0;
		int totalCptPu = 0;
		int totalHeures = societeCourante.getTotalHeuresCE();

		for (Lot l : societeCourante.getLots())
		{
			totalVVS += l.getValeurVente();
			totalPieces += l.getNbPieces();
			if (l.getNbPieces() > 0)
			{
				totalPUSum += l.getPrixUnitaire();
				totalCptPu++;
			}
		}

		double totalPU = totalCptPu > 0 ? totalPUSum / totalCptPu : 0.0;
		majTuile(lblNbLots, "Lots affectés", String.format("%,d", societeCourante.getLots().size()), IhmUtils.BLEU);
		majTuile(lblVVS,    "VVS Total",     String.format("%,d €", totalVVS), IhmUtils.VERT);
		majTuile(lblPieces, "Nb Pièces",     String.format("%,d", totalPieces), new Color(0, 80, 140));
		majTuile(lblPU,     "PU Moyen",      String.format("%.2f €", totalPU), IhmUtils.AMBER);
		majTuile(lblHeures, "H CE rest.",    String.format("%,d h", totalHeures), IhmUtils.ROUGE);

		// ── Construction tableau ──────────────────────────────────────────
		modelFdr.setRowCount(0);
		rowToLot.clear();

		Map<String, List<Lot>> lotParAce = new LinkedHashMap<>();
		for (Ace ace : societeCourante.getAces())
			lotParAce.put(ace.getNom(), new ArrayList<>(ace.getLots() != null ? ace.getLots() : new ArrayList<>()));

		int rowNum = 0;
		int aceIndex = 0;
		for (Map.Entry<String, List<Lot>> entry : lotParAce.entrySet())
		{
			String aceNom = entry.getKey();
			List<Lot> lots = entry.getValue();

			// En-tête ACE
			Object[] headerRow = new Object[COLS.length];
			headerRow[0] = ACE_HEADER_MARKER + aceIndex;
			modelFdr.addRow(headerRow);
			rowToLot.add(null);
			rowNum++;

			// Lots de cet ACE
			for (Lot l : lots)
			{
				Object[] row = new Object[COLS.length];
				int col = 0;
				row[col++] = l.getPriorite();
				row[col++] = l.getNumCDE();
				row[col++] = s(l.getTypologie());
				row[col++] = s(l.getTypologie());
				row[col++] = l.getValeurVente();
				row[col++] = l.getNbPieces();
				row[col++] = String.format("%.2f", l.getPrixUnitaire());
				row[col++] = false; // PRE_TRI
				row[col++] = false; // SUR_PISTE
				row[col++] = false; // SORTIE_ETIQ
				row[col++] = false; // TRI
				row[col++] = false; // FINI
				row[col++] = s(l.getMethode());
				row[col++] = s(l.getDistribution());
				row[col++] = s(l.getEmplacement());
				row[col++] = l.getSuivieProd().getNbPieceEtiq();
				row[col++] = l.getSuivieProd().getNbPieceRepart();
				row[col++] = l.getSuivieProd().getAvancementEtiqPct();
				row[col++] = l.getSuivieProd().getAvancementPartsPct();
				row[col++] = String.valueOf(l.getSuivieProd().getNbHeureEtiqRestant());
				row[col++] = String.valueOf(l.getSuivieProd().getNbHeureRepartRestant());
				row[col++] = s(l.getCommentaire());

				modelFdr.addRow(row);
				rowToLot.add(l);
				rowNum++;
			}

			aceIndex++;
		}

		reconstruireRecapAce(fdr);
	}

	private void viderRecap()
	{
		majTuile(lblNbLots, "Lots affectés", "—", IhmUtils.BLEU);
		majTuile(lblVVS,    "VVS Total",     "—", IhmUtils.VERT);
		majTuile(lblPieces, "Nb Pièces",     "—", new Color(0, 80, 140));
		majTuile(lblPU,     "PU Moyen",      "—", IhmUtils.AMBER);
		majTuile(lblHeures, "H CE restantes","—", IhmUtils.ROUGE);
	}

	private void sauvegarderSuiviLigne(int row)
	{
		// À implémenter : sauvegarder les heures et pièces
	}

	private void sauvegarderMethodeDistribution(int row, int col)
	{
		// À implémenter : sauvegarder la méthode ou la distribution
	}

	private void sauvegarderPhase(int row, int col)
	{
		// À implémenter : marquer une phase comme complétée
	}

	private void exporterTexte()
	{
		// À implémenter : exporter en texte
	}

	private void marquerTermine()
	{
		// À implémenter : marquer la fiche de route comme terminée
	}

	private static String s(String v) { return v != null ? v : ""; }
}

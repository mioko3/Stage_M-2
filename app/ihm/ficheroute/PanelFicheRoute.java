package app.ihm.ficheroute;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.ficheroute.FicheRoute;
import app.metier.ficheroute.Phase;
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
		return new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v,
					boolean sel, boolean foc, int r, int c)
			{
				super.getTableCellRendererComponent(t, v, sel, foc, r, c);
				if (estLigneAce(r))
				{
					Color bg = couleurAcePourLigne(r);
					setBackground(bg);
					setForeground(Color.WHITE);
					setFont(getFont().deriveFont(Font.BOLD, 12f));
					setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
				}
				else
				{
					if (!sel) setBackground(Color.WHITE);
					setForeground(Color.BLACK);
					setFont(getFont().deriveFont(Font.PLAIN));
				}
				return this;
			}
		};
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

	private boolean estLigneAce(int row)
	{
		if (row < 0 || row >= rowToLot.size()) return false;
		return rowToLot.get(row) == null;
	}

	/**
	 * Retourne la couleur ACE pour une ligne d'en-tête en parcourant
	 * vers le haut pour trouver quel ACE est concerné.
	 */
	private Color couleurAcePourLigne(int row)
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
		return new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v,
					boolean sel, boolean foc, int r, int c)
			{
				if (estLigneAce(r))
				{
					setBackground(couleurAcePourLigne(r));
					setText("");
					return this;
				}
				super.getTableCellRendererComponent(t, v, sel, foc, r, c);
				setHorizontalAlignment(CENTER);
				try
				{
					double pct = Double.parseDouble(v.toString().replace("%","").trim());
					if (!sel) setBackground(pct >= 80 ? new Color(210,240,210)
						: pct >= 50 ? new Color(255,250,200) : new Color(255,220,220));
					setForeground(pct >= 80 ? IhmUtils.VERT : pct >= 50 ? IhmUtils.AMBER : IhmUtils.ROUGE);
					setFont(getFont().deriveFont(Font.BOLD));
				}
				catch (Exception ex) { setBackground(Color.WHITE); setForeground(Color.BLACK); }
				return this;
			}
		};
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
		double totalPU = 0;
		int countPU = 0;

		for (Lot lot : societeCourante.getLots())
		{
			totalVVS += lot.getValeurVente();
			totalPieces += lot.getNbPieces();

			if (lot.getNbPieces() > 0)
			{
				totalPU += lot.getPrixUnitaire();
				countPU++;
			}
		}

		double puMoy = countPU > 0 ? totalPU / countPU : 0;

		// ── Récap globaux CORRIGÉS ────────────────────────────────────
		majTuile(lblNbLots, "Lots affectés",
			String.valueOf(societeCourante.getLots().size()), IhmUtils.BLEU);

		majTuile(lblVVS, "VVS Total",
			totalVVS > 0 ? String.format("%,d €", totalVVS) : "—", IhmUtils.VERT);

		majTuile(lblPieces, "Nb Pièces",
			String.format("%,d", totalPieces), new Color(0,80,140));

		majTuile(lblPU, "PU Moyen",
			puMoy > 0 ? String.format("%.2f €", puMoy) : "—", IhmUtils.AMBER);

		majTuile(lblHeures, "H CE restantes",
			societeCourante.getTotalHeuresCE() + "h", IhmUtils.ROUGE);

		// ── Récap par ACE ─────────────────────────────────────────────
		reconstruireRecapAce(fdr);

		// ── Tableau ──────────────────────────────────────────────────
		modelFdr.setRowCount(0);
		rowToLot.clear();

		List<Ace> aces = societeCourante.getAces();

		if (aces == null || aces.isEmpty())
		{
			for (Lot lot : societeCourante.getLots())
			{
				modelFdr.addRow(creerLigneLot(lot));
				rowToLot.add(lot);
			}
			return;
		}

		// 🔥 REGROUPEMENT PROPRE VIA ACE.getLots()
		Map<Ace, List<Lot>> lotsParAce = new LinkedHashMap<>();
		List<Lot> lotsSansAce = new ArrayList<>();

		for (Ace ace : aces)
		{
			List<Lot> lots = ace.getLots();
			lotsParAce.put(ace, lots != null ? lots : new ArrayList<>());
		}

		Set<Lot> lotsDansAce = new HashSet<>();
		for (Ace ace : aces)
		{
			if (ace.getLots() != null)
				lotsDansAce.addAll(ace.getLots());
		}

		for (Lot lot : societeCourante.getLots())
		{
			if (!lotsDansAce.contains(lot))
				lotsSansAce.add(lot);
		}

		// ── Affichage ────────────────────────────────────────────────
		for (int i = 0; i < aces.size(); i++)
		{
			Ace ace = aces.get(i);
			List<Lot> lotsAce = lotsParAce.get(ace);

			ajouterLigneEnteteAce(ace, i, lotsAce);

			for (Lot lot : lotsAce)
			{
				modelFdr.addRow(creerLigneLot(lot));
				rowToLot.add(lot);
			}
		}

		// Lots sans ACE
		if (!lotsSansAce.isEmpty())
		{
			Object[] ligne = new Object[COLS.length];
			ligne[0] = ACE_HEADER_MARKER + "99";
			ligne[1] = "▶ Sans ACE (" + lotsSansAce.size() + " lot(s))";

			for (int k = 2; k < COLS.length; k++) ligne[k] = "";

			modelFdr.addRow(ligne);
			rowToLot.add(null);

			for (Lot lot : lotsSansAce)
			{
				modelFdr.addRow(creerLigneLot(lot));
				rowToLot.add(lot);
			}
		}
	}

	/**
	 * Insère une ligne colorée d'en-tête pour un ACE avec ses totaux inline.
	 */
	private void ajouterLigneEnteteAce(Ace ace, int idxAce, List<Lot> lots)
	{
		int    vvs    = 0;
		int    pieces = 0;
		double puSum  = 0;
		int    cptPu  = 0;

		for (Lot l : lots)
		{
			vvs    += l.getValeurVente();
			pieces += l.getNbPieces();
			if (l.getNbPieces() > 0) { puSum += l.getPrixUnitaire(); cptPu++; }
		}

		double puMoy = cptPu > 0 ? puSum / cptPu : 0;

		Object[] ligne = new Object[COLS.length];
		ligne[0] = ACE_HEADER_MARKER + idxAce; // marqueur + index pour la couleur
		ligne[1] = "▶  " + ace.getNom();
		for (int k = 2; k < COLS.length; k++) ligne[k] = "";

		modelFdr.addRow(ligne);
		rowToLot.add(null); // null = ligne ACE
	}

	/** Construit le tableau de valeurs d'une ligne de lot. */
	private Object[] creerLigneLot(Lot lot)
	{
		int hEtiq  = lot.getSuivieProd().getNbHeureEtiqRestant();
		int hParts = lot.getSuivieProd().getNbHeureRepartRestant();

		return new Object[]{
			lot.getPriorite(),
			lot.getNumCDE(),
			s(lot.getAffaire()),
			s(lot.getTypologie()),
			lot.getValeurVente() > 0 ? String.format("%,d", lot.getValeurVente()) : "—",
			String.format("%,d", lot.getNbPieces()),
			lot.getValeurVente() > 0 && lot.getNbPieces() > 0
				? String.format("%.2f", (double)lot.getValeurVente()/lot.getNbPieces()) : "—",
			lot.getPhase().isPreTri(),
			lot.getPhase().isSurPiste(),
			lot.getPhase().isSortieEtiq(),
			lot.getPhase().isTri(),
			lot.getPhase().isFinit(),
			s(lot.getTypologie()),
			s(lot.getLotACharge()),
			lot.getEmplacement(),
			String.valueOf(lot.getSuivieProd().getNbPieceEtiq()),
			String.valueOf(lot.getSuivieProd().getNbPieceRepart()),
			lot.getSuivieProd().getAvancementEtiqPct(),
			lot.getSuivieProd().getAvancementPartsPct(),
			String.valueOf(hEtiq),
			String.valueOf(hParts),
			s(lot.getCommentaire())
		};
	}

	private void viderRecap()
	{
		majTuile(lblNbLots, "Lots affectés",  "—", IhmUtils.BLEU);
		majTuile(lblVVS,    "VVS Total",      "—", IhmUtils.VERT);
		majTuile(lblPieces, "Nb Pièces",      "—", new Color(0,80,140));
		majTuile(lblPU,     "PU Moyen",       "—", IhmUtils.AMBER);
		majTuile(lblHeures, "H CE restantes", "—", IhmUtils.ROUGE);
	}

	// ── Mise à jour suivi prod depuis le tableau ──────────────────────────

	private void sauvegarderSuiviLigne(int row)
	{
		if (societeCourante == null) return;
		if (row < 0 || row >= rowToLot.size()) return;
		Lot lot = rowToLot.get(row);
		if (lot == null) return;
		try
		{
			int nbE = parseInt(modelFdr.getValueAt(row, C_NBETIQ));
			int nbP = parseInt(modelFdr.getValueAt(row, C_NBPARTS));
			ctrl.mettreAJourSuiviProd(lot, nbE, nbP);
			modelFdr.setValueAt(lot.getSuivieProd().getAvancementEtiqPct(),  row, C_AVATIQ);
			modelFdr.setValueAt(lot.getSuivieProd().getAvancementPartsPct(), row, C_AVAPARTS);
			chargerFicheRoute();
		}
		catch (Exception ignored) {}
	}

	private void sauvegarderMethodeDistribution(int row, int col)
	{
		if (societeCourante == null) return;
		if (row < 0 || row >= rowToLot.size()) return;
		Lot lot = rowToLot.get(row);
		if (lot == null) return;
		try
		{
			String valeur = modelFdr.getValueAt(row, col) != null
				? modelFdr.getValueAt(row, col).toString().trim() : "";
			if (col == C_METHODE)
				ctrl.modifierLotMethodeDistribution(lot, valeur, lot.getLotACharge());
			else if (col == C_DISTRIB)
				ctrl.modifierLotMethodeDistribution(lot, lot.getTypologie(), valeur);
			chargerFicheRoute();
		}
		catch (Exception ignored) {}
	}

	private void sauvegarderPhase(int row, int col)
	{
		if (societeCourante == null) return;
		if (row < 0 || row >= rowToLot.size()) return;
		Lot lot = rowToLot.get(row);
		if (lot == null) return;
		try
		{
			boolean valeur = Boolean.TRUE.equals(modelFdr.getValueAt(row, col));
			boolean preTri    = lot.getPhase().isPreTri();
			boolean surPiste  = lot.getPhase().isSurPiste();
			boolean sortieEtiq= lot.getPhase().isSortieEtiq();
			boolean tri       = lot.getPhase().isTri();
			boolean finit     = lot.getPhase().isFinit();
			switch (col)
			{
				case C_PRETRI:   preTri     = valeur; break;
				case C_SURPISTE: surPiste   = valeur; break;
				case C_SORETIQ:  sortieEtiq = valeur; break;
				case C_TRI:      tri        = valeur; break;
				case C_FINI:     finit      = valeur; break;
			}
			ctrl.modifierPhase(lot, preTri, surPiste, sortieEtiq, tri, finit);
			chargerFicheRoute();
		}
		catch (Exception ignored) {}
	}

	private int parseInt(Object v)
	{ try { return Integer.parseInt(v != null ? v.toString().trim() : "0"); } catch (Exception e) { return 0; } }

	// ── Export texte ──────────────────────────────────────────────────────

	private void exporterTexte()
	{
		if (societeCourante == null)
		{
			JOptionPane.showMessageDialog(this, "Sélectionnez d'abord une société.",
				"Export", JOptionPane.WARNING_MESSAGE);
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("FICHE DE ROUTE — ").append(societeCourante.getNom()).append("\n");
		sb.append("=".repeat(90)).append("\n");

		List<Ace> aces = societeCourante.getAces();
		FicheRoute fdr = ctrl.genererFicheRoute(societeCourante);

		if (aces == null || aces.isEmpty())
		{
			// Export classique
			appendEnteteExport(sb);
			for (Lot lot : societeCourante.getLots())
				appendLigneLotExport(sb, lot);
		}
		else
		{
			// Export regroupé par ACE
			Map<String, List<Lot>> lotsParAce = new LinkedHashMap<>();
			for (Ace ace : aces)
				lotsParAce.put(ace.getNom().toLowerCase(), new ArrayList<>());
			List<Lot> lotsSansAce = new ArrayList<>();
			for (Lot lot : societeCourante.getLots())
			{
				String emp = lot.getEmplacement() != null ? lot.getEmplacement().toLowerCase() : "";
				if (lotsParAce.containsKey(emp)) lotsParAce.get(emp).add(lot);
				else lotsSansAce.add(lot);
			}

			for (Ace ace : aces)
			{
				List<Lot> lotsAce = lotsParAce.get(ace.getNom().toLowerCase());
				sb.append("\n── ").append(ace.getNom())
				  .append(" (").append(lotsAce.size()).append(" lot(s)) ──\n");
				appendEnteteExport(sb);
				for (Lot lot : lotsAce)
					appendLigneLotExport(sb, lot);
				sb.append("-".repeat(90)).append("\n");
			}

			if (!lotsSansAce.isEmpty())
			{
				sb.append("\n── Sans ACE ──\n");
				appendEnteteExport(sb);
				for (Lot lot : lotsSansAce)
					appendLigneLotExport(sb, lot);
				sb.append("-".repeat(90)).append("\n");
			}
		}

		sb.append("\n").append("=".repeat(90)).append("\n");
		sb.append(String.format("TOTAL : VVS=%,d €  |  Pièces=%,d  |  H CE dispo=%dh\n",
			fdr.getSommeVVS(), fdr.getSommePieces(), societeCourante.getTotalHeuresCE()));

		JTextArea ta = new JTextArea(sb.toString(), 28, 90);
		ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
		ta.setEditable(false);
		JOptionPane.showMessageDialog(this, new JScrollPane(ta),
			"Fiche de Route — " + societeCourante.getNom(), JOptionPane.INFORMATION_MESSAGE);
	}

	private void appendEnteteExport(StringBuilder sb)
	{
		sb.append(String.format("%-8s %-40s %-12s %-10s %-8s %-10s\n",
			"N° CDE","Désignation","Nb Pièces","VVS (€)","Heures","Av.Étiq %"));
		sb.append("-".repeat(90)).append("\n");
	}

	private void appendLigneLotExport(StringBuilder sb, Lot lot)
	{
		double totH = lot.getHeures();
		int hE = lot.getSuivieProd().getNbHeureEtiqRestant();
		double av = totH > 0 ? Math.max(0, 100.0 - hE/totH*100) : 0;
		sb.append(String.format("%-8d %-40s %-12s %-10s %-8.1f %-10.1f%%\n",
			lot.getNumCDE(),
			(s(lot.getAffaire())+" "+s(lot.getTypologie())).trim(),
			String.format("%,d", lot.getNbPieces()),
			lot.getValeurVente()>0 ? String.format("%,d",lot.getValeurVente()) : "—",
			totH, av));
	}

	// ── Rafraîchissement ──────────────────────────────────────────────────

	public void rafraichir()
	{
		remplirComboSocietes();
		if (societeCourante != null) chargerFicheRoute();
	}

	private String s(String v) { return v != null ? v : ""; }

	// ── Marquer lot comme terminé ──────────────────────────────────────────

	private void marquerTermine()
	{
		int selectedRow = tblFdr.getSelectedRow();
		if (selectedRow < 0)
		{
			JOptionPane.showMessageDialog(this, "Sélectionnez un lot dans le tableau.",
				"Marquer terminé", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (estLigneAce(selectedRow))
		{
			JOptionPane.showMessageDialog(this,
				"Sélectionnez un lot (pas un en-tête ACE).",
				"Marquer terminé", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (societeCourante == null) return;
		Lot lot = rowToLot.get(selectedRow);
		if (lot == null) return;

		int confirm = JOptionPane.showConfirmDialog(this,
			"Marquer le lot \"" + lot.getNumCDE() + "\" comme terminé en production ?",
			"Confirmer", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION)
		{
			ctrl.marquerLotTermine(lot);
			chargerFicheRoute();
			JOptionPane.showMessageDialog(this,
				"Le lot a été marqué comme terminé.",
				"Terminé", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
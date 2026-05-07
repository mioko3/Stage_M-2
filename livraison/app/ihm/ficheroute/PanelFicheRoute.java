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
 * Panel fusionné "Fiches de Route" avec deux sous-onglets :
 *  - "Par Société" (ex-PanelFicheRoute)
 *  - "Par ACE"     (ex-PanelFicheAce)
 *
 * L'ACE courante est mémorisée par son nom pour survivre aux rafraîchissements.
 */
public class PanelFicheRoute extends JPanel
{
	private final Controleur        ctrl;
	private final FenetrePrincipale fenetre;

	// ── Colonnes communes ─────────────────────────────────────────────────
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

	private static final String ACE_HEADER_MARKER = "__ACE_HEADER__";

	private static final Color[] ACE_BG = {
		new Color(13, 71, 161),
		new Color(27, 94, 32),
		new Color(130, 0, 0),
		new Color(100, 50, 0),
	};

	// ════════════════════════════════════════════════════════════════════
	// Sous-onglet "Par Société"
	// ════════════════════════════════════════════════════════════════════

	private JComboBox<String>   combSociete;
	private JLabel              lblVVS_s, lblPieces_s, lblPU_s, lblHeures_s, lblNbLots_s;
	private JPanel              panelRecapAce_s;
	private DefaultTableModel   modelSoc;
	private JTable              tblSoc;
	private final List<Lot>     rowToLot_s = new ArrayList<>();
	private Societe             societeCourante;

	// ════════════════════════════════════════════════════════════════════
	// Sous-onglet "Par ACE"
	// ════════════════════════════════════════════════════════════════════

	private JComboBox<String>   combAce;
	private JLabel              lblVVS_a, lblPieces_a, lblPU_a, lblHeures_a, lblNbLots_a;
	private DefaultTableModel   modelAce;
	private JTable              tblAce;
	private final List<Lot>     rowToLot_a = new ArrayList<>();
	private Ace                 aceCourante;
	/** Nom de l'ACE mémorisé pour survivre au rafraîchissement du combo */
	private String              nomAceMemorise = null;

	// ════════════════════════════════════════════════════════════════════
	// Construction
	// ════════════════════════════════════════════════════════════════════

	public PanelFicheRoute(Controleur ctrl, FenetrePrincipale fenetre)
	{
		this.ctrl    = ctrl;
		this.fenetre = fenetre;
		setLayout(new BorderLayout());
		setBackground(IhmUtils.FOND);

		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
		tabs.addTab("🏢 Par Société", creerPanelSociete());
		tabs.addTab("👤 Par ACE",     creerPanelAce());
		add(tabs, BorderLayout.CENTER);
	}

	// ════════════════════════════════════════════════════════════════════
	// ── ONGLET "PAR SOCIÉTÉ" ────────────────────────────────────────────
	// ════════════════════════════════════════════════════════════════════

	private JPanel creerPanelSociete()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setBackground(IhmUtils.FOND);
		p.add(creerHautSociete(),    BorderLayout.NORTH);
		p.add(creerTableauSociete(), BorderLayout.CENTER);
		return p;
	}

	private JPanel creerHautSociete()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBackground(IhmUtils.FOND);

		JPanel ligneSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		ligneSelect.setBackground(IhmUtils.FOND);

		JLabel lbl = new JLabel("Société : ");
		lbl.setFont(new Font("SansSerif", Font.BOLD, 13));

		combSociete = new JComboBox<>();
		combSociete.setFont(new Font("SansSerif", Font.PLAIN, 13));
		combSociete.setPreferredSize(new Dimension(280, 28));
		combSociete.addActionListener(e -> changerSociete());

		JButton btnExport   = IhmUtils.bouton("🖨 Aperçu / Export", IhmUtils.BLEU, Color.WHITE);
		btnExport.addActionListener(e -> exporterTexte());
		JButton btnTerminer = IhmUtils.bouton("✓ Marquer terminé", IhmUtils.VERT, Color.WHITE);
		btnTerminer.addActionListener(e -> marquerTermine());
		JButton btnMeth = IhmUtils.bouton("👁 Voir la Méthode", IhmUtils.VERT, Color.WHITE);
		btnMeth.addActionListener(e -> ouvrirMeth());

		ligneSelect.add(lbl);
		ligneSelect.add(combSociete);
		ligneSelect.add(btnExport);
		ligneSelect.add(btnTerminer);
		ligneSelect.add(btnMeth);

		JPanel tuiles = new JPanel(new GridLayout(1, 5, 6, 0));
		tuiles.setBackground(IhmUtils.FOND);
		tuiles.setPreferredSize(new Dimension(0, 64));

		lblNbLots_s = creerTuile("Lots affectés", "—", IhmUtils.BLEU);
		lblVVS_s    = creerTuile("VVS Total",     "—", IhmUtils.VERT);
		lblPieces_s = creerTuile("Nb Pièces",     "—", new Color(0, 80, 140));
		lblPU_s     = creerTuile("PU Moyen",      "—", IhmUtils.AMBER);
		lblHeures_s = creerTuile("H CE restantes","—", IhmUtils.ROUGE);

		tuiles.add(lblNbLots_s);
		tuiles.add(lblVVS_s);
		tuiles.add(lblPieces_s);
		tuiles.add(lblPU_s);
		tuiles.add(lblHeures_s);

		panelRecapAce_s = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		panelRecapAce_s.setBackground(IhmUtils.FOND);

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
		nord.add(panelRecapAce_s);

		p.add(nord, BorderLayout.NORTH);
		return p;
	}

	private JPanel creerTableauSociete()
	{
		modelSoc = new DefaultTableModel(COLS, 0)
		{
			@Override public boolean isCellEditable(int row, int col)
			{
				if (estLigneAce_s(row)) return false;
				return col == C_NBETIQ || col == C_NBPARTS
					|| col == C_HETIQ  || col == C_HPARTS
					|| col == C_METHODE || col == C_DISTRIB || col == C_COMMENT
					|| col == C_PRETRI || col == C_SURPISTE || col == C_SORETIQ
					|| col == C_TRI    || col == C_FINI;
			}
			@Override public Class<?> getColumnClass(int col)
			{
				if (col >= C_PRETRI && col <= C_FINI) return Boolean.class;
				return String.class;
			}
		};

		tblSoc = IhmUtils.creerTable(modelSoc);
		tblSoc.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		int[] w = {35,75,160,90,80,70,55,55,65,70,40,40,80,80,90,70,65,65,65,80,80,120};
		for (int i = 0; i < w.length && i < tblSoc.getColumnCount(); i++)
			tblSoc.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

		TableCellRenderer phaseRenderer = new DefaultTableCellRenderer()
		{
			private final JCheckBox cb = new JCheckBox();
			{ cb.setHorizontalAlignment(JCheckBox.CENTER); }
			@Override public Component getTableCellRendererComponent(
					JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				if (estLigneAce_s(r)) return rendererEnteteAce_s(r);
				boolean val = Boolean.TRUE.equals(v);
				cb.setSelected(val);
				cb.setBackground(sel ? IhmUtils.SEL : val ? new Color(210,240,210) : Color.WHITE);
				return cb;
			}
		};
		for (int col = C_PRETRI; col <= C_FINI; col++)
			tblSoc.getColumnModel().getColumn(col).setCellRenderer(phaseRenderer);

		tblSoc.getColumnModel().getColumn(C_AVATIQ) .setCellRenderer(rendererPct_s());
		tblSoc.getColumnModel().getColumn(C_AVAPARTS).setCellRenderer(rendererPct_s());

		TableCellRenderer rendererAce = creerRendererLigneAce_s();
		for (int col = 0; col < COLS.length; col++)
		{
			if (col >= C_PRETRI && col <= C_FINI) continue;
			if (col == C_AVATIQ || col == C_AVAPARTS) continue;
			tblSoc.getColumnModel().getColumn(col).setCellRenderer(rendererAce);
		}

		modelSoc.addTableModelListener(e ->
		{
			if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
			int row = e.getFirstRow(), col = e.getColumn();
			if (estLigneAce_s(row)) return;
			if (col == C_NBETIQ || col == C_NBPARTS || col == C_HETIQ || col == C_HPARTS)
				sauvegarderSuivi_s(row);
			else if (col == C_METHODE || col == C_DISTRIB)
				sauvegarderMethodeDistrib_s(row, col);
			else if (col >= C_PRETRI && col <= C_FINI)
				sauvegarderPhase_s(row, col);
			else if (col == C_COMMENT)
				sauvegarderCommentaire_s(row);
		});

		JScrollPane scroll = new JScrollPane(tblSoc,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));

		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.add(creerLegende(), BorderLayout.NORTH);
		p.add(scroll,         BorderLayout.CENTER);
		return p;
	}

	private boolean estLigneAce_s(int row)
	{
		if (row < 0 || row >= rowToLot_s.size()) return false;
		return rowToLot_s.get(row) == null;
	}

	private Color couleurAcePourLigne_s(int row)
	{
		String txt = modelSoc.getValueAt(row, 0) != null
			? modelSoc.getValueAt(row, 0).toString() : "";
		if (txt.startsWith(ACE_HEADER_MARKER))
		{
			try { int idx = Integer.parseInt(txt.substring(ACE_HEADER_MARKER.length()));
				  return ACE_BG[idx % ACE_BG.length]; }
			catch (Exception e) { /* ignore */ }
		}
		return new Color(80, 80, 80);
	}

	private Component rendererEnteteAce_s(int r)
	{
		JLabel l = new JLabel(""); l.setOpaque(true); l.setBackground(couleurAcePourLigne_s(r)); return l;
	}

	private TableCellRenderer creerRendererLigneAce_s()
	{
		return new DefaultTableCellRenderer()
		{
			@Override public Component getTableCellRendererComponent(
					JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				super.getTableCellRendererComponent(t, v, sel, foc, r, c);
				if (estLigneAce_s(r))
				{
					setBackground(couleurAcePourLigne_s(r));
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

	private TableCellRenderer rendererPct_s()
	{
		return new DefaultTableCellRenderer()
		{
			@Override public Component getTableCellRendererComponent(
					JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				if (estLigneAce_s(r)) { setBackground(couleurAcePourLigne_s(r)); setText(""); return this; }
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
			viderRecap_s();
			modelSoc.setRowCount(0);
			rowToLot_s.clear();
			panelRecapAce_s.removeAll();
			panelRecapAce_s.revalidate();
			panelRecapAce_s.repaint();
			return;
		}
		societeCourante = ctrl.getSocietes().get(idx);
		chargerFicheRouteSociete();
	}

	private void chargerFicheRouteSociete()
	{
		if (societeCourante == null) return;

		FicheRoute fdr = ctrl.genererFicheRoute(societeCourante);

		int totalVVS = 0, totalPieces = 0, countPU = 0;
		double totalPU = 0;
		for (Lot lot : societeCourante.getLots())
		{
			totalVVS    += lot.getValeurVente();
			totalPieces += lot.getNbPieces();
			if (lot.getNbPieces() > 0) { totalPU += lot.getPrixUnitaire(); countPU++; }
		}
		double puMoy = countPU > 0 ? totalPU / countPU : 0;

		majTuile(lblNbLots_s, "Lots affectés", String.valueOf(societeCourante.getLots().size()), IhmUtils.BLEU);
		majTuile(lblVVS_s,    "VVS Total",     totalVVS > 0 ? String.format("%,d €", totalVVS) : "—", IhmUtils.VERT);
		majTuile(lblPieces_s, "Nb Pièces",     String.format("%,d", totalPieces), new Color(0,80,140));
		majTuile(lblPU_s,     "PU Moyen",      puMoy > 0 ? String.format("%.2f €", puMoy) : "—", IhmUtils.AMBER);
		majTuile(lblHeures_s, "H CE restantes",societeCourante.getTotalHeuresCE() + "h", IhmUtils.ROUGE);

		reconstruireRecapAce_s(fdr);

		modelSoc.setRowCount(0);
		rowToLot_s.clear();

		List<Ace> aces = societeCourante.getAces();
		if (aces == null || aces.isEmpty())
		{
			for (Lot lot : societeCourante.getLots()) { modelSoc.addRow(creerLigneLot(lot)); rowToLot_s.add(lot); }
			return;
		}

		Map<Ace, List<Lot>> lotsParAce = new LinkedHashMap<>();
		for (Ace ace : aces)
			lotsParAce.put(ace, ace.getLots() != null ? ace.getLots() : new ArrayList<>());

		Set<Lot> lotsDansAce = new HashSet<>();
		for (Ace ace : aces) if (ace.getLots() != null) lotsDansAce.addAll(ace.getLots());

		List<Lot> lotsSansAce = new ArrayList<>();
		for (Lot lot : societeCourante.getLots()) if (!lotsDansAce.contains(lot)) lotsSansAce.add(lot);

		for (int i = 0; i < aces.size(); i++)
		{
			Ace ace = aces.get(i);
			ajouterEnteteAce_s(ace, i);
			for (Lot lot : lotsParAce.get(ace)) { modelSoc.addRow(creerLigneLot(lot)); rowToLot_s.add(lot); }
		}

		if (!lotsSansAce.isEmpty())
		{
			Object[] ligne = new Object[COLS.length];
			ligne[0] = ACE_HEADER_MARKER + "99"; ligne[1] = "▶ Sans ACE (" + lotsSansAce.size() + " lot(s))";
			for (int k = 2; k < COLS.length; k++) ligne[k] = "";
			modelSoc.addRow(ligne); rowToLot_s.add(null);
			for (Lot lot : lotsSansAce) { modelSoc.addRow(creerLigneLot(lot)); rowToLot_s.add(lot); }
		}
	}

	private void ajouterEnteteAce_s(Ace ace, int idxAce)
	{
		Object[] ligne = new Object[COLS.length];
		ligne[0] = ACE_HEADER_MARKER + idxAce; ligne[1] = "▶  " + ace.getNom();
		for (int k = 2; k < COLS.length; k++) ligne[k] = "";
		modelSoc.addRow(ligne); rowToLot_s.add(null);
	}

	private void reconstruireRecapAce_s(FicheRoute fdr)
	{
		panelRecapAce_s.removeAll();
		if (societeCourante == null || societeCourante.getAces() == null || societeCourante.getAces().isEmpty())
		{ panelRecapAce_s.revalidate(); panelRecapAce_s.repaint(); return; }

		List<Ace> aces = societeCourante.getAces();
		for (int i = 0; i < aces.size(); i++)
		{
			Ace ace = aces.get(i);
			List<Lot> lots = ace.getLots() != null ? ace.getLots() : new ArrayList<>();
			int vvs = 0, pieces = 0, cptPu = 0, nbPieceEtiq = 0; double puSum = 0;
			for (Lot l : lots)
			{
				vvs += l.getValeurVente(); pieces += l.getNbPieces();
				if (l.getNbPieces() > 0) { puSum += l.getPrixUnitaire(); cptPu++; }
				if (l.getNbPieces() > 0 && l.getSuivieProd() != null) nbPieceEtiq += l.getSuivieProd().getNbPieceEtiq();
			}
			double puMoy  = cptPu > 0 ? puSum / cptPu : 0;
			double pAvanc = pieces > 0 ? 100.0 * nbPieceEtiq / pieces : 0;
			Color couleur = ACE_BG[i % ACE_BG.length];

			JPanel groupe = new JPanel(new BorderLayout(0, 2));
			groupe.setBackground(IhmUtils.FOND);
			groupe.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(couleur, 2),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));

			JLabel titreLbl = new JLabel(" " + ace.getNom(), JLabel.LEFT);
			titreLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
			titreLbl.setForeground(Color.WHITE); titreLbl.setOpaque(true); titreLbl.setBackground(couleur);
			titreLbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

			JPanel tuiles3 = new JPanel(new GridLayout(1, 4, 4, 0));
			tuiles3.setBackground(IhmUtils.FOND);
			tuiles3.add(creerTuileMini("VVS",     vvs > 0 ? String.format("%,d €", vvs) : "—", couleur));
			tuiles3.add(creerTuileMini("Pièces",  String.format("%,d", pieces), couleur));
			tuiles3.add(creerTuileMini("PU Moy.", puMoy > 0 ? String.format("%.2f €", puMoy) : "—", couleur));
			tuiles3.add(creerTuileMini("Av.",     pAvanc > 0 ? String.format("%.1f %%", pAvanc) : "—", couleur));

			groupe.add(titreLbl, BorderLayout.NORTH);
			groupe.add(tuiles3,  BorderLayout.CENTER);
			panelRecapAce_s.add(groupe);
		}
		panelRecapAce_s.revalidate(); panelRecapAce_s.repaint();
	}

	private void sauvegarderSuivi_s(int row)
	{
		if (societeCourante == null || row < 0 || row >= rowToLot_s.size()) return;
		Lot lot = rowToLot_s.get(row); if (lot == null) return;
		try
		{
			ctrl.mettreAJourSuiviProd(lot, parseInt(modelSoc.getValueAt(row, C_NBETIQ)), parseInt(modelSoc.getValueAt(row, C_NBPARTS)));
			modelSoc.setValueAt(lot.getSuivieProd().getAvancementEtiqPct(),  row, C_AVATIQ);
			modelSoc.setValueAt(lot.getSuivieProd().getAvancementPartsPct(), row, C_AVAPARTS);
			chargerFicheRouteSociete();
		} catch (Exception ignored) {}
	}

	private void sauvegarderMethodeDistrib_s(int row, int col)
	{
		if (societeCourante == null || row < 0 || row >= rowToLot_s.size()) return;
		Lot lot = rowToLot_s.get(row); if (lot == null) return;

		try
		{
			String v = modelSoc.getValueAt(row, col) != null
				? modelSoc.getValueAt(row, col).toString().trim()
				: "";

			if (col == C_METHODE)
				ctrl.modifierLotMethodeDistribution(lot, v, lot.getLotACharge());
			else
				ctrl.modifierLotMethodeDistribution(lot, nomMethode(lot), v);

			chargerFicheRouteSociete();
		}
		catch (Exception ignored) {}
	}

	private void sauvegarderPhase_s(int row, int col)
	{
		if (societeCourante == null || row < 0 || row >= rowToLot_s.size()) return;
		Lot lot = rowToLot_s.get(row); if (lot == null) return;
		try
		{
			boolean val = Boolean.TRUE.equals(modelSoc.getValueAt(row, col));
			boolean preTri = lot.getPhase().isPreTri(), surPiste = lot.getPhase().isSurPiste(),
				sortieEtiq = lot.getPhase().isSortieEtiq(), tri = lot.getPhase().isTri(), finit = lot.getPhase().isFinit();
			switch (col) {
				case C_PRETRI:   preTri     = val; break; case C_SURPISTE: surPiste   = val; break;
				case C_SORETIQ:  sortieEtiq = val; break; case C_TRI:      tri        = val; break;
				case C_FINI:     finit      = val; break;
			}
			ctrl.modifierPhase(lot, preTri, surPiste, sortieEtiq, tri, finit);
			chargerFicheRouteSociete();
		} catch (Exception ignored) {}
	}

	private void sauvegarderCommentaire_s(int row)
	{
		if (societeCourante == null || row < 0 || row >= rowToLot_s.size()) return;
		Lot lot = rowToLot_s.get(row); if (lot == null) return;
		try { lot.setCommentaire(modelSoc.getValueAt(row, C_COMMENT) != null ? modelSoc.getValueAt(row, C_COMMENT).toString().trim() : ""); }
		catch (Exception ignored) {}
	}

	private void viderRecap_s()
	{
		majTuile(lblNbLots_s, "Lots affectés",  "—", IhmUtils.BLEU);
		majTuile(lblVVS_s,    "VVS Total",       "—", IhmUtils.VERT);
		majTuile(lblPieces_s, "Nb Pièces",       "—", new Color(0,80,140));
		majTuile(lblPU_s,     "PU Moyen",        "—", IhmUtils.AMBER);
		majTuile(lblHeures_s, "H CE restantes",  "—", IhmUtils.ROUGE);
	}

	private void marquerTermine()
	{
		int sel = tblSoc.getSelectedRow();
		if (sel < 0) { JOptionPane.showMessageDialog(this, "Sélectionnez un lot.", "Marquer terminé", JOptionPane.WARNING_MESSAGE); return; }
		if (estLigneAce_s(sel)) { JOptionPane.showMessageDialog(this, "Sélectionnez un lot (pas un en-tête ACE).", "Marquer terminé", JOptionPane.WARNING_MESSAGE); return; }
		if (societeCourante == null) return;
		Lot lot = rowToLot_s.get(sel); if (lot == null) return;
		if (JOptionPane.showConfirmDialog(this, "Marquer le lot \"" + lot.getNumCDE() + "\" comme terminé ?", "Confirmer", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			ctrl.marquerLotTermine(lot);
			chargerFicheRouteSociete();
			JOptionPane.showMessageDialog(this, "Le lot a été marqué comme terminé.", "Terminé", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void ouvrirMeth()
	{
		int sel = tblSoc.getSelectedRow();

		if (sel < 0)
		{
			JOptionPane.showMessageDialog(
				this,
				"Sélectionnez un lot.",
				"Voir Méthode",
				JOptionPane.WARNING_MESSAGE
			);
			return;
		}

		if (estLigneAce_s(sel))
		{
			JOptionPane.showMessageDialog(
				this,
				"Sélectionnez un lot (pas un en-tête ACE).",
				"Voir Méthode",
				JOptionPane.WARNING_MESSAGE
			);
			return;
		}

		if (societeCourante == null)
			return;

		Lot lot = rowToLot_s.get(sel);

		if (lot == null)
			return;

		// ✅ Vérification correcte
		if (lot.getMethode() == null)
		{
			JOptionPane.showMessageDialog(
				this,
				"Aucune méthode associée à ce lot.",
				"Méthode introuvable",
				JOptionPane.WARNING_MESSAGE
			);
			return;
		}

		// ✅ Ouvrir le PDF
		lot.getMethode().ouvrir();
	}

	private void exporterTexte()
	{
		if (societeCourante == null) { JOptionPane.showMessageDialog(this, "Sélectionnez d'abord une société.", "Export", JOptionPane.WARNING_MESSAGE); return; }
		StringBuilder sb = new StringBuilder();
		sb.append("FICHE DE ROUTE — ").append(societeCourante.getNom()).append("\n").append("=".repeat(90)).append("\n");
		FicheRoute fdr = ctrl.genererFicheRoute(societeCourante);
		List<Ace> aces = societeCourante.getAces();
		if (aces == null || aces.isEmpty()) { appendEnteteExport(sb); for (Lot lot : societeCourante.getLots()) appendLigneLotExport(sb, lot); }
		else
		{
			Map<String, List<Lot>> lotsParAce = new LinkedHashMap<>();
			for (Ace ace : aces) lotsParAce.put(ace.getNom().toLowerCase(), new ArrayList<>());
			List<Lot> sans = new ArrayList<>();
			for (Lot lot : societeCourante.getLots()) { String e = lot.getEmplacement()!=null?lot.getEmplacement().toLowerCase():""; if(lotsParAce.containsKey(e))lotsParAce.get(e).add(lot); else sans.add(lot); }
			for (Ace ace : aces) { List<Lot> l=lotsParAce.get(ace.getNom().toLowerCase()); sb.append("\n── ").append(ace.getNom()).append(" (").append(l.size()).append(" lot(s)) ──\n"); appendEnteteExport(sb); for(Lot lot:l)appendLigneLotExport(sb,lot); sb.append("-".repeat(90)).append("\n"); }
			if (!sans.isEmpty()) { sb.append("\n── Sans ACE ──\n"); appendEnteteExport(sb); for(Lot lot:sans)appendLigneLotExport(sb,lot); sb.append("-".repeat(90)).append("\n"); }
		}
		sb.append("\n").append("=".repeat(90)).append("\n").append(String.format("TOTAL : VVS=%,d €  |  Pièces=%,d  |  H CE dispo=%dh\n", fdr.getSommeVVS(), fdr.getSommePieces(), societeCourante.getTotalHeuresCE()));
		JTextArea ta = new JTextArea(sb.toString(), 28, 90); ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); ta.setEditable(false);
		JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Fiche de Route — " + societeCourante.getNom(), JOptionPane.INFORMATION_MESSAGE);
	}

	private void appendEnteteExport(StringBuilder sb)
	{
		sb.append(String.format("%-8s %-40s %-12s %-10s %-8s %-10s\n","N° CDE","Désignation","Nb Pièces","VVS (€)","Heures","Av.Étiq %"));
		sb.append("-".repeat(90)).append("\n");
	}

	private void appendLigneLotExport(StringBuilder sb, Lot lot)
	{
		double totH = lot.getHeures(); int hE = lot.getSuivieProd().getNbHeureEtiqRestant();
		double av = totH > 0 ? Math.max(0, 100.0 - hE/totH*100) : 0;
		sb.append(String.format("%-8d %-40s %-12s %-10s %-8.1f %-10.1f%%\n", lot.getNumCDE(), (s(lot.getAffaire())+" "+s(lot.getTypologie())).trim(), String.format("%,d", lot.getNbPieces()), lot.getValeurVente()>0?String.format("%,d",lot.getValeurVente()):"—", totH, av));
	}

	// ════════════════════════════════════════════════════════════════════
	// ── ONGLET "PAR ACE" ────────────────────────────────────────────────
	// ════════════════════════════════════════════════════════════════════

	private JPanel creerPanelAce()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setBackground(IhmUtils.FOND);
		p.add(creerHautAce(),    BorderLayout.NORTH);
		p.add(creerTableauAce(), BorderLayout.CENTER);
		return p;
	}

	private JPanel creerHautAce()
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(IhmUtils.FOND);

		JPanel ligneSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		ligneSelect.setBackground(IhmUtils.FOND);

		JLabel lbl = new JLabel("ACE : ");
		lbl.setFont(new Font("SansSerif", Font.BOLD, 13));

		combAce = new JComboBox<>();
		combAce.setPreferredSize(new Dimension(280, 28));
		combAce.addActionListener(e -> changerAce());

		ligneSelect.add(lbl); ligneSelect.add(combAce);

		JPanel tuiles = new JPanel(new GridLayout(1, 5, 6, 0));
		tuiles.setBackground(IhmUtils.FOND);
		tuiles.setPreferredSize(new Dimension(0, 64));

		lblNbLots_a = creerTuile("Lots",       "—", IhmUtils.BLEU);
		lblVVS_a    = creerTuile("VVS",         "—", IhmUtils.VERT);
		lblPieces_a = creerTuile("Pièces",      "—", new Color(0,80,140));
		lblPU_a     = creerTuile("PU Moyen",    "—", IhmUtils.AMBER);
		lblHeures_a = creerTuile("H restantes", "—", IhmUtils.ROUGE);

		tuiles.add(lblNbLots_a); tuiles.add(lblVVS_a); tuiles.add(lblPieces_a);
		tuiles.add(lblPU_a); tuiles.add(lblHeures_a);

		JPanel nord = new JPanel();
		nord.setLayout(new BoxLayout(nord, BoxLayout.Y_AXIS));
		nord.setBackground(IhmUtils.FOND);
		nord.add(ligneSelect); nord.add(Box.createVerticalStrut(5)); nord.add(tuiles);

		p.add(nord, BorderLayout.NORTH);
		return p;
	}

	private JPanel creerTableauAce()
	{
		modelAce = new DefaultTableModel(COLS, 0)
		{
			@Override public boolean isCellEditable(int row, int col)
			{
				if (estLigneAce_a(row)) return false;
				return col == C_NBETIQ || col == C_NBPARTS
					|| col == C_HETIQ  || col == C_HPARTS
					|| col == C_METHODE || col == C_DISTRIB || col == C_COMMENT
					|| col == C_PRETRI || col == C_SURPISTE || col == C_SORETIQ
					|| col == C_TRI    || col == C_FINI;
			}
			@Override public Class<?> getColumnClass(int col)
			{ return (col >= C_PRETRI && col <= C_FINI) ? Boolean.class : String.class; }
		};

		tblAce = IhmUtils.creerTable(modelAce);
		tblAce.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tblAce.setRowHeight(26);

		int[] w = {35,75,160,90,80,70,55,55,65,70,40,40,80,80,90,70,65,65,65,80,80,120};
		for (int i = 0; i < w.length && i < tblAce.getColumnCount(); i++)
			tblAce.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

		TableCellRenderer phaseRenderer = new DefaultTableCellRenderer()
		{
			private final JCheckBox cb = new JCheckBox(); { cb.setHorizontalAlignment(JCheckBox.CENTER); }
			@Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				if (estLigneAce_a(r)) return rendererEnteteAce_a(r);
				boolean val = Boolean.TRUE.equals(v); cb.setSelected(val);
				cb.setBackground(sel ? IhmUtils.SEL : val ? new Color(210,240,210) : Color.WHITE); return cb;
			}
		};
		for (int col = C_PRETRI; col <= C_FINI; col++)
			tblAce.getColumnModel().getColumn(col).setCellRenderer(phaseRenderer);

		tblAce.getColumnModel().getColumn(C_AVATIQ) .setCellRenderer(rendererPct_a());
		tblAce.getColumnModel().getColumn(C_AVAPARTS).setCellRenderer(rendererPct_a());

		TableCellRenderer rendGen = new DefaultTableCellRenderer()
		{
			@Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				super.getTableCellRendererComponent(t, v, sel, foc, r, c);
				if (estLigneAce_a(r)) { setBackground(couleurAcePourLigne_a(r)); setForeground(Color.WHITE); setFont(getFont().deriveFont(Font.BOLD, 12f)); return this; }
				if (r < rowToLot_a.size()) {
					Lot lot = rowToLot_a.get(r);
					if (lot != null) {
						if      (lot.isEstSousDouane())    setBackground(new Color(235,220,255));
						else if (lot.getPhase().isFinit()) setBackground(new Color(220,255,220));
						else if (lot.getPriorite() >= 8)   setBackground(new Color(255,235,235));
						else                               setBackground(Color.WHITE);
					}
				}
				setForeground(Color.BLACK); setFont(getFont().deriveFont(Font.PLAIN)); return this;
			}
		};
		for (int col = 0; col < COLS.length; col++) {
			if (col >= C_PRETRI && col <= C_FINI) continue;
			if (col == C_AVATIQ || col == C_AVAPARTS || col == C_EMPLACEMENT) continue;
			tblAce.getColumnModel().getColumn(col).setCellRenderer(rendGen);
		}

		tblAce.getColumnModel().getColumn(C_EMPLACEMENT).setCellRenderer(new DefaultTableCellRenderer()
		{
			@Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				super.getTableCellRendererComponent(t, v, sel, foc, r, c);
				String empl = v != null ? v.toString() : "";
				if      (empl.startsWith("A"))   setBackground(new Color(210,230,255));
				else if (empl.startsWith("B"))   setBackground(new Color(210,255,215));
				else if (empl.startsWith("C"))   setBackground(new Color(255,235,200));
				else if (empl.startsWith("D"))   setBackground(new Color(240,215,255));
				else if (empl.startsWith("HD"))  setBackground(new Color(255,210,210));
				else if (empl.startsWith("LTS")) setBackground(new Color(220,220,220));
				else                             setBackground(Color.WHITE);
				setHorizontalAlignment(CENTER); return this;
			}
		});

		modelAce.addTableModelListener(e ->
		{
			if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
			int row = e.getFirstRow(), col = e.getColumn();
			if (estLigneAce_a(row)) return;
			if (col == C_NBETIQ || col == C_NBPARTS || col == C_HETIQ || col == C_HPARTS)
				sauvegarderSuivi_a(row);
			else if (col == C_METHODE || col == C_DISTRIB)
				sauvegarderMethodeDistrib_a(row, col);
			else if (col >= C_PRETRI && col <= C_FINI)
				sauvegarderPhase_a(row, col);
			else if (col == C_COMMENT)
				sauvegarderCommentaire_a(row);
		});

		JScrollPane scroll = new JScrollPane(tblAce,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));

		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.add(creerLegende(), BorderLayout.NORTH);
		p.add(scroll,         BorderLayout.CENTER);
		return p;
	}

	private boolean estLigneAce_a(int row)
	{
		if (row < 0 || row >= rowToLot_a.size()) return false;
		return rowToLot_a.get(row) == null;
	}

	private Color couleurAcePourLigne_a(int row)
	{
		Object v = modelAce.getValueAt(row, 0);
		if (v == null) return ACE_BG[0];
		try { return ACE_BG[Integer.parseInt(v.toString().replace(ACE_HEADER_MARKER,"")) % ACE_BG.length]; }
		catch (Exception e) { return ACE_BG[0]; }
	}

	private Component rendererEnteteAce_a(int r)
	{ JLabel l = new JLabel(""); l.setOpaque(true); l.setBackground(couleurAcePourLigne_a(r)); return l; }

	private TableCellRenderer rendererPct_a()
	{
		return new DefaultTableCellRenderer()
		{
			@Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c)
			{
				if (estLigneAce_a(r)) { setBackground(couleurAcePourLigne_a(r)); setText(""); return this; }
				super.getTableCellRendererComponent(t, v, sel, foc, r, c); setHorizontalAlignment(CENTER);
				try {
					double pct = Double.parseDouble(v.toString().replace("%","").trim());
					if (!sel) setBackground(pct>=80?new Color(210,240,210):pct>=50?new Color(255,250,200):new Color(255,220,220));
					setForeground(pct>=80?IhmUtils.VERT:pct>=50?IhmUtils.AMBER:IhmUtils.ROUGE); setFont(getFont().deriveFont(Font.BOLD));
				} catch (Exception ex) { setBackground(Color.WHITE); setForeground(Color.BLACK); }
				return this;
			}
		};
	}

	private String nomMethode(Lot lot)
	{
		if (lot == null || lot.getMethode() == null) return "";
		return lot.getMethode().getNom();
	}

	/**
	 * Remplit le combo ACE en mémorisant l'ACE courante par son nom,
	 * pour la retrouver après un rafraîchissement.
	 */
	public void remplirComboAces()
	{
		if (aceCourante != null) nomAceMemorise = aceCourante.getNom();

		combAce.removeAllItems();
		combAce.addItem("— Choisir une ACE —");
		int idxARetablir = 0;
		List<Ace> aces = ctrl.getTouteAces();
		for (int i = 0; i < aces.size(); i++)
		{
			Ace a = aces.get(i);
			combAce.addItem(a.getNom() + " (" + a.getLots().size() + " lots)");
			if (nomAceMemorise != null && nomAceMemorise.equals(a.getNom())) idxARetablir = i + 1;
		}
		if (idxARetablir > 0) combAce.setSelectedIndex(idxARetablir);
	}

	private void changerAce()
	{
		int idx = combAce.getSelectedIndex() - 1;
		if (idx < 0 || idx >= ctrl.getTouteAces().size())
		{
			aceCourante = null; nomAceMemorise = null;
			modelAce.setRowCount(0); rowToLot_a.clear(); viderRecap_a(); return;
		}
		aceCourante    = ctrl.getTouteAces().get(idx);
		nomAceMemorise = aceCourante.getNom();
		chargerFicheRouteAce();
	}

	private void chargerFicheRouteAce()
	{
		if (aceCourante == null) return;
		modelAce.setRowCount(0); rowToLot_a.clear();

		List<Lot> lots = aceCourante.getLots();
		int totalVVS = 0, totalPieces = 0, countPU = 0; double totalPU = 0;
		for (Lot lot : lots)
		{
			totalVVS += lot.getValeurVente();
			totalPieces += lot.getNbPieces();
			if (lot.getNbPieces() > 0) { totalPU += lot.getPrixUnitaire(); countPU++; }
		}
		double puMoy = countPU > 0 ? totalPU / countPU : 0;

		majTuile(lblNbLots_a, "Lots",       String.valueOf(lots.size()), IhmUtils.BLEU);
		majTuile(lblVVS_a,    "VVS",         totalVVS > 0 ? String.format("%,d €", totalVVS) : "—", IhmUtils.VERT);
		majTuile(lblPieces_a, "Pièces",      String.format("%,d", totalPieces), new Color(0,80,140));
		majTuile(lblPU_a,     "PU Moyen",    puMoy > 0 ? String.format("%.2f €", puMoy) : "—", IhmUtils.AMBER);
		majTuile(lblHeures_a, "H restantes", "—", IhmUtils.ROUGE);

		Object[] ligneAce = new Object[COLS.length];
		ligneAce[0] = ACE_HEADER_MARKER + "0"; ligneAce[1] = "▶ " + aceCourante.getNom();
		for (int i = 2; i < COLS.length; i++) ligneAce[i] = "";
		modelAce.addRow(ligneAce); rowToLot_a.add(null);

		for (Lot lot : lots) { modelAce.addRow(creerLigneLot(lot)); rowToLot_a.add(lot); }
	}

	private void viderRecap_a()
	{
		majTuile(lblNbLots_a, "Lots",       "—", IhmUtils.BLEU);
		majTuile(lblVVS_a,    "VVS",        "—", IhmUtils.VERT);
		majTuile(lblPieces_a, "Pièces",     "—", new Color(0,80,140));
		majTuile(lblPU_a,     "PU Moyen",   "—", IhmUtils.AMBER);
		majTuile(lblHeures_a, "H restantes","—", IhmUtils.ROUGE);
	}

	private void sauvegarderSuivi_a(int row)
	{
		if (aceCourante == null || row < 0 || row >= rowToLot_a.size()) return;
		Lot lot = rowToLot_a.get(row); if (lot == null) return;
		try {
			ctrl.mettreAJourSuiviProd(lot, parseInt(modelAce.getValueAt(row, C_NBETIQ)), parseInt(modelAce.getValueAt(row, C_NBPARTS)));
			modelAce.setValueAt(lot.getSuivieProd().getAvancementEtiqPct(),  row, C_AVATIQ);
			modelAce.setValueAt(lot.getSuivieProd().getAvancementPartsPct(), row, C_AVAPARTS);
			chargerFicheRouteAce();
		} catch (Exception ignored) {}
	}

	private void sauvegarderMethodeDistrib_a(int row, int col)
	{
		if (aceCourante == null || row < 0 || row >= rowToLot_a.size()) return;
		Lot lot = rowToLot_a.get(row); if (lot == null) return;

		try
		{
			String v = modelAce.getValueAt(row, col) != null
				? modelAce.getValueAt(row, col).toString().trim()
				: "";

			if (col == C_METHODE)
				ctrl.modifierLotMethodeDistribution(lot, v, lot.getLotACharge());
			else
				ctrl.modifierLotMethodeDistribution(lot, nomMethode(lot), v);

			chargerFicheRouteAce();
		}
		catch (Exception ignored) {}
	}

	private void sauvegarderPhase_a(int row, int col)
	{
		if (aceCourante == null || row < 0 || row >= rowToLot_a.size()) return;
		Lot lot = rowToLot_a.get(row); if (lot == null) return;
		try {
			boolean val = Boolean.TRUE.equals(modelAce.getValueAt(row, col));
			boolean preTri = lot.getPhase().isPreTri(), surPiste = lot.getPhase().isSurPiste(),
				sortieEtiq = lot.getPhase().isSortieEtiq(), tri = lot.getPhase().isTri(), finit = lot.getPhase().isFinit();
			switch (col) {
				case C_PRETRI:   preTri     = val; break; case C_SURPISTE: surPiste   = val; break;
				case C_SORETIQ:  sortieEtiq = val; break; case C_TRI:      tri        = val; break;
				case C_FINI:     finit      = val; break;
			}
			ctrl.modifierPhase(lot, preTri, surPiste, sortieEtiq, tri, finit);
			chargerFicheRouteAce();
		} catch (Exception ignored) {}
	}

	private void sauvegarderCommentaire_a(int row)
	{
		if (aceCourante == null || row < 0 || row >= rowToLot_a.size()) return;
		Lot lot = rowToLot_a.get(row); if (lot == null) return;
		try { lot.setCommentaire(modelAce.getValueAt(row, C_COMMENT) != null ? modelAce.getValueAt(row, C_COMMENT).toString().trim() : ""); }
		catch (Exception ignored) {}
	}

	// ════════════════════════════════════════════════════════════════════
	// ── ÉLÉMENTS COMMUNS ────────────────────────────────────────────────
	// ════════════════════════════════════════════════════════════════════

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
			lot.getValeurVente()>0 && lot.getNbPieces()>0
				? String.format("%.2f",(double)lot.getValeurVente()/lot.getNbPieces())
				: "—",

			lot.getPhase().isPreTri(),
			lot.getPhase().isSurPiste(),
			lot.getPhase().isSortieEtiq(),
			lot.getPhase().isTri(),
			lot.getPhase().isFinit(),

			// ✅ FIX ICI
			nomMethode(lot),

			s(lot.getLotACharge()),
			s(lot.getEmplacement()),

			String.valueOf(lot.getSuivieProd().getNbPieceEtiq()),
			String.valueOf(lot.getSuivieProd().getNbPieceRepart()),
			lot.getSuivieProd().getAvancementEtiqPct(),
			lot.getSuivieProd().getAvancementPartsPct(),
			String.valueOf(hEtiq),
			String.valueOf(hParts),
			s(lot.getCommentaire())
		};
	}

	private JLabel creerTuile(String titre, String val, Color couleur)
	{
		JLabel l = new JLabel(buildTuileHtml(titre, val, couleur));
		l.setOpaque(true); l.setBackground(Color.WHITE);
		l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(IhmUtils.BORD), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return l;
	}

	private JLabel creerTuileMini(String titre, String val, Color couleur)
	{
		String hex = String.format("#%02x%02x%02x", couleur.getRed(), couleur.getGreen(), couleur.getBlue());
		JLabel l = new JLabel("<html><span style='font-size:8px;color:#888;'>" + titre + "</span><br><b style='font-size:12px;color:" + hex + ";'>" + val + "</b></html>");
		l.setOpaque(true); l.setBackground(Color.WHITE);
		l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(IhmUtils.BORD), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
		l.setPreferredSize(new Dimension(100, 46)); return l;
	}

	private String buildTuileHtml(String titre, String val, Color c)
	{
		String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
		return "<html><span style='font-size:9px;color:#888;'>" + titre + "</span><br><b style='font-size:14px;color:" + hex + ";'>" + val + "</b></html>";
	}

	private void majTuile(JLabel tuile, String titre, String val, Color couleur)
	{ tuile.setText(buildTuileHtml(titre, val, couleur)); }

	private JPanel creerLegende()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
		p.setBackground(new Color(248, 250, 252));
		p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, IhmUtils.BORD));
		for (String s : new String[]{"✏ Colonnes blanches = éditables","🟩 Av. ≥ 80%","🟨 50-79%","🟥 < 50%","  |  Ligne colorée = en-tête ACE","  |  🟣 Sous douane  🟢 Terminé  🔴 Prio ≥ 8"})
		{ JLabel l = new JLabel(s); l.setFont(new Font("SansSerif", Font.PLAIN, 11)); l.setForeground(Color.GRAY); p.add(l); }
		return p;
	}

	private int parseInt(Object v)
	{ try { return Integer.parseInt(v != null ? v.toString().trim() : "0"); } catch (Exception e) { return 0; } }

	private String s(String v) { return v != null ? v : ""; }

	// ════════════════════════════════════════════════════════════════════
	// ── RAFRAÎCHISSEMENT GLOBAL ──────────────────────────────────────────
	// ════════════════════════════════════════════════════════════════════

	/**
	 * Appelé par FenetrePrincipale.rafraichirTout().
	 * L'ACE courante est préservée grâce à nomAceMemorise.
	 */
	public void rafraichir()
	{
		// Sauvegarde des sélections actuelles
		Societe s = societeCourante != null ? societeCourante : null;
		Ace     a = aceCourante != null ? aceCourante : null;

		// Recharge des combos
		remplirComboSocietes();
		remplirComboAces();

		// ─────────────────────────────────────────────
		// Restauration société
		// ─────────────────────────────────────────────
		if (s != null)
		{
			societeCourante = s;

			for (int i = 0; i < combSociete.getItemCount(); i++)
			{
				String item = combSociete.getItemAt(i);

				if (item != null && item.startsWith(s.getNom()))
				{
					combSociete.setSelectedIndex(i);
					break;
				}
			}
		}
			chargerFicheRouteSociete();

		// ─────────────────────────────────────────────
		// Restauration ACE
		// ─────────────────────────────────────────────
		if (a != null)
		{
			aceCourante = a;


			for (int i = 0; i < combAce.getItemCount(); i++)
			{
				String item = combAce.getItemAt(i);

				if (item != null && item.startsWith(a.getNom()))
				{
					combAce.setSelectedIndex(i);
					break;
				}
			}

			chargerFicheRouteAce();
		}
	}
}
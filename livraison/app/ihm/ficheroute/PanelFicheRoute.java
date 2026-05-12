package app.ihm.ficheroute;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.ficheroute.FicheRoute;
import app.metier.lot.Lot;
import app.metier.personelle.Ace;
import app.metier.personelle.Societe;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Panel "Fiches de Route" — affichage en cartes par lot.
 *  - Haut conservé tel quel : stats globales + récap ACE
 *  - Bas remplacé par un panneau de cartes scrollable
 */
public class PanelFicheRoute extends JPanel
{
	private final Controleur        ctrl;
	private final FenetrePrincipale fenetre;

	// ── Sous-onglet "Par Société" ──────────────────────────────────────
	private JComboBox<String> combSociete;
	private JLabel  lblVVS_s, lblPieces_s, lblPU_s, lblHeures_s, lblNbLots_s;
	private JPanel  panelRecapAce_s;
	private JPanel  panelCartes_s;
	private Societe societeCourante;

	// ── Sous-onglet "Par ACE" ──────────────────────────────────────────
	private JComboBox<String> combAce;
	private JLabel  lblVVS_a, lblPieces_a, lblPU_a, lblHeures_a, lblNbLots_a;
	private JPanel  panelCartes_a;
	private Ace     aceCourante;
	private String  nomAceMemorise = null;

	// ══════════════════════════════════════════════════════════════════
	// Construction
	// ══════════════════════════════════════════════════════════════════

	public PanelFicheRoute(Controleur ctrl, FenetrePrincipale fenetre)
	{
		this.ctrl    = ctrl;
		this.fenetre = fenetre;
		setLayout(new BorderLayout());
		setBackground(IhmUtils.FOND);

		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
		tabs.addTab("\uD83C\uDFE2 Par Société", creerPanelSociete());
		tabs.addTab("\uD83D\uDC64 Par ACE",     creerPanelAce());
		add(tabs, BorderLayout.CENTER);
	}

	// ══════════════════════════════════════════════════════════════════
	// ONGLET PAR SOCIÉTÉ
	// ══════════════════════════════════════════════════════════════════

	private JPanel creerPanelSociete()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setBackground(IhmUtils.FOND);
		p.add(creerHautSociete(), BorderLayout.NORTH);

		panelCartes_s = new JPanel();
		panelCartes_s.setLayout(new BoxLayout(panelCartes_s, BoxLayout.Y_AXIS));
		panelCartes_s.setBackground(new Color(240, 242, 245));

		JScrollPane scroll = new JScrollPane(panelCartes_s,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));
		p.add(scroll, BorderLayout.CENTER);
		return p;
	}

	private JPanel creerHautSociete()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBackground(IhmUtils.FOND);

		// Ligne sélection + boutons
		JPanel ligneSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		ligneSelect.setBackground(IhmUtils.FOND);
		JLabel lbl = new JLabel("Société : ");
		lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
		combSociete = new JComboBox<>();
		combSociete.setFont(new Font("SansSerif", Font.PLAIN, 13));
		combSociete.setPreferredSize(new Dimension(280, 28));
		combSociete.addActionListener(e -> changerSociete());
		JButton btnExport   = IhmUtils.bouton("\uD83D\uDDB8 Aperçu / Export", IhmUtils.BLEU, Color.WHITE);
		btnExport.addActionListener(e -> exporterTexte());
		JButton btnTerminer = IhmUtils.bouton("✓ Marquer terminé", IhmUtils.VERT, Color.WHITE);
		btnTerminer.addActionListener(e -> marquerTermine_s());
		JButton btnMeth = IhmUtils.bouton("\uD83D\uDC41 Voir la Méthode", IhmUtils.VERT, Color.WHITE);
		btnMeth.addActionListener(e -> ouvrirMeth_s());
		ligneSelect.add(lbl); ligneSelect.add(combSociete);
		ligneSelect.add(btnExport); ligneSelect.add(btnTerminer); ligneSelect.add(btnMeth);

		// Tuiles stats globales
		JPanel tuiles = new JPanel(new GridLayout(1, 5, 6, 0));
		tuiles.setBackground(IhmUtils.FOND);
		tuiles.setPreferredSize(new Dimension(0, 64));
		lblNbLots_s = creerTuile("Lots affectés",  "—", IhmUtils.BLEU);
		lblVVS_s    = creerTuile("VVS Total",       "—", IhmUtils.VERT);
		lblPieces_s = creerTuile("Nb Pièces",       "—", new Color(0, 80, 140));
		lblPU_s     = creerTuile("PU Moyen",        "—", IhmUtils.AMBER);
		lblHeures_s = creerTuile("H CE restantes",  "—", IhmUtils.ROUGE);
		tuiles.add(lblNbLots_s); tuiles.add(lblVVS_s); tuiles.add(lblPieces_s);
		tuiles.add(lblPU_s); tuiles.add(lblHeures_s);

		// Récap ACE
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

	// ══════════════════════════════════════════════════════════════════
	// ONGLET PAR ACE
	// ══════════════════════════════════════════════════════════════════

	private JPanel creerPanelAce()
	{
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setBackground(IhmUtils.FOND);
		p.add(creerHautAce(), BorderLayout.NORTH);

		panelCartes_a = new JPanel();
		panelCartes_a.setLayout(new BoxLayout(panelCartes_a, BoxLayout.Y_AXIS));
		panelCartes_a.setBackground(new Color(240, 242, 245));

		JScrollPane scroll = new JScrollPane(panelCartes_a,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		scroll.setBorder(BorderFactory.createLineBorder(IhmUtils.BORD));
		p.add(scroll, BorderLayout.CENTER);
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
		lblNbLots_a = creerTuile("Lots",        "—", IhmUtils.BLEU);
		lblVVS_a    = creerTuile("VVS",          "—", IhmUtils.VERT);
		lblPieces_a = creerTuile("Pièces",       "—", new Color(0, 80, 140));
		lblPU_a     = creerTuile("PU Moyen",     "—", IhmUtils.AMBER);
		lblHeures_a = creerTuile("H restantes",  "—", IhmUtils.ROUGE);
		tuiles.add(lblNbLots_a); tuiles.add(lblVVS_a); tuiles.add(lblPieces_a);
		tuiles.add(lblPU_a); tuiles.add(lblHeures_a);

		JPanel nord = new JPanel();
		nord.setLayout(new BoxLayout(nord, BoxLayout.Y_AXIS));
		nord.setBackground(IhmUtils.FOND);
		nord.add(ligneSelect);
		nord.add(Box.createVerticalStrut(5));
		nord.add(tuiles);
		p.add(nord, BorderLayout.NORTH);
		return p;
	}

	// ── Helpers de carte ────────────────────────────────────────────────

	private JLabel badge(String txt, Color col)
	{
		JLabel l = new JLabel(txt);
		l.setFont(new Font("SansSerif", Font.BOLD, 10));
		l.setOpaque(true); l.setBackground(col); l.setForeground(Color.WHITE);
		l.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
		return l;
	}

	private JCheckBox checkPhase(String label, boolean etat, Lot lot, String code, Color accent)
	{
		JCheckBox cb = new JCheckBox(label, etat);
		cb.setFont(new Font("SansSerif", Font.PLAIN, 11));
		cb.setOpaque(false);
		cb.setForeground(etat ? IhmUtils.VERT : new Color(100, 100, 100));
		cb.addActionListener(e -> {
			boolean v = cb.isSelected();
			boolean pt = lot.getPhase().isPreTri(), sp = lot.getPhase().isSurPiste(),
			        se = lot.getPhase().isSortieEtiq(), tr = lot.getPhase().isTri(), fi = lot.getPhase().isFinit();
			switch (code) {
				case "PRETRI":   pt = v; break; case "SURPISTE": sp = v; break;
				case "SORETIQ":  se = v; break; case "TRI":      tr = v; break;
				case "FINI":     fi = v; break;
			}
			ctrl.modifierPhase(lot, pt, sp, se, tr, fi);
		});
		return cb;
	}

	private JPanel barreProg(int val, int max, int w)
	{
		JPanel p = new JPanel(null) {
			@Override protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int W = getWidth(), H = getHeight();
				g2.setColor(new Color(215, 220, 228)); g2.fillRoundRect(0, 0, W, H, H, H);
				int fill = max > 0 ? (int)(W * val / (double) max) : 0;
				Color c = val == max ? IhmUtils.VERT : val > 0 ? IhmUtils.AMBER : new Color(200, 200, 200);
				if (fill > 0) { g2.setColor(c); g2.fillRoundRect(0, 0, fill, H, H, H); }
			}
		};
		p.setPreferredSize(new Dimension(w, 10));
		p.setOpaque(false);
		return p;
	}

	private void info(JPanel p, String label, String val, Color bg)
	{
		JPanel bloc = new JPanel(new BorderLayout(0, 0));
		bloc.setBackground(bg);
		JLabel l = new JLabel(label);
		l.setFont(new Font("SansSerif", Font.PLAIN, 10));
		l.setForeground(new Color(130, 130, 130));
		JLabel v = new JLabel(val);
		v.setFont(new Font("SansSerif", Font.BOLD, 12));
		v.setForeground(Color.DARK_GRAY);
		bloc.add(l, BorderLayout.NORTH);
		bloc.add(v, BorderLayout.CENTER);
		p.add(bloc);
	}

	private JSeparator separateur(Color bg)
	{
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setForeground(new Color(220, 225, 232));
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return sep;
	}
	private JPanel champEditableTexte(String label, String valeur, Color bg, Consumer<String> setter)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setBackground(bg);

		JLabel l = new JLabel(label + " :");
		l.setFont(new Font("SansSerif", Font.BOLD, 11));
		l.setForeground(new Color(90, 90, 90));

		JTextField tf = new JTextField(valeur, 10);
		tf.setFont(new Font("SansSerif", Font.PLAIN, 12));

		tf.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				setter.accept(tf.getText().trim());
			}
		});

		p.add(l);
		p.add(tf);

		return p;
	}

	private JPanel champEditableInt(String label, int valeur, Color bg, IntConsumer setter)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setBackground(bg);

		JLabel l = new JLabel(label + " :");
		l.setFont(new Font("SansSerif", Font.BOLD, 11));
		l.setForeground(new Color(90, 90, 90));

		JSpinner spinner = new JSpinner(new SpinnerNumberModel(valeur, 0, Integer.MAX_VALUE, 1));

		JComponent editor = spinner.getEditor();
		JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
		tf.setColumns(5);

		spinner.addChangeListener(e ->
		{
			setter.accept((Integer) spinner.getValue());
		});

		p.add(l);
		p.add(spinner);

		return p;
	}

	private String fmt(int n) { return String.format("%,d", n); }

	// ── En-têtes de section ─────────────────────────────────────────────

	private JPanel creerEnteteAce(Ace ace, int idx, List<Lot> lots)
	{
		JPanel p = new JPanel(new BorderLayout(8, 0));
		p.setBackground(ace.getColor());
		p.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
		JLabel titre = new JLabel("▶  " + ace.getNom() + "   (" + lots.size() + " lot(s))");
		titre.setFont(new Font("SansSerif", Font.BOLD, 13));
		titre.setForeground(Color.WHITE);
		int vvs = 0, pcs = 0;
		for (Lot l : lots) { vvs += l.getValeurVente(); pcs += l.getNbPieces(); }
		JLabel stats = new JLabel(String.format("VVS : %,d €   |   Pièces : %,d", vvs, pcs));
		stats.setFont(new Font("SansSerif", Font.PLAIN, 11));
		stats.setForeground(new Color(200, 220, 255));
		p.add(titre, BorderLayout.CENTER);
		p.add(stats, BorderLayout.EAST);
		return p;
	}

	private JPanel creerEnteteSection(String titre, Color c)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
		p.setBackground(c);
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		JLabel l = new JLabel(titre);
		l.setFont(new Font("SansSerif", Font.BOLD, 12));
		l.setForeground(Color.WHITE);
		p.add(l);
		return p;
	}

	// ══════════════════════════════════════════════════════════════════
	// CHARGEMENT SOCIÉTÉ
	// ══════════════════════════════════════════════════════════════════

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
		if (idx < 0 || idx >= ctrl.getSocietes().size()) {
			societeCourante = null; viderRecap_s();
			panelCartes_s.removeAll(); panelCartes_s.revalidate(); panelCartes_s.repaint();
			panelRecapAce_s.removeAll(); panelRecapAce_s.revalidate(); panelRecapAce_s.repaint();
			return;
		}
		societeCourante = ctrl.getSocietes().get(idx);
		chargerFicheRouteSociete();
	}

	private void chargerFicheRouteSociete()
	{
		if (societeCourante == null) return;
		FicheRoute fdr = ctrl.genererFicheRoute(societeCourante);

		int vvs = 0, pcs = 0, cntPU = 0; double sumPU = 0;
		for (Lot lot : societeCourante.getLots()) {
			vvs += lot.getValeurVente(); pcs += lot.getNbPieces();
			if (lot.getNbPieces() > 0) { sumPU += lot.getPrixUnitaire(); cntPU++; }
		}
		double puMoy = cntPU > 0 ? sumPU / cntPU : 0;
		majTuile(lblNbLots_s, "Lots affectés",  String.valueOf(societeCourante.getLots().size()), IhmUtils.BLEU);
		majTuile(lblVVS_s,    "VVS Total",       vvs > 0 ? String.format("%,d €", vvs) : "—", IhmUtils.VERT);
		majTuile(lblPieces_s, "Nb Pièces",       String.format("%,d", pcs), new Color(0, 80, 140));
		majTuile(lblPU_s,     "PU Moyen",        puMoy > 0 ? String.format("%.2f €", puMoy) : "—", IhmUtils.AMBER);
		majTuile(lblHeures_s, "H CE restantes",  societeCourante.getTotalHeuresCE() + "h", IhmUtils.ROUGE);

		reconstruireRecapAce_s(fdr);

		panelCartes_s.removeAll();
		List<Ace> aces = societeCourante.getAces();
		if (aces == null || aces.isEmpty())
		{
			for (Lot lot : societeCourante.getLots())
				panelCartes_s.add(new CarteLot(lot, IhmUtils.BLEU,this.ctrl,this));
		}
		else
		{
			java.util.Set<Lot> dansAce = new java.util.HashSet<>();
			for (Ace ace : aces) if (ace.getLots() != null) dansAce.addAll(ace.getLots());
			for (int i = 0; i < aces.size(); i++)
			{
				Ace ace = aces.get(i);
				List<Lot> lotsAce = ace.getLots() != null ? ace.getLots() : new ArrayList<>();
				panelCartes_s.add(creerEnteteAce(ace, i, lotsAce));
				for (Lot lot : lotsAce)
					panelCartes_s.add(new CarteLot(lot, ace.getColor(),this.ctrl,this));
			}
			List<Lot> sans = new ArrayList<>();
			for (Lot lot : societeCourante.getLots()) if (!dansAce.contains(lot)) sans.add(lot);
			if (!sans.isEmpty())
			{
				panelCartes_s.add(creerEnteteSection("Sans ACE (" + sans.size() + " lot(s))", new Color(80, 80, 80)));
				for (Lot lot : sans) panelCartes_s.add(new CarteLot(lot, new Color(80, 80, 80),this.ctrl,this));
			}
		}
		panelCartes_s.add(Box.createVerticalGlue());
		panelCartes_s.revalidate(); panelCartes_s.repaint();
	}

	// ══════════════════════════════════════════════════════════════════
	// CHARGEMENT ACE
	// ══════════════════════════════════════════════════════════════════

	public void remplirComboAces()
	{
		if (aceCourante != null) nomAceMemorise = aceCourante.getNom();
		combAce.removeAllItems();
		combAce.addItem("— Choisir une ACE —");
		int restore = 0;
		List<Ace> aces = ctrl.getTouteAces();
		for (int i = 0; i < aces.size(); i++) {
			Ace a = aces.get(i);
			combAce.addItem(a.getNom() + " (" + a.getLots().size() + " lots)");
			if (nomAceMemorise != null && nomAceMemorise.equals(a.getNom())) restore = i + 1;
		}
		if (restore > 0) combAce.setSelectedIndex(restore);
	}

	private void changerAce()
	{
		int idx = combAce.getSelectedIndex() - 1;
		if (idx < 0 || idx >= ctrl.getTouteAces().size()) {
			aceCourante = null; nomAceMemorise = null;
			panelCartes_a.removeAll(); panelCartes_a.revalidate(); panelCartes_a.repaint();
			viderRecap_a(); return;
		}
		aceCourante    = ctrl.getTouteAces().get(idx);
		nomAceMemorise = aceCourante.getNom();
		chargerFicheRouteAce();
	}

	private void chargerFicheRouteAce()
	{
		if (aceCourante == null) return;
		List<Lot> lots = aceCourante.getLots();
		int vvs = 0, pcs = 0, cntPU = 0; double sumPU = 0;
		for (Lot lot : lots) {
			vvs += lot.getValeurVente(); pcs += lot.getNbPieces();
			if (lot.getNbPieces() > 0) { sumPU += lot.getPrixUnitaire(); cntPU++; }
		}
		double puMoy = cntPU > 0 ? sumPU / cntPU : 0;
		majTuile(lblNbLots_a, "Lots",        String.valueOf(lots.size()), IhmUtils.BLEU);
		majTuile(lblVVS_a,    "VVS",          vvs > 0 ? String.format("%,d €", vvs) : "—", IhmUtils.VERT);
		majTuile(lblPieces_a, "Pièces",       String.format("%,d", pcs), new Color(0, 80, 140));
		majTuile(lblPU_a,     "PU Moyen",     puMoy > 0 ? String.format("%.2f €", puMoy) : "—", IhmUtils.AMBER);
		majTuile(lblHeures_a, "H restantes",  "—", IhmUtils.ROUGE);

		panelCartes_a.removeAll();
		panelCartes_a.add(creerEnteteSection("▶  " + aceCourante.getNom() + "  (" + lots.size() + " lot(s))", aceCourante.getColor()));
		for (Lot lot : lots) panelCartes_a.add(new CarteLot(lot, this.ctrl.getAceDuLot(lot).getColor(),this.ctrl,this));
		panelCartes_a.add(Box.createVerticalGlue());
		panelCartes_a.revalidate(); panelCartes_a.repaint();
	}

	// ══════════════════════════════════════════════════════════════════
	// RÉCAP ACE (haut société)
	// ══════════════════════════════════════════════════════════════════

	private void reconstruireRecapAce_s(FicheRoute fdr)
	{
		panelRecapAce_s.removeAll();
		if (societeCourante == null || societeCourante.getAces() == null || societeCourante.getAces().isEmpty())
		{ panelRecapAce_s.revalidate(); panelRecapAce_s.repaint(); return; }
		List<Ace> aces = societeCourante.getAces();
		for (int i = 0; i < aces.size(); i++) {
			Ace ace = aces.get(i);
			List<Lot> lots = ace.getLots() != null ? ace.getLots() : new ArrayList<>();
			int vvs = 0, pcs = 0, cnt = 0, etiq = 0; double puSum = 0;
			for (Lot l : lots) {
				vvs += l.getValeurVente(); pcs += l.getNbPieces();
				if (l.getNbPieces() > 0) { puSum += l.getPrixUnitaire(); cnt++; }
				if (l.getNbPieces() > 0 && l.getSuivieProd() != null) etiq += l.getSuivieProd().getNbPieceEtiq();
			}
			double puMoy  = cnt > 0 ? puSum / cnt : 0;
			double av     = pcs > 0 ? 100.0 * etiq / pcs : 0;
			Color col     = ace.getColor();
			JPanel grp = new JPanel(new BorderLayout(0, 2));
			grp.setBackground(IhmUtils.FOND);
			grp.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(col, 2),
				BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			JLabel titLbl = new JLabel(" " + ace.getNom(), JLabel.LEFT);
			titLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
			titLbl.setForeground(Color.WHITE); titLbl.setOpaque(true); titLbl.setBackground(col);
			titLbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			JPanel t3 = new JPanel(new GridLayout(1, 4, 4, 0)); t3.setBackground(IhmUtils.FOND);
			t3.add(creerTuileMini("VVS",     vvs > 0 ? String.format("%,d €", vvs) : "—", col));
			t3.add(creerTuileMini("Pièces",  String.format("%,d", pcs), col));
			t3.add(creerTuileMini("PU Moy.", puMoy > 0 ? String.format("%.2f €", puMoy) : "—", col));
			t3.add(creerTuileMini("Av.",     av > 0 ? String.format("%.1f %%", av) : "—", col));
			grp.add(titLbl, BorderLayout.NORTH); grp.add(t3, BorderLayout.CENTER);
			panelRecapAce_s.add(grp);
		}
		panelRecapAce_s.revalidate(); panelRecapAce_s.repaint();
	}

	// ══════════════════════════════════════════════════════════════════
	// ACTIONS BOUTONS
	// ══════════════════════════════════════════════════════════════════

	private void marquerTermine_s()
	{
		if (societeCourante == null) return;
		List<Lot> lots = societeCourante.getLots();
		if (lots.isEmpty()) { JOptionPane.showMessageDialog(this, "Aucun lot.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
		String[] opts = lots.stream().map(l -> "N°" + l.getNumCDE() + " — " + s(l.getAffaire())).toArray(String[]::new);
		String choix = (String) JOptionPane.showInputDialog(this, "Choisir le lot à terminer :", "Marquer terminé",
			JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
		if (choix == null) return;
		Lot lot = lots.get(Arrays.asList(opts).indexOf(choix));
		if (JOptionPane.showConfirmDialog(this, "Marquer le lot \"" + lot.getNumCDE() + "\" comme terminé ?",
			"Confirmer", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			ctrl.marquerLotTermine(lot); chargerFicheRouteSociete();
			JOptionPane.showMessageDialog(this, "Lot marqué comme terminé.", "Terminé", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void ouvrirMeth_s()
	{
		if (societeCourante == null) return;
		List<Lot> avecMeth = societeCourante.getLots().stream()
			.filter(l -> l.getMethode() != null).collect(Collectors.toList());
		if (avecMeth.isEmpty()) { JOptionPane.showMessageDialog(this, "Aucun lot avec méthode.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
		String[] opts = avecMeth.stream().map(l -> "N°" + l.getNumCDE() + " — " + s(l.getAffaire()) + "  [" + l.getMethode().getNom() + "]").toArray(String[]::new);
		String choix = (String) JOptionPane.showInputDialog(this, "Choisir le lot :", "Voir Méthode",
			JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
		if (choix == null) return;
		avecMeth.get(Arrays.asList(opts).indexOf(choix)).getMethode().ouvrir();
	}

	private void exporterTexte()
	{
		if (societeCourante == null) { JOptionPane.showMessageDialog(this, "Sélectionnez d'abord une société.", "Export", JOptionPane.WARNING_MESSAGE); return; }
		StringBuilder sb = new StringBuilder();
		sb.append("FICHE DE ROUTE — ").append(societeCourante.getNom()).append("\n").append("=".repeat(90)).append("\n");
		FicheRoute fdr = ctrl.genererFicheRoute(societeCourante);
		sb.append(String.format("%-8s %-40s %-12s %-10s %-8s %-10s\n", "N° CDE", "Désignation", "Nb Pièces", "VVS (€)", "Heures", "Av.Étiq %"));
		sb.append("-".repeat(90)).append("\n");
		for (Lot lot : societeCourante.getLots()) {
			double totH = lot.getHeures(); int hE = lot.getSuivieProd().getNbHeureEtiqRestant();
			double av = totH > 0 ? Math.max(0, 100.0 - hE / totH * 100) : 0;
			sb.append(String.format("%-8d %-40s %-12s %-10s %-8.1f %-10.1f%%\n",
				lot.getNumCDE(), (s(lot.getAffaire()) + " " + s(lot.getTypologie())).trim(),
				String.format("%,d", lot.getNbPieces()),
				lot.getValeurVente() > 0 ? String.format("%,d", lot.getValeurVente()) : "—", totH, av));
		}
		sb.append("\n").append("=".repeat(90)).append("\n")
		  .append(String.format("TOTAL : VVS=%,d €  |  Pièces=%,d  |  H CE dispo=%dh\n", fdr.getSommeVVS(), fdr.getSommePieces(), societeCourante.getTotalHeuresCE()));
		JTextArea ta = new JTextArea(sb.toString(), 28, 90);
		ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); ta.setEditable(false);
		JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Fiche de Route — " + societeCourante.getNom(), JOptionPane.INFORMATION_MESSAGE);
	}

	// ══════════════════════════════════════════════════════════════════
	// TUILES COMMUNES
	// ══════════════════════════════════════════════════════════════════

	private JLabel creerTuile(String titre, String val, Color c)
	{
		JLabel l = new JLabel(buildTuileHtml(titre, val, c));
		l.setOpaque(true); l.setBackground(Color.WHITE);
		l.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(IhmUtils.BORD),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return l;
	}

	private JLabel creerTuileMini(String titre, String val, Color c)
	{
		String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
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

	private void majTuile(JLabel t, String titre, String val, Color c) { t.setText(buildTuileHtml(titre, val, c)); }

	private void viderRecap_s() {
		majTuile(lblNbLots_s,"Lots affectés","—",IhmUtils.BLEU); majTuile(lblVVS_s,"VVS Total","—",IhmUtils.VERT);
		majTuile(lblPieces_s,"Nb Pièces","—",new Color(0,80,140)); majTuile(lblPU_s,"PU Moyen","—",IhmUtils.AMBER);
		majTuile(lblHeures_s,"H CE restantes","—",IhmUtils.ROUGE);
	}
	private void viderRecap_a() {
		majTuile(lblNbLots_a,"Lots","—",IhmUtils.BLEU); majTuile(lblVVS_a,"VVS","—",IhmUtils.VERT);
		majTuile(lblPieces_a,"Pièces","—",new Color(0,80,140)); majTuile(lblPU_a,"PU Moyen","—",IhmUtils.AMBER);
		majTuile(lblHeures_a,"H restantes","—",IhmUtils.ROUGE);
	}

	private String s(String v) { return v != null ? v : ""; }

	// ══════════════════════════════════════════════════════════════════
	// RAFRAÎCHISSEMENT
	// ══════════════════════════════════════════════════════════════════

	public void rafraichir()
	{
		Societe sv = societeCourante;
		Ace     av = aceCourante;
		remplirComboSocietes();
		remplirComboAces();
		if (sv != null) {
			societeCourante = sv;
			for (int i = 0; i < combSociete.getItemCount(); i++) {
				String item = combSociete.getItemAt(i);
				if (item != null && item.startsWith(sv.getNom())) { combSociete.setSelectedIndex(i); break; }
			}
		}
		chargerFicheRouteSociete();
		if (av != null) {
			aceCourante = av;
			for (int i = 0; i < combAce.getItemCount(); i++) {
				String item = combAce.getItemAt(i);
				if (item != null && item.startsWith(av.getNom())) { combAce.setSelectedIndex(i); break; }
			}
			chargerFicheRouteAce();
		}
	}
}
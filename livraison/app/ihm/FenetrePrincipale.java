package app.ihm;

import app.Controleur;
import app.ihm.map.entrepot.PanelMap;
import app.ihm.ficheroute.fiche_route.PanelFicheRoute;
import app.ihm.gestionlot.affectation.PanelAffectation;
import app.ihm.gestionlot.lots.PanelLots;
import app.ihm.gestionlot.societes.PanelSocietes;
import app.metier.lot.Lot;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class FenetrePrincipale extends JFrame
{
	private final Controleur ctrl;

	private PanelAffectation panelAffectation;
	private PanelSocietes    panelSocietes;
	private PanelLots        panelLots;
	private PanelFicheRoute  panelFicheRoute;
	private PanelMap         panelMap;
	private JLabel           lblInfo;

	public FenetrePrincipale(Controleur ctrl)
	{
		this.ctrl = ctrl;
		setTitle("Planning Global Futura — PAM S07/2026");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1280, 780);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		getContentPane().setBackground(IhmUtils.FOND);

		setJMenuBar(creerMenuBar());
		add(creerEntete(), BorderLayout.NORTH);

		panelAffectation = new PanelAffectation(ctrl, this);
		panelSocietes    = new PanelSocietes   (ctrl, this);
		panelLots        = new PanelLots       (ctrl, this);
		panelFicheRoute  = new PanelFicheRoute (ctrl, this);
		panelMap         = new PanelMap        (ctrl, this);
		JTabbedPane onglets = new JTabbedPane();
		onglets.setFont(new Font("SansSerif", Font.PLAIN, 13));
		onglets.addTab("⊕ Affectation",      panelAffectation);
		onglets.addTab("▤ Sociétés & heures", panelSocietes);
		onglets.addTab("☰ Liste des lots",    panelLots);
		onglets.addTab("📋 Fiches de Route",   panelFicheRoute);
		onglets.addTab("🗺 Carte entrepôt",    panelMap);
		add(onglets, BorderLayout.CENTER);

		// Rafraîchir fiche de route quand on clique sur l'onglet
		onglets.addChangeListener(e -> {
			if (onglets.getSelectedComponent() == panelFicheRoute)
				panelFicheRoute.rafraichir();
			if (onglets.getSelectedComponent() == panelMap)
				panelMap.rafraichir();
		});

		panelAffectation.remplirComboSocietes();
		rafraichirTout();
		setVisible(true);
	}

	// ── Menu Fichier ──────────────────────────────────────────────────────

	private JMenuBar creerMenuBar()
	{
		JMenuBar bar = new JMenuBar();

		JMenu menuFichier = new JMenu("Fichier");
		menuFichier.setFont(new Font("SansSerif", Font.PLAIN, 13));

		JMenuItem itemOuvrir      = new JMenuItem("📂  Ouvrir une sauvegarde…");
		JMenuItem itemSauvegarder = new JMenuItem("💾  Sauvegarder          Ctrl+S");
		JMenuItem itemNouveaux    = new JMenuItem("🆕  Nouveaux fichiers JSON…");

		itemOuvrir     .addActionListener(e -> ouvrirSauvegarde());
		itemSauvegarder.addActionListener(e -> sauvegarder());
		itemNouveaux   .addActionListener(e -> nouveaux());

		itemSauvegarder.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
		itemOuvrir     .setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
		itemNouveaux   .setAccelerator(KeyStroke.getKeyStroke("ctrl N"));

		menuFichier.add(itemOuvrir);
		menuFichier.addSeparator();
		menuFichier.add(itemSauvegarder);
		menuFichier.addSeparator();
		menuFichier.add(itemNouveaux);

		bar.add(menuFichier);
		return bar;
	}

	private void ouvrirSauvegarde()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Ouvrir une sauvegarde JSON");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

		String dossier = fc.getSelectedFile().getAbsolutePath();
		try
		{
			ctrl.chargerDonnees(dossier);
			panelAffectation.remplirComboSocietes();
			panelFicheRoute.remplirComboSocietes();
			rafraichirTout();
			JOptionPane.showMessageDialog(this,
				"Sauvegarde chargée : " + fc.getSelectedFile().getName(),
				"Chargement OK", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(),
				"Erreur", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void sauvegarder()
	{
		// Demande toujours le dossier de destination pour copier les fichiers JSON
		sauvegarderSous();
	}

	private void sauvegarderSous()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Copier les fichiers JSON vers…");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		
		if (fc.showDialog(this, "Copier") != JFileChooser.APPROVE_OPTION) return;

		String dossier = fc.getSelectedFile().getAbsolutePath();
		// Demande le numéro de semaine avant la sauvegarde
		String numSeamine = JOptionPane.showInputDialog(
			this,
			"Numéro de semaine :",
			"Sauvegarde — semaine",
			JOptionPane.PLAIN_MESSAGE
		);

		if (numSeamine == null || numSeamine.isBlank())
			return; // l'utilisateur a annulé ou laissé vide

		numSeamine = numSeamine.trim();

		try
		{
			ctrl.sauvegarderDonnees(dossier, numSeamine);
			JOptionPane.showMessageDialog(this,
				"Fichiers copiés vers : " + fc.getSelectedFile().getName(),
				"Sauvegarde OK", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(),
				"Erreur", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void nouveaux()
	{
		int res = JOptionPane.showConfirmDialog(this,
			"Voulez-vous vraiment réinitialiser les données ?",
			"Nouvelle session", JOptionPane.YES_NO_OPTION);
		if (res == JOptionPane.YES_OPTION)
		{
			ctrl.nouveaux();
			panelAffectation.remplirComboSocietes();
			panelFicheRoute.remplirComboSocietes();
			rafraichirTout();
		}
	}

	// ── En-tête (identique à l'original) ─────────────────────────────────

	private JPanel creerEntete()
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(IhmUtils.HEADER);
		p.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

		JLabel titre = new JLabel("Planning Global Futura — Gestion des lots");
		titre.setForeground(Color.WHITE);
		titre.setFont(new Font("SansSerif", Font.BOLD, 17));

		Button btnRafraichir = new Button("⟳");
		btnRafraichir.setFont(new Font("SansSerif", Font.PLAIN, 12));
		btnRafraichir.setBackground(IhmUtils.HEADER);
		btnRafraichir.setForeground(new Color(255, 255, 180));
		btnRafraichir.addActionListener(e -> rafraichirTout());

		lblInfo = new JLabel(buildInfo());
		lblInfo.setForeground(new Color(180, 180, 180));
		lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));

		

		p.add(titre,   BorderLayout.WEST);
		p.add(btnRafraichir, BorderLayout.SOUTH);
		p.add(lblInfo, BorderLayout.EAST);
		return p;
	}

	private String buildInfo()
	{
		long nbAff = ctrl.getSocietes().stream().mapToLong(s -> s.getLots().size()).sum();
		int  nbH   = ctrl.getSocietes().stream().mapToInt(s -> s.getTotalHeuresCE()).sum();
		return ctrl.getLots().size() + " lots  |  " + " Heures total Lot"+ getHeureLotTotal() +
			+ ctrl.getSocietes().size() + " sociétés  |  "
			+ nbAff + " affectés  |  "
			+ nbH + "h disponibles";
	}

	public String getHeureLotTotal()
	{
		int total = 0;
		for (Lot lot : ctrl.getLots())
		{
			if (!lot.getStatut().contains("bloqué") && !lot.isEstSousDouane())
			{
				total += lot.getHeures();
			}
		}
	
		return total > 0 ? " (" + total + "h)  |  " : "  |  ";
	}

	// ── Rafraîchissement global (identique à l'original) ─────────────────

	public void rafraichirTout()
	{
		SwingUtilities.invokeLater(() -> {
			panelAffectation.rafraichir();
			panelSocietes   .rafraichir();
			panelLots       .rafraichir();
			panelFicheRoute .rafraichir();
			panelMap        .rafraichir();
			if (lblInfo != null) lblInfo.setText(buildInfo());
		});
	}

	public PanelAffectation getPanelAffectation() { return panelAffectation; }
}
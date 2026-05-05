package app.ihm.dialogue.edit_lot;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.ihm.gestionlot.affectation.PanelAffectation;
import app.metier.lot.Lot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Dialogue d'édition complète d'un lot existant.
 *
 * CORRIGÉS :
 *   - Chef d'équipe mis à jour (était absent de l'appel à modifierLot)
 *   - Calcul heures = NbPieces / Cadence (une seule cadence, pas étiq+répart)
 *   - Tous les champs du lot sont éditables
 */
public class DialogEditLot extends JDialog
{
	private final Controleur        ctrl;
	private final Lot               lot;
	private final PanelAffectation  panelAff;   // peut être null
	private final FenetrePrincipale fenetre;

	// Champs
	private JTextField  fTypologie, fAffaire, fNbPieces, fCadence, fValeur;
	private JTextField  fSemaine, fLotACharge, fDateRec, fDatePai, fCommentaire;
	private JCheckBox   fDouane;
	private JComboBox<String> fStatut, fStatutEchant, femplacement;
	private JSpinner    fPriorite;

	// Affichage heures calculées
	private JLabel      lblHeures, lblHeuresAce;
	private JLabel      lblErreur;

	public DialogEditLot(FenetrePrincipale fenetre, Controleur ctrl, Lot lot, PanelAffectation panelAff)
	{
		super(fenetre, "Modifier le lot — N° " + lot.getNumCDE(), true);
		this.fenetre  = fenetre;
		this.ctrl     = ctrl;
		this.lot      = lot;
		this.panelAff = panelAff;
		setSize(520, 650);
		setLocationRelativeTo(fenetre);
		setLayout(new BorderLayout());
		add(creerFormulaire(), BorderLayout.CENTER);
		add(creerBas(),        BorderLayout.SOUTH);
		preRemplir();
	}

	// ── Formulaire ────────────────────────────────────────────────────────

	private JScrollPane creerFormulaire()
	{
		fTypologie   = new JTextField();
		fAffaire     = new JTextField();
		fNbPieces    = new JTextField();
		fCadence     = new JTextField();
		fValeur      = new JTextField();
		fSemaine     = new JTextField();
		fLotACharge  = new JTextField();
		fDateRec     = new JTextField();
		fDatePai     = new JTextField();
		fCommentaire = new JTextField();
		fDouane      = new JCheckBox("Sous douane");
		fPriorite    = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));

		fStatut = new JComboBox<>(new String[]{
			"","OU", "TC","MR"});
		fStatutEchant = new JComboBox<>(new String[]{
			"","VA - Validé avec le CP", "BL - Bloqué", "EP - Envoi au CP",
			"A faire", "En cours"});
		femplacement = new JComboBox<>(new String[]{
			"","B42", "A99", "LTS", "HD", "B21", "C"
		});

		lblHeures = new JLabel("—");
		lblHeures.setForeground(IhmUtils.BLEU);
		lblHeures.setFont(new Font("SansSerif", Font.BOLD, 13));

		lblHeuresAce = new JLabel("—");
		lblHeuresAce.setForeground(IhmUtils.BLEU);
		lblHeuresAce.setFont(new Font("SansSerif", Font.BOLD, 13));

		// Recalcul en temps réel
		KeyAdapter majH = new CalculHeuresKeyListenerEdit(this);
		fNbPieces.addKeyListener(majH);
		fCadence .addKeyListener(majH);

		Object[][] champs = {
			{"Typologie *",        fTypologie},
			{"Affaire",            fAffaire},
			{"Nb pièces *",        fNbPieces},
			{"Cadence (p/h) *",    fCadence},
			{"Heures calculées",   lblHeures},
			{"heures ACE",         lblHeuresAce},
			{"Valeur vente (€)",   fValeur},
			{"Statut interne",     fStatut},
			{"Statut échant.",     fStatutEchant},
			{"Semaine",            fSemaine},
			{"Priorité",           fPriorite},
			{"Lot à charge",       fLotACharge},
			{"Emplacement",        femplacement},
			{"Date réception",     fDateRec},
			{"Date paiement",      fDatePai},
			{"",                   fDouane},
			{"Commentaire",        fCommentaire},
		};

		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));
		form.setBackground(Color.WHITE);
		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(4, 4, 4, 4);
		gc.fill   = GridBagConstraints.HORIZONTAL;

		for (int i = 0; i < champs.length; i++)
		{
			gc.gridx = 0; gc.gridy = i; gc.weightx = 0.28;
			JLabel lbl = new JLabel((String) champs[i][0]);
			lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
			lbl.setForeground(Color.GRAY);
			form.add(lbl, gc);
			gc.gridx = 1; gc.weightx = 0.72;
			form.add((Component) champs[i][1], gc);
		}
		return new JScrollPane(form);
	}

	private JPanel creerBas()
	{
		lblErreur = new JLabel(" ");
		lblErreur.setForeground(IhmUtils.ROUGE);
		lblErreur.setFont(new Font("SansSerif", Font.ITALIC, 12));

		JButton btnOk  = IhmUtils.bouton("Enregistrer", IhmUtils.VERT,          Color.WHITE);
		JButton btnAnn = IhmUtils.bouton("Annuler",     new Color(100,100,100), Color.WHITE);
		btnAnn.addActionListener(e -> dispose());
		btnOk .addActionListener(e -> valider());

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		p.setBackground(Color.WHITE);
		p.add(lblErreur); p.add(btnAnn); p.add(btnOk);
		return p;
	}

	// ── Pré-remplissage ───────────────────────────────────────────────────

	private void preRemplir()
	{
		fTypologie  .setText(s(lot.getTypologie()));
		fAffaire    .setText(s(lot.getAffaire()));
		fNbPieces   .setText(String.valueOf(lot.getNbPieces()));
		fCadence    .setText(String.format("%.2f", lot.getCadence()));
		fValeur     .setText(String.valueOf(lot.getValeurVente()));
		fSemaine    .setText(s(lot.getSemaine()));
		fLotACharge .setText(s(lot.getLotACharge()));
		femplacement.setSelectedItem(s(lot.getEmplacement()));
		fDateRec    .setText(s(lot.getDateReception()));
		fDatePai    .setText(s(lot.getDatePaiement()));
		fCommentaire.setText(s(lot.getCommentaire()));
		fDouane     .setSelected(lot.isEstSousDouane());
		fPriorite   .setValue(lot.getPriorite());

		selectCombo(fStatut,      lot.getStatut());
		selectCombo(fStatutEchant, lot.getStatutEchant());
		calculerHeures();
	}

	private void selectCombo(JComboBox<String> combo, String valeur)
	{
		for (int i = 0; i < combo.getItemCount(); i++)
			if (combo.getItemAt(i).equals(valeur)) { combo.setSelectedIndex(i); return; }
	}

	// ── Calcul heures ─────────────────────────────────────────────────────

	void calculerHeures()
	{
		try
		{
			int    nb  = Integer.parseInt(fNbPieces.getText().trim());
			double cad = Double.parseDouble(fCadence.getText().trim().replace(",", "."));
			double h   = (cad > 0) ? nb / cad : 0.0;
			lblHeures.setText(String.format("%.2fh  (%.0f pièces ÷ %.2f p/h)", h, (double) nb, cad));
			lblHeures.setForeground(IhmUtils.BLEU);
			// Heures ACE = Heures totales / effectif ACE (si affecté à un ACE)
			double hAce = 0.0;
			if (panelAff != null)
			{
				hAce = lot.getHeuresAce();
				lblHeuresAce.setText(String.format("%.2fh", hAce));
			}
		}
		catch (NumberFormatException ex)
		{
			lblHeures.setText("Saisie invalide");
			lblHeures.setForeground(IhmUtils.ROUGE);
		}
	}

	// ── Validation ────────────────────────────────────────────────────────

	private void valider()
	{
		try
		{
			String typo = fTypologie.getText().trim();
			if (typo.isEmpty()) { lblErreur.setText("La typologie est obligatoire."); return; }

			int    nbPieces  = Integer.parseInt(fNbPieces.getText().trim());
			double cadence   = Double.parseDouble(fCadence.getText().trim().replace(",", "."));
			int    valeur    = fValeur.getText().trim().isEmpty() ? 0
							   : Integer.parseInt(fValeur.getText().trim());

			ctrl.modifierLot(lot,
				typo,
				fAffaire    .getText().trim(),
				nbPieces,
				cadence,
				valeur,
				(String) fStatut     .getSelectedItem(),
				(String) fStatutEchant.getSelectedItem(),
				fSemaine    .getText().trim(),
				(int) fPriorite.getValue(),
				fLotACharge .getText().trim(),
				(String) femplacement.getSelectedItem(),
				fDouane     .isSelected(),
				fDateRec    .getText().trim(),
				fDatePai    .getText().trim(),
				fCommentaire.getText().trim()
			);

			if (panelAff != null) fenetre.getPanelAffectation().remplirComboSocietes();
			fenetre.rafraichirTout();
			if (panelAff != null) panelAff.afficherStatut(
				"Lot " + lot.getNumCDE() + " mis à jour.", IhmUtils.VERT);
			dispose();
		}
		catch (NumberFormatException ex)
		{
			lblErreur.setText("Valeur numérique invalide : " + ex.getMessage());
		}
	}

	private String s(String v) { return v != null ? v : ""; }
}

package app;

import app.ihm.FenetrePrincipale;
import app.metier.PlanningGlobal;
import app.metier.collecte.DonneesSauvegarder;
import app.metier.ficheroute.FicheRoute;
import app.metier.lot.Lot;
import app.metier.personelle.Ace;
import app.metier.personelle.Societe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Contrôleur MVC.
 *
 * Responsabilités :
 *   - Gérer TOUS les dialogues utilisateur (JFileChooser, JOptionPane)
 *   - Appeler la couche métier (PlanningGlobal) avec des données propres
 *   - Déclencher l'auto-sauvegarde après chaque modification
 *
 * PlanningGlobal ne contient plus aucun import javax.swing.
 */
public class Controleur
{
	private FenetrePrincipale  fenetre;
	private PlanningGlobal     metier;
	private DonneesSauvegarder savDonnees;
	private String             cheminLotsJson;
	private String             cheminSocietesJson;

	private static final String LOTS_JSON     = "app/data/courutilisation/lots.json";
	private static final String SOCIETES_JSON = "app/data/courutilisation/societes.json";
	private static final String SOCIETES_REF  = "app/data/pastouche/societes.json";

	public Controleur()
	{
		this.metier             = new PlanningGlobal();
		this.savDonnees         = new DonneesSauvegarder();
		this.cheminLotsJson     = LOTS_JSON;
		this.cheminSocietesJson = SOCIETES_JSON;
		chargerDonneesDemarrage();
		this.fenetre = new FenetrePrincipale(this);
	}

	// ── Chargement au démarrage ───────────────────────────────────────────

	private void chargerDonneesDemarrage()
	{
		String xlsx = demanderFichierExcel("Sélectionner le fichier des lots (XLSX / XLSM)");
		if (xlsx != null)
		{
			try
			{
				ArrayList<Lot> tempLots = app.metier.collecte.ExcelReader.lireLots(xlsx);
				int semaine = 0;
				if (!tempLots.isEmpty())
				{
					String sem = tempLots.get(0).getSemaine();
					try { semaine = Integer.parseInt("" + sem.charAt(sem.length()-2) + sem.charAt(sem.length()-1)); }
					catch (NumberFormatException ignored) {}
				}
				String xlsxHeures = demanderFichierExcel("Sélectionner le fichier des heures ACE (XLSX / XLSM)");
				if (xlsxHeures == null) xlsxHeures = xlsx;
				metier.chargerDepuisExcel(xlsx, SOCIETES_REF, semaine, xlsxHeures);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null,
					"Erreur lors du chargement Excel :\n" + e.getMessage() + "\n\nRetour aux données JSON.",
					"Erreur de chargement", JOptionPane.WARNING_MESSAGE);
				chargerFallbackJson();
			}
		}
		else
		{
			chargerFallbackJson();
		}
	}

	private void chargerFallbackJson()
	{
		int rep = JOptionPane.showConfirmDialog(null,
			"Aucun fichier Excel sélectionné.\nVoulez-vous utiliser les données JSON existantes ?",
			"Chargement des lots", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (rep != JOptionPane.YES_OPTION)
		{
			System.out.println("[Controleur] Chargement annulé par l'utilisateur.");
			System.exit(0);
		}
		try
		{
			metier.chargerDepuisJson(LOTS_JSON, SOCIETES_JSON);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null,
				"Impossible de charger les données JSON :\n" + e.getMessage(),
				"Erreur fatale", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	// ── Helper fichier Excel ──────────────────────────────────────────────

	private String demanderFichierExcel(String titre)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(titre);
		chooser.setFileFilter(new FileNameExtensionFilter("Fichiers Excel (*.xlsx, *.xlsm)", "xlsx", "xlsm"));
		File dossierDefaut = new File("app/data");
		if (dossierDefaut.exists() && dossierDefaut.isDirectory())
			chooser.setCurrentDirectory(dossierDefaut);
		int resultat = chooser.showOpenDialog(null);
		if (resultat == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile().getAbsolutePath();
		return null;
	}

	// ── Données ───────────────────────────────────────────────────────────

	public ArrayList<Societe> getSocietes() { return metier.getSocietes(); }
	public ArrayList<Lot>     getLots()     { return metier.getLots();     }

	// ── Gestion des lots ──────────────────────────────────────────────────

	public void supprimerLot(Lot lot)
	{ metier.supprimerLot(lot); autoSauvegarderLots(); }

	public void ajouterLot(int numCDE, String typologie, String affaire,
	                       int nbPieces, double cadence, int valeurVente,
	                       String statut, String statutEchant,
	                       String semaine, int priorite,
	                       String lotACharge, String emplacement,
	                       boolean sousDouane, String dateReception,
	                       String datePaiement, String commentaire)
	{
		metier.ajouterLot(numCDE, typologie, affaire, nbPieces, cadence, valeurVente,
		                  statut, statutEchant, semaine, priorite,
		                  lotACharge, emplacement, sousDouane, dateReception, datePaiement, commentaire);
		autoSauvegarderLots();
	}

	public void exportNewLot()
	{
		String xlsx = demanderFichierExcel("Sélectionner le fichier des nouveaux lots");
		if (xlsx == null)
		{
			JOptionPane.showMessageDialog(null, "Aucun fichier sélectionné. Opération annulée.",
				"Import lots", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		try
		{
			metier.importerNouveauxLots(xlsx);
			autoSauvegarderLots();
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, "Erreur lors de l'import :\n" + e.getMessage(),
				"Import lots", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ── Affectation ───────────────────────────────────────────────────────

	public boolean affecterLot(Lot lot, Societe societe, Ace ace)
	{
		boolean ok = metier.affecterLot(lot, societe, ace);
		if (ok) autoSauvegarderSocietes();
		return ok;
	}

	public void desaffecterLot(Lot lot)
	{ metier.desaffecterLot(lot); autoSauvegarderSocietes(); }

	// ── Modification lots ─────────────────────────────────────────────────

	public void modifierLot(Lot lot, String typologie, String affaire,
	                        int nbPieces, double cadence, int valeurVente,
	                        String statut, String statutEchant,
	                        String semaine, int priorite,
	                        String lotACharge, String emplacement,
	                        boolean sousDouane, String dateReception,
	                        String datePaiement, String commentaire)
	{
		metier.modifierLot(lot, typologie, affaire, nbPieces, cadence, valeurVente,
		                   statut, statutEchant, semaine, priorite,
		                   lotACharge, emplacement, sousDouane, dateReception, datePaiement, commentaire);
		autoSauvegarderLots();
	}

	public void modifierLotMethodeDistribution(Lot lot, String typologie, String lotACharge)
	{ metier.modifierLotMethodeDistribution(lot, typologie, lotACharge); autoSauvegarderLots(); }

	public void modifierPhase(Lot lot, boolean preTri, boolean surPiste,
	                          boolean sortieEtiq, boolean tri, boolean finit)
	{ metier.modifierPhase(lot, preTri, surPiste, sortieEtiq, tri, finit); autoSauvegarderLots(); }

	public void marquerLotTermine(Lot lot)
	{ metier.marquerLotTermine(lot); autoSauvegarderLots(); }

	public void commencerLot(Lot l) { this.metier.commencerLot(l);}
	public void annulerLot(Lot l)   { this.metier.annulerLot(l);  }

	// ── Modification sociétés ─────────────────────────────────────────────

	public void modifierSociete(Societe soc, String nom, String ce, int totalHeuresCE, int effectif)
	{ metier.modifierSociete(soc, nom, ce, totalHeuresCE, effectif); autoSauvegarderSocietes(); }

	/**
	 * Met à jour la liste complète des ACE d'une société.
	 * Refuse si on essaie de supprimer un ACE qui a encore des lots affectés.
	 */
	public boolean mettreAJourAces(Societe soc, List<Ace> nouvellesAces)
	{
		List<Ace> aces = soc.getAces();
		for (int i = nouvellesAces.size(); i < aces.size(); i++)
			if (!aces.get(i).getLots().isEmpty()) return false;

		int min = Math.min(aces.size(), nouvellesAces.size());
		for (int i = 0; i < min; i++)
		{
			Ace ancien  = aces.get(i);
			Ace nouveau = nouvellesAces.get(i);
			metier.modifierAce(ancien, nouveau.getNom(), nouveau.getNbPers(), nouveau.getEffectifActuel());
		}
		for (int i = aces.size() - 1; i >= nouvellesAces.size(); i--)
			aces.remove(i);
		for (int i = min; i < nouvellesAces.size(); i++)
		{
			Ace n = nouvellesAces.get(i);
			aces.add(new Ace(n.getNom(), n.getNbPers(), n.getEffectifActuel()));
		}
		autoSauvegarderSocietes();
		return true;
	}

	public void nouvelleHeurePourSociete(int semaine)
	{
		String xlsx = demanderFichierExcel("Sélectionner le fichier des heures ACE");
		if (xlsx == null)
		{
			JOptionPane.showMessageDialog(null, "Aucun fichier sélectionné. Opération annulée.",
				"Nouvelle heure", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		try
		{
			metier.mettreAJourHeuresSocietes(xlsx, semaine);
			autoSauvegarderSocietes();
			JOptionPane.showMessageDialog(null, "Heures ajoutées avec succès !", "Nouvelle heure", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, "Erreur lors du chargement du fichier Excel :\n" + e.getMessage(),
				"Nouvelle heure", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ── Suivi production ──────────────────────────────────────────────────

	public void mettreAJourSuiviProd(Lot lot, int nbPieceEtiq, int nbPieceRepart)
	{
		if (nbPieceEtiq <= lot.getNbPieces() && nbPieceRepart <= lot.getNbPieces())
		{
			lot.getSuivieProd().setNbPieceEtiq  (nbPieceEtiq);
			lot.getSuivieProd().setNbPieceRepart(nbPieceRepart);
		}
		autoSauvegarderLots();
	}

	// ── Recherche ─────────────────────────────────────────────────────────

	public Societe getSocieteDuLot(Lot lot) { return metier.getSocieteDuLot(lot); }
	public Ace     getAceDuLot    (Lot lot) { return metier.getAceDuLot(lot);     }
	public ArrayList<Ace> getTouteAces() { return metier.getTouteAces(); }

	// ── Fiche de route ────────────────────────────────────────────────────

	public FicheRoute genererFicheRoute(Societe societe)
	{ return metier.genererFicheRoute(societe); }

	// ── Sauvegarde / Chargement ───────────────────────────────────────────

	public void sauvegarderDonnees(String cheminDossier, String semaine)
	{
		try
		{
			String dossierSemaine = cheminDossier + "/S" + semaine;
			if (!Files.exists(Paths.get(dossierSemaine)))
				Files.createDirectories(Paths.get(dossierSemaine));
			String destLots     = dossierSemaine + "/lots.json";
			String destSocietes = dossierSemaine + "/societes.json";
			Files.copy(Paths.get(cheminLotsJson),     Paths.get(destLots),     java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Paths.get(cheminSocietesJson), Paths.get(destSocietes), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			this.cheminLotsJson     = destLots;
			this.cheminSocietesJson = destSocietes;
			System.out.println("[Controleur] Données sauvegardées dans : " + dossierSemaine);
		}
		catch (IOException e)
		{
			System.err.println("[Controleur] Erreur lors de la sauvegarde : " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void chargerDonnees(String chemin) throws IOException
	{
		savDonnees.charger(metier, chemin);
		this.cheminLotsJson     = chemin + "/lots.json";
		this.cheminSocietesJson = chemin + "/societes.json";
	}

	public void nouveaux()
	{
		this.cheminLotsJson     = LOTS_JSON;
		this.cheminSocietesJson = SOCIETES_JSON;
		this.metier = new PlanningGlobal();
		autoSauvegarderLots();
		autoSauvegarderSocietes();
	}

	// ── Auto-sauvegarde ───────────────────────────────────────────────────
	public void autoSauvegarde()
	{
		autoSauvegarderLots();
		autoSauvegarderSocietes();
	}

	private void autoSauvegarderLots()
	{
		if (cheminLotsJson != null)
			try { savDonnees.sauvegarderLots(metier.getLots(), cheminLotsJson); }
			catch (Exception e) { System.err.println("[AutoSave Lots] Échec : " + e.getMessage()); }
	}

	private void autoSauvegarderSocietes()
	{
		if (cheminSocietesJson != null)
			try { savDonnees.sauvegarderSocietes(metier.getSocietes(), metier.getLots(), cheminSocietesJson); }
			catch (Exception e) { System.err.println("[AutoSave Sociétés] Échec : " + e.getMessage()); }
	}

	// ── Point d'entrée ────────────────────────────────────────────────────

	public static void main(String[] args)
	{
		new Controleur();
	}
}

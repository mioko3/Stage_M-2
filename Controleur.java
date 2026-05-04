package app;

import app.ihm.FenetrePrincipale;
import app.metier.PlanningGlobal;
import app.metier.collecte.DonneesSauvegarder;
import app.metier.ficheroute.FicheRoute;
import app.metier.lot.Lot;
import app.metier.personelle.Ace;
import app.metier.personelle.Societe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Controleur
{
	private FenetrePrincipale  fenetre;
	private PlanningGlobal     metier;
	private DonneesSauvegarder savDonnees;
	private String             cheminLotsJson;     // Chemin vers lots.json source
	private String             cheminSocietesJson; // Chemin vers societes.json source

	public Controleur()
	{
		this.metier           = new PlanningGlobal();
		this.savDonnees       = new DonneesSauvegarder();
		// Chemins par défaut vers les fichiers JSON
		this.cheminLotsJson     = "app/data/courutilisation/lots.json";
		this.cheminSocietesJson = "app/data/courutilisation/societes.json";
		this.fenetre = new FenetrePrincipale(this);
	}

	// ── Données ───────────────────────────────────────────────────────────
	public ArrayList<Societe> getSocietes()         { return metier.getSocietes(); }
	public ArrayList<Lot>     getLots()             { return metier.getLots();     }
	public String             getCheminLotsJson()    { return cheminLotsJson;    }
	public String             getCheminSocietesJson() { return cheminSocietesJson; }

	// ── Gestion des lots ──────────────────────────────────────────────────
	public void ajouterLot(Lot lot)
	{ metier.ajouterLot(lot); autoSauvegarderLots(); }

	public void supprimerLot(Lot lot)
	{ metier.supprimerLot(lot); autoSauvegarderLots(); }

	/** Force la sauvegarde des lots (pour les modifications rapides). */
	public void sauvegarderLots()
	{ autoSauvegarderLots(); }

	// ── Affectation ───────────────────────────────────────────────────────
	public boolean affecterLot(Lot lot, Societe societe, Ace ace)
	{
		boolean ok = this.metier.affecterLot(lot, societe, ace);
		if (ok) autoSauvegarderSocietes();
		return ok;
	}

	public void desaffecterLot(Lot lot)
	{ this.metier.desaffecterLot(lot); autoSauvegarderSocietes(); }

	// ── Modification ──────────────────────────────────────────────────────
	public void modifierLot(Lot lot, String typologie, String affaire,
							int nbPieces, double cadence, int valeurVente,
							String statut, String statutEchant,
							String semaine, int priorite,
							String lotACharge, String emplacement,
							boolean sousDouane, String dateReception,
							String datePaiement, String commentaire)
	{
		this.metier.modifierLot(lot, typologie, affaire, nbPieces, cadence, valeurVente,
								statut, statutEchant, semaine, priorite,
								lotACharge, emplacement, sousDouane, dateReception, datePaiement,
								commentaire);
		autoSauvegarderLots();
	}

	public void ajouterLot(int numCDE, String typologie, String affaire,
					int nbPieces, double cadence, int valeurVente,
					String statut, String statutEchant,
					String semaine, int priorite,
					String lotACharge, String emplacement,
					boolean sousDouane, String dateReception,
					String datePaiement, String commentaire)
	{
		this.metier.ajouterLot(numCDE, typologie, affaire, nbPieces, cadence, valeurVente,
							statut, statutEchant, semaine, priorite,
							lotACharge, emplacement, sousDouane, dateReception, datePaiement,
							commentaire);
		autoSauvegarderLots();
	}

	public void modifierLotMethodeDistribution(Lot lot, String typologie, String lotACharge)
	{
		this.metier.modifierLotMethodeDistribution(lot, typologie, lotACharge);
		autoSauvegarderLots();
	}

	public void modifierPhase(Lot lot, boolean preTri, boolean surPiste,
						boolean sortieEtiq, boolean tri, boolean finit)
	{
		this.metier.modifierPhase(lot, preTri, surPiste, sortieEtiq, tri, finit);
		autoSauvegarderLots();
	}

	public void marquerLotTermine(Lot lot)
	{
		this.metier.marquerLotTermine(lot);
		autoSauvegarderLots();
	}

	public void modifierSociete(Societe soc, String nom, String ce, int totalHeuresCE, int effectif)
	{ this.metier.modifierSociete(soc, nom, ce, totalHeuresCE,effectif); autoSauvegarderSocietes(); }

	public void modifierAce(Ace ace, String nom, int nbPers, int effectif)
	{ this.metier.modifierAce(ace, nom, nbPers, effectif); autoSauvegarderSocietes(); }

	public boolean mettreAJourAces(Societe soc, List<Ace> nouvellesAces)
	{
		List<Ace> aces = soc.getAces();
		for (int i = nouvellesAces.size(); i < aces.size(); i++)
		{
			if (!aces.get(i).getLots().isEmpty()) return false;
		}

		int min = Math.min(aces.size(), nouvellesAces.size());
		for (int i = 0; i < min; i++)
		{
			Ace ancien = aces.get(i);
			Ace nouveau = nouvellesAces.get(i);
			modifierAce(ancien, nouveau.getNom(), nouveau.getNbPers(), nouveau.getEffectifActuel());
		}

		for (int i = aces.size() - 1; i >= nouvellesAces.size(); i--)
		{
			aces.remove(i);
		}

		for (int i = min; i < nouvellesAces.size(); i++)
		{
			Ace nouveau = nouvellesAces.get(i);
			aces.add(new Ace(nouveau.getNom(), nouveau.getNbPers(), nouveau.getEffectifActuel()));
		}

		autoSauvegarderSocietes();
		return true;
	}

	// ── Suivi production ──────────────────────────────────────────────────
	public void mettreAJourSuiviProd(Lot lot, int nbPieceEtiq, int nbPieceRepart)
	{
		lot.getSuivieProd().setNbPieceEtiq   (nbPieceEtiq);
		lot.getSuivieProd().setNbPieceRepart (nbPieceRepart);
		autoSauvegarderLots();
	}

	// ── Recherche ─────────────────────────────────────────────────────────
	public Societe getSocieteDuLot(Lot lot) { return this.metier.getSocieteDuLot(lot); }
	public Ace     getAceDuLot    (Lot lot) { return this.metier.getAceDuLot(lot);     }

	// ── Fiche de route ────────────────────────────────────────────────────
	public FicheRoute genererFicheRoute(Societe societe)
	{
		return this.metier.genererFicheRoute(societe);
	}

	// ── Sauvegarde / Chargement ───────────────────────────────────────────

	/** Sauvegarde/déplace les fichiers JSON vers le chemin donné. */
	public void sauvegarderDonnees(String cheminDossier, String semaine)
	{
		try
		{
			// Crée le dossier ET le sous-dossier S{semaine} si ils n'existent pas
			String dossierSemaine = cheminDossier + "/S" + semaine;
			if (!Files.exists(Paths.get(dossierSemaine)))
				Files.createDirectories(Paths.get(dossierSemaine));

			String cheminLotsDestination     = dossierSemaine + "/lots.json";
			String cheminSocietesDestination = dossierSemaine + "/societes.json";
			
			// Copie les fichiers JSON vers le nouveau dossier
			Files.copy(Paths.get(cheminLotsJson), Paths.get(cheminLotsDestination),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Paths.get(cheminSocietesJson), Paths.get(cheminSocietesDestination),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			
			// Met à jour les chemins de référence
			this.cheminLotsJson = cheminLotsDestination;
			this.cheminSocietesJson = cheminSocietesDestination;
			
			System.out.println("[Controleur] Données sauvegardées dans : " + cheminDossier);
		}
		catch (IOException e)
		{
			System.err.println("[Controleur] Erreur lors de la sauvegarde : " + e.getMessage());
			e.printStackTrace();
		}
	}

	/** Charge une sauvegarde et remplace le planning en cours. */
	public void chargerDonnees(String chemin) throws IOException
	{
		savDonnees.charger(metier, chemin);
		this.cheminLotsJson = chemin + "/lots.json";
		this.cheminSocietesJson = chemin + "/societes.json";
	}

	/** Auto-sauvegarde silencieuse des lots. */
	private void autoSauvegarderLots()
	{
		if (cheminLotsJson != null)
			try { savDonnees.sauvegarderLots(metier.getLots(), cheminLotsJson); }
			catch (Exception e) { System.err.println("[AutoSave Lots] Échec : " + e.getMessage()); }
	}

	/** Auto-sauvegarde silencieuse des sociétés. */
	private void autoSauvegarderSocietes()
	{
		if (cheminSocietesJson != null)
			try { savDonnees.sauvegarderSocietes(metier.getSocietes(), metier.getLots(), cheminSocietesJson); }
			catch (Exception e) { System.err.println("[AutoSave Sociétés] Échec : " + e.getMessage()); }
	}

	public void nouveaux()
	{
		this.cheminLotsJson = "app/data/courutilisation/lots.json";
		this.cheminSocietesJson = "app/data/courutilisation/societes.json";
		this.metier = new PlanningGlobal(); // Réinitialise le planning
		autoSauvegarderLots();
		autoSauvegarderSocietes();
	}

	public void NouvelleHeurePourSociete(int semaine)
	{
		this.metier.nouvelleHeurePourSociete(semaine);
		autoSauvegarderSocietes();
	}

	public static void main(String[] args)
	{
		new Controleur();
	}
}
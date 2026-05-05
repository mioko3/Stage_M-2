package app.metier;

import app.metier.collecte.ExcelReader;
import app.metier.ficheroute.FicheRoute;
import app.metier.ficheroute.Phase;
import app.metier.lot.Lot;
import app.metier.personelle.Ace;
import app.metier.personelle.Societe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PlanningGlobal
{
	private ArrayList<Societe> societes;
	private ArrayList<Lot>     lots;
	private ArrayList<FicheRoute> ficheRoute;

	public PlanningGlobal()
	{
		this.societes   = new ArrayList<>();
		this.lots       = new ArrayList<>();
		this.ficheRoute = new ArrayList<>();
		ExcelReader.donnerPlanningGlobal(this);
		chargerDonnees();
	}

	public void exportNewLot()
	{
		try
		{
			String xlsx = choisirFichier();
			if (xlsx != null)
			{
				ArrayList<Lot> templots = ExcelReader.lireLots(xlsx);
				for (Lot temp : templots)
				{
					this.lots.add(temp);
				}
			}
		}catch (IOException e)
		{
			System.err.println("[PlanningGlobal] Impossible de charger les données : " + e.getMessage());
			System.err.println("→ Placez les fichiers Excel dans app/data/ ou utilisez les JSON existants.");
		}
	}

	private void chargerDonnees()
	{
		try
		{
			String xlsx = choisirFichier();

			if (xlsx != null)
			{
				this.lots = ExcelReader.lireLots(xlsx);
				this.societes = ExcelReader.lireSocietes("app/data/pastouche/societes.json");
				// Extraire la semaine du premier lot pour charger les heures correspondantes
				String semaine = this.lots.get(0).getSemaine();
				int sem = Integer.parseInt(semaine.charAt(semaine.length()-2)+""+semaine.charAt(semaine.length()-1));
				ExcelReader.ajouterHeuresDepuisExcel(choisirFichier(), this.societes, sem);
			}
			else
			{
				this.lots = ExcelReader.lireLots("app/data/courutilisation/lots.json");
				this.societes = ExcelReader.lireSocietes("app/data/courutilisation/societes.json");
			}
		}
		catch (IOException e)
		{
			System.err.println("[PlanningGlobal] Impossible de charger les données : " + e.getMessage());
			System.err.println("→ Placez les fichiers Excel dans app/data/ ou utilisez les JSON existants.");
		}
	}

	/**
	 * Ouvre un JFileChooser pour que l'utilisateur sélectionne le fichier XLSX des lots.
	 *
	 * @return le chemin absolu du fichier choisi, ou null si l'utilisateur annule.
	 */
	private String choisirFichier()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Sélectionner le fichier des lots (XLSX / XLSM)");
		chooser.setFileFilter(new FileNameExtensionFilter(
			"Fichiers Excel (*.xlsx, *.xlsm)", "xlsx", "xlsm"));
 
		// Ouvrir par défaut dans app/data/ si le dossier existe
		File dossierDefaut = new File("app/data");
		if (dossierDefaut.exists() && dossierDefaut.isDirectory())
			chooser.setCurrentDirectory(dossierDefaut);
 
		int resultat = chooser.showOpenDialog(null);
		if (resultat == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile().getAbsolutePath();
 
		// L'utilisateur a annulé → proposer le fallback JSON
		int rep = JOptionPane.showConfirmDialog(
			null,
			"Aucun fichier sélectionné.\nVoulez-vous utiliser les données JSON existantes ?",
			"Chargement des lots",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE
		);
 
		if (rep == JOptionPane.YES_OPTION)
			return null; // null → fallback JSON dans chargerDonnees()
 
		// L'utilisateur refuse le fallback → quitter proprement
		System.err.println("[PlanningGlobal] Chargement annulé par l'utilisateur.");
		System.exit(0);
		return null;
	}

	public void nouvelleHeurePourSociete(int semaine)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Sélectionner le fichier des lots (XLSX / XLSM)");
		chooser.setFileFilter(new FileNameExtensionFilter(
			"Fichiers Excel (*.xlsx, *.xlsm)", "xlsx", "xlsm"));
 
		// Ouvrir par défaut dans app/data/ si le dossier existe
		File dossierDefaut = new File("app/data");
		if (dossierDefaut.exists() && dossierDefaut.isDirectory())
			chooser.setCurrentDirectory(dossierDefaut);
 
		int resultat = chooser.showOpenDialog(null);
		if (resultat == JFileChooser.APPROVE_OPTION)
		{
			String xml = chooser.getSelectedFile().getAbsolutePath();
			try
			{
				ExcelReader.ajouterHeuresDepuisExcel(xml, this.societes, semaine);
				JOptionPane.showMessageDialog(null, "Heures ajoutées avec succès !", "Nouvelle heure", JOptionPane.INFORMATION_MESSAGE);
			}
			catch (IOException e)
			{
				System.err.println("[PlanningGlobal] Impossible de charger les données : " + e.getMessage());
				JOptionPane.showMessageDialog(null, "Erreur lors du chargement du fichier Excel.", "Nouvelle heure", JOptionPane.ERROR_MESSAGE);
			}
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Aucun fichier sélectionné. Opération annulée.", "Nouvelle heure", JOptionPane.INFORMATION_MESSAGE);
		}

		
	}
	//
	// Methode IHM
	//
	public void modifierLot(Lot lot, String typologie, String affaire,
							 int nbPieces, double cadence, int valeurVente,
							 String statut, String statutEchant,
							 String semaine, int priorite,
							 String lotACharge,String emplacment,
							 boolean sousDouane, String dateReception, 
							 String datePaiement, String commentaire)
	{
		int heuresAvant = (int) Math.ceil(lot.getHeures());

		lot.setTypologie    (typologie);
		lot.setAffaire      (affaire);
		lot.setNbPieces     (nbPieces);
		lot.setCadence      (cadence);
		lot.recalculerHeures();
		lot.setValeurVente  (valeurVente);
		lot.setStatut       (statut);
		lot.setStatutEchant (statutEchant);
		lot.setSemaine      (semaine);
		lot.setPriorite     (priorite);
		lot.setLotACharge   (lotACharge);
		lot.setEmplacement  (emplacment);
		lot.setEstSousDouane(sousDouane);
		lot.setDateReception(dateReception);
		lot.setDatePaiement (datePaiement);
		lot.setCommentaire  (commentaire);

		// Ajuster les heures disponibles de la société si le lot est affecté
		int heuresApres = (int) Math.ceil(lot.getHeures());
		int delta = heuresAvant - heuresApres;
		if (delta != 0)
		{
			Societe soc = getSocieteDuLot(lot);
			if (soc != null)
				soc.setTotalHeuresCE(soc.getTotalHeuresCE() + delta);
		}
	}

	public void modifierLotMethodeDistribution(Lot lot, String typologie, String lotACharge)
	{
		lot.setTypologie(typologie != null ? typologie : "");
		lot.setLotACharge(lotACharge != null ? lotACharge : "");
	}

	public void modifierPhase(Lot lot, boolean preTri, boolean surPiste,
						boolean sortieEtiq, boolean tri, boolean finit)
	{
		Phase phase = lot.getPhase();
		phase.setPreTri(preTri);
		phase.setSurPiste(surPiste);
		phase.setSortieEtiq(sortieEtiq);
		phase.setTri(tri);
		phase.setFinit(finit);
	}

	public void marquerLotTermine(Lot lot)
	{
		modifierPhase(lot, true, true, true, true, true);
		lot.getSuivieProd().setNbPieceEtiq(lot.getNbPieces());
		lot.getSuivieProd().setNbPieceRepart(lot.getNbPieces());
	}

	public void modifierSociete(Societe soc, String nom, String ce, int totalHeuresCE,int effectif)
	{
		soc.setNom(nom);
		soc.setCe(ce);
		soc.setTotalHeuresCE(totalHeuresCE);
		soc.setEffectifTotal(effectif);
	}

	public void modifierAce(Ace ace, String nom, int nbPers, int effectif)
	{
		ace.setNom(nom);
		ace.setNbPers(nbPers);
		ace.setEffectifActuel(effectif);
	}

	// ── Recherche ─────────────────────────────────────────────────────────

	public Societe getSocieteDuLot(Lot lot)
	{
		for (Societe s : this.societes)
			for (Lot l : s.getLots())
				if (l == lot) return s;
		return null;
	}

	public Ace getAceDuLot(Lot lot)
	{
		for (Societe s : this.societes)
			for (Ace a : s.getAces())
				for (Lot l : a.getLots())
					if (l == lot) return a;
		return null;
	}

	// ── Affectation ───────────────────────────────────────────────────────

	/**
	 * Affecte un lot à une société et un ACE.
	 * Seules les heures de la SOCIÉTÉ sont vérifiées et décomptées.
	 * L'ACE n'a pas de compteur d'heures (plus de risque de valeur négative).
	 *
	 * @return true si succès, false si la société n'a pas assez d'heures
	 */
	public boolean affecterLot(Lot lot, Societe societe, Ace ace)
	{
		int     heuresLot  = (int) Math.ceil(lot.getHeures());
		Societe ancSociete = getSocieteDuLot(lot);
		Ace     ancAce     = getAceDuLot(lot);

		// Déjà affecté à la même cible → no-op
		if (ancSociete == societe && ancAce == ace) return true;

		// Vérifier les heures disponibles (seulement si changement de société)
		// Si même société, les heures ont déjà été décomptées → OK
		if (ancSociete != societe && societe.getTotalHeuresCE() < heuresLot)
			return false;

		// Retrait de l'ancienne affectation
		if (ancSociete != null)
		{
			if (ancAce != null) ancSociete.enleverLotACE(ancAce, lot);
			ancSociete.enleverLot(lot);
		}

		// Nouvelle affectation
		if (ace != null) societe.ajouterLot(lot, ace);

		return true;
	}

	public void desaffecterLot(Lot lot)
	{
		Societe soc = getSocieteDuLot(lot);
		Ace     ace = getAceDuLot(lot);
		if (soc != null)
		{
			if (ace != null) soc.enleverLotACE(ace, lot);
			soc.enleverLot(lot);
		}
	}

	public void ajouterLot  (Lot lot) { lots.add(lot);    }

	public void ajouterLot(int numCDE, String typologie, String affaire,
					int nbPieces, double cadence, int valeurVente,
					String statut, String statutEchant,
					String semaine, int priorite,
					String lotACharge, String emplacement,
					boolean sousDouane, String dateReception,
					String datePaiement, String commentaire)
	{
		Lot lot = new Lot(numCDE, nbPieces, cadence,
						 nbPieces > 0 && cadence > 0 ? nbPieces / cadence : 0.0,
						 valeurVente, statut, statutEchant);
		lot.setTypologie(typologie != null ? typologie : "");
		lot.setAffaire(affaire != null ? affaire : "");
		lot.setSemaine(semaine != null ? semaine : "");
		lot.setPriorite(priorite);
		lot.setLotACharge(lotACharge != null ? lotACharge : "");
		lot.setEmplacement(emplacement != null ? emplacement : "");
		lot.setEstSousDouane(sousDouane);
		lot.setDateReception(dateReception != null ? dateReception : "");
		lot.setDatePaiement(datePaiement != null ? datePaiement : "");
		lot.setCommentaire(commentaire != null ? commentaire : "");
		lots.add(lot);
	}

	public void supprimerLot(Lot lot) { lots.remove(lot); }

	// ── Fiche de route ────────────────────────────────────────────────────
	public FicheRoute genererFicheRoute(Societe societe)
	{
		for (FicheRoute fr : ficheRoute)
		{
			if (fr.getSociete() == societe) return fr;
		}
		FicheRoute fr2 = new FicheRoute(societe);
		this.ficheRoute.add(fr2);
		return fr2;
	}

	//
	// getters et setters
	//
	public ArrayList<Societe> getSocietes()       { return societes; }
	public ArrayList<Lot>     getLots()           { return lots;     }
	public void setSocietes(ArrayList<Societe> s) { this.societes = s; }
	public void setLots    (ArrayList<Lot>     l) { this.lots     = l; }
}

package app.metier.lot;

import app.metier.ficheroute.Phase;
import app.metier.ficheroute.SuivieProd;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Représente un lot de production issu du fichier export.XLSX.
 *
 * Heures = NbPieces / Cadence (valeur directe du fichier, pas étiq+répart séparés).
 * Pas de champ Societe : l'affectation est gérée exclusivement par Societe.ajouterLot().
 *
 * Identifiant interne : id (UUID généré à la création, sauvegardé en JSON).
 * numCDE reste pour l'affichage mais n'est PAS unique → ne jamais l'utiliser comme clé.
 */
public class Lot
{
	public static final String[] F_CARTON = new String[]{"1/16","1/8","1/4","1/2","box"};

	private static ArrayList<String> tabId;
	// ── Identité ──────────────────────────────────────────────────────────
	private String  id;      // clé technique unique (UUID), persistée en JSON
	private int     numCDE;  // numéro affiché à l'utilisateur — pas unique
	private String  typologie;
	private String  affaire;
	private int     nbPieces;
	private double  cadence;
	private double  heures;
	private double  heuresAce; // les tempt que ça prend (heure / nbpersonne)
	private int     valeurVente;
	private double  prixUnitaire;
	private String  semaine;
	private int     priorite;

	// ── Statuts ───────────────────────────────────────────────────────────
	private String  statut;         // statut interne (OU, TC, MC)
	private String  statutEchant;   // Statut échantillonnage (affiché dans l'IHM)

	// ── Informations logistiques ──────────────────────────────────────────
	private String  lotACharge;
	private boolean estSousDouane;
	private String  dateReception, datePaiement, commentaire, emplacement;

	// -- fiche de route --
	private SuivieProd suivieProd;
	private Phase      phase;
	private Methode     methode; // une class pour stoker le lien des méthodes
	private int    nbPalettes,nbColisPrevue, nbColisRecup, collisage;
	private String  distribution, poucentrecupCartonFour;
	private String formatCarton, dateDebut, finTheorique;
	private boolean estMachine;  // ← Ajouté : indique si le lot est produit à la machine

	public Lot(int numCDE, int nbPieces, double cadence, double heures,
			   int valeurVente, String statut, String statutEchant)
	{
		String uuid = UUID.randomUUID().toString();
		this.id          = verifUUID(uuid); // probas est trop faible pour avoir des collisions
		this.numCDE      = numCDE;
		this.nbPieces    = nbPieces;
		this.cadence     = cadence;
		this.heures      = heures;
		this.valeurVente = valeurVente;
		this.prixUnitaire = calculerPU();
		this.statut      = statut      != null ? statut      : "";
		this.statutEchant = statutEchant != null ? statutEchant : "";
		this.typologie    = "";
		this.affaire      = "";
		this.semaine      = "";
		this.priorite     = 0;
		this.lotACharge   = "";
		this.estSousDouane= false;
		this.dateReception= "";
		this.datePaiement = "";
		this.commentaire  = "";
		this.emplacement  = "";
		this.suivieProd   = new SuivieProd();
		this.suivieProd.setLot(this);
		this.phase        = new Phase();
		this.estMachine   = false;  // Par défaut, lot à la main
	}

	private String verifUUID(String uuid)
	{
		if (tabId == null) tabId = new ArrayList<>();
		for (String s : tabId)
		{
			if (uuid.equals(s))
			{
				String uuid2 = UUID.randomUUID().toString();
				return verifUUID(uuid2);
			}
		}
		return uuid;
	}

	// ── Recalcul ───────────────────────────────────────────────
	/** Recalcule les heures après modification de nbPieces ou cadence. */
	public void recalculerHeures()
	{
		this.heures = (this.cadence > 0) ? this.nbPieces / this.cadence : 0.0;
	}

	private double calculerPU()
	{
		double pu = 0.0;
		if (this.nbPieces > 0)
		{
			pu = Math.round(((double) this.valeurVente / this.nbPieces)*100.0) / 100.0;
		}
		return pu;
	}

	public void recalculNbPalette()
	{
		if (collisage <= 0 || formatCarton == null) return;

		int nbcarton = (int) Math.ceil(this.nbPieces / (double) this.collisage);
		this.nbColisPrevue = nbcarton;
		switch (formatCarton)
		{
			// 64 carton par palette
			case "1/16":
			{
				this.nbPalettes = (int) Math.ceil(nbcarton / 64);
				break;
			}
			// 32 par palette
			case "1/8":
			{
				this.nbPalettes = (int) Math.ceil(nbcarton / 32);
				break;
			}
			// 16 par palette
			case "1/4":
			{
				this.nbPalettes = (int) Math.ceil(nbcarton / 16);
				break;
			}
			// 8 par palette
			case "1/2":
			{
				this.nbPalettes = (int) Math.ceil(nbcarton / 8);
				break;
			}
			// 1 par palette
			case "box":
			{
				this.nbPalettes = (int) Math.ceil(nbcarton);
				break;
			}
				
			default:
				throw new AssertionError();
		}

	}

	// ── Getters ───────────────────────────────────────────────────────────
	public String  getId()             { return id;            }
	public int     getNumCDE()         { return numCDE;        }
	public String  getTypologie()      { return typologie;     }
	public String  getAffaire()        { return affaire;       }
	public int     getNbPieces()       { return nbPieces;      }
	public double  getCadence()        { return cadence;       }
	public double  getHeures()         { return heures;        }
	public int     getValeurVente()    { return valeurVente;   }
	public double  getPrixUnitaire()   { return prixUnitaire;  }
	public String  getSemaine()        { return semaine;       }
	public int     getPriorite()       { return priorite;      }
	public String  getStatut()         { return statut;        }
	public String  getStatutEchant()   { return statutEchant;  }
	public String  getLotACharge()     { return lotACharge;    }
	public boolean isEstSousDouane()   { return estSousDouane; }
	public String  getDateReception()  { return dateReception; }
	public String  getDatePaiement()   { return datePaiement;  }
	public String  getCommentaire()    { return commentaire;   }
	public String  getEmplacement()    { return emplacement;   }
	// fiche de route
	public SuivieProd getSuivieProd   () { return suivieProd; }
	public Phase      getPhase        () { return phase;      }
	public Methode    getMethode      () {return methode;     }
	public String     getDistribution () {return distribution;}
	public String     getFormatCarton () {return formatCarton;}
	public double     getHeuresAce    () {return heuresAce;   }
	public int        getNbPalettes   () {return nbPalettes;}
	public int        getNbColisPrevue() {return nbColisPrevue;}
	public int        getNbColisRecup () {return nbColisRecup;}
	public int        getCollisage    () {return collisage;}
	public String     getPoucentrecupCartonFour() {return poucentrecupCartonFour;}
	public String     getDateDebut    () {return dateDebut;}
	public String     getFinTheorique () {return finTheorique;}
	public boolean    estMachine      () {return estMachine;  }

	// ── Setters ───────────────────────────────────────────────────────────
	/** Utilisé UNIQUEMENT au chargement JSON pour restaurer l'UUID persisté. */
	public void setId(String v)            { this.id           = v; }
	public void setNumCDE(int v)           { this.numCDE       = v; }
	public void setTypologie(String v)     { this.typologie    = v; }
	public void setAffaire(String v)       { this.affaire      = v; }
	public void setNbPieces(int v)         { this.nbPieces     = v; this.recalculerHeures();}
	public void setCadence(double v)       { this.cadence      = v; }
	public void setHeures(double v)        { this.heures       = v; }
	public void setValeurVente(int v)      { this.valeurVente  = v; this.prixUnitaire = calculerPU();}
	public void setPrixUnitaire(double v)  { this.prixUnitaire = v; }
	public void setSemaine(String v)       { this.semaine      = v; }
	public void setPriorite(int v)         { this.priorite     = v; }
	public void setStatut(String v)        { this.statut       = v; }
	public void setStatutEchant(String v)  { this.statutEchant = v; }
	public void setLotACharge(String v)    { this.lotACharge   = v; }
	public void setEstSousDouane(boolean v){ this.estSousDouane= v; }
	public void setDateReception(String v) { this.dateReception= v; }
	public void setDatePaiement(String v)  { this.datePaiement = v; }
	public void setCommentaire(String v)   { this.commentaire  = v; }
	public void setEmplacement(String v)   { this.emplacement  = v; }
	// fiche de route
	public void setSuivieProd(SuivieProd v) { this.suivieProd = v; this.suivieProd.setLot(this);}
	public void setPhase(Phase v)           { this.phase      = v; }
	public void setMethode(String methode)  {this.methode = Methode.getMetode(methode);}
	public void setDistribution(String v)   {this.distribution = v; }
	public void setFormatCarton(String v)   {this.formatCarton = v;recalculNbPalette();}
	public void setHeuresAce(double v)      {this.heuresAce = v;       }
		public void setNbPalettes(int v)    {this.nbPalettes = v;}
	public void setNbColisPrevue(int v)     {this.nbColisPrevue = v;}
	public void setNbColisRecup(int v)      {this.nbColisRecup = v;}
	public void setCollisage(int v)         {this.collisage = v;recalculNbPalette();}
	public void setPoucentrecupCartonFour(String v) {this.poucentrecupCartonFour = v;}
	public void setDateDebut(String v)      {this.dateDebut = v;}
	public void setFinTheorique(String v)   {this.finTheorique = v;}
	public void setEstMachine(boolean v)    {this.estMachine = v; }
}
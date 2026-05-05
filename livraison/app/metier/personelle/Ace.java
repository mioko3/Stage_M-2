package app.metier.personelle;

import app.metier.lot.Lot;
import java.util.ArrayList;

/**
 * Chef d'ACE (Atelier de Conditionnement et d'Expédition).
 *
 * IMPORTANT : les heures de l'ACE ne sont PAS décomptées lors d'une affectation.
 *             Seules les heures de la SOCIÉTÉ sont décomptées (totalHeuresCE).
 *             L'ACE sert uniquement à identifier le responsable d'un lot.
 */
public class Ace
{
	private String         nom;
	private int            nbPers;
	private int            totalHeures;      // ← Ajouté : heures théoriques de l'ACE
	private int            effectifActuel;
	private boolean        hasMachine;      // ← Ajouté : indique si l'ACE a une machine
	private ArrayList<Lot> lots;

	public Ace(String nom, int nbPers, int effectifActuel)
	{
		this.nom            = nom;
		this.nbPers         = nbPers;
		this.totalHeures    = 0;  // Par défaut, sera mis à jour si nécessaire
		this.effectifActuel = effectifActuel;
		this.hasMachine     = false;  // Par défaut, pas de machine
		this.lots           = new ArrayList<>();
	}

	// ← Constructeur alternatif avec totalHeures
	public Ace(String nom, int nbPers, int totalHeures, int effectifActuel)
	{
		this.nom            = nom;
		this.nbPers         = nbPers;
		this.totalHeures    = totalHeures;
		this.effectifActuel = effectifActuel;
		this.hasMachine     = false;  // Par défaut, pas de machine
		this.lots           = new ArrayList<>();
	}


	/**
	 * Associe un lot à un ACE (sans toucher aux heures).
	 * Pré-condition : le lot doit déjà être affecté à cette société.
	 */
	public void donnerLotACE(Lot lot)
	{
		if (!this.lots.contains(lot))
		{
			this.lots.add(lot);
			lot.setHeuresAce(lot.getHeures() / this.effectifActuel);
		}
	}

	/** Dissocie un lot d'un ACE (sans toucher aux heures). */
	public void enleverLotACE(Lot lot)
	{
		this.lots.remove(lot);
	}

	/**
	 * Retourne les lots produits à la MACHINE pour cet ACE.
	 * Ne s'applique que si l'ACE a une machine.
	 */
	public ArrayList<Lot> getLotsJMachine()
	{
		ArrayList<Lot> lotsJMachine = new ArrayList<>();
		if (this.hasMachine)
		{
			for (Lot lot : this.lots)
			{
				if (lot.isEstMachine())
					lotsJMachine.add(lot);
			}
		}
		return lotsJMachine;
	}

	/**
	 * Retourne les lots produits À LA MAIN pour cet ACE.
	 */
	public ArrayList<Lot> getLotsJMain()
	{
		ArrayList<Lot> lotsJMain = new ArrayList<>();
		for (Lot lot : this.lots)
		{
			if (!lot.isEstMachine())
				lotsJMain.add(lot);
		}
		return lotsJMain;
	}

	public String         getNom()            { return nom;            }
	public int            getNbPers()         { return nbPers;         }
	public int            getTotalHeures()    { return totalHeures;    }
	public int            getEffectifActuel() { return effectifActuel; }
	public boolean        hasMachine()        { return hasMachine;     }
	public boolean        isHasMachine()      { return hasMachine;     }
	public ArrayList<Lot> getLots()           { return lots;           }

	public void setNom(String v)              { this.nom            = v; }
	public void setNbPers(int v)              { this.nbPers         = v; }
	public void setTotalHeures(int v)         { this.totalHeures    = v; }
	public void setHasMachine(boolean v)      { this.hasMachine     = v; }
	public void setEffectifActuel(int v) 
	{ 
		this.effectifActuel = v; 
		for (Lot lot : this.lots)
		{
			lot.setHeuresAce(lot.getHeures() / this.effectifActuel);
		}
	}
}

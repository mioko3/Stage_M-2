package app.metier.ficheroute;

import app.metier.lot.Lot;
import app.metier.personelle.Societe;

public class FicheRoute
{
	// les lien
	private Societe societe;

	// resultat de la fiche de route globale
	private int sommeVVS;
	private int sommePieces;
	private double prixUntaireMoy;
	private int effectif;

	// resultat de la fiche de route par ACE
	private int[] sommeVVSAce;
	private int[] sommePiecesAce;
	private double[] prixUntaireMoyAce;
	private int[] effectifAce;
	private int[] nbPalettesAce;

	// referenciel
	private int objProd;
	private int objVVS;


	public FicheRoute(Societe societe)
	{
		this.societe = societe;
		this.sommeVVS = 0;
		this.sommePieces = 0;
		this.prixUntaireMoy = 0;
		this.effectif = 0;

		// Initialisation vide - sera redimensionné lors de setSociete
		this.sommeVVSAce = new int[0];
		this.sommePiecesAce = new int[0];
		this.prixUntaireMoyAce = new double[0];
		this.effectifAce = new int[0];
		this.nbPalettesAce = new int[0];

		calculSommeVVS();
		caculSommePieces();
		if (!societe.getLots().isEmpty()) calculPrixUnit();
	}

	// calcule
	public int calculSommeVVS()
	{
		for(Lot l : this.societe.getLots())
		{
			this.sommeVVS += l.getValeurVente();
		}
		return sommeVVS;
	}
	public int caculSommePieces()
	{
		for(Lot l : this.societe.getLots())
		{
			this.sommePieces += l.getNbPieces();
		}
		return sommePieces;
	}
	public double calculPrixUnit()
	{
		int cpt = 0;
		double moyPrixUnit = 0.0;
		for (Lot l : this.societe.getLots())
		{
			if (l.getNbPieces() == 0) continue;
			moyPrixUnit += l.getPrixUnitaire();
			cpt++;
		}
		return this.prixUntaireMoy = (cpt == 0) ? 0 : moyPrixUnit / cpt;
	}

	//
	// getters et setters 
	//
	public Societe getSociete() {return societe;}
	public int getSommeVVS() {return sommeVVS;}
	public int getSommePieces() {return sommePieces;}
	public double getPrixUntaireMoy() {return prixUntaireMoy;}
	public int getEffectif() {return effectif;}

	public void setSociete(Societe societe)
	{
		this.societe = societe;
		if (societe != null && societe.getAces() != null)
		{
			int nbAce = societe.getAces().size();
			this.sommeVVSAce = new int[nbAce];
			this.sommePiecesAce = new int[nbAce];
			this.prixUntaireMoyAce = new double[nbAce];
			this.effectifAce = new int[nbAce];
			this.nbPalettesAce = new int[nbAce];
		}
	}
	public void setSommeVVS(int sommeVVS) {this.sommeVVS = sommeVVS;}
	public void setSommePieces(int sommePieces) {this.sommePieces = sommePieces;}
	public void setPrixUntaireMoy(double prixUntaireMoy) {this.prixUntaireMoy = prixUntaireMoy;}
	public void setEffectif(int effectif) {this.effectif = effectif;}

}

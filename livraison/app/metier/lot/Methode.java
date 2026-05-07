package app.metier.lot;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Methode {

	private static final String LIEN_DOSIER = "app/data/pastouche/methodes/";

	private static final Map<String, Methode> CACHE = new HashMap<>();

	private String nom;
	private String lien;

	private Methode(String nom)
	{
		this.nom = nom;
		this.lien = LIEN_DOSIER + nom;
	}

	public static Methode getMetode(String nom)
	{
		if (!verifExist(nom))
		{
			return null;
		}

		return CACHE.computeIfAbsent(nom, Methode::new);
	}

	private static boolean verifExist(String nom)
	{
		File fichier = new File(LIEN_DOSIER, nom);

		if (fichier.exists() && fichier.isFile())
		{
			System.out.println("Le fichier existe !");
			return true;
		}
		else
		{
			System.out.println("Le fichier n'existe pas.");
			return false;
		}
	}

	public String getNom() { return nom; }
	public String getLien() { return lien; }
}
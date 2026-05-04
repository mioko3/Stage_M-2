package pasUtiliser;

/**
 * Heures allouées à un lot pour l'étiquetage et le répartiment.
 * CORRECTIF : les setters étaient nommés "getCadenceReel(int)" etc. → renommés setCadenceReel(int).
 */
public class HeureAlloue
{
	private int cadenceEtiqueReel;
	private int etiquetage;
	private int cadenceParts;
	private int repart;
	private int totalHeuresAlloues;

	public HeureAlloue(int total)
	{
		this.cadenceEtiqueReel = 0;
		this.etiquetage        = 0;
		this.cadenceParts      = 0;
		this.repart            = 0;
		this.totalHeuresAlloues = total;
	}

	// ─── Getters ──────────────────────────────────────────────────────────
	public int getCadenceReel  () { return this.cadenceEtiqueReel;  }
	public int getEtiquetage   () { return this.etiquetage;         }
	public int getCadencePart  () { return this.cadenceParts;       }
	public int getRepart       () { return this.repart;             }
	public int getTotalHeures  () { return this.totalHeuresAlloues; }

	// ─── Setters ──────────────────────────────────────────────────────────
	public void setCadenceReel (int cr)         { this.cadenceEtiqueReel  = cr;         }
	public void setEtiquetage  (int etiquetage) { this.etiquetage         = etiquetage; }
	public void setCadencePart (int cp)         { this.cadenceParts       = cp;         }
	public void setRepart      (int repart)     { this.repart             = repart;     }
	public void setTotalHeures (int totalHeures){ this.totalHeuresAlloues = totalHeures;}
}

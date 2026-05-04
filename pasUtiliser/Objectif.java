package pasUtiliser;

public class Objectif
{
	private int cadenceTraitement;
	private int repart;

	public Objectif(int cadenceTraitement, int repart)
	{
		this.cadenceTraitement = cadenceTraitement;
		this.repart            = repart;
	}

	public int getCadenceTraitement() { return this.cadenceTraitement; }
	public int getRepart()            { return this.repart;            }

	public void setCadenceTraitement(int ce)   { this.cadenceTraitement = ce;    }
	public void setRepart(int repart)          { this.repart            = repart; }
}

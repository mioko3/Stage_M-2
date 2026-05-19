public class Launcher
{
	public static void main(String[] args)
	{
		String cp =
			"Gestion-Lot-NOZ.jar;" +
			"app/jar/poi-bin-5.2.3/*;" +
			"app/jar/poi-bin-5.2.3/lib/*;" +
			"app/jar/poi-bin-5.2.3/ooxml-lib/*";

		System.setProperty("java.class.path", cp);

		app.Controleur.main(args);
	}
}
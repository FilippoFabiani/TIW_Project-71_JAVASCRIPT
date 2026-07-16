package it.polimi.tiw.project.utils;

/**
 * Piccole utility di parsing dei parametri di richiesta, condivise dai controller
 * del responsabile.
 */
public final class ServletUtils {

	private ServletUtils() {
	}

	/** ritorna la stringa senza spazi ai bordi, oppure null se vuota/nulla */
	public static String trimToNull(String s) {
		if (s == null)
			return null;
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	/** interpreta la stringa come intero; ritorna null se assente o non numerica */
	public static Integer parseIntOrNull(String s) {
		if (s == null || s.trim().isEmpty())
			return null;
		try {
			return Integer.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

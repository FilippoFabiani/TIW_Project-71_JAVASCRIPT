package it.polimi.tiw.project.beans;

import java.util.List;
import java.util.Map;

/**
 * Piccoli DTO (record, serializzati con Gson) usati dagli endpoint di lettura
 * della HOME RESPONSABILE (versione JavaScript). Servono a esporre al client solo
 * i campi necessari, senza dipendere direttamente dai bean di dominio.
 */
public final class ManagerDtos {

	private ManagerDtos() {
	}

	/** riga della tendina progetti */
	public record ProjectDTO(String titolo, int durata, String stato) {
	}

	/** opzione generica di una tendina (WP o task): id numerico + etichetta */
	public record OptionDTO(int id, String label) {
	}

	/** persona (collaboratore) mostrata nelle checkbox */
	public record PersonDTO(int id, String nome, String cognome) {
	}

	/** dettaglio del task selezionato nella HOME RESPONSABILE */
	public record TaskDetailDTO(
			List<Integer> months,
			Map<Integer, Integer> plannedHours,
			List<PersonDTO> collaborators,
			List<Integer> assignedIds,
			boolean editable,
			String status) {
	}
}

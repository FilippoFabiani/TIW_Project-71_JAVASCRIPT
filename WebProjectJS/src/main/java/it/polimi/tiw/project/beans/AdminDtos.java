package it.polimi.tiw.project.beans;

import java.util.List;

/**
 * Contenitore dei DTO (Data Transfer Object) usati dall'interfaccia JavaScript
 * dell'AMMINISTRATORE, serializzati/deserializzati con Gson.
 *
 * <p>I nomi dei campi coincidono volutamente con quelli attesi dal client
 * ({@code homeAdmin.js}): Gson usa il nome del campo come chiave JSON.</p>
 *
 * <ul>
 *   <li>{@link ManagerOption} - una voce dell'elenco dei possibili responsabili.</li>
 *   <li>{@link ProjectItem} - una voce dell'elenco dei progetti creati dall'admin.</li>
 *   <li>{@link Structure} - la gerarchia WP/task di un progetto (VERIFICA PROGETTI).</li>
 *   <li>{@link SaveProjectRequest} - il corpo JSON inviato dal client con SALVA.</li>
 * </ul>
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    /* ---------------- risposte (server -> client) ---------------- */

    /** { id, nome, cognome } */
    public static class ManagerOption {
        public int id;
        public String nome;
        public String cognome;

        public ManagerOption(int id, String nome, String cognome) {
            this.id = id;
            this.nome = nome;
            this.cognome = cognome;
        }
    }

    /** { titolo, stato } */
    public static class ProjectItem {
        public String titolo;
        public String stato;

        public ProjectItem(String titolo, String stato) {
            this.titolo = titolo;
            this.stato = stato;
        }
    }

    /** { title, wps:[ Wp ] } */
    public static class Structure {
        public String title;
        public List<Wp> wps;
    }

    /** { label, meseInizio, meseFine, tasks:[ TaskItem ] } */
    public static class Wp {
        public String label;
        public int meseInizio;
        public int meseFine;
        public List<TaskItem> tasks;
    }

    /** { label, meseInizio, meseFine, orePreviste, oreLavorate } */
    public static class TaskItem {
        public String label;
        public int meseInizio;
        public int meseFine;
        public int orePreviste;
        public int oreLavorate;
    }

    /* ---------------- richiesta (client -> server) ---------------- */

    /**
     * Corpo JSON della richiesta SALVA:
     * { action, titolo, durata, responsabile, wps:[ { titolo, meseInizio, meseFine,
     *   tasks:[ { titolo, descrizione, meseInizio, meseFine } ] } ] }
     */
    public static class SaveProjectRequest {
        public String action;
        public String titolo;
        public Integer durata;
        public Integer responsabile;
        public List<WpReq> wps;
    }

    public static class WpReq {
        public String titolo;
        public Integer meseInizio;
        public Integer meseFine;
        public List<TaskReq> tasks;
    }

    public static class TaskReq {
        public String titolo;
        public String descrizione;
        public Integer meseInizio;
        public Integer meseFine;
    }
}

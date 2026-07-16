package it.polimi.tiw.project.exceptions;

public enum ErrorCode {
	// 		Error codes for user-related errors
    INVALID_CREDENTIALS("ERR_001", "Credenziali non valide"),
	
	// 		Error codes for logic errors, throwned by exceptions
    
    /** the userId is not valid after authentication succeded */
	USER_NOT_FOUND("EX_001", "Utente non trovato"),
    DATABASE_CONNECTION_ERROR("EX_002", "Errore di connessione al database"),
    /** some constraint have been violated */
	INCONSISTENT_DB_DATA("EX_003", "Dati incoerenti nel database");



    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
